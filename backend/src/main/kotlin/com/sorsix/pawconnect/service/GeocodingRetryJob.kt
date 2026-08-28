package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.repository.BusinessRepository
import com.sorsix.pawconnect.repository.ListingRepository
import com.sorsix.pawconnect.repository.MunicipalityRepository
import com.sorsix.pawconnect.common.geocodeQuery
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GeocodingRetryJob(
    private val municipalityRepository: MunicipalityRepository,
    private val businessRepository: BusinessRepository,
    private val listingRepository: ListingRepository,
    private val geocodingService: GeocodingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${app.geocoding.retry-interval}")
    @Transactional
    @CacheEvict("municipalities", allEntries = true)
    fun retryMissingCoordinates() {
        val municipalitiesResolved = backfillMunicipalities()
        val businessesUpgraded = upgradeBusinessAddresses()
        val listingsSynced = syncListingsFromMunicipality()

        if (municipalitiesResolved > 0 || businessesUpgraded > 0 || listingsSynced > 0) {
            log.info(
                "Geocoding retry: {} municipality(ies) resolved, {} business(es) upgraded to precise address, {} listing(s) synced from municipality",
                municipalitiesResolved, businessesUpgraded, listingsSynced
            )
        }
    }

    private fun backfillMunicipalities(): Int {
        val municipalities = municipalityRepository.findByLatitudeIsNull()
        var resolved = 0
        for (municipality in municipalities) {
            val coordinates = runCatching { geocodingService.geocode(municipality.geocodeQuery()) }.getOrNull()
            if (coordinates != null) {
                municipality.latitude = coordinates.latitude
                municipality.longitude = coordinates.longitude
                municipalityRepository.save(municipality)
                resolved++
            }
            Thread.sleep(NOMINATIM_REQUEST_DELAY_MS)
        }
        return resolved
    }

    private fun upgradeBusinessAddresses(): Int {
        val businesses = businessRepository.findByAddressGeocodedFalseAndDeletedAtIsNull()
        var upgraded = 0
        for (business in businesses) {
            val coordinates = runCatching { geocodingService.geocode(business.address) }.getOrNull()
            if (coordinates != null) {
                business.latitude = coordinates.latitude
                business.longitude = coordinates.longitude
                business.addressGeocoded = true
                businessRepository.save(business)
                upgraded++
            } else if (business.latitude == null && business.municipality.latitude != null) {
                business.latitude = business.municipality.latitude
                business.longitude = business.municipality.longitude
                businessRepository.save(business)
            }
            Thread.sleep(NOMINATIM_REQUEST_DELAY_MS)
        }
        return upgraded
    }

    private fun syncListingsFromMunicipality(): Int {
        val listings = listingRepository.findByLatitudeIsNullAndDeletedAtIsNull()
        var synced = 0
        for (listing in listings) {
            val municipality = listing.municipality
            if (municipality.latitude != null && municipality.longitude != null) {
                listing.latitude = municipality.latitude
                listing.longitude = municipality.longitude
                listingRepository.save(listing)
                synced++
            }
        }
        return synced
    }

    private companion object {
        const val NOMINATIM_REQUEST_DELAY_MS = 1100L
    }
}
