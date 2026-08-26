ALTER TABLE municipalities
    ADD COLUMN latitude  DECIMAL(9, 6),
    ADD COLUMN longitude DECIMAL(9, 6);

ALTER TABLE businesses
    ADD COLUMN address_geocoded BOOLEAN NOT NULL DEFAULT false;
