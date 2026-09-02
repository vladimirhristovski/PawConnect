CREATE UNIQUE INDEX uq_listings_pet_open ON listings (pet_id) WHERE status_id IN (1, 2) AND deleted_at IS NULL;

CREATE INDEX idx_listings_pet ON listings (pet_id);
CREATE INDEX idx_listings_posted_by ON listings (posted_by);
CREATE INDEX idx_listings_business ON listings (business_id);
CREATE INDEX idx_listings_municipality ON listings (municipality_id);
CREATE INDEX idx_listings_status ON listings (status_id);

CREATE INDEX idx_adoption_applications_listing ON adoption_applications (listing_id);
CREATE INDEX idx_adoption_applications_applicant ON adoption_applications (applicant_id);
CREATE INDEX idx_adoption_applications_status ON adoption_applications (status_id);
CREATE INDEX idx_adoption_applications_reviewed_by ON adoption_applications (reviewed_by);

CREATE INDEX idx_businesses_type ON businesses (type_id);
CREATE INDEX idx_businesses_municipality ON businesses (municipality_id);
CREATE INDEX idx_businesses_owner ON businesses (owner_id);

CREATE INDEX idx_pet_photos_pet ON pet_photos (pet_id);
CREATE INDEX idx_pet_breed_links_breed ON pet_breed_links (breed_id);
CREATE INDEX idx_pets_species ON pets (species_id);
CREATE INDEX idx_pet_breeds_species ON pet_breeds (species_id);

CREATE INDEX idx_cities_country ON cities (country_id);
CREATE INDEX idx_municipalities_city ON municipalities (city_id);

CREATE INDEX idx_user_roles_role ON user_roles (role_id);
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id);
CREATE INDEX idx_business_photos_business ON business_photos (business_id);

ALTER TABLE pets
    ADD COLUMN created_by BIGINT REFERENCES users (id);

UPDATE pets p
SET created_by = earliest.posted_by
FROM (
    SELECT DISTINCT ON (pet_id) pet_id, posted_by
    FROM listings
    ORDER BY pet_id, created_at, id
) earliest
WHERE p.id = earliest.pet_id;

ALTER TABLE pets
    ALTER COLUMN created_by SET NOT NULL;

CREATE INDEX idx_pets_created_by ON pets (created_by);
