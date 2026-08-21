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
    cat_id
BIGINT;
BEGIN
SELECT id
INTO dog_id
FROM pet_species
WHERE code = 'DOG';
SELECT id
INTO cat_id
FROM pet_species
WHERE code = 'CAT';

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

    IF
cat_id IS NOT NULL THEN
        INSERT INTO pet_breeds (code, name, species_id) VALUES
            ('PERSIAN', 'Persian', cat_id),
            ('SIAMESE', 'Siamese', cat_id),
            ('MAINE_COON', 'Maine Coon', cat_id),
            ('RAGDOLL', 'Ragdoll', cat_id),
            ('MIXED_CAT', 'Mixed Breed', cat_id)
        ON CONFLICT (code) DO NOTHING;
END IF;
END $$;