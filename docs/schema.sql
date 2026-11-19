-- FIRSTSTEP — Schéma SQLite Room Version 2 (Offline-First)
-- Correspond exactement aux schémas exportés KSP (2.json) et à MIGRATION_1_2

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS domains (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name_fr TEXT NOT NULL,
    name_ar TEXT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS index_domains_name_fr ON domains (name_fr);
CREATE UNIQUE INDEX IF NOT EXISTS index_domains_name_ar ON domains (name_ar);

CREATE TABLE IF NOT EXISTS locations (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    city_fr TEXT NOT NULL,
    city_ar TEXT NOT NULL,
    region_fr TEXT NOT NULL,
    region_ar TEXT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS index_locations_city_fr_region_fr ON locations (city_fr, region_fr);

CREATE TABLE IF NOT EXISTS companies (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name TEXT NOT NULL,
    sector_fr TEXT NOT NULL,
    sector_ar TEXT NOT NULL,
    description_fr TEXT NOT NULL,
    description_ar TEXT NOT NULL,
    website TEXT NOT NULL,
    logo_url TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS skills (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name_fr TEXT NOT NULL,
    name_ar TEXT NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS index_skills_name_fr ON skills (name_fr);
CREATE UNIQUE INDEX IF NOT EXISTS index_skills_name_ar ON skills (name_ar);

CREATE TABLE IF NOT EXISTS student_profiles (
    id INTEGER NOT NULL PRIMARY KEY,
    full_name TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT NOT NULL,
    university TEXT NOT NULL,
    degree_level TEXT NOT NULL,
    bio TEXT NOT NULL,
    cv_uri TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS offers (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    company_id INTEGER NOT NULL,
    domain_id INTEGER NOT NULL,
    location_id INTEGER NOT NULL,
    title_fr TEXT NOT NULL,
    title_ar TEXT NOT NULL,
    description_fr TEXT NOT NULL,
    description_ar TEXT NOT NULL,
    duration_months INTEGER NOT NULL,
    is_paid INTEGER NOT NULL,
    monthly_stipend_dzd INTEGER NOT NULL DEFAULT 0,
    start_date TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL,
    deadline TEXT NOT NULL,
    created_at TEXT NOT NULL,
    FOREIGN KEY(company_id) REFERENCES companies(id) ON UPDATE NO ACTION ON DELETE CASCADE,
    FOREIGN KEY(domain_id) REFERENCES domains(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
    FOREIGN KEY(location_id) REFERENCES locations(id) ON UPDATE NO ACTION ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS index_offers_company_id ON offers (company_id);
CREATE INDEX IF NOT EXISTS index_offers_domain_id ON offers (domain_id);
CREATE INDEX IF NOT EXISTS index_offers_location_id ON offers (location_id);
CREATE INDEX IF NOT EXISTS index_offers_deadline ON offers (deadline);
CREATE INDEX IF NOT EXISTS index_offers_status ON offers (status);

CREATE TABLE IF NOT EXISTS offer_skills (
    offer_id INTEGER NOT NULL,
    skill_id INTEGER NOT NULL,
    PRIMARY KEY(offer_id, skill_id),
    FOREIGN KEY(offer_id) REFERENCES offers(id) ON UPDATE NO ACTION ON DELETE CASCADE,
    FOREIGN KEY(skill_id) REFERENCES skills(id) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS index_offer_skills_skill_id ON offer_skills (skill_id);

CREATE TABLE IF NOT EXISTS favorites (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    offer_id INTEGER NOT NULL,
    added_at TEXT NOT NULL,
    offer_title_fr TEXT NOT NULL DEFAULT '',
    offer_title_ar TEXT NOT NULL DEFAULT '',
    company_name TEXT NOT NULL DEFAULT '',
    city_fr TEXT NOT NULL DEFAULT '',
    domain_fr TEXT NOT NULL DEFAULT '',
    monthly_stipend_dzd INTEGER NOT NULL DEFAULT 0,
    start_date TEXT NOT NULL DEFAULT '',
    deadline TEXT NOT NULL DEFAULT '',
    personal_note TEXT NOT NULL DEFAULT '',
    FOREIGN KEY(offer_id) REFERENCES offers(id) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE UNIQUE INDEX IF NOT EXISTS index_favorites_offer_id ON favorites (offer_id);

CREATE TABLE IF NOT EXISTS applications (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    reference TEXT NOT NULL,
    offer_id INTEGER NOT NULL,
    applicant_name TEXT NOT NULL,
    applicant_email TEXT NOT NULL,
    applicant_phone TEXT NOT NULL,
    cover_letter TEXT NOT NULL,
    cv_uri TEXT NOT NULL,
    current_status TEXT NOT NULL,
    applied_at TEXT NOT NULL,
    offer_title_snapshot TEXT NOT NULL DEFAULT '',
    company_name_snapshot TEXT NOT NULL DEFAULT '',
    monthly_stipend_dzd INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(offer_id) REFERENCES offers(id) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE UNIQUE INDEX IF NOT EXISTS index_applications_reference ON applications (reference);
CREATE UNIQUE INDEX IF NOT EXISTS index_applications_offer_id ON applications (offer_id);

CREATE TABLE IF NOT EXISTS status_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    application_id INTEGER NOT NULL,
    old_status TEXT,
    new_status TEXT NOT NULL,
    changed_at TEXT NOT NULL,
    note TEXT NOT NULL,
    FOREIGN KEY(application_id) REFERENCES applications(id) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS index_status_history_application_id ON status_history (application_id);
