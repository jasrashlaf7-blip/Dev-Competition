#!/usr/bin/env python3
"""
FirstStep Live SQLite PC Dashboard (Offline USB ADB Monitor)
------------------------------------------------------------
Streams the live SQLite database (firststep_offline.db) from a connected
Android debug device over USB (no INTERNET permission needed in the app)
and serves a real-time auto-updating dashboard at http://127.0.0.1:8765.
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
DB_REMOTE_PATH = "databases/firststep_offline.db"
HOST = "127.0.0.1"
PORT = 8765
PROJECT_ROOT = Path(__file__).resolve().parent.parent


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
            return False, "adb.exe not found. Make sure Android SDK Platform-Tools is installed or set local.properties."

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

    def read_snapshot(self, custom_sql: str | None = None) -> dict:
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

                fav_cols = {
                    r["name"] for r in cur.execute("PRAGMA table_info(favorites)").fetchall()
                }
                offer_cols = {
                    r["name"] for r in cur.execute("PRAGMA table_info(offers)").fetchall()
                }
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
                                   c.name AS company_name,
                                   d.name_fr AS domain_fr, d.name_ar AS domain_ar,
                                   l.city_fr, l.city_ar,
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
                    for r in cur.execute(
                        "SELECT * FROM status_history ORDER BY changed_at ASC, id ASC"
                    ).fetchall()
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
                           COALESCE(o.title_ar, '') AS offer_title_ar,
                           COALESCE(c.name, '') AS company_name,
                           COALESCE(l.city_fr, '') AS city_fr,
                           COALESCE(d.name_fr, '') AS domain_fr,
                           CAST(julianday(?) - julianday(substr(a.applied_at, 1, 10)) AS INTEGER) AS days_since_applied
                    FROM applications a
                    LEFT JOIN offers o ON o.id = a.offer_id
                    LEFT JOIN companies c ON c.id = o.company_id
                    LEFT JOIN locations l ON l.id = o.location_id
                    LEFT JOIN domains d ON d.id = o.domain_id
                    ORDER BY a.applied_at DESC, a.id DESC
                    """,
                    (today_str,),
                ).fetchall():
                    item = dict(r)
                    days = item.get("days_since_applied") or 0
                    item["unanswered_over_7_days"] = (
                        item.get("current_status") == "SUBMITTED" and days > 7
                    )
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

                custom_result = None
                if custom_sql and custom_sql.strip():
                    cleaned = custom_sql.strip().rstrip(";")
                    if re.match(r"^(SELECT|PRAGMA|EXPLAIN)\b", cleaned, flags=re.IGNORECASE):
                        qcur = cur.execute(cleaned)
                        cols = [d[0] for d in qcur.description] if qcur.description else []
                        rows = [dict(row) for row in qcur.fetchall()[:200]]
                        custom_result = {"columns": cols, "rows": rows, "error": None}
                    else:
                        custom_result = {
                            "columns": [],
                            "rows": [],
                            "error": "Read-only monitor: only SELECT / PRAGMA queries are allowed here.",
                        }

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
                        "history": len(history_rows),
                    },
                    "profile": profile,
                    "favorites": favorites,
                    "applications": applications,
                    "offers": offers,
                    "customQuery": custom_result,
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

