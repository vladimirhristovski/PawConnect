INSERT INTO roles (name)
VALUES ('ADMIN'),
       ('USER'),
       ('BUSINESS_OWNER') ON CONFLICT (name) DO NOTHING;

INSERT INTO listing_statuses (code, name)
VALUES ('DRAFT', 'Draft'),
       ('ACTIVE', 'Active'),
       ('PENDING_ADOPTION', 'Pending Adoption'),
       ('ADOPTED', 'Adopted'),
       ('EXPIRED', 'Expired'),
       ('CANCELLED', 'Cancelled') ON CONFLICT (code) DO NOTHING;

INSERT INTO application_statuses (code, name)
VALUES ('SUBMITTED', 'Submitted'),
       ('UNDER_REVIEW', 'Under Review'),
       ('APPROVED', 'Approved'),
       ('REJECTED', 'Rejected'),
       ('WITHDRAWN', 'Withdrawn'),
       ('CLOSED', 'Closed') ON CONFLICT (code) DO NOTHING;

INSERT INTO business_types (code, name)
VALUES ('VET', 'Veterinary Clinic'),
       ('GROOMER', 'Groomer'),
       ('PET_SHOP', 'Pet Shop'),
       ('SHELTER', 'Shelter'),
       ('RESCUE', 'Rescue Organization'),
       ('BOARDING', 'Boarding Facility'),
       ('TRAINER', 'Trainer') ON CONFLICT (code) DO NOTHING;

INSERT INTO pet_species (code, name)
VALUES ('DOG', 'Dog'),
       ('CAT', 'Cat'),
       ('RABBIT', 'Rabbit'),
       ('BIRD', 'Bird'),
       ('REPTILE', 'Reptile'),
       ('SMALL_MAMMAL', 'Small Mammal'),
       ('OTHER', 'Other') ON CONFLICT (code) DO NOTHING;

DO
$$
DECLARE
dog_id BIGINT;
BEGIN
SELECT id
INTO dog_id
FROM pet_species
WHERE code = 'DOG';
IF
dog_id IS NOT NULL THEN
        INSERT INTO pet_breeds (code, name, species_id) VALUES
            ('LABRADOR', 'Labrador Retriever', dog_id),
            ('GERMAN_SHEPHERD', 'German Shepherd', dog_id),
            ('GOLDEN_RETRIEVER', 'Golden Retriever', dog_id),
            ('BULLDOG', 'Bulldog', dog_id),
            ('POODLE', 'Poodle', dog_id),
            ('MIXED', 'Mixed Breed', dog_id)
        ON CONFLICT (code) DO NOTHING;
END IF;
END $$;

INSERT INTO users (username, email, password, first_name, last_name, is_active)
SELECT 'admin',
       'admin@pawconnect.com',
       '$2a$10$7Z9p5JNlXwOqK7VZ4r5nA.jxXUvW3YmLnWqEfJtR8sNQo2P6J0K9e',
       'Admin',
       'User',
       true WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u,
     roles r
WHERE u.username = 'admin'
  AND r.name = 'ADMIN' ON CONFLICT (user_id, role_id) DO NOTHING;