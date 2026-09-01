package com.sorsix.pawconnect.common

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.net.URI

fun problemResponse(
    status: HttpStatus,
    detail: String,
    properties: Map<String, Any?> = emptyMap()
): ResponseEntity<ProblemDetail> {
    val pd = ProblemDetail.forStatusAndDetail(status, detail)
    pd.type = URI.create("about:blank")
    val requestUri = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request?.requestURI
    if (requestUri != null) {
        pd.instance = URI.create("uri=$requestUri")
    }
    properties.forEach { (key, value) -> pd.setProperty(key, value) }
    return ResponseEntity.status(status).body(pd)
}
