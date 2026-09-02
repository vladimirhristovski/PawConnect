package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.dto.request.CreateListingRequest
import com.sorsix.pawconnect.dto.request.UpdateListingRequest
import com.sorsix.pawconnect.dto.response.ListingSummaryResponse
import com.sorsix.pawconnect.domain.*
import com.sorsix.pawconnect.domain.result.CancelListingResult
import com.sorsix.pawconnect.domain.result.CreateListingResult
import com.sorsix.pawconnect.domain.result.CreatePetResult
import com.sorsix.pawconnect.domain.result.DeleteListingResult
import com.sorsix.pawconnect.domain.result.PublishListingResult
import com.sorsix.pawconnect.domain.result.UpdateListingResult
import com.sorsix.pawconnect.common.*
import com.sorsix.pawconnect.repository.*
import org.hibernate.exception.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Service
class ListingService(
    private val listingRepository: ListingRepository,
    private val petService: PetService,
    private val businessRepository: BusinessRepository,
    private val municipalityRepository: MunicipalityRepository,
    private val listingStatusRepository: ListingStatusRepository,
    private val petRepository: PetRepository,
    private val adoptionApplicationRepository: AdoptionApplicationRepository,
    private val applicationStatusRepository: ApplicationStatusRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun findListingWithAssociations(id: Long): Listing? =
        listingRepository.findByIdWithAllAssociations(id)

    @Transactional
    fun createListing(request: CreateListingRequest, currentUser: User): CreateListingResult {
        val municipality = municipalityRepository.findByCode(request.municipalityCode)
            ?: return CreateListingResult.NotFound("Municipality not found: ${request.municipalityCode}")

        var business: Business? = null
        request.businessId?.let { id ->
            business = businessRepository.findById(id).orElse(null)
                ?: return CreateListingResult.NotFound("Business not found: $id")
            if (business.owner?.id != currentUser.id && !currentUser.isAdmin()) {
                return CreateListingResult.Forbidden("You do not own this business")
            }
        }

        val pet = when {
            request.petId != null && request.pet != null -> throw IllegalArgumentException("Provide either petId or pet, not both")

            request.petId != null -> petRepository.findById(request.petId).orElse(null)
                ?: return CreateListingResult.NotFound("Pet not found: ${request.petId}")

            request.pet != null -> when (val result = petService.createPet(request.pet, currentUser)) {
                is CreatePetResult.Success -> result.pet
                is CreatePetResult.NotFound -> return CreateListingResult.NotFound(result.message)
            }

            else -> throw IllegalArgumentException("Either petId or pet must be provided")
        }

        if (request.petId != null && !currentUser.isAdmin() && pet.createdBy.id != currentUser.id) {
            return CreateListingResult.Forbidden("You do not own this pet")
        }

        if (listingRepository.existsByPet_IdAndStatus_CodeInAndDeletedAtIsNull(
                pet.requireId(), ListingStatusCodes.OPEN_STATUSES
            )
        ) {
            return CreateListingResult.Conflict("This pet already has an open listing")
        }

        val initialStatusCode = if (request.saveAsDraft) ListingStatusCodes.DRAFT else ListingStatusCodes.ACTIVE
        val status = listingStatusRepository.findByCode(initialStatusCode)
            ?: throw IllegalStateException("Status '$initialStatusCode' not found")

        val listing = Listing(
            pet = pet,
            postedBy = currentUser,
            municipality = municipality,
            status = status,
            business = business,
            title = request.title,
            description = request.description,
            adoptionFee = request.adoptionFee,
            latitude = request.latitude ?: municipality.latitude,
            longitude = request.longitude ?: municipality.longitude,
            expiresAt = request.expiresAt
        )

        val saved = try {
            listingRepository.save(listing)
        } catch (ex: DataIntegrityViolationException) {
            val constraintName = (ex.cause as? ConstraintViolationException)?.constraintName
            if (constraintName == "uq_listings_pet_open") {
                throw IllegalArgumentException("This pet already has an open listing")
            }
            throw ex
        }
        log.info("Listing {} created by user {} (status {})", saved.id, currentUser.id, status.code)
        val reloaded = findListingWithAssociations(saved.requireId())
            ?: return CreateListingResult.NotFound("Listing not found: ${saved.id}")
        return CreateListingResult.Success(reloaded)
    }

    @Transactional(readOnly = true)
    fun getVisibleListing(id: Long, currentUser: User?): Listing? {
        val listing = listingRepository.findByIdWithAllAssociations(id) ?: return null

        if (listing.deletedAt != null) return null
        val status = listing.status.code
        if (status in ListingStatusCodes.VISIBLE_PUBLIC) {
            return listing
        }
        if (currentUser != null && (currentUser.isAdmin() || currentUser.id == listing.postedBy.id)) {
            return listing
        }
        return null
    }

    @Transactional(readOnly = true)
    fun searchListings(
        speciesCode: String?,
        municipalityCode: String?,
        petSize: Size?,
        gender: Gender?,
        goodWithKids: Boolean?,
        goodWithOtherPets: Boolean?,
        minFee: BigDecimal?,
        maxFee: BigDecimal?,
        pageable: Pageable
    ): Page<Listing> {
        return listingRepository.searchListings(
            speciesCode, municipalityCode, petSize, gender, goodWithKids, goodWithOtherPets, minFee, maxFee, pageable
        )
    }

    @Transactional(readOnly = true)
    fun searchNearby(
        lat: BigDecimal,
        lng: BigDecimal,
        radiusKm: Double,
        speciesCode: String?,
        pageable: Pageable
    ): Page<ListingSummaryResponse> {
        val page = listingRepository.findNearby(lat.toDouble(), lng.toDouble(), radiusKm, speciesCode, pageable)
        return page.map { ListingSummaryResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun listMyListings(currentUser: User, pageable: Pageable): Page<Listing> {
        return listingRepository.findMyListingsWithAssociations(currentUser.requireId(), pageable)
    }

    @Transactional(readOnly = true)
    fun adminSearchListings(statusCode: String?, pageable: Pageable): Page<Listing> {
        return listingRepository.findAllWithAssociations(statusCode, pageable)
    }

    @Transactional
    fun publishListing(id: Long, currentUser: User): PublishListingResult {
        val listing = findListingWithAssociations(id) ?: return PublishListingResult.NotFound("Listing not found: $id")
        ownershipDenialReason(listing, currentUser)?.let { return PublishListingResult.Forbidden(it) }
        if (listing.status.code != ListingStatusCodes.DRAFT) {
            return PublishListingResult.Conflict("Listing is not in DRAFT state")
        }
        val activeStatus = listingStatusRepository.findByCode(ListingStatusCodes.ACTIVE) ?: throw IllegalStateException(
            "ACTIVE status not found"
        )
        listing.status = activeStatus
        val saved = listingRepository.save(listing)
        log.info("Listing {} published by user {}", saved.id, currentUser.id)
        val reloaded = findListingWithAssociations(saved.requireId())
            ?: return PublishListingResult.NotFound("Listing not found: ${saved.id}")
        return PublishListingResult.Success(reloaded)
    }

    @Transactional
    fun updateListing(id: Long, request: UpdateListingRequest, currentUser: User): UpdateListingResult {
        val listing = findListingWithAssociations(id) ?: return UpdateListingResult.NotFound("Listing not found: $id")
        ownershipDenialReason(listing, currentUser)?.let { return UpdateListingResult.Forbidden(it) }
        if (listing.status.code !in setOf(ListingStatusCodes.DRAFT, ListingStatusCodes.ACTIVE)) {
            return UpdateListingResult.Conflict("Listing cannot be updated in its current status")
        }

        request.title?.let { listing.title = it }
        request.description?.let { listing.description = it }
        request.adoptionFee?.let { listing.adoptionFee = it }
        request.municipalityCode?.let { code ->
            val municipality = municipalityRepository.findByCode(code)
                ?: return UpdateListingResult.NotFound("Municipality not found: $code")
            listing.municipality = municipality
        }

        if (request.latitude != null && request.longitude != null) {
            listing.latitude = request.latitude
            listing.longitude = request.longitude
        } else if (request.municipalityCode != null) {
            listing.latitude = listing.municipality.latitude
            listing.longitude = listing.municipality.longitude
        }

        request.expiresAt?.let { listing.expiresAt = it }

        val saved = listingRepository.save(listing)
        log.info("Listing {} updated by user {}", saved.id, currentUser.id)
        val reloaded = findListingWithAssociations(saved.requireId())
            ?: return UpdateListingResult.NotFound("Listing not found: ${saved.id}")
        return UpdateListingResult.Success(reloaded)
    }

    @Transactional
    fun cancelListing(id: Long, currentUser: User): CancelListingResult {
        val listing = findListingWithAssociations(id) ?: return CancelListingResult.NotFound("Listing not found: $id")
        ownershipDenialReason(listing, currentUser)?.let { return CancelListingResult.Forbidden(it) }
        if (listing.status.code == ListingStatusCodes.ADOPTED || listing.status.code == ListingStatusCodes.CANCELLED) {
            return CancelListingResult.Conflict("Listing is already adopted or cancelled")
        }
        val cancelledStatus = listingStatusRepository.findByCode(ListingStatusCodes.CANCELLED)
            ?: throw IllegalStateException("CANCELLED status not found")
        listing.status = cancelledStatus

        val pendingApps = adoptionApplicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(
            listing.requireId(), ApplicationStatusCodes.PENDING_STATUSES
        )
        if (pendingApps.isNotEmpty()) {
            val rejectedStatus = applicationStatusRepository.findByCode(ApplicationStatusCodes.REJECTED)
                ?: throw IllegalStateException("REJECTED status not found")
            val now = Instant.now()
            pendingApps.forEach { app ->
                app.status = rejectedStatus
                app.reviewedBy = currentUser
                app.reviewedAt = now
            }
            adoptionApplicationRepository.saveAll(pendingApps)
        }

        val saved = listingRepository.save(listing)
        log.info("Listing {} cancelled by user {}; {} pending application(s) rejected", listing.id, currentUser.id, pendingApps.size)
        val reloaded = findListingWithAssociations(saved.requireId())
            ?: return CancelListingResult.NotFound("Listing not found: ${saved.id}")
        return CancelListingResult.Success(reloaded)
    }

    @Transactional
    fun deleteListing(id: Long, currentUser: User): DeleteListingResult {
        val listing = findListingWithAssociations(id) ?: return DeleteListingResult.NotFound("Listing not found: $id")
        ownershipDenialReason(listing, currentUser)?.let { return DeleteListingResult.Forbidden(it) }
        if (listing.deletedAt != null) return DeleteListingResult.Success
        listing.deletedAt = Instant.now()
        listingRepository.save(listing)
        log.info("Listing {} soft-deleted by user {}", listing.id, currentUser.id)
        return DeleteListingResult.Success
    }

    @Transactional
    fun markAdopted(listing: Listing) {
        val adoptedStatus = listingStatusRepository.findByCode(ListingStatusCodes.ADOPTED)
            ?: throw IllegalStateException("ADOPTED status not found")
        listing.status = adoptedStatus
        listingRepository.save(listing)
        log.info("Listing {} marked adopted", listing.id)
    }

    private fun ownershipDenialReason(listing: Listing, currentUser: User): String? =
        denialReason(currentUser.isAdmin() || listing.postedBy.id == currentUser.id, "You do not own this listing")
}
