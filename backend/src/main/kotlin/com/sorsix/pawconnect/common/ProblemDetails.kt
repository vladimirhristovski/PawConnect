package com.sorsix.pawconnect.common

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.net.URI

fun problemResponse(status: HttpStatus, detail: String): ResponseEntity<ProblemDetail> {
    val pd = ProblemDetail.forStatusAndDetail(status, detail)
    val requestUri = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request?.requestURI
    if (requestUri != null) {
        pd.instance = URI.create("uri=$requestUri")
    }
    return ResponseEntity.status(status).body(pd)
}
