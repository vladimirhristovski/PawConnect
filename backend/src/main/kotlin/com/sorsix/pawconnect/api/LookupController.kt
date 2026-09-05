package com.sorsix.pawconnect.api

import com.sorsix.pawconnect.dto.response.*
import com.sorsix.pawconnect.service.LookupService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/lookups")
class LookupController(
    private val lookupService: LookupService,
) {
    @GetMapping("/species")
    fun getSpecies() = lookupService.getAllSpecies().map { SpeciesResponse.from(it) }

    @GetMapping("/breeds")
    fun getBreeds(
        @RequestParam(required = false) speciesCode: String?,
    ) = lookupService.getBreeds(speciesCode).map { BreedResponse.from(it) }

    @GetMapping("/business-types")
    fun getBusinessTypes() = lookupService.getAllBusinessTypes().map { BusinessTypeResponse.from(it) }

    @GetMapping("/countries")
    fun getCountries() = lookupService.getAllCountries().map { CountryResponse.from(it) }

    @GetMapping("/cities")
    fun getCities(
        @RequestParam(required = false) countryCode: String?,
    ) = lookupService.getCities(countryCode).map { CityResponse.from(it) }

    @GetMapping("/municipalities")
    fun getMunicipalities(
        @RequestParam(required = false) cityCode: String?,
    ) = lookupService.getMunicipalities(cityCode).map { MunicipalityResponse.from(it) }

    @GetMapping("/listing-statuses")
    fun getListingStatuses() = lookupService.getAllListingStatuses().map { ListingStatusResponse.from(it) }

    @GetMapping("/application-statuses")
    fun getApplicationStatuses() = lookupService.getAllApplicationStatuses().map { ApplicationStatusResponse.from(it) }

    @GetMapping("/roles")
    fun getRoles() = lookupService.getAllRoles().map { it.name }
}
