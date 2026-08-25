package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.dto.request.CreateListingRequest
import com.sorsix.pawconnect.dto.request.UpdateListingRequest
import com.sorsix.pawconnect.exception.ConflictException
import com.sorsix.pawconnect.exception.ForbiddenOperationException
import com.sorsix.pawconnect.exception.ResourceNotFoundException
import com.sorsix.pawconnect.model.*
import com.sorsix.pawconnect.model.enums.Gender
import com.sorsix.pawconnect.model.enums.Size
import com.sorsix.pawconnect.repository.*
import com.sorsix.pawconnect.util.ApplicationStatusCodes
import com.sorsix.pawconnect.util.ListingStatusCodes
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
    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    private fun getListingOrThrow(id: Long): Listing {
        return listingRepository.findByIdWithAllAssociations(id)
            ?: throw ResourceNotFoundException("Listing not found: $id")
    }

    @Transactional
    fun createListing(request: CreateListingRequest, currentUser: User): Listing {
        val pet = when {
            request.petId != null && request.pet != null -> throw IllegalArgumentException("Provide either petId or pet, not both")

            request.petId != null -> petRepository.findById(request.petId)
                .orElseThrow { ResourceNotFoundException("Pet not found: ${request.petId}") }

            request.pet != null -> petService.createPet(request.pet)
            else -> throw IllegalArgumentException("Either petId or pet must be provided")
        }

        if (listingRepository.existsByPet_IdAndStatus_CodeInAndDeletedAtIsNull(
                pet.id!!, ListingStatusCodes.OPEN_STATUSES
            )
        ) {
            throw ConflictException("This pet already has an open listing")
        }

        val municipality = municipalityRepository.findByCode(request.municipalityCode)
            ?: throw ResourceNotFoundException("Municipality not found: ${request.municipalityCode}")

        var business: Business? = null
        request.businessId?.let { id ->
            business =
                businessRepository.findById(id).orElseThrow { ResourceNotFoundException("Business not found: $id") }
            if (business.owner?.id != currentUser.id && !currentUser.isAdmin()) {
                throw ForbiddenOperationException("You do not own this business")
            }
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
            latitude = request.latitude,
            longitude = request.longitude,
            expiresAt = request.expiresAt
        )

        val saved = listingRepository.save(listing)
        log.info("Listing {} created by user {} (status {})", saved.id, currentUser.id, status.code)
        return getListingWithAssociationsOrThrow(saved.id!!)
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
    fun listMyListings(currentUser: User, pageable: Pageable): Page<Listing> {
        return listingRepository.findMyListingsWithAssociations(currentUser.id!!, pageable)
    }

    @Transactional
    fun publishListing(id: Long, currentUser: User): Listing {
        val listing = getListingOrThrow(id)
        ensureOwnership(listing, currentUser)
        if (listing.status.code != ListingStatusCodes.DRAFT) {
            throw ConflictException("Listing is not in DRAFT state")
        }
        val activeStatus = listingStatusRepository.findByCode(ListingStatusCodes.ACTIVE) ?: throw IllegalStateException(
            "ACTIVE status not found"
        )
        listing.status = activeStatus
        val saved = listingRepository.save(listing)
        log.info("Listing {} published by user {}", saved.id, currentUser.id)
        return getListingWithAssociationsOrThrow(saved.id!!)
    }

    @Transactional
    fun updateListing(id: Long, request: UpdateListingRequest, currentUser: User): Listing {
        val listing = getListingOrThrow(id)
        ensureOwnership(listing, currentUser)
        if (listing.status.code !in setOf(ListingStatusCodes.DRAFT, ListingStatusCodes.ACTIVE)) {
            throw ConflictException("Listing cannot be updated in its current status")
        }

        request.title?.let { listing.title = it }
        request.description?.let { listing.description = it }
        request.adoptionFee?.let { listing.adoptionFee = it }
        request.municipalityCode?.let { code ->
            listing.municipality = municipalityRepository.findByCode(code)
                ?: throw ResourceNotFoundException("Municipality not found: $code")
        }
        request.latitude?.let { listing.latitude = it }
        request.longitude?.let { listing.longitude = it }
        request.expiresAt?.let { listing.expiresAt = it }

        val saved = listingRepository.save(listing)
        log.info("Listing {} updated by user {}", saved.id, currentUser.id)
        return getListingWithAssociationsOrThrow(saved.id!!)
    }

    @Transactional
    fun cancelListing(id: Long, currentUser: User): Listing {
        val listing = getListingOrThrow(id)
        ensureOwnership(listing, currentUser)
        if (listing.status.code == ListingStatusCodes.ADOPTED || listing.status.code == ListingStatusCodes.CANCELLED) {
            throw ConflictException("Listing is already adopted or cancelled")
        }
        val cancelledStatus = listingStatusRepository.findByCode(ListingStatusCodes.CANCELLED)
            ?: throw IllegalStateException("CANCELLED status not found")
        listing.status = cancelledStatus

        val pendingApps = adoptionApplicationRepository.findByListing_IdAndStatus_CodeInAndDeletedAtIsNull(
            listing.id!!, ApplicationStatusCodes.PENDING_STATUSES
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
        return getListingWithAssociationsOrThrow(saved.id!!)
    }

    @Transactional
    fun deleteListing(id: Long, currentUser: User) {
        val listing = getListingOrThrow(id)
        ensureOwnership(listing, currentUser)
        if (listing.deletedAt != null) return
        listing.deletedAt = Instant.now()
        listingRepository.save(listing)
        log.info("Listing {} soft-deleted by user {}", listing.id, currentUser.id)
    }

    @Transactional
    fun markAdopted(listing: Listing) {
        val adoptedStatus = listingStatusRepository.findByCode(ListingStatusCodes.ADOPTED)
            ?: throw IllegalStateException("ADOPTED status not found")
        listing.status = adoptedStatus
        listingRepository.save(listing)
        log.info("Listing {} marked adopted", listing.id)
    }

    private fun getListingWithAssociationsOrThrow(id: Long): Listing {
        return listingRepository.findByIdWithAllAssociations(id)
            ?: throw ResourceNotFoundException("Listing not found: $id")
    }

    private fun ensureOwnership(listing: Listing, currentUser: User) {
        if (currentUser.isAdmin()) return
        if (listing.postedBy.id != currentUser.id) {
            throw ForbiddenOperationException("You do not own this listing")
        }
    }
}