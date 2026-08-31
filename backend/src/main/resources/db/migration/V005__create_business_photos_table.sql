CREATE TABLE business_photos
(
    id            BIGSERIAL PRIMARY KEY,
    business_id   BIGINT        NOT NULL REFERENCES businesses (id) ON DELETE CASCADE,
    url           VARCHAR(1024) NOT NULL,
    is_primary    BOOLEAN       NOT NULL DEFAULT FALSE,
    display_order INTEGER       NOT NULL DEFAULT 0
);