HTML_PAGE = """<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>FirstStep — Live SQLite Monitor (Phone USB)</title>
  <style>
    :root {
      --primary: #1565C0;
      --secondary: #00897B;
      --bg: #F8FAFC;
      --surface: #FFFFFF;
      --text: #0F172A;
      --muted: #64748B;
      --border: #E2E8F0;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      background: var(--bg);
      color: var(--text);
    }
    header {
      background: linear-gradient(135deg, #132248, #1565C0);
      color: #fff;
      padding: 18px 24px;
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      box-shadow: 0 2px 10px rgba(0,0,0,0.12);
    }
    header h1 { margin: 0; font-size: 20px; }
    header p { margin: 4px 0 0; font-size: 13px; opacity: 0.85; }
    .status-bar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    .pill {
      padding: 6px 12px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 600;
      background: rgba(255,255,255,0.16);
    }
    .pill.live { background: #16A34A; }
    .pill.warn { background: #D97706; }
    .btn {
      border: none;
      border-radius: 8px;
      padding: 8px 14px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      background: #fff;
      color: #132248;
      text-decoration: none;
    }
    .btn.secondary { background: var(--secondary); color: #fff; }
    main { max-width: 1280px; margin: 20px auto; padding: 0 18px 40px; }
    .kpis {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 14px;
      margin-bottom: 18px;
    }
    .kpi {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 12px;
      padding: 14px 16px;
    }
    .kpi-label { font-size: 12px; color: var(--muted); font-weight: 600; text-transform: uppercase; }
    .kpi-val { font-size: 26px; font-weight: 800; color: var(--primary); margin-top: 4px; }
    .error-banner {
      background: #FEF3C7;
      border: 1px solid #F59E0B;
      color: #92400E;
      padding: 10px 14px;
      border-radius: 10px;
      margin-bottom: 14px;
      font-size: 13px;
      display: none;
    }
    .tabs { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 14px; }
    .tab-btn {
      border: 1px solid var(--border);
      background: var(--surface);
      color: var(--text);
      padding: 8px 14px;
      border-radius: 999px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
    }
    .tab-btn.active { background: var(--primary); color: #fff; border-color: var(--primary); }
    .panel {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 14px;
      padding: 16px;
      overflow-x: auto;
    }
    table { width: 100%; border-collapse: collapse; font-size: 13px; }
    th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid var(--border); vertical-align: top; }
    th { font-size: 11px; text-transform: uppercase; color: var(--muted); background: #F1F5F9; }
    .badge {
      display: inline-block;
      padding: 3px 9px;
      border-radius: 999px;
      font-size: 11px;
      font-weight: 700;
      color: #fff;
      background: var(--primary);
    }
    .badge.SUBMITTED { background: #1976D2; }
    .badge.UNDER_REVIEW { background: #F57C00; }
    .badge.INTERVIEW_SCHEDULED { background: #7B1FA2; }
    .badge.ACCEPTED { background: #2E7D32; }
    .badge.REJECTED { background: #C62828; }
    .badge.CANCELLED, .badge.CLOSED { background: #616161; }
    .badge.OPEN, .badge.PAID { background: #00897B; }
    .history-list { margin: 6px 0 0; padding-left: 16px; font-size: 12px; color: var(--muted); }
    .sql-bar { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
    .sql-input {
      flex: 1;
      min-width: 260px;
      padding: 9px 12px;
      border: 1px solid var(--border);
      border-radius: 8px;
      font-family: Consolas, monospace;
      font-size: 13px;
    }
    .empty { padding: 28px; text-align: center; color: var(--muted); font-size: 14px; }
  </style>
</head>
<body>
  <header>
    <div>
      <h1>FirstStep — Live SQLite Monitor (USB)</h1>
      <p>Reads <code>firststep_offline.db</code> live from your phone over USB (100% offline-first)</p>
    </div>
    <div class="status-bar">
      <span id="statusPill" class="pill warn">Connecting...</span>
      <span id="devicePill" class="pill">Device: —</span>
      <span id="syncPill" class="pill">Last sync: —</span>
      <a class="btn" href="/api/download">Download .db</a>
    </div>
  </header>

  <main>
    <div id="errorBanner" class="error-banner"></div>

    <div class="kpis">
      <div class="kpi"><div class="kpi-label">Offers</div><div class="kpi-val" id="kpiOffers">—</div></div>
      <div class="kpi"><div class="kpi-label">Favorites</div><div class="kpi-val" id="kpiFavorites">—</div></div>
      <div class="kpi"><div class="kpi-label">Applications</div><div class="kpi-val" id="kpiApps">—</div></div>
      <div class="kpi"><div class="kpi-label">Status History Logs</div><div class="kpi-val" id="kpiHistory">—</div></div>
    </div>

    <div class="tabs">
      <button class="tab-btn active" onclick="switchTab('applications', this)">Applications &amp; History</button>
      <button class="tab-btn" onclick="switchTab('favorites', this)">Favorites (Full Details)</button>
      <button class="tab-btn" onclick="switchTab('profile', this)">Student Profile</button>
      <button class="tab-btn" onclick="switchTab('offers', this)">All Offers</button>
      <button class="tab-btn" onclick="switchTab('sql', this)">SQL Query Console</button>
    </div>

    <div class="panel" id="panelContent">
      <div class="empty">Connecting to phone database...</div>
    </div>
  </main>

  <script>
    let currentTab = 'applications';
    let lastData = null;
    let activeSql = 'SELECT id, offer_id, offer_title_fr, company_name, city_fr, monthly_stipend_dzd, start_date, deadline, personal_note, added_at FROM favorites ORDER BY id DESC';

    function esc(v) {
      if (v === null || v === undefined) return '';
      return String(v).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
    }

    function switchTab(tab, btn) {
      currentTab = tab;
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      render();
    }

    function runCustomSql() {
      const inp = document.getElementById('sqlInput');
      if (inp) activeSql = inp.value;
      fetchSnapshot();
    }

    function presetSql(sql) {
      activeSql = sql;
      const inp = document.getElementById('sqlInput');
      if (inp) inp.value = sql;
      fetchSnapshot();
    }

    function render() {
      if (!lastData || !lastData.counts) return;
      const d = lastData;
      const panel = document.getElementById('panelContent');

      if (currentTab === 'applications') {
        if (!d.applications.length) {
          panel.innerHTML = '<div class="empty">No applications yet. Submit one on your phone to see it appear live!</div>';
          return;
        }
        panel.innerHTML = `<table>
          <thead><tr><th>Ref</th><th>Offer &amp; Snapshot</th><th>Applicant Details</th><th>Cover Letter &amp; CV</th><th>Status</th><th>Status History</th></tr></thead>
          <tbody>${d.applications.map(a => `
            <tr>
              <td><strong>${esc(a.reference)}</strong><br><small>${esc(a.applied_at)}</small>${a.unanswered_over_7_days ? `<br><span class="badge UNDER_REVIEW">&gt;7j (${esc(a.days_since_applied)}j)</span>` : ''}</td>
              <td><strong>${esc(a.offer_title_snapshot || a.offer_title_fr)}</strong><br><small>${esc(a.company_name_snapshot || a.company_name)} · ${esc(a.city_fr)}${a.monthly_stipend_dzd > 0 ? ' · ' + esc(a.monthly_stipend_dzd) + ' DA' : ''}</small></td>
              <td><strong>${esc(a.applicant_name)}</strong><br>${esc(a.applicant_email)}<br>${esc(a.applicant_phone)}</td>
              <td>${esc(a.cover_letter)}<br><small>CV: ${esc(a.cv_uri || '—')}</small></td>
              <td><span class="badge ${esc(a.current_status)}">${esc(a.current_status)}</span></td>
              <td><ul class="history-list">${a.history.map(h => `<li><strong>${esc(h.new_status)}</strong> (${esc(h.changed_at)}) — ${esc(h.note)}</li>`).join('')}</ul></td>
            </tr>`).join('')}</tbody>
        </table>`;
      } else if (currentTab === 'favorites') {
        if (!d.favorites.length) {
          panel.innerHTML = '<div class="empty">No favorites yet. Tap a heart icon on your phone!</div>';
          return;
        }
        panel.innerHTML = `<table>
          <thead><tr><th>Fav ID</th><th>Saved Offer Metadata (in <code>favorites</code>)</th><th>Company &amp; City</th><th>Stipend &amp; Dates</th><th>Personal Note</th><th>Status &amp; Added At</th></tr></thead>
          <tbody>${d.favorites.map(f => `
            <tr>
              <td>#${esc(f.favorite_id)}<br><small>offer_id=${esc(f.offer_id)}</small></td>
              <td><strong>${esc(f.title_fr)}</strong><br><small>${esc(f.title_ar)}</small></td>
              <td>${esc(f.company_name)}<br><small>${esc(f.city_fr)} · ${esc(f.domain_fr)}</small></td>
              <td><span class="badge ${f.is_paid ? 'PAID' : 'CLOSED'}">${f.monthly_stipend_dzd > 0 ? esc(f.monthly_stipend_dzd) + ' DA/m' : (f.is_paid ? 'PAID' : 'UNPAID')}</span><br><small>Début: ${esc(f.start_date || '—')} · Lim: ${esc(f.deadline || '—')}</small></td>
              <td>${f.personal_note ? `<strong>${esc(f.personal_note)}</strong>` : '<small>—</small>'}</td>
              <td>${f.application_status ? `<span class="badge ${esc(f.application_status)}">${esc(f.application_reference)} (${esc(f.application_status)})</span>` : '<small>Not applied</small>'}<br><small>${esc(f.added_at)}</small></td>
            </tr>`).join('')}</tbody>
        </table>`;
      } else if (currentTab === 'profile') {
        const p = d.profile;
        if (!p) {
          panel.innerHTML = '<div class="empty">Student profile not initialized yet.</div>';
          return;
        }
        panel.innerHTML = `<table>
          <thead><tr><th>Field</th><th>Saved Value in <code>student_profiles</code></th></tr></thead>
          <tbody>
            <tr><td>Full Name</td><td><strong>${esc(p.full_name)}</strong></td></tr>
            <tr><td>Email</td><td>${esc(p.email)}</td></tr>
            <tr><td>Phone</td><td>${esc(p.phone)}</td></tr>
            <tr><td>University</td><td>${esc(p.university)}</td></tr>
            <tr><td>Degree Level</td><td>${esc(p.degree_level)}</td></tr>
            <tr><td>Bio</td><td>${esc(p.bio)}</td></tr>
            <tr><td>CV URI</td><td>${esc(p.cv_uri || '—')}</td></tr>
            <tr><td>Updated At</td><td>${esc(p.updated_at)}</td></tr>
          </tbody>
        </table>`;
      } else if (currentTab === 'offers') {
        panel.innerHTML = `<table>
          <thead><tr><th>ID</th><th>Title (FR / AR)</th><th>Company &amp; City</th><th>Domain &amp; Stipend</th><th>Status &amp; Dates</th><th>Fav / App</th></tr></thead>
          <tbody>${d.offers.map(o => `
            <tr>
              <td>${esc(o.id)}</td>
              <td><strong>${esc(o.title_fr)}</strong><br><small>${esc(o.title_ar)}</small></td>
              <td>${esc(o.company_name)} · ${esc(o.city_fr)}</td>
              <td>${esc(o.domain_fr)} (${esc(o.duration_months)}m)<br><small>${o.monthly_stipend_dzd > 0 ? esc(o.monthly_stipend_dzd) + ' DA/mois' : (o.is_paid ? 'Payé' : 'Non rémunéré')}</small></td>
              <td><span class="badge ${esc(o.status)}">${esc(o.status)}</span><br><small>Début: ${esc(o.start_date || '—')} · Lim: ${esc(o.deadline)}</small></td>
              <td>${o.is_favorite ? '❤️ Fav ' : ''}${o.application_reference ? `<span class="badge ${esc(o.application_status)}">${esc(o.application_reference)}</span>` : ''}</td>
            </tr>`).join('')}</tbody>
        </table>`;
      } else if (currentTab === 'sql') {
        const q = d.customQuery;
        const quickBtns = (d.tables || []).map(t => `<button class="tab-btn" onclick="presetSql('SELECT * FROM ${t} LIMIT 50')">${t}</button>`).join('');
        let tableHtml = '';
        if (q && q.error) {
          tableHtml = `<div class="empty">${esc(q.error)}</div>`;
        } else if (q && q.columns) {
          tableHtml = q.rows.length
            ? `<table><thead><tr>${q.columns.map(c => `<th>${esc(c)}</th>`).join('')}</tr></thead><tbody>${q.rows.map(r => `<tr>${q.columns.map(c => `<td>${esc(r[c])}</td>`).join('')}</tr>`).join('')}</tbody></table>`
            : '<div class="empty">Query returned 0 rows.</div>';
        }
        const existingResults = document.getElementById('sqlResultsOnly');
        if (existingResults) {
          existingResults.innerHTML = tableHtml;
        } else {
          panel.innerHTML = `
            <div class="sql-bar">
              <input id="sqlInput" class="sql-input" value="${esc(activeSql)}" oninput="activeSql = this.value" />
              <button class="btn secondary" onclick="runCustomSql()">Run SELECT</button>
            </div>
            <div class="tabs">${quickBtns}</div>
            <div id="sqlResultsOnly">${tableHtml}</div>
          `;
        }
      }
    }

    async function fetchSnapshot() {
      try {
        const url = currentTab === 'sql' ? `/api/db?sql=${encodeURIComponent(activeSql)}` : '/api/db';
        const res = await fetch(url);
        const data = await res.json();
        lastData = data;

        const pill = document.getElementById('statusPill');
        const dev = document.getElementById('devicePill');
        const sync = document.getElementById('syncPill');
        const err = document.getElementById('errorBanner');

        dev.textContent = 'Device: ' + (data.device || '—');
        sync.textContent = 'Last sync: ' + (data.lastSyncAt || '—');

        if (data.ok) {
          pill.textContent = 'LIVE SYNCING';
          pill.className = 'pill live';
          err.style.display = 'none';
        } else {
          pill.textContent = data.stale ? 'STALE (Retrying)' : 'WAITING';
          pill.className = 'pill warn';
          err.textContent = data.error || 'Waiting for phone connection...';
          err.style.display = 'block';
        }

        if (data.counts) {
          document.getElementById('kpiOffers').textContent = data.counts.offers;
          document.getElementById('kpiFavorites').textContent = data.counts.favorites;
          document.getElementById('kpiApps').textContent = data.counts.applications;
          document.getElementById('kpiHistory').textContent = data.counts.history;
        }
        render();
      } catch (e) {
        console.error(e);
      }
    }

    fetchSnapshot();
    setInterval(fetchSnapshot, 1200);
  </script>
</body>
</html>
"""


class DashboardHandler(http.server.BaseHTTPRequestHandler):
    def log_message(self, format: str, *args) -> None:
        return

    def do_GET(self) -> None:
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path == "/":
            body = HTML_PAGE.encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif parsed.path == "/api/db":
            params = urllib.parse.parse_qs(parsed.query)
            sql = params.get("sql", [None])[0]
            payload = json.dumps(BRIDGE.read_snapshot(sql), ensure_ascii=False).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(payload)))
            self.end_headers()
            self.wfile.write(payload)
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


class ReusableTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
    allow_reuse_address = True


def main() -> None:
    url = f"http://{HOST}:{PORT}"
    print(f"FirstStep Live SQLite Monitor starting at {url}")
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
