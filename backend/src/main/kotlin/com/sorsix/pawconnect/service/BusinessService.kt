package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.dto.request.BusinessPhotoRequest
import com.sorsix.pawconnect.dto.request.CreateBusinessRequest
import com.sorsix.pawconnect.dto.request.UpdateBusinessRequest
import com.sorsix.pawconnect.dto.response.BusinessPhotoResponse
import com.sorsix.pawconnect.dto.response.BusinessResponse
import com.sorsix.pawconnect.domain.Business
import com.sorsix.pawconnect.domain.BusinessPhoto
import com.sorsix.pawconnect.domain.BusinessType
import com.sorsix.pawconnect.domain.Municipality
import com.sorsix.pawconnect.domain.User
import com.sorsix.pawconnect.domain.result.AddBusinessPhotoResult
import com.sorsix.pawconnect.domain.result.CreateBusinessResult
import com.sorsix.pawconnect.domain.result.DeleteBusinessResult
import com.sorsix.pawconnect.domain.result.RemoveBusinessPhotoResult
import com.sorsix.pawconnect.domain.result.UpdateBusinessResult
import com.sorsix.pawconnect.repository.BusinessPhotoRepository
import com.sorsix.pawconnect.repository.BusinessRepository
import com.sorsix.pawconnect.repository.BusinessTypeRepository
import com.sorsix.pawconnect.repository.MunicipalityRepository
import com.sorsix.pawconnect.common.denialReason
import com.sorsix.pawconnect.common.normalizePrimaryPhoto
import com.sorsix.pawconnect.common.requireId
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.Instant

