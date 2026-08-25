package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.model.PetSpecies
import com.sorsix.pawconnect.repository.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LookupServiceTest {

    private val speciesRepository = mockk<PetSpeciesRepository>()
    private val breedRepository = mockk<PetBreedRepository>()
    private val businessTypeRepository = mockk<BusinessTypeRepository>(relaxed = true)
    private val countryRepository = mockk<CountryRepository>(relaxed = true)
    private val cityRepository = mockk<CityRepository>(relaxed = true)
    private val municipalityRepository = mockk<MunicipalityRepository>(relaxed = true)

    private val service = LookupService(
        speciesRepository, breedRepository, businessTypeRepository,
        countryRepository, cityRepository, municipalityRepository
    )

    @Test
    fun `getAllSpecies delegates to the repository`() {
        val species = listOf(mockk<PetSpecies>(relaxed = true))
        every { speciesRepository.findAll() } returns species

        val result = service.getAllSpecies()

        assertEquals(species, result)
    }

    @Test
    fun `getBreeds passes the species filter through unchanged`() {
        every { breedRepository.findWithSpeciesBySpeciesCode("DOG") } returns emptyList()

        service.getBreeds("DOG")

        verify { breedRepository.findWithSpeciesBySpeciesCode("DOG") }
    }

    @Test
    fun `getBreeds with null species code returns all breeds`() {
        every { breedRepository.findWithSpeciesBySpeciesCode(null) } returns emptyList()

        service.getBreeds(null)

        verify { breedRepository.findWithSpeciesBySpeciesCode(null) }
    }

    @Test
    fun `getMunicipalities passes the city filter through unchanged`() {
        every { municipalityRepository.findWithCityByCityCode("SK") } returns emptyList()

        service.getMunicipalities("SK")

        verify { municipalityRepository.findWithCityByCityCode("SK") }
    }
}