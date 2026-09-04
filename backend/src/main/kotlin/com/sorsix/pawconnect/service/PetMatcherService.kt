package com.sorsix.pawconnect.service

import com.sorsix.pawconnect.domain.result.PetMatcherResult
import com.sorsix.pawconnect.dto.request.PetMatcherRequest
import com.sorsix.pawconnect.dto.response.PetMatcherResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.body

@Service
class PetMatcherService(
    @Qualifier("petMatcherRestClient") private val petMatcherRestClient: RestClient,
) {
    private val logger = LoggerFactory.getLogger(PetMatcherService::class.java)

    fun recommend(request: PetMatcherRequest): PetMatcherResult {
        return try {
            val response =
                petMatcherRestClient.post().uri("/recommend").body(request).retrieve().body<PetMatcherResponse>()

            if (response != null) {
                PetMatcherResult.Success(response)
            } else {
                logger.warn("Pet matcher service returned an empty response body")
                PetMatcherResult.ServiceUnavailable("Pet matcher service returned an empty response")
            }
        } catch (ex: RestClientException) {
            logger.error("Call to pet-matcher service failed", ex)
            PetMatcherResult.ServiceUnavailable("Pet matcher service is currently unavailable")
        }
    }
}