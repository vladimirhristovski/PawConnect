package com.sorsix.pawconnect.api

import com.sorsix.pawconnect.common.problemResponse
import com.sorsix.pawconnect.domain.result.PetMatcherResult
import com.sorsix.pawconnect.dto.request.PetMatcherRequest
import com.sorsix.pawconnect.service.PetMatcherService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/pet-matcher")
class PetMatcherController(
    private val petMatcherService: PetMatcherService,
) {

    @PostMapping("/recommend")
    fun recommend(@Valid @RequestBody request: PetMatcherRequest): ResponseEntity<*> {
        return when (val result = petMatcherService.recommend(request)) {
            is PetMatcherResult.Success -> ResponseEntity.ok(result.response)
            is PetMatcherResult.ServiceUnavailable -> problemResponse(HttpStatus.SERVICE_UNAVAILABLE, result.message)
        }
    }
}