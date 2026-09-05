package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.domain.Business
import com.sorsix.pawconnect.domain.Listing
import com.sorsix.pawconnect.domain.Municipality
import com.sorsix.pawconnect.repository.BusinessRepository
import com.sorsix.pawconnect.repository.ListingRepository
import com.sorsix.pawconnect.repository.MunicipalityRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class GeocodingRetryJobTest {
    private val municipalityRepository = mockk<MunicipalityRepository>()
    private val businessRepository = mockk<BusinessRepository>()
    private val listingRepository = mockk<ListingRepository>()
    private val geocodingService = mockk<GeocodingService>()
    private lateinit var job: GeocodingRetryJob

    @BeforeEach
    fun setup() {
        job = GeocodingRetryJob(municipalityRepository, businessRepository, listingRepository, geocodingService)
        every { businessRepository.findByAddressGeocodedFalseAndDeletedAtIsNull() } returns emptyList()
        every { listingRepository.findByLatitudeIsNullAndDeletedAtIsNull() } returns emptyList()
        every { municipalityRepository.findByLatitudeIsNull() } returns emptyList()
    }

    private fun mockMunicipality(): Municipality {
        val municipality = mockk<Municipality>(relaxed = true)
        every { municipality.latitude = any() } just runs
        every { municipality.longitude = any() } just runs
        return municipality
    }

    private fun mockBusiness(municipality: Municipality = mockMunicipality()): Business {
        val business = mockk<Business>(relaxed = true)
        every { business.municipality } returns municipality
        every { business.latitude = any() } just runs
        every { business.longitude = any() } just runs
        every { business.addressGeocoded = any() } just runs
        return business
    }

    private fun mockListing(municipality: Municipality): Listing {
        val listing = mockk<Listing>(relaxed = true)
        every { listing.municipality } returns municipality
        every { listing.latitude = any() } just runs
        every { listing.longitude = any() } just runs
        return listing
    }

    @Test
    fun `resolves coordinates for municipalities missing them`() {
        val municipality = mockMunicipality()
        every { municipalityRepository.findByLatitudeIsNull() } returns listOf(municipality)
        every { geocodingService.geocode(any()) } returns GeoCoordinates(BigDecimal("42.0"), BigDecimal("21.4"))
        every { municipalityRepository.save(any()) } returns municipality

        job.retryMissingCoordinates()

        verify { municipality.latitude = BigDecimal("42.0") }
        verify { municipality.longitude = BigDecimal("21.4") }
        verify { municipalityRepository.save(municipality) }
    }

    @Test
    fun `leaves municipality untouched when geocoding fails`() {
        val municipality = mockMunicipality()
        every { municipalityRepository.findByLatitudeIsNull() } returns listOf(municipality)
        every { geocodingService.geocode(any()) } returns null

        job.retryMissingCoordinates()

        verify(exactly = 0) { municipalityRepository.save(any()) }
    }

    @Test
    fun `upgrades business to precise coordinates when geocoding succeeds`() {
        val business = mockBusiness()
        every { businessRepository.findByAddressGeocodedFalseAndDeletedAtIsNull() } returns listOf(business)
        every { geocodingService.geocode(any()) } returns GeoCoordinates(BigDecimal("42.1"), BigDecimal("21.5"))
        every { businessRepository.save(any()) } returns business

        job.retryMissingCoordinates()

        verify { business.latitude = BigDecimal("42.1") }
        verify { business.longitude = BigDecimal("21.5") }
        verify { business.addressGeocoded = true }
        verify { businessRepository.save(business) }
    }

    @Test
    fun `falls back to municipality coordinates when business geocoding fails but municipality is resolved`() {
        val municipality = mockMunicipality()
        every { municipality.latitude } returns BigDecimal("42.2")
        every { municipality.longitude } returns BigDecimal("21.6")
        val business = mockBusiness(municipality)
        every { business.latitude } returns null
        every { businessRepository.findByAddressGeocodedFalseAndDeletedAtIsNull() } returns listOf(business)
        every { geocodingService.geocode(any()) } returns null
        every { businessRepository.save(any()) } returns business

        job.retryMissingCoordinates()

        verify { business.latitude = BigDecimal("42.2") }
        verify { business.longitude = BigDecimal("21.6") }
        verify(exactly = 0) { business.addressGeocoded = true }
        verify { businessRepository.save(business) }
    }

    @Test
    fun `leaves business untouched when geocoding fails and municipality has no coordinates either`() {
        val municipality = mockMunicipality()
        every { municipality.latitude } returns null
        val business = mockBusiness(municipality)
        every { business.latitude } returns null
        every { businessRepository.findByAddressGeocodedFalseAndDeletedAtIsNull() } returns listOf(business)
        every { geocodingService.geocode(any()) } returns null

        job.retryMissingCoordinates()

        verify(exactly = 0) { businessRepository.save(any()) }
    }

    @Test
    fun `syncs listing coordinates from its municipality once resolved`() {
        val municipality = mockMunicipality()
        every { municipality.latitude } returns BigDecimal("42.3")
        every { municipality.longitude } returns BigDecimal("21.7")
        val listing = mockListing(municipality)
        every { listingRepository.findByLatitudeIsNullAndDeletedAtIsNull() } returns listOf(listing)
        every { listingRepository.save(any()) } returns listing

        job.retryMissingCoordinates()

        verify { listing.latitude = BigDecimal("42.3") }
        verify { listing.longitude = BigDecimal("21.7") }
        verify { listingRepository.save(listing) }
    }

    @Test
    fun `does not sync listing when its municipality still has no coordinates`() {
        val municipality = mockMunicipality()
        every { municipality.latitude } returns null
        val listing = mockListing(municipality)
        every { listingRepository.findByLatitudeIsNullAndDeletedAtIsNull() } returns listOf(listing)

        job.retryMissingCoordinates()

        verify(exactly = 0) { listingRepository.save(any()) }
    }
}