@Service
class BusinessService(
    private val businessRepository: BusinessRepository,
    private val businessPhotoRepository: BusinessPhotoRepository,
    private val businessTypeRepository: BusinessTypeRepository,
    private val municipalityRepository: MunicipalityRepository,
    private val blobStorageService: BlobStorageService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createBusiness(request: CreateBusinessRequest, currentUser: User): CreateBusinessResult {
        val type = businessTypeRepository.findByCode(request.typeCode)
            ?: return CreateBusinessResult.NotFound("Business type not found: ${request.typeCode}")
        val municipality = municipalityRepository.findByCode(request.municipalityCode)
            ?: return CreateBusinessResult.NotFound("Municipality not found: ${request.municipalityCode}")

        val explicitCoordinates = request.latitude != null && request.longitude != null

        val business = Business(
            type = type,
            name = request.name,
            description = request.description,
            phone = request.phone,
            email = request.email,
            address = request.address,
            municipality = municipality,
            owner = currentUser,
            latitude = if (explicitCoordinates) request.latitude else municipality.latitude,
            longitude = if (explicitCoordinates) request.longitude else municipality.longitude,
            addressGeocoded = explicitCoordinates
        )

        request.photos.forEachIndexed { idx, photoReq ->
            val photo = BusinessPhoto(
                business = business,
                url = photoReq.url,
                isPrimary = photoReq.isPrimary,
                displayOrder = photoReq.displayOrder ?: idx)
            business.photos.add(photo)
        }
        normalizePrimaryPhoto(business.photos)

        val saved = businessRepository.save(business)
        log.info("Business {} created by user {} ({} photo(s))", saved.id, currentUser.id, saved.photos.size)
        return CreateBusinessResult.Success(BusinessResponse.from(saved))
    }

    @Transactional(readOnly = true)
    fun findBusiness(id: Long): BusinessResponse? {
        val business = businessRepository.findByIdWithAssociations(id) ?: return null
        return BusinessResponse.from(business)
    }

    @Transactional(readOnly = true)
    fun searchBusinesses(
        typeCode: String?, municipalityCode: String?, pageable: Pageable
    ): Page<BusinessResponse> {
        var spec = Specification.unrestricted<Business>()
        typeCode?.let { code ->
            spec = spec.and { root, _, cb ->
                cb.equal(root.get<BusinessType>("type").get<String>("code"), code)
            }
        }
        municipalityCode?.let { code ->
            spec = spec.and { root, _, cb ->
                cb.equal(root.get<Municipality>("municipality").get<String>("code"), code)
            }
        }
        val page = businessRepository.findAll(spec, pageable)
        return page.map { BusinessResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun searchNearby(
        lat: BigDecimal, lng: BigDecimal, radiusKm: Double, typeCode: String?, pageable: Pageable
    ): Page<BusinessResponse> {
        val page = businessRepository.findNearby(lat.toDouble(), lng.toDouble(), radiusKm, typeCode, pageable)
        return page.map { BusinessResponse.from(it) }
    }

    @Transactional
    fun updateBusiness(id: Long, request: UpdateBusinessRequest, currentUser: User): UpdateBusinessResult {
        val business = businessRepository.findByIdWithAssociations(id)
            ?: return UpdateBusinessResult.NotFound("Business not found: $id")
        canManageReason(business, currentUser)?.let { return UpdateBusinessResult.Forbidden(it) }

        request.typeCode?.let { code ->
            val type = businessTypeRepository.findByCode(code)
                ?: return UpdateBusinessResult.NotFound("Business type not found: $code")
            business.type = type
        }
        request.name?.let { business.name = it }
        request.description?.let { business.description = it }
        request.phone?.let { business.phone = it }
        request.email?.let { business.email = it }
        request.address?.let { business.address = it }
        request.municipalityCode?.let { code ->
            val municipality = municipalityRepository.findByCode(code)
                ?: return UpdateBusinessResult.NotFound("Municipality not found: $code")
            business.municipality = municipality
        }

        if (request.latitude != null && request.longitude != null) {
            business.latitude = request.latitude
            business.longitude = request.longitude
            business.addressGeocoded = true
        } else if (request.address != null || request.municipalityCode != null) {
            business.latitude = business.municipality.latitude
            business.longitude = business.municipality.longitude
            business.addressGeocoded = false
        }

        val updated = businessRepository.save(business)
        log.info("Business {} updated by user {}", updated.id, currentUser.id)
        return UpdateBusinessResult.Success(BusinessResponse.from(updated))
    }

    @Transactional
    fun deleteBusiness(id: Long, currentUser: User): DeleteBusinessResult {
        val business = businessRepository.findByIdWithAssociations(id)
            ?: return DeleteBusinessResult.NotFound("Business not found: $id")
        canManageReason(business, currentUser)?.let { return DeleteBusinessResult.Forbidden(it) }
        business.deletedAt = Instant.now()
        businessRepository.save(business)
        log.info("Business {} soft-deleted by user {}", business.id, currentUser.id)
        return DeleteBusinessResult.Success
    }

    @Transactional
    fun addPhoto(businessId: Long, request: BusinessPhotoRequest, currentUser: User): AddBusinessPhotoResult {
        val business = businessRepository.findByIdWithAssociations(businessId)
            ?: return AddBusinessPhotoResult.NotFound("Business not found: $businessId")
        canManageReason(business, currentUser)?.let { return AddBusinessPhotoResult.Forbidden(it) }

        if (request.isPrimary) {
            business.photos.filter { it.isPrimary }.forEach { it.isPrimary = false }
        }

        val photo = BusinessPhoto(
            business = business, url = request.url, isPrimary = request.isPrimary,
            displayOrder = request.displayOrder ?: business.photos.size
        )
        business.photos.add(photo)
        normalizePrimaryPhoto(business.photos)

        val savedPhoto = businessPhotoRepository.save(photo)
        businessRepository.save(business)
        log.info("Photo {} added to business {} by user {}", savedPhoto.id, business.id, currentUser.id)
        return AddBusinessPhotoResult.Success(BusinessPhotoResponse.from(savedPhoto))
    }

    fun uploadAndAddPhoto(
        businessId: Long, file: MultipartFile, isPrimary: Boolean, displayOrder: Int, currentUser: User
    ): AddBusinessPhotoResult {
        val business = businessRepository.findByIdWithAssociations(businessId)
            ?: return AddBusinessPhotoResult.NotFound("Business not found: $businessId")
        canManageReason(business, currentUser)?.let { return AddBusinessPhotoResult.Forbidden(it) }

        val url = blobStorageService.upload(file, "businesses/$businessId")
        log.info("Photo uploaded to blob for business {} by user {}", businessId, currentUser.id)
        return addPhoto(
            businessId, BusinessPhotoRequest(url = url, isPrimary = isPrimary, displayOrder = displayOrder), currentUser
        )
    }

    @Transactional
    fun removePhoto(businessId: Long, photoId: Long, currentUser: User): RemoveBusinessPhotoResult {
        val business = businessRepository.findByIdWithAssociations(businessId)
            ?: return RemoveBusinessPhotoResult.NotFound("Business not found: $businessId")
        canManageReason(business, currentUser)?.let { return RemoveBusinessPhotoResult.Forbidden(it) }

        val photo = business.photos.find { it.id == photoId }
            ?: return RemoveBusinessPhotoResult.NotFound("Photo not found: $photoId")
        business.photos.remove(photo)
        businessPhotoRepository.delete(photo)
        log.info("Photo {} removed from business {} by user {}", photoId, businessId, currentUser.id)
        runCatching { blobStorageService.delete(photo.url) }.onFailure {
                log.warn(
                    "Failed to delete blob for photo {} (business {}): {}", photoId, businessId, it.message
                )
            }
        return RemoveBusinessPhotoResult.Success
    }

    private fun canManageReason(business: Business, currentUser: User): String? =
        denialReason(currentUser.isAdmin() || business.owner?.id == currentUser.id, "You do not own this business")
}
