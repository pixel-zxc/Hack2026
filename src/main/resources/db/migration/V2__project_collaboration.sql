CREATE TABLE project_members (
    id          UUID PRIMARY KEY,
    project_id  UUID NOT NULL REFERENCES map_projects(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL,
    invited_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_project_members_project_user UNIQUE (project_id, user_id)
);
CREATE INDEX idx_project_members_user_id ON project_members(user_id);
CREATE INDEX idx_project_members_project_id ON project_members(project_id);

CREATE TABLE project_invitations (
    id           UUID PRIMARY KEY,
    project_id   UUID NOT NULL REFERENCES map_projects(id) ON DELETE CASCADE,
    email        VARCHAR(255) NOT NULL,
    role         VARCHAR(20) NOT NULL,
    token_hash   VARCHAR(128) NOT NULL,
    invited_by   UUID NOT NULL REFERENCES users(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at   TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    accepted_at  TIMESTAMPTZ
);
CREATE INDEX idx_project_invitations_project_id ON project_invitations(project_id);
CREATE INDEX idx_project_invitations_email ON project_invitations(email);
CREATE UNIQUE INDEX uk_project_invitations_pending_email
    ON project_invitations(project_id, lower(email))
    WHERE status = 'PENDING';

CREATE TABLE project_audit_logs (
    id             UUID PRIMARY KEY,
    project_id     UUID NOT NULL REFERENCES map_projects(id) ON DELETE CASCADE,
    actor_user_id  UUID NOT NULL REFERENCES users(id),
    action         VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(100),
    entity_id      UUID,
    details        JSONB,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_project_audit_logs_project_created ON project_audit_logs(project_id, created_at DESC);
