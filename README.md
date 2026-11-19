# FIRSTSTEP — Application Mobile Android Offline-First (Oran 2026)

**FIRSTSTEP** est une application Android native développée en **Kotlin + Jetpack Compose (Material Design 3)** avec architecture **MVVM + Clean Architecture** et persistance locale **Room / SQLite + DataStore**.  
Conçue pour les **Olympiades Africaines des Métiers — Oran 2026**, elle permet aux étudiants et stagiaires de découvrir, filtrer, sauvegarder en favoris et suivre leurs candidatures de stage **100 % hors ligne** (zéro connexion Internet requise).

---

## Fonctionnalités principales

- **Catalogue d'offres bilingue (FR / AR + RTL complet)** : 32 offres pré-chargées depuis `assets/firststep_seed.json`, couvrant 4 domaines et les **58 wilayas d'Algérie (01 Adrar → 58 El Meniaa)** en français et en arabe.
- **Recherche multi-mots & tolérante aux accents/variantes** : recherche par n'importe quel mot dans n'importe quel ordre sur le titre FR/AR, entreprise, wilaya, domaine, compétences et description (classement par pertinence avec priorité aux titres).
- **Filtres combinables & tri avancé (Room v2)** :
  - Tous / Payé / Gratification `≥ 25 000 DA` / Ouvert / Domaine / Wilaya (58 wilayas).
  - Tri par : Plus récentes, Date limite proche, **Gratification max (`HIGHEST_STIPEND`)**.
- **Favoris enrichis dans SQLite** : enregistrement dans la table `favorites` avec toutes les métadonnées descriptives de l'offre (`offer_title_fr`, `offer_title_ar`, `company_name`, `city_fr`, `domain_fr`, `monthly_stipend_dzd`, `start_date`, `deadline`) + **note personnelle (`personal_note`)** éditable.
- **Suivi des candidatures & alerte > 7 jours** :
  - Génération automatique de référence (`FS-2026-XXXX`) et snapshot de l'offre dans `applications`.
  - Machine à états stricte avec verrouillage des états finaux (`ACCEPTED`, `REJECTED`, `CANCELLED`) et historique complet dans `status_history`.
  - **Badge & bannière d'alerte** pour toute candidature `SUBMITTED` sans réponse depuis plus de 7 jours.
  - **Export CSV filtré** partagé localement via `FileProvider` (avec neutralisation OWASP des formules CSV).
- **Sécurité locale & R8** :
  - Aucune permission `INTERNET`, `android:allowBackup="false"`, règles `backup_rules.xml` & `data_extraction_rules.xml` restrictives.
  - Option de **verrouillage par code PIN à 4 chiffres** (hachage SHA-256 salé sur 1 000 itérations stocké dans DataStore).
  - R8 activé en mode `release` (`isMinifyEnabled = true`, `isShrinkResources = true`).
- **Moniteur SQLite PC en direct (`tools/live_db_dashboard.py`)** : tableau de bord web local via USB ADB permettant d'inspecter en temps réel toutes les tables (`favorites`, `applications`, `offers`, `status_history`, `student_profiles`) pendant l'exécution sur téléphone.

---

## Jeu de données & Profil de test par défaut

- **Profil étudiant pré-chargé :** `Amina Saidi` (`amina.saidi@email.dz`, `+213 555 12 34 56`, `Univ. Oran 1`, `L3 Informatique`).
- **Code PIN local :** désactivé par défaut au premier lancement ; activable à tout moment depuis *Profil → Sécurité locale → Verrouillage par code PIN*.

---

## Prérequis & Compilation Hors Ligne

- **Android Studio** (AGP 9+ / Kotlin / Compile SDK 36, Min SDK 24)
- **JDK 11+**
- **Python 3.10+** (optionnel, pour le moniteur PC USB `tools/live_db_dashboard.py`)

### Correctif Windows (`debug.keystore not found`) après export ZIP
Le dépôt inclut `debug.keystore.base64` (sans le binaire `debug.keystore`). Sur Windows dans Android Studio :
1. Double-cliquez sur **`Fix_Windows_Keystore.bat`** à la racine du projet (ou exécutez dans CMD : `certutil -decode debug.keystore.base64 debug.keystore`).
2. Relancez **Build / Run** dans Android Studio.

```bash
# Script reproductible hors ligne (Linux/macOS/Git Bash)
bash tools/build_offline.sh

# Ou manuellement :
gradle --offline :app:assembleDebug
gradle --offline :app:testDebugUnitTest

# Lancer le moniteur SQLite PC en temps réel (téléphone branché en USB Debugging)
python tools/live_db_dashboard.py
```

---

## Structure du projet

```text
app/src/main/java/com/olympiades/firststep/
├── data/
│   ├── local/          # Room v2 DB, MIGRATION_1_2, DAOs, Entities, SeedDataLoader
│   ├── preferences/    # DataStore (langue, thème, filtres, hash PIN salé)
│   └── repository/     # FirstStepRepositoryImpl & mappers
├── domain/
│   ├── export/         # CsvExporter (RFC-4180 + protection injection de formules)
│   ├── model/          # Modèles métier, DateTimeHelper, Enums
│   ├── security/       # PinSecurityHelper (SHA-256 salé 1000 tours + vérif temps constant)
│   ├── usecase/        # Cas d'utilisation métier
│   └── validation/     # ApplicationValidator & ApplicationStateMachine
└── ui/
    ├── components/     # OfferCard, OfferEditorDialog, PinLockScreen, Badges...
    ├── i18n/           # AppStrings (FR / AR)
    ├── navigation/     # FirstStepNavGraph (NavigationBar / NavigationRail adaptatifs)
    ├── screens/        # OffersScreen, OfferDetailScreen, FavoritesScreen, ApplicationsScreen, ProfileScreen
    ├── theme/          # Palette, Typographie & Thème M3 Clair/Sombre
    └── viewmodel/      # ViewModels MVVM + StateFlow
```

---

## Documentation & Livrables Module C

- [Rapport de Sécurité OWASP MASVS / Mobile Top 10](docs/SECURITY_REPORT.md)
- [Plan de Test & Recette Complète (25+ cas)](docs/TEST_PLAN_AND_RECETTE.md)
- [Schéma SQL Room v2](docs/schema.sql)
- [Schémas JSON Room exportés](app/schemas/com.olympiades.firststep.data.local.FirstStepDatabase/)
