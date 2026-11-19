# Rapport de Sécurité — FIRSTSTEP (Module C.3)

**Référentiel :** OWASP MASVS / OWASP Mobile Top 10  
**Périmètre :** Application Android native offline-first (`com.aistudio.firststep.oran26`), Room SQLite v2, DataStore, export CSV via `FileProvider`.

---

## 1. Surface d'attaque & Architecture 100 % Hors-Ligne

- **Absence totale de permissions réseau (`MASVS-NETWORK`) :** `INTERNET` et `ACCESS_NETWORK_STATE` ne sont pas déclarées dans `AndroidManifest.xml`. Aucune fuite réseau, aucun appel HTTP non chiffré ni dépendance client HTTP active.
- **Moindre privilège (`M6 / MASVS-PLATFORM`) :** Zéro permission dangereuse (`READ_EXTERNAL_STORAGE`, `READ_MEDIA_*` absentes). La sélection de CV utilise le sélecteur système sans permission (`OpenDocument` / Photo/Doc picker) et le partage CSV passe exclusivement par `androidx.core.content.FileProvider` (`android:exported="false"`, `grantUriPermissions="true"`).

---

## 2. Protection des données locales (`M9 / MASVS-STORAGE`)

- **Sauvegarde & transfert désactivés :**
  - `android:allowBackup="false"` dans `AndroidManifest.xml`.
  - `backup_rules.xml` et `data_extraction_rules.xml` excluent explicitement `root`, `file`, `database` et `sharedpref` pour `<cloud-backup>` et `<device-transfer>`.
- **Zéro PII dans Logcat :**
  - Le callback SQL de `FirstStepDatabase` (actif uniquement si `BuildConfig.DEBUG == true`) journalise exclusivement le modèle SQL et le nombre de paramètres (`args=${bindArgs.size}`), sans jamais afficher les valeurs liées (`applicant_name`, `email`, `phone`, `cover_letter`, `pin`).
  - R8 en mode `release` supprime les appels `Log.v/d/i` via `-assumenosideeffects` dans `proguard-rules.pro`.
- **Verrouillage optionnel par PIN à 4 chiffres (`M1 / MASVS-AUTH`) :**
  - Le PIN n'est jamais stocké en clair : génération d'un sel aléatoire de 16 octets (`SecureRandom`), hachage **SHA-256 itéré 1 000 fois** et comparaison en temps constant (`MessageDigest.isEqual`) via `PinSecurityHelper`.
  - Portée : si activé dans *Profil → Sécurité locale*, l'accès aux écrans applicatifs au lancement est bloqué par `PinLockScreen`.

---

## 3. Protection contre les injections & validation (`M4 / MASVS-CODE`)

- **SQLite / Room :** 100 % des requêtes passent par des DAO Room paramétrés (`:query`, `:minStipendDzd`, `:offerId`). Les caractères jokers `LIKE` (`%`, `_`, `\`) sont échappés avec `ESCAPE '\'` dans `OfferDao` et les entrées utilisateur sont assainies via `sanitizeInputQuery`.
- **Export CSV sécurisé (OWASP CSV / Formula Injection) :** `CsvExporter.sanitizeCsvCell` neutralise les préfixes dangereux (`=`, `+`, `-`, `@`, `\t`, `\r`) en les préfixant par `'` avant encadrement RFC-4180 entre guillemets doubles.
- **Intégrité métier :** Validation stricte des formulaires (`ApplicationValidator`) et machine à états (`ApplicationStateMachine`) empêchant toute soumission sur offre clôturée/expirée, toute double candidature (`UNIQUE(offer_id)`), et toute régression d'un statut final (`ACCEPTED`, `REJECTED`, `CANCELLED`).

---

## 4. Durcissement Build Release (`M7 / MASVS-RESILIENCE`)

- `isMinifyEnabled = true` et `isShrinkResources = true` activés sur le build `release` avec `proguard-android-optimize.txt` et `proguard-rules.pro`.
- Activité unique exportée (`MainActivity`, `exported="true"` pour le lanceur) et `FileProvider` strictement non exporté (`exported="false"`).

---

## 5. Risques résiduels & Remédiations futures

1. **Chiffrement SQLite au repos :** sur appareil rooté avec accès physique, `firststep_offline.db` reste lisible en clair ; une évolution future consisterait à intégrer SQLCipher avec clé stockée dans Android Keystore.
2. **Authentification biométrique :** l'intégration de `BiometricPrompt` (AndroidX Biometric) complèterait le code PIN SHA-256 pour un déverrouillage matériel fort.
3. **Limitation des tentatives PIN :** l'ajout d'un délai exponentiel après 5 saisies erronées renforcerait la protection contre le brute-force hors ligne.
