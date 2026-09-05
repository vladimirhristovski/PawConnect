package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.domain.*
import com.sorsix.pawconnect.repository.*
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class LookupService(
    private val speciesRepository: PetSpeciesRepository,
    private val breedRepository: PetBreedRepository,
    private val businessTypeRepository: BusinessTypeRepository,
    private val countryRepository: CountryRepository,
    private val cityRepository: CityRepository,
    private val municipalityRepository: MunicipalityRepository,
    private val listingStatusRepository: ListingStatusRepository,
    private val applicationStatusRepository: ApplicationStatusRepository,
    private val roleRepository: RoleRepository,
) {
    @Cacheable("species")
    fun getAllSpecies(): List<PetSpecies> = speciesRepository.findAll()

    @Cacheable("breeds")
    fun getBreeds(speciesCode: String?): List<PetBreed> = breedRepository.findWithSpeciesBySpeciesCode(speciesCode)

    @Cacheable("businessTypes")
    fun getAllBusinessTypes(): List<BusinessType> = businessTypeRepository.findAll()

    @Cacheable("countries")
    fun getAllCountries(): List<Country> = countryRepository.findAll()

    @Cacheable("cities")
    fun getCities(countryCode: String?): List<City> = cityRepository.findWithCountryByCountryCode(countryCode)

    @Cacheable("municipalities")
    fun getMunicipalities(cityCode: String?): List<Municipality> = municipalityRepository.findWithCityByCityCode(cityCode)

    @Cacheable("listingStatuses")
    fun getAllListingStatuses(): List<ListingStatus> = listingStatusRepository.findAll()

    @Cacheable("applicationStatuses")
    fun getAllApplicationStatuses(): List<ApplicationStatus> = applicationStatusRepository.findAll()

    @Cacheable("roles")
    fun getAllRoles(): List<Role> = roleRepository.findAll()
}
