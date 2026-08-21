package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.model.BusinessType
import com.sorsix.pawconnect.model.City
import com.sorsix.pawconnect.model.Country
import com.sorsix.pawconnect.model.Municipality
import com.sorsix.pawconnect.model.PetBreed
import com.sorsix.pawconnect.model.PetSpecies
import com.sorsix.pawconnect.repository.BusinessTypeRepository
import com.sorsix.pawconnect.repository.CityRepository
import com.sorsix.pawconnect.repository.CountryRepository
import com.sorsix.pawconnect.repository.MunicipalityRepository
import com.sorsix.pawconnect.repository.PetBreedRepository
import com.sorsix.pawconnect.repository.PetSpeciesRepository
import org.springframework.stereotype.Service

@Service
class LookupService(
    private val speciesRepository: PetSpeciesRepository,
    private val breedRepository: PetBreedRepository,
    private val businessTypeRepository: BusinessTypeRepository,
    private val countryRepository: CountryRepository,
    private val cityRepository: CityRepository,
    private val municipalityRepository: MunicipalityRepository
) {

    fun getAllSpecies(): List<PetSpecies> = speciesRepository.findAll()

    fun getBreeds(speciesCode: String?): List<PetBreed> = breedRepository.findWithSpeciesBySpeciesCode(speciesCode)

    fun getAllBusinessTypes(): List<BusinessType> = businessTypeRepository.findAll()

    fun getAllCountries(): List<Country> = countryRepository.findAll()

    fun getCities(countryCode: String?): List<City> = cityRepository.findWithCountryByCountryCode(countryCode)

    fun getMunicipalities(cityCode: String?): List<Municipality> =
        municipalityRepository.findWithCityByCityCode(cityCode)
}