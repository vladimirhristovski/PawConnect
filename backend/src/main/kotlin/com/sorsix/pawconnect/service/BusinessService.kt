package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.dto.request.CreateBusinessRequest
import com.sorsix.pawconnect.dto.request.UpdateBusinessRequest
import com.sorsix.pawconnect.dto.response.BusinessResponse
import com.sorsix.pawconnect.exception.ForbiddenOperationException
import com.sorsix.pawconnect.exception.ResourceNotFoundException
import com.sorsix.pawconnect.model.Business
import com.sorsix.pawconnect.model.BusinessType
import com.sorsix.pawconnect.model.Municipality
import com.sorsix.pawconnect.model.User
import com.sorsix.pawconnect.repository.BusinessRepository
import com.sorsix.pawconnect.repository.BusinessTypeRepository
import com.sorsix.pawconnect.repository.MunicipalityRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class BusinessService(
    private val businessRepository: BusinessRepository,
    private val businessTypeRepository: BusinessTypeRepository,
    private val municipalityRepository: MunicipalityRepository
) {

    @Transactional
    fun createBusiness(request: CreateBusinessRequest, currentUser: User): BusinessResponse {
        val type = businessTypeRepository.findByCode(request.typeCode)
            ?: throw ResourceNotFoundException("Business type not found: ${request.typeCode}")
        val municipality = municipalityRepository.findByCode(request.municipalityCode)
            ?: throw ResourceNotFoundException("Municipality not found: ${request.municipalityCode}")

        val business = Business(
            type = type,
            name = request.name,
            description = request.description,
            phone = request.phone,
            email = request.email,
            address = request.address,
            municipality = municipality,
            owner = currentUser,
            latitude = request.latitude,
            longitude = request.longitude
        )
        val saved = businessRepository.save(business)
        return BusinessResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun getBusinessOrThrow(id: Long): BusinessResponse {
        val business = businessRepository.findByIdWithAssociations(id)
            ?: throw ResourceNotFoundException("Business not found: $id")
        return BusinessResponse.from(business)
    }

    @Transactional(readOnly = true)
    fun searchBusinesses(
        typeCode: String?,
        municipalityCode: String?,
        pageable: Pageable
    ): Page<BusinessResponse> {
        var spec = Specification<Business> { _, _, _ -> null }
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

    @Transactional
    fun updateBusiness(id: Long, request: UpdateBusinessRequest, currentUser: User): BusinessResponse {
        val business = businessRepository.findByIdWithAssociations(id)
            ?: throw ResourceNotFoundException("Business not found: $id")
        ensureCanManage(business, currentUser)

        request.typeCode?.let { code ->
            business.type = businessTypeRepository.findByCode(code)
                ?: throw ResourceNotFoundException("Business type not found: $code")
        }
        request.name?.let { business.name = it }
        request.description?.let { business.description = it }
        request.phone?.let { business.phone = it }
        request.email?.let { business.email = it }
        request.address?.let { business.address = it }
        request.municipalityCode?.let { code ->
            business.municipality = municipalityRepository.findByCode(code)
                ?: throw ResourceNotFoundException("Municipality not found: $code")
        }
        request.latitude?.let { business.latitude = it }
        request.longitude?.let { business.longitude = it }

        val updated = businessRepository.save(business)
        return BusinessResponse.from(updated)
    }

    @Transactional
    fun deleteBusiness(id: Long, currentUser: User) {
        val business = businessRepository.findByIdWithAssociations(id)
            ?: throw ResourceNotFoundException("Business not found: $id")
        ensureCanManage(business, currentUser)
        business.deletedAt = Instant.now()
        businessRepository.save(business)
    }

    private fun ensureCanManage(business: Business, currentUser: User) {
        if (currentUser.isAdmin()) return
        if (business.owner?.id != currentUser.id) {
            throw ForbiddenOperationException("You do not own this business")
        }
    }
}