#!/usr/bin/env python3
"""
FirstStep Live SQLite PC Dashboard & Two-Way Editor (Offline USB ADB)
---------------------------------------------------------------------
Serves tools/Live_DB_Editor.html at http://127.0.0.1:8765, pulls
firststep_offline.db live over USB ADB, and lets you enter/edit/delete
records directly from the browser and push changes back to the phone.
"""

from __future__ import annotations

import http.server
import json
import os
import re
import shutil
import socketserver
import sqlite3
import subprocess
import sys
import tempfile
import threading
import time
import urllib.parse
import webbrowser
from pathlib import Path

PACKAGE_ID = "com.aistudio.firststep.oran26"
MAIN_ACTIVITY = "com.example.MainActivity"
DB_REMOTE_PATH = "databases/firststep_offline.db"
HOST = "127.0.0.1"
PORT = 8765
PROJECT_ROOT = Path(__file__).resolve().parent.parent
HTML_FILE = Path(__file__).resolve().parent / "Live_DB_Editor.html"


def find_adb_path() -> str | None:
    env_adb = shutil.which("adb")
    if env_adb:
        return env_adb

    candidates: list[Path] = []
    local_props = PROJECT_ROOT / "local.properties"
    if local_props.exists():
        for line in local_props.read_text(encoding="utf-8", errors="ignore").splitlines():
            if line.strip().startswith("sdk.dir="):
                raw_sdk = line.split("=", 1)[1].strip()
                raw_sdk = raw_sdk.replace("\\\\", "\\").replace("\\:", ":")
                candidates.append(Path(raw_sdk) / "platform-tools" / "adb.exe")
                candidates.append(Path(raw_sdk) / "platform-tools" / "adb")

    for env_var in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        val = os.environ.get(env_var)
        if val:
            candidates.append(Path(val) / "platform-tools" / "adb.exe")
            candidates.append(Path(val) / "platform-tools" / "adb")

    local_app_data = os.environ.get("LOCALAPPDATA")
    if local_app_data:
        candidates.append(Path(local_app_data) / "Android" / "Sdk" / "platform-tools" / "adb.exe")

    user_home = Path.home()
    candidates.extend([
        user_home / "AppData" / "Local" / "Android" / "Sdk" / "platform-tools" / "adb.exe",
        user_home / "Android" / "Sdk" / "platform-tools" / "adb",
        Path("/usr/bin/adb"),
    ])

    for path in candidates:
        if path.exists():
            return str(path)
    return None


