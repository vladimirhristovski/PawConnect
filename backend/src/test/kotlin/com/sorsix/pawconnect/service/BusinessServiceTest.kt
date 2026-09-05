package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.dto.request.CreateBusinessRequest
import com.sorsix.pawconnect.dto.request.UpdateBusinessRequest
import com.sorsix.pawconnect.domain.Business
import com.sorsix.pawconnect.domain.BusinessType
import com.sorsix.pawconnect.domain.Municipality
import com.sorsix.pawconnect.domain.User
import com.sorsix.pawconnect.domain.result.CreateBusinessResult
import com.sorsix.pawconnect.domain.result.DeleteBusinessResult
import com.sorsix.pawconnect.domain.result.UpdateBusinessResult
import com.sorsix.pawconnect.repository.BusinessRepository
import com.sorsix.pawconnect.repository.BusinessPhotoRepository
import com.sorsix.pawconnect.repository.BusinessTypeRepository
import com.sorsix.pawconnect.repository.MunicipalityRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class BusinessServiceTest {

    private val businessRepository = mockk<BusinessRepository>()
    private val businessPhotoRepository = mockk<BusinessPhotoRepository>()
    private val businessTypeRepository = mockk<BusinessTypeRepository>()
    private val municipalityRepository = mockk<MunicipalityRepository>()
    private val blobStorageService = mockk<BlobStorageService>()
    private lateinit var service: BusinessService

    @BeforeEach
    fun setup() {
        service = BusinessService(
            businessRepository,
            businessPhotoRepository,
            businessTypeRepository,
            municipalityRepository,
            blobStorageService
        )
    }

    private fun mockUser(id: Long = 1L, admin: Boolean = false): User {
        val user = mockk<User>(relaxed = true)
        every { user.id } returns id
        every { user.isAdmin() } returns admin
        return user
    }

    private fun mockBusiness(
        id: Long = 1L,
        owner: User = mockUser(),
        type: BusinessType = mockk(relaxed = true),
        municipality: Municipality = mockk(relaxed = true)
    ): Business {
        val business = mockk<Business>(relaxed = true)
        every { business.id } returns id
        every { business.owner } returns owner
        every { business.type } returns type
        every { business.municipality } returns municipality
        every { business.name = any() } just runs
        every { business.description = any() } just runs
        every { business.phone = any() } just runs
        every { business.email = any() } just runs
        every { business.address = any() } just runs
        every { business.latitude = any() } just runs
        every { business.longitude = any() } just runs
        every { business.deletedAt = any() } just runs
        return business
    }

    @Test
    fun `createBusiness should create business successfully with all fields`() {
        val type = mockk<BusinessType>(relaxed = true)
        val municipality = mockk<Municipality>(relaxed = true)
        every { businessTypeRepository.findByCode("SHELTER") } returns type
        every { municipalityRepository.findByCode("SK-CENTAR") } returns municipality

        val request = CreateBusinessRequest(
            typeCode = "SHELTER",
            name = "Happy Paws",
            description = "A great shelter",
            phone = "+38971234567",
            email = "info@happypaws.mk",
            address = "Street 1",
            municipalityCode = "SK-CENTAR",
            latitude = 42.0.toBigDecimal(),
            longitude = 21.4.toBigDecimal()
        )
        val user = mockUser()
        val savedBusiness = mockBusiness(id = 100L, owner = user, type = type, municipality = municipality)
        every { businessRepository.save(any<Business>()) } returns savedBusiness

        val result = service.createBusiness(request, user)

        val slot = slot<Business>()
        verify { businessRepository.save(capture(slot)) }
        val captured = slot.captured
        assertEquals(type, captured.type)
        assertEquals(municipality, captured.municipality)
        assertEquals("Happy Paws", captured.name)
        assertEquals("A great shelter", captured.description)
        assertEquals("+38971234567", captured.phone)
        assertEquals("info@happypaws.mk", captured.email)
        assertEquals("Street 1", captured.address)
        assertEquals(42.0.toBigDecimal(), captured.latitude)
        assertEquals(21.4.toBigDecimal(), captured.longitude)
        assertEquals(user, captured.owner)

        assertIs<CreateBusinessResult.Success>(result)
        assertNotNull(result.business)
        assertEquals(100L, result.business.id)
    }

    @Test
    fun `createBusiness should throw when type not found`() {
        every { businessTypeRepository.findByCode("UNKNOWN") } returns null
        val request = CreateBusinessRequest(
            typeCode = "UNKNOWN", name = "Test", phone = "123", address = "St", municipalityCode = "SK"
        )
        val result = service.createBusiness(request, mockUser())
        assertIs<CreateBusinessResult.NotFound>(result)
        verify(exactly = 0) { businessRepository.save(any()) }
    }

    @Test
    fun `createBusiness should throw when municipality not found`() {
        val type = mockk<BusinessType>(relaxed = true)
        every { businessTypeRepository.findByCode("SHELTER") } returns type
        every { municipalityRepository.findByCode("BAD") } returns null
        val request = CreateBusinessRequest(
            typeCode = "SHELTER", name = "Test", phone = "123", address = "St", municipalityCode = "BAD"
        )
        val result = service.createBusiness(request, mockUser())
        assertIs<CreateBusinessResult.NotFound>(result)
        verify(exactly = 0) { businessRepository.save(any()) }
    }

    @Test
    fun `findBusiness should return business when found`() {
        val business = mockBusiness(id = 5L)
        every { businessRepository.findByIdWithAssociations(5L) } returns business
        val response = service.findBusiness(5L)
        assertNotNull(response)
        assertEquals(5L, response.id)
    }

    @Test
    fun `findBusiness should return null when business not found`() {
        every { businessRepository.findByIdWithAssociations(99L) } returns null
        assertEquals(null, service.findBusiness(99L))
    }

    @Test
    fun `searchBusinesses should return all businesses when no filters`() {
        val pageable = PageRequest.of(0, 10)
        val business = mockBusiness(id = 1L)
        val page = PageImpl(listOf(business))
        every { businessRepository.findAll(any<Specification<Business>>(), eq(pageable)) } returns page

        val result = service.searchBusinesses(null, null, pageable)
        assertEquals(1, result.totalElements)
        assertEquals(1L, result.content[0].id)
        verify { businessRepository.findAll(any<Specification<Business>>(), eq(pageable)) }
    }

    @Test
    fun `searchBusinesses should filter by type code`() {
        val pageable = PageRequest.of(0, 10)
        val business = mockBusiness(id = 2L)
        val page = PageImpl(listOf(business))
        every { businessRepository.findAll(any<Specification<Business>>(), eq(pageable)) } returns page

        val result = service.searchBusinesses("SHELTER", null, pageable)
        assertEquals(1, result.totalElements)
        verify { businessRepository.findAll(any<Specification<Business>>(), eq(pageable)) }
    }

    @Test
    fun `searchBusinesses should filter by municipality code`() {
        val pageable = PageRequest.of(0, 10)
        val business = mockBusiness(id = 3L)
        val page = PageImpl(listOf(business))
        every { businessRepository.findAll(any<Specification<Business>>(), eq(pageable)) } returns page

        service.searchBusinesses(null, "SK-CENTAR", pageable)
        verify { businessRepository.findAll(any<Specification<Business>>(), eq(pageable)) }
    }

    @Test
    fun `searchBusinesses should filter by both type and municipality`() {
        val pageable = PageRequest.of(0, 10)
        val business = mockBusiness(id = 4L)
        val page = PageImpl(listOf(business))
        every { businessRepository.findAll(any<Specification<Business>>(), eq(pageable)) } returns page

        service.searchBusinesses("SHELTER", "SK-CENTAR", pageable)
        verify { businessRepository.findAll(any<Specification<Business>>(), eq(pageable)) }
    }

    @Test
    fun `searchNearby should delegate to repository with location, radius, type and municipality`() {
        val pageable = PageRequest.of(0, 10)
        val business = mockBusiness(id = 7L)
        val page = PageImpl(listOf(business))
        every { businessRepository.findNearby(42.0, 21.4, 10.0, "VET", "SK-CENTAR", pageable) } returns page

        val result = service.searchNearby(
            42.0.toBigDecimal(), 21.4.toBigDecimal(), 10.0, "VET", "SK-CENTAR", pageable
        )

        assertEquals(1, result.totalElements)
        assertEquals(7L, result.content[0].id)
        verify { businessRepository.findNearby(42.0, 21.4, 10.0, "VET", "SK-CENTAR", pageable) }
    }

    @Test
    fun `updateBusiness should update all fields for owner`() {
        val owner = mockUser(id = 1L)
        val business = mockBusiness(id = 10L, owner = owner)
        every { businessRepository.findByIdWithAssociations(10L) } returns business
        every { businessRepository.save(any()) } returns business

        val request = UpdateBusinessRequest(
            name = "New Name",
            description = "New desc",
            phone = "111",
            email = "new@mail",
            address = "New address",
            municipalityCode = "NEW-MUN",
            latitude = 1.0.toBigDecimal(),
            longitude = 2.0.toBigDecimal(),
            typeCode = "VET"
        )
        val newType = mockk<BusinessType>(relaxed = true)
        val newMunicipality = mockk<Municipality>(relaxed = true)
        every { businessTypeRepository.findByCode("VET") } returns newType
        every { municipalityRepository.findByCode("NEW-MUN") } returns newMunicipality

        service.updateBusiness(10L, request, owner)

        verify { business.name = "New Name" }
        verify { business.description = "New desc" }
        verify { business.phone = "111" }
        verify { business.email = "new@mail" }
        verify { business.address = "New address" }
        verify { business.municipality = newMunicipality }
        verify { business.type = newType }
        verify { business.latitude = 1.0.toBigDecimal() }
        verify { business.longitude = 2.0.toBigDecimal() }
        verify { businessRepository.save(business) }
    }

    @Test
    fun `updateBusiness should throw when business not found`() {
        every { businessRepository.findByIdWithAssociations(99L) } returns null
        val result = service.updateBusiness(99L, UpdateBusinessRequest(name = "x"), mockUser())
        assertIs<UpdateBusinessResult.NotFound>(result)
    }

    @Test
    fun `updateBusiness should throw when updating type with non-existent code`() {
        val owner = mockUser()
        val business = mockBusiness(owner = owner)
        every { businessRepository.findByIdWithAssociations(1L) } returns business
        every { businessTypeRepository.findByCode("UNKNOWN") } returns null

        val request = UpdateBusinessRequest(typeCode = "UNKNOWN")
        val result = service.updateBusiness(1L, request, owner)
        assertIs<UpdateBusinessResult.NotFound>(result)
    }

    @Test
    fun `updateBusiness should throw when updating municipality with non-existent code`() {
        val owner = mockUser()
        val business = mockBusiness(owner = owner)
        every { businessRepository.findByIdWithAssociations(1L) } returns business
        every { municipalityRepository.findByCode("BAD") } returns null

        val request = UpdateBusinessRequest(municipalityCode = "BAD")
        val result = service.updateBusiness(1L, request, owner)
        assertIs<UpdateBusinessResult.NotFound>(result)
    }

    @Test
    fun `updateBusiness should throw when non-owner tries to update`() {
        val owner = mockUser(id = 1L)
        val business = mockBusiness(owner = owner)
        every { businessRepository.findByIdWithAssociations(10L) } returns business

        val result = service.updateBusiness(10L, UpdateBusinessRequest(name = "Hacked"), mockUser(id = 2L))
        assertIs<UpdateBusinessResult.Forbidden>(result)
    }

    @Test
    fun `admin can update a business they don't own`() {
        val owner = mockUser(id = 1L)
        val business = mockBusiness(owner = owner)
        every { businessRepository.findByIdWithAssociations(10L) } returns business
        every { businessRepository.save(any()) } returns business

        service.updateBusiness(10L, UpdateBusinessRequest(name = "Admin renamed"), mockUser(id = 99L, admin = true))
        verify { business.name = "Admin renamed" }
    }

    @Test
    fun `deleteBusiness should soft-delete for owner`() {
        val owner = mockUser(id = 1L)
        val business = mockBusiness(owner = owner)
        every { businessRepository.findByIdWithAssociations(10L) } returns business
        every { businessRepository.save(any()) } returns business

        service.deleteBusiness(10L, owner)
        verify { business.deletedAt = any<Instant>() }
        verify { businessRepository.save(business) }
    }


    @Test
    fun `deleteBusiness should throw when business not found`() {
        every { businessRepository.findByIdWithAssociations(99L) } returns null
        val result = service.deleteBusiness(99L, mockUser())
        assertIs<DeleteBusinessResult.NotFound>(result)
    }

    @Test
    fun `deleteBusiness should throw when non-owner tries to delete`() {
        val owner = mockUser(id = 1L)
        val business = mockBusiness(owner = owner)
        every { businessRepository.findByIdWithAssociations(10L) } returns business

        val result = service.deleteBusiness(10L, mockUser(id = 2L))
        assertIs<DeleteBusinessResult.Forbidden>(result)
    }
}