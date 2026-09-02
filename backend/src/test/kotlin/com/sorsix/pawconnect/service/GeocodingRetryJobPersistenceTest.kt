package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.TestcontainersConfiguration
import com.sorsix.pawconnect.common.requireId
import com.sorsix.pawconnect.domain.Business
import com.sorsix.pawconnect.domain.Municipality
import com.sorsix.pawconnect.repository.BusinessRepository
import com.sorsix.pawconnect.repository.BusinessTypeRepository
import com.sorsix.pawconnect.repository.CityRepository
import com.sorsix.pawconnect.repository.MunicipalityRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class)
class GeocodingRetryJobPersistenceTest {

    @Autowired
    private lateinit var job: GeocodingRetryJob

    @Autowired
    private lateinit var municipalityRepository: MunicipalityRepository

    @Autowired
    private lateinit var cityRepository: CityRepository

    @Autowired
    private lateinit var businessRepository: BusinessRepository

    @Autowired
    private lateinit var businessTypeRepository: BusinessTypeRepository

    @MockitoBean
    private lateinit var geocodingService: GeocodingService

    @MockitoBean
    private lateinit var emailService: EmailService

    @BeforeEach
    fun setup() {
        whenever(geocodingService.geocode(any())).thenReturn(null)
    }

    @Test
    fun `resolves a municipality's lazy city and country chain, then a dependent business falls back to it`() {
        val skopje = cityRepository.findByCode("SK")!!
        val municipality = municipalityRepository.save(
            Municipality(code = "SK-TEST-${System.currentTimeMillis()}", name = "Test Municipality", city = skopje)
        )
        val vet = businessTypeRepository.findByCode("VET")!!
        val business = businessRepository.save(
            Business(
                type = vet,
                name = "Test Vet",
                phone = "070000000",
                address = "some unresolvable address",
                municipality = municipality
            )
        )

        val coordinates = GeoCoordinates(BigDecimal("41.998100"), BigDecimal("21.425400"))
        whenever(geocodingService.geocode(argThat { startsWith(municipality.name) })).thenReturn(coordinates)

        job.retryMissingCoordinates()

        val reloadedMunicipality = municipalityRepository.findById(municipality.requireId()).orElseThrow()
        assertEquals(coordinates.latitude, reloadedMunicipality.latitude)
        assertEquals(coordinates.longitude, reloadedMunicipality.longitude)

        val reloadedBusiness = businessRepository.findById(business.requireId()).orElseThrow()
        assertEquals(coordinates.latitude, reloadedBusiness.latitude)
        assertEquals(coordinates.longitude, reloadedBusiness.longitude)
        assertFalse(reloadedBusiness.addressGeocoded)
    }
}