class LiveDbBridge:
    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._tmp_dir = Path(tempfile.mkdtemp(prefix="firststep_live_db_"))
        self._db_file = self._tmp_dir / "firststep_offline.db"
        self.adb_path = find_adb_path()
        self.last_sync_at: str | None = None
        self.last_error: str | None = None
        self.connected_device: str = "Checking..."

    def pull_latest_db(self) -> tuple[bool, str | None]:
        if not self.adb_path or not Path(self.adb_path).exists():
            self.adb_path = find_adb_path()
        if not self.adb_path:
            return False, "adb.exe not found. Make sure Android SDK Platform-Tools is installed."

        try:
            dev_res = subprocess.run(
                [self.adb_path, "devices"],
                capture_output=True,
                text=True,
                timeout=4,
            )
            lines = [
                ln.strip()
                for ln in dev_res.stdout.splitlines()[1:]
                if ln.strip() and "\tdevice" in ln
            ]
            if not lines:
                self.connected_device = "No phone detected"
                return False, "No authorized USB Android phone found. Plug in your phone and enable USB Debugging."
            self.connected_device = lines[0].split("\t")[0]

            pull_res = subprocess.run(
                [
                    self.adb_path,
                    "exec-out",
                    "run-as",
                    PACKAGE_ID,
                    "cat",
                    DB_REMOTE_PATH,
                ],
                capture_output=True,
                timeout=6,
            )
            data = pull_res.stdout
            if not data.startswith(b"SQLite format 3\x00"):
                err_txt = (data + pull_res.stderr).decode("utf-8", errors="ignore").strip()
                if not err_txt:
                    err_txt = "Database file not created yet. Open the FirstStep app on your phone once."
                return False, err_txt

            staging = self._tmp_dir / "firststep_offline.staging.db"
            staging.write_bytes(data)
            with self._lock:
                os.replace(staging, self._db_file)
                self.last_sync_at = time.strftime("%H:%M:%S")
                self.last_error = None
            return True, None
        except subprocess.TimeoutExpired:
            return False, "ADB timed out communicating with device."
        except Exception as exc:
            return False, str(exc)

    def _push_db_to_phone(self) -> tuple[bool, str | None]:
        if not self.adb_path or not self._db_file.exists():
            return False, "ADB or local database snapshot unavailable."
        tmp_remote = "/data/local/tmp/firststep_push.db"
        try:
            subprocess.run(
                [self.adb_path, "shell", "am", "force-stop", PACKAGE_ID],
                capture_output=True,
                timeout=5,
            )
            push_file = subprocess.run(
                [self.adb_path, "push", str(self._db_file), tmp_remote],
                capture_output=True,
                timeout=10,
            )
            if push_file.returncode != 0:
                err = (push_file.stderr or push_file.stdout).decode("utf-8", errors="ignore").strip()
                return False, err or "adb push failed."

            subprocess.run(
                [self.adb_path, "shell", "chmod", "644", tmp_remote],
                capture_output=True,
                timeout=5,
            )
            subprocess.run(
                [self.adb_path, "shell", "run-as", PACKAGE_ID, "mkdir", "-p", "databases"],
                capture_output=True,
                timeout=5,
            )
            subprocess.run(
                [
                    self.adb_path,
                    "shell",
                    "run-as",
                    PACKAGE_ID,
                    "rm",
                    "-f",
                    f"{DB_REMOTE_PATH}-wal",
                    f"{DB_REMOTE_PATH}-shm",
                    f"{DB_REMOTE_PATH}-journal",
                ],
                capture_output=True,
                timeout=5,
            )
            copy_res = subprocess.run(
                [self.adb_path, "shell", "run-as", PACKAGE_ID, "cp", tmp_remote, DB_REMOTE_PATH],
                capture_output=True,
                timeout=10,
            )
            subprocess.run(
                [self.adb_path, "shell", "rm", "-f", tmp_remote],
                capture_output=True,
                timeout=5,
            )
            if copy_res.returncode != 0:
                err = (copy_res.stderr or copy_res.stdout).decode("utf-8", errors="ignore").strip()
                return False, err or "Failed copying DB into app sandbox."

            subprocess.run(
                [self.adb_path, "shell", "am", "start", "-n", f"{PACKAGE_ID}/{MAIN_ACTIVITY}"],
                capture_output=True,
                timeout=5,
            )
            self.last_sync_at = time.strftime("%H:%M:%S")
            return True, None
        except Exception as exc:
            return False, str(exc)

    def restart_phone_app(self) -> tuple[bool, str | None]:
        if not self.adb_path:
            return False, "ADB not found."
        try:
            subprocess.run([self.adb_path, "shell", "am", "force-stop", PACKAGE_ID], capture_output=True, timeout=5)
            subprocess.run(
                [self.adb_path, "shell", "am", "start", "-n", f"{PACKAGE_ID}/{MAIN_ACTIVITY}"],
                capture_output=True,
                timeout=5,
            )
            return True, None
        except Exception as exc:
            return False, str(exc)

    def mutate(self, fn) -> dict:
        ok, err = self.pull_latest_db()
        if not ok and not self._db_file.exists():
            return {"ok": False, "error": err or "Phone DB not pulled yet."}

        with self._lock:
            conn = sqlite3.connect(self._db_file)
            conn.row_factory = sqlite3.Row
            conn.execute("PRAGMA foreign_keys = ON")
            try:
                result = fn(conn)
                conn.commit()
            except Exception as exc:
                conn.rollback()
                return {"ok": False, "error": str(exc)}
            finally:
                conn.close()

            pushed, push_err = self._push_db_to_phone()
            if not pushed:
                return {"ok": False, "error": f"Updated locally, but failed pushing to phone: {push_err}"}
            return {"ok": True, **(result if isinstance(result, dict) else {"message": str(result)})}

    def read_snapshot(self) -> dict:
        ok, err = self.pull_latest_db()
        if not ok:
            self.last_error = err

        with self._lock:
            if not self._db_file.exists():
                return {
                    "ok": False,
                    "error": self.last_error or "Waiting for first database sync...",
                    "device": self.connected_device,
                    "lastSyncAt": self.last_sync_at,
                    "adbPath": self.adb_path,
                }

            conn = sqlite3.connect(f"file:{self._db_file}?mode=ro", uri=True)
            conn.row_factory = sqlite3.Row
            try:
                cur = conn.cursor()
                tables = [
                    r["name"]
                    for r in cur.execute(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name != 'room_master_table' ORDER BY name"
                    ).fetchall()
                ]
                profile_row = cur.execute("SELECT * FROM student_profiles WHERE id = 1 LIMIT 1").fetchone()
                profile = dict(profile_row) if profile_row else None

                companies = [dict(r) for r in cur.execute("SELECT id, name FROM companies ORDER BY name ASC").fetchall()]
                domains = [dict(r) for r in cur.execute("SELECT id, name_fr, name_ar FROM domains ORDER BY id ASC").fetchall()]
                locations = [dict(r) for r in cur.execute("SELECT id, city_fr, city_ar, region_fr, region_ar FROM locations ORDER BY id ASC").fetchall()]

                fav_cols = {r["name"] for r in cur.execute("PRAGMA table_info(favorites)").fetchall()}
                offer_cols = {r["name"] for r in cur.execute("PRAGMA table_info(offers)").fetchall()}
                has_v2_fav = "offer_title_fr" in fav_cols
                has_v2_offer = "monthly_stipend_dzd" in offer_cols

                if has_v2_fav:
                    favorites = [
                        dict(r)
                        for r in cur.execute(
                            """
                            SELECT f.id AS favorite_id, f.offer_id, f.added_at,
                                   f.offer_title_fr AS title_fr, f.offer_title_ar AS title_ar,
                                   f.company_name, f.city_fr, f.domain_fr,
                                   f.monthly_stipend_dzd, f.start_date, f.deadline, f.personal_note,
                                   COALESCE(o.duration_months, 0) AS duration_months,
                                   COALESCE(o.is_paid, CASE WHEN f.monthly_stipend_dzd > 0 THEN 1 ELSE 0 END) AS is_paid,
                                   a.reference AS application_reference, a.current_status AS application_status
                            FROM favorites f
                            LEFT JOIN offers o ON o.id = f.offer_id
                            LEFT JOIN applications a ON a.offer_id = f.offer_id
                            ORDER BY f.added_at DESC, f.id DESC
                            """
                        ).fetchall()
                    ]
                else:
                    favorites = [
                        dict(r)
                        for r in cur.execute(
                            """
                            SELECT f.id AS favorite_id, f.offer_id, f.added_at,
                                   o.title_fr, o.title_ar, o.duration_months, o.is_paid, o.status, o.deadline,
                                   0 AS monthly_stipend_dzd, '' AS start_date, '' AS personal_note,
                                   c.name AS company_name, d.name_fr AS domain_fr, l.city_fr,
                                   a.reference AS application_reference, a.current_status AS application_status
                            FROM favorites f
                            INNER JOIN offers o ON o.id = f.offer_id
                            INNER JOIN companies c ON c.id = o.company_id
                            INNER JOIN domains d ON d.id = o.domain_id
                            INNER JOIN locations l ON l.id = o.location_id
                            LEFT JOIN applications a ON a.offer_id = o.id
                            ORDER BY f.added_at DESC, f.id DESC
                            """
                        ).fetchall()
                    ]

                history_rows = [
                    dict(r)
                    for r in cur.execute("SELECT * FROM status_history ORDER BY changed_at ASC, id ASC").fetchall()
                ]
                history_by_app: dict[int, list[dict]] = {}
                for h in history_rows:
                    history_by_app.setdefault(int(h["application_id"]), []).append(h)

                today_str = time.strftime("%Y-%m-%d")
                applications = []
                for r in cur.execute(
                    """
                    SELECT a.*,
                           COALESCE(o.title_fr, '') AS offer_title_fr,
                           COALESCE(c.name, '') AS company_name,
                           COALESCE(l.city_fr, '') AS city_fr,
                           CAST(julianday(?) - julianday(substr(a.applied_at, 1, 10)) AS INTEGER) AS days_since_applied
                    FROM applications a
                    LEFT JOIN offers o ON o.id = a.offer_id
                    LEFT JOIN companies c ON c.id = o.company_id
                    LEFT JOIN locations l ON l.id = o.location_id
                    ORDER BY a.applied_at DESC, a.id DESC
                    """,
                    (today_str,),
                ).fetchall():
                    item = dict(r)
                    days = item.get("days_since_applied") or 0
                    item["unanswered_over_7_days"] = item.get("current_status") == "SUBMITTED" and days > 7
                    item["history"] = history_by_app.get(int(item["id"]), [])
                    applications.append(item)

                offers = [
                    dict(r)
                    for r in cur.execute(
                        """
                        SELECT o.*, c.name AS company_name, d.name_fr AS domain_fr, l.city_fr,
                               CASE WHEN f.id IS NOT NULL THEN 1 ELSE 0 END AS is_favorite,
                               a.reference AS application_reference, a.current_status AS application_status
                        FROM offers o
                        INNER JOIN companies c ON c.id = o.company_id
                        INNER JOIN domains d ON d.id = o.domain_id
                        INNER JOIN locations l ON l.id = o.location_id
                        LEFT JOIN favorites f ON f.offer_id = o.id
                        LEFT JOIN applications a ON a.offer_id = o.id
                        ORDER BY o.id ASC
                        """
                    ).fetchall()
                ]
                if not has_v2_offer:
                    for o in offers:
                        o.setdefault("monthly_stipend_dzd", 0)
                        o.setdefault("start_date", "")

                return {
                    "ok": ok,
                    "stale": not ok,
                    "error": self.last_error,
                    "device": self.connected_device,
                    "lastSyncAt": self.last_sync_at,
                    "adbPath": self.adb_path,
                    "tables": tables,
                    "counts": {
                        "offers": len(offers),
                        "favorites": len(favorites),
                        "applications": len(applications),
                        "locations": len(locations),
                        "history": len(history_rows),
                    },
                    "companies": companies,
                    "domains": domains,
                    "locations": locations,
                    "profile": profile,
                    "favorites": favorites,
                    "applications": applications,
                    "offers": offers,
                }
            except Exception as exc:
                return {
                    "ok": False,
                    "error": f"SQLite read error: {exc}",
                    "device": self.connected_device,
                    "lastSyncAt": self.last_sync_at,
                }
            finally:
                conn.close()

    def get_db_bytes(self) -> bytes | None:
        with self._lock:
            if self._db_file.exists():
                return self._db_file.read_bytes()
            return None


