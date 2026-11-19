# Plan de Test & Recette — FIRSTSTEP (Module C.2)

**Périmètre :** Fonctionnalités principales + Évolutions Module C (Migration Room v1→v2, Gratification & Date de début, Export CSV protégé, Alerte > 7 jours, Favoris enrichis, Sécurité PIN & Hardening).

---

## 1. Cahier de Recette Fonctionnelle & Technique (26 Cas de Test)

| ID | Catégorie | Scénario testé | Résultat attendu | Statut |
|----|-----------|----------------|------------------|--------|
| TC-01 | Seed & Init | Premier lancement hors ligne | 32 offres, profil `Amina Saidi`, domaines/villes créés sans doublon | OK |
| TC-02 | Seed Idempotence | Relancer `ensureInitialized()` | Aucun doublon dans `offers` ni `student_profiles` | OK |
| TC-03 | Recherche | Saisir un mot du milieu/fin du titre (ex: `Kotlin Mobile`) | Retourne l'offre correspondante, classée en tête (Tier 0) | OK |
| TC-04 | Recherche | Saisir sans accent (`developpement`) ou arabe normalisé (`اندرويد`) | Correspondance insensible aux accents et variantes Alif/Ta/Ya | OK |
| TC-05 | Filtres | Combiner Domaine + Ville + Payé + Ouvert | Seules les offres satisfaisant tous les critères s'affichent | OK |
| TC-06 | Évolution C.1 | Activer la puce `≥ 25 000 DA` | Filtre les offres avec `monthly_stipend_dzd >= 25000` | OK |
| TC-07 | Évolution C.1 | Trier par `Gratification max` (`HIGHEST_STIPEND`) | Liste ordonnée par `monthly_stipend_dzd DESC` | OK |
| TC-08 | Évolution C.1 | Créer/éditer une offre avec gratification & date de début | Validation format ISO `YYYY-MM-DD`, persistance Room v2 | OK |
| TC-09 | Favoris | Ajouter un favori depuis `OfferCard` ou `OfferDetailScreen` | Enregistre ligne dans `favorites` avec titre, entreprise, ville, domaine, gratification, dates | OK |
| TC-10 | Favoris | Modifier & sauvegarder une note personnelle sur un favori | `favorites.personal_note` mis à jour dans SQLite & visible sur PC | OK |
| TC-11 | Favoris | Retirer un favori puis cliquer sur *Annuler* (Undo) | Ligne supprimée puis restaurée sans doublon | OK |
| TC-12 | Candidature | Soumettre une candidature valide | Référence `FS-2026-0001` générée + entrée `status_history` initiale | OK |
| TC-13 | Candidature | Tenter une 2e candidature sur la même offre | Bloquée (`ApplicationSubmissionError.AlreadyApplied`) | OK |
| TC-14 | Candidature | Tenter de postuler à une offre clôturée ou expirée | Bouton désactivé / bloqué (`OfferClosed` / `OfferExpired`) | OK |
| TC-15 | Édition Candidature | Modifier nom/email/tél/lettre/CV d'une candidature active | Mise à jour SQLite + nouvelle trace dans `status_history` | OK |
| TC-16 | Machine à états | Passer de `SUBMITTED` → `UNDER_REVIEW` → `ACCEPTED` | Historique complet de 3 entrées ; `ACCEPTED` demande confirmation | OK |
| TC-17 | Verrou final | Tenter de rétrograder une candidature `ACCEPTED` | Rejeté (`StatusTransitionError.FinalStateLocked`) + UI verrouillée | OK |
| TC-18 | Évolution C.1 | Candidature `SUBMITTED` > 7 jours (`applied_at`) | Badge onglet Candidatures + bannière d'alerte + badge orange sur la carte | OK |
| TC-19 | Évolution C.1 | Candidature `UNDER_REVIEW` > 7 jours ou `SUBMITTED` ≤ 7 jours | Aucune fausse alerte déclenchée | OK |
| TC-20 | Export CSV | Cliquer sur *Exporter en CSV* avec filtre actif | Exporte uniquement les candidatures filtrées via `FileProvider` | OK |
| TC-21 | Sécurité CSV | Lettre contenant `=SUM(A1)`, `+cmd`, `-1`, `@SUM`, `\t=1` | Cellules préfixées par `'` et échappées RFC-4180 | OK |
| TC-22 | Migration v1→v2 | Ouvrir une base Room v1 peuplée avec `MIGRATION_1_2` | Zéro perte de favoris/candidatures/historique + nouvelles colonnes remplies | OK |
| TC-23 | Sécurité PIN | Activer PIN 4 chiffres dans Profil & relancer | Hachage SHA-256 salé stocké dans DataStore ; `PinLockScreen` bloque le mauvais PIN et déverrouille le bon | OK |
| TC-24 | Sécurité Logcat | Exécuter requêtes SQL en mode Debug | Tag `FIRSTSTEP_SQL` logue uniquement la requête et `args=N`, zéro PII | OK |
| TC-25 | I18n & Thème | Basculer FR ↔ AR et Clair ↔ Sombre | Bascule immédiate en RTL pour l'arabe et conservation après redémarrage | OK |
| TC-26 | Moniteur PC USB | Lancer `tools/live_db_dashboard.py` avec app en cours | Affiche en direct `favorites` (dont `personal_note`), `applications`, `offers` et alertes `>7j` | OK |
| TC-27 | Données Algérie | Ouvrir le filtre *Wilaya / Ville* dans Offres & Éditeur d'offre | Les **58 wilayas d'Algérie (01 Adrar → 58 El Meniaa)** sont disponibles en FR/AR | OK |

