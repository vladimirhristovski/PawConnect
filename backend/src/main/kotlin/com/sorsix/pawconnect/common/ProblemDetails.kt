package com.sorsix.pawconnect.common

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity

fun problemResponse(status: HttpStatus, detail: String): ResponseEntity<ProblemDetail> =
    ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail))