BRIDGE = LiveDbBridge()


class DashboardHandler(http.server.BaseHTTPRequestHandler):
    def log_message(self, format: str, *args) -> None:
        return

    def _send_json(self, payload: dict, status: int = 200) -> None:
        raw = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def _read_json_body(self) -> dict:
        length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(length) if length > 0 else b"{}"
        return json.loads(raw.decode("utf-8"))

    def do_GET(self) -> None:
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path == "/":
            body = HTML_FILE.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif parsed.path == "/api/db":
            self._send_json(BRIDGE.read_snapshot())
        elif parsed.path == "/api/download":
            BRIDGE.pull_latest_db()
            db_bytes = BRIDGE.get_db_bytes()
            if db_bytes is None:
                self.send_error(404, "Database not pulled yet")
                return
            self.send_response(200)
            self.send_header("Content-Type", "application/octet-stream")
            self.send_header("Content-Disposition", 'attachment; filename="firststep_offline.db"')
            self.send_header("Content-Length", str(len(db_bytes)))
            self.end_headers()
            self.wfile.write(db_bytes)
        else:
            self.send_error(404)

    def do_POST(self) -> None:
        parsed = urllib.parse.urlparse(self.path)
        try:
            body = self._read_json_body()
        except Exception as exc:
            self._send_json({"ok": False, "error": f"Invalid JSON: {exc}"}, status=400)
            return

        if parsed.path == "/api/app/restart":
            ok, err = BRIDGE.restart_phone_app()
            self._send_json({"ok": ok, "message": "Phone app restarted!" if ok else None, "error": err})

        elif parsed.path == "/api/profile/update":
            def tx(conn: sqlite3.Connection) -> dict:
                now = time.strftime("%Y-%m-%d %H:%M:%S")
                conn.execute(
                    """
                    UPDATE student_profiles
                    SET full_name = ?, email = ?, phone = ?, university = ?, degree_level = ?, bio = ?, updated_at = ?
                    WHERE id = 1
                    """,
                    (
                        body.get("full_name", "").strip(),
                        body.get("email", "").strip(),
                        body.get("phone", "").strip(),
                        body.get("university", "").strip(),
                        body.get("degree_level", "").strip(),
                        body.get("bio", "").strip(),
                        now,
                    ),
                )
                return {"message": "Student profile saved & synced to phone!"}
            self._send_json(BRIDGE.mutate(tx))

        elif parsed.path == "/api/favorites/note":
            def tx(conn: sqlite3.Connection) -> dict:
                conn.execute(
                    "UPDATE favorites SET personal_note = ? WHERE offer_id = ?",
                    (body.get("personalNote", "").strip()[:200], int(body["offerId"])),
                )
                return {"message": "Favorite personal note saved & synced to phone!"}
            self._send_json(BRIDGE.mutate(tx))

        elif parsed.path == "/api/favorites/toggle":
            def tx(conn: sqlite3.Connection) -> dict:
                offer_id = int(body["offerId"])
                existing = conn.execute("SELECT id FROM favorites WHERE offer_id = ?", (offer_id,)).fetchone()
                if existing:
                    conn.execute("DELETE FROM favorites WHERE offer_id = ?", (offer_id,))
                    return {"message": f"Removed offer #{offer_id} from favorites on phone."}
                row = conn.execute(
                    """
                    SELECT o.title_fr, o.title_ar, c.name AS company_name, l.city_fr, d.name_fr AS domain_fr,
                           o.monthly_stipend_dzd, o.start_date, o.deadline
                    FROM offers o
                    INNER JOIN companies c ON c.id = o.company_id
                    INNER JOIN locations l ON l.id = o.location_id
                    INNER JOIN domains d ON d.id = o.domain_id
                    WHERE o.id = ?
                    """,
                    (offer_id,),
                ).fetchone()
                if not row:
                    raise ValueError(f"Offer #{offer_id} not found")
                now = time.strftime("%Y-%m-%d %H:%M:%S")
                conn.execute(
                    """
                    INSERT INTO favorites (offer_id, added_at, offer_title_fr, offer_title_ar, company_name, city_fr, domain_fr, monthly_stipend_dzd, start_date, deadline, personal_note)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '')
                    """,
                    (
                        offer_id,
                        now,
                        row["title_fr"],
                        row["title_ar"],
                        row["company_name"],
                        row["city_fr"],
                        row["domain_fr"],
                        row["monthly_stipend_dzd"],
                        row["start_date"],
                        row["deadline"],
                    ),
                )
                return {"message": f"Added offer #{offer_id} to favorites on phone!"}
            self._send_json(BRIDGE.mutate(tx))

        elif parsed.path == "/api/applications/status":
            def tx(conn: sqlite3.Connection) -> dict:
                app_id = int(body["applicationId"])
                new_status = str(body["newStatus"]).strip()
                note = str(body.get("note", "")).strip() or f"Updated from PC ({new_status})"
                row = conn.execute("SELECT current_status FROM applications WHERE id = ?", (app_id,)).fetchone()
                if not row:
                    raise ValueError(f"Application #{app_id} not found")
                old_status = row["current_status"]
                now = time.strftime("%Y-%m-%d %H:%M:%S")
                conn.execute("UPDATE applications SET current_status = ? WHERE id = ?", (new_status, app_id))
                conn.execute(
                    "INSERT INTO status_history (application_id, old_status, new_status, changed_at, note) VALUES (?, ?, ?, ?, ?)",
                    (app_id, old_status, new_status, now, note),
                )
                return {"message": f"Application #{app_id} moved {old_status} → {new_status} & pushed to phone!"}
            self._send_json(BRIDGE.mutate(tx))

        elif parsed.path == "/api/offers/save":
            def tx(conn: sqlite3.Connection) -> dict:
                offer_id = body.get("id")
                title_fr = str(body["title_fr"]).strip()
                title_ar = str(body.get("title_ar") or title_fr).strip()
                desc_fr = str(body["description_fr"]).strip()
                desc_ar = str(body.get("description_ar") or desc_fr).strip()
                company_id = int(body["company_id"])
                domain_id = int(body["domain_id"])
                location_id = int(body["location_id"])
                duration_months = max(1, min(24, int(body.get("duration_months", 3))))
                is_paid = 1 if body.get("is_paid", True) else 0
                stipend = int(body.get("monthly_stipend_dzd", 0)) if is_paid else 0
                start_date = str(body.get("start_date", "2026-12-01")).strip()
                deadline = str(body.get("deadline", "2027-01-31")).strip()
                status = str(body.get("status", "OPEN")).strip()
                now = time.strftime("%Y-%m-%d %H:%M:%S")

                if offer_id:
                    conn.execute(
                        """
                        UPDATE offers
                        SET company_id=?, domain_id=?, location_id=?, title_fr=?, title_ar=?, description_fr=?, description_ar=?,
                            duration_months=?, is_paid=?, monthly_stipend_dzd=?, start_date=?, status=?, deadline=?
                        WHERE id=?
                        """,
                        (company_id, domain_id, location_id, title_fr, title_ar, desc_fr, desc_ar,
                         duration_months, is_paid, stipend, start_date, status, deadline, int(offer_id)),
                    )
                    return {"message": f"Updated Offer #{offer_id} & pushed to phone!"}
                else:
                    cur = conn.execute(
                        """
                        INSERT INTO offers (company_id, domain_id, location_id, title_fr, title_ar, description_fr, description_ar,
                                            duration_months, is_paid, monthly_stipend_dzd, start_date, status, deadline, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        (company_id, domain_id, location_id, title_fr, title_ar, desc_fr, desc_ar,
                         duration_months, is_paid, stipend, start_date, status, deadline, now),
                    )
                    return {"message": f"Created Offer #{cur.lastrowid} & pushed to phone!"}
            self._send_json(BRIDGE.mutate(tx))

        elif parsed.path == "/api/offers/delete":
            def tx(conn: sqlite3.Connection) -> dict:
                offer_id = int(body["offerId"])
                conn.execute("DELETE FROM offers WHERE id = ?", (offer_id,))
                return {"message": f"Deleted Offer #{offer_id} & pushed to phone!"}
            self._send_json(BRIDGE.mutate(tx))

        elif parsed.path == "/api/sql":
            sql_text = str(body.get("sql", "")).strip()
            if not sql_text:
                self._send_json({"ok": False, "error": "Empty SQL statement."}, status=400)
                return
            is_read = bool(re.match(r"^(SELECT|PRAGMA|EXPLAIN)\b", sql_text, flags=re.IGNORECASE))
            if is_read:
                BRIDGE.pull_latest_db()
                with BRIDGE._lock:
                    if not BRIDGE._db_file.exists():
                        self._send_json({"ok": False, "error": "No DB snapshot yet."})
                        return
                    conn = sqlite3.connect(f"file:{BRIDGE._db_file}?mode=ro", uri=True)
                    conn.row_factory = sqlite3.Row
                    try:
                        qcur = conn.execute(sql_text)
                        cols = [d[0] for d in qcur.description] if qcur.description else []
                        rows = [dict(r) for r in qcur.fetchall()[:200]]
                        self._send_json({"ok": True, "message": "Query completed", "result": {"columns": cols, "rows": rows, "error": None}})
                    except Exception as exc:
                        self._send_json({"ok": False, "error": str(exc)})
                    finally:
                        conn.close()
            else:
                def tx(conn: sqlite3.Connection) -> dict:
                    qcur = conn.execute(sql_text)
                    return {"message": f"SQL executed ({qcur.rowcount} rows) & pushed to phone!", "result": {"affectedRows": qcur.rowcount}}
                self._send_json(BRIDGE.mutate(tx))
        else:
            self._send_json({"ok": False, "error": "Unknown endpoint"}, status=404)


class ReusableTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    allow_reuse_address = True


def main() -> None:
    url = f"http://{HOST}:{PORT}"
    print(f"FirstStep Live SQLite Editor & Monitor starting at {url}")
    print(f"Serving HTML UI from: {HTML_FILE}")
    print(f"ADB detected: {BRIDGE.adb_path or 'NOT FOUND'}")
    print("Press Ctrl+C to stop.")
    threading.Timer(0.8, lambda: webbrowser.open(url)).start()
    with ReusableTCPServer((HOST, PORT), DashboardHandler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nStopped.")
            sys.exit(0)


if __name__ == "__main__":
    main()
