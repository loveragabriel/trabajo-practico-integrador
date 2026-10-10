-- PostgreSQL 14+ schema for Paseadores
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- enums
CREATE TYPE user_role AS ENUM ('OWNER', 'WALKER');
CREATE TYPE pet_size AS ENUM ('SMALL', 'MEDIUM', 'LARGE');
CREATE TYPE walk_status AS ENUM ('PUBLISHED', 'COMPLETED', 'CANCELLED');
CREATE TYPE booking_status AS ENUM ('JOINED', 'CANCELLED');

-- predefined neighborhoods, no geometry
CREATE TABLE zones (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    city       VARCHAR(100) NOT NULL DEFAULT 'Buenos Aires',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_zones_name_city UNIQUE (name, city)
);

-- single table, two fixed roles
CREATE TABLE users (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name        VARCHAR(150) NOT NULL,
    email            VARCHAR(150) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    phone            VARCHAR(30),
    role             user_role NOT NULL,
    zone_id          UUID NOT NULL REFERENCES zones (id),
    is_active        BOOLEAN NOT NULL DEFAULT true,
    default_price    NUMERIC(10, 2),
    default_capacity INTEGER,
    rating_avg       NUMERIC(2, 1) NOT NULL DEFAULT 0,
    review_count     INTEGER NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_default_price_nonneg CHECK (default_price IS NULL OR default_price >= 0),
    CONSTRAINT ck_users_default_capacity_pos CHECK (default_capacity IS NULL OR default_capacity > 0),
    CONSTRAINT ck_users_owner_has_no_walker_defaults
        CHECK (role = 'WALKER' OR (default_price IS NULL AND default_capacity IS NULL)),
    CONSTRAINT ck_users_rating_avg_range CHECK (rating_avg >= 0 AND rating_avg <= 5),
    CONSTRAINT ck_users_review_count_nonneg CHECK (review_count >= 0)
);

-- case-insensitive unique email
CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));
CREATE INDEX idx_users_zone ON users (zone_id);
CREATE INDEX idx_users_role ON users (role);

-- walker coverage, role checked in service layer
CREATE TABLE walker_zones (
    walker_id  UUID NOT NULL REFERENCES users (id),
    zone_id    UUID NOT NULL REFERENCES zones (id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (walker_id, zone_id)
);

CREATE INDEX idx_walker_zones_zone ON walker_zones (zone_id);

-- owner's pets
CREATE TABLE pets (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id   UUID NOT NULL REFERENCES users (id),
    name       VARCHAR(100) NOT NULL,
    breed      VARCHAR(100),
    size       pet_size,
    notes      TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_pets_owner ON pets (owner_id);

-- group walk published by a walker
CREATE TABLE walks (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    walker_id        UUID NOT NULL REFERENCES users (id),
    zone_id          UUID NOT NULL REFERENCES zones (id),
    start_time       TIMESTAMP NOT NULL,
    duration_minutes INTEGER NOT NULL,
    -- derived end_time, used to detect overlaps
    end_time         TIMESTAMP GENERATED ALWAYS AS
                         (start_time + (duration_minutes * INTERVAL '1 minute')) STORED,
    capacity         INTEGER NOT NULL,
    price_per_pet    NUMERIC(10, 2) NOT NULL,
    meeting_point    TEXT NOT NULL,
    status           walk_status NOT NULL DEFAULT 'PUBLISHED',
    created_at       TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT ck_walks_duration_positive CHECK (duration_minutes > 0),
    CONSTRAINT ck_walks_capacity_positive CHECK (capacity > 0),
    CONSTRAINT ck_walks_price_nonneg CHECK (price_per_pet >= 0)
);

CREATE INDEX idx_walks_zone_status_start ON walks (zone_id, status, start_time);
CREATE INDEX idx_walks_walker_start ON walks (walker_id, start_time);

-- booking is its own entity, not a plain join table
CREATE TABLE walk_pets (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    walk_id   UUID NOT NULL REFERENCES walks (id),
    pet_id    UUID NOT NULL REFERENCES pets (id),
    status    booking_status NOT NULL DEFAULT 'JOINED',
    joined_at TIMESTAMP NOT NULL DEFAULT now(),
    notes     TEXT,
    CONSTRAINT uq_walk_pets_walk_pet UNIQUE (walk_id, pet_id)
);

CREATE INDEX idx_walk_pets_walk_status ON walk_pets (walk_id, status);
CREATE INDEX idx_walk_pets_pet ON walk_pets (pet_id);

-- one review per owner per walk
CREATE TABLE reviews (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    walk_id    UUID NOT NULL REFERENCES walks (id),
    author_id  UUID NOT NULL REFERENCES users (id),
    rating     SMALLINT NOT NULL,
    comment    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_reviews_walk_author UNIQUE (walk_id, author_id),
    CONSTRAINT ck_reviews_rating_range CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_walk ON reviews (walk_id);
CREATE INDEX idx_reviews_author ON reviews (author_id);
