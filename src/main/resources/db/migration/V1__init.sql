-- SpaceMap initial schema

CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL DEFAULT 'ORGANIZER',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE organizations (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    owner_id    UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_organizations_owner_id ON organizations(owner_id);

CREATE TABLE organization_members (
    id               UUID PRIMARY KEY,
    organization_id  UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role             VARCHAR(50) NOT NULL DEFAULT 'EDITOR',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_organization_members_organization_user UNIQUE (organization_id, user_id)
);
CREATE INDEX idx_organization_members_user_id ON organization_members(user_id);

CREATE TABLE assets (
    id            UUID PRIMARY KEY,
    owner_id      UUID NOT NULL REFERENCES users(id),
    key           VARCHAR(512) NOT NULL UNIQUE,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT NOT NULL,
    status        VARCHAR(50) NOT NULL DEFAULT 'READY',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_assets_owner_id ON assets(owner_id);

CREATE TABLE map_projects (
    id               UUID PRIMARY KEY,
    organization_id  UUID NOT NULL REFERENCES organizations(id),
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    cover_asset_id   UUID REFERENCES assets(id) ON DELETE SET NULL,
    status           VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    public_slug      VARCHAR(100) UNIQUE,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_map_projects_organization_id ON map_projects(organization_id);

CREATE TABLE floors (
    id             UUID PRIMARY KEY,
    project_id     UUID NOT NULL REFERENCES map_projects(id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    sort_order     INTEGER NOT NULL DEFAULT 0,
    plan_asset_id  UUID REFERENCES assets(id) ON DELETE SET NULL,
    width          NUMERIC(12, 4),
    height         NUMERIC(12, 4),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_floors_project_id ON floors(project_id);
CREATE INDEX idx_floors_project_sort ON floors(project_id, sort_order);

CREATE TABLE places (
    id           UUID PRIMARY KEY,
    floor_id     UUID NOT NULL REFERENCES floors(id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    category     VARCHAR(100) NOT NULL DEFAULT 'ROOM',
    description  TEXT,
    geometry     JSONB,
    x            NUMERIC(12, 6),
    y            NUMERIC(12, 6),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_places_floor_id ON places(floor_id);
CREATE INDEX idx_places_floor_category ON places(floor_id, category);

CREATE TABLE panoramas (
    id             UUID PRIMARY KEY,
    place_id       UUID REFERENCES places(id) ON DELETE SET NULL,
    floor_id       UUID NOT NULL REFERENCES floors(id) ON DELETE CASCADE,
    asset_id       UUID NOT NULL REFERENCES assets(id),
    name           VARCHAR(255),
    initial_yaw    NUMERIC(8, 4) DEFAULT 0,
    initial_pitch  NUMERIC(8, 4) DEFAULT 0,
    x              NUMERIC(12, 6),
    y              NUMERIC(12, 6),
    status         VARCHAR(50) NOT NULL DEFAULT 'READY',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_panoramas_floor_id ON panoramas(floor_id);
CREATE INDEX idx_panoramas_place_id ON panoramas(place_id);

CREATE TABLE hotspots (
    id                   UUID PRIMARY KEY,
    panorama_id          UUID NOT NULL REFERENCES panoramas(id) ON DELETE CASCADE,
    target_panorama_id   UUID REFERENCES panoramas(id) ON DELETE SET NULL,
    place_id             UUID REFERENCES places(id) ON DELETE SET NULL,
    yaw                  NUMERIC(8, 4) NOT NULL,
    pitch                NUMERIC(8, 4) NOT NULL,
    label                VARCHAR(255),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_hotspots_panorama_id ON hotspots(panorama_id);

CREATE TABLE route_nodes (
    id          UUID PRIMARY KEY,
    floor_id    UUID NOT NULL REFERENCES floors(id) ON DELETE CASCADE,
    place_id    UUID REFERENCES places(id) ON DELETE SET NULL,
    x           NUMERIC(12, 6) NOT NULL,
    y           NUMERIC(12, 6) NOT NULL,
    node_type   VARCHAR(50) NOT NULL DEFAULT 'WALKWAY',
    accessible  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_route_nodes_floor_id ON route_nodes(floor_id);

CREATE TABLE route_edges (
    id              UUID PRIMARY KEY,
    from_node_id    UUID NOT NULL REFERENCES route_nodes(id) ON DELETE CASCADE,
    to_node_id      UUID NOT NULL REFERENCES route_nodes(id) ON DELETE CASCADE,
    cost            NUMERIC(12, 4) NOT NULL DEFAULT 1,
    bidirectional   BOOLEAN NOT NULL DEFAULT TRUE,
    edge_type       VARCHAR(50) NOT NULL DEFAULT 'WALK',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_route_edges_from ON route_edges(from_node_id);
CREATE INDEX idx_route_edges_to ON route_edges(to_node_id);

CREATE TABLE publication_versions (
    id            UUID PRIMARY KEY,
    project_id    UUID NOT NULL REFERENCES map_projects(id) ON DELETE CASCADE,
    version       INTEGER NOT NULL,
    snapshot      JSONB NOT NULL,
    published_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_publication_versions_project_version UNIQUE (project_id, version)
);
CREATE INDEX idx_publication_versions_project_id ON publication_versions(project_id);