---

## 2. Vérification Multi-Largeurs, Rotation, Accessibilité & Lint

- **Compact 360 dp (ex: Petit smartphone) :**
  - Puces de filtres dans `Row.horizontalScroll` sans débordement horizontal.
  - `LazyColumn` à 1 colonne avec `OfferCard` adaptatif (`FlowRow` pour les badges).
- **Standard 411 dp (Pixel / Smartphone standard) :**
  - Barre de navigation basse (`NavigationBar`) avec badges animés sur *Favoris* et *Candidatures*.
  - Cibles tactiles `>= 48.dp` (`minimumInteractiveComponentSize()` sur icônes cœur, édition, boutons).
- **Tablette / Déplié `>= 600 dp` & `>= 840 dp` :**
  - `OffersScreen` passe automatiquement en grille 2 colonnes (`LazyVerticalGrid(GridCells.Fixed(2))`) dès `maxWidth >= 600.dp`.
  - `FirstStepNavScaffold` remplace la `NavigationBar` par un `NavigationRail` latéral dès `maxWidth >= 840.dp`.
  - Largeur maximale des formulaires/listes contrainte (`widthIn(max = 600.dp / 680.dp)`).
- **Accessibilité, Police 200 % & Internationalisation :**
  - `heightIn(min = ...)` sur puces, badges et boutons principaux afin d'éviter toute troncature jusqu'à **200 % d'agrandissement de police**.
  - `contentDescription` renseigné sur tous les `IconButton` et images ; `testTag` `snake_case` sur les contrôles interactifs.
  - Support complet RTL (`LayoutDirection.Rtl`) pour l'arabe.
- **Analyse Android Lint (`gradle :app:lintDebug`) :**
  - **0 erreur bloquante** détectée (vérification sécurité manifeste, RTL, accessibilité et Room SQLite validée).
- **Tests automatisés (`gradle :app:testDebugUnitTest`) :**
  - **21 tests unitaires & Robolectric passants** couvrant Seed 58 wilayas, migration Room v1→v2, règles métier, DAO Room, `FakeFirstStepRepository` + ViewModels, CSV OWASP, PIN SHA-256 salé et alertes `> 7 jours`.

