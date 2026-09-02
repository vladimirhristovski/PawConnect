package com.sorsix.pawconnect.common

import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException

fun DataIntegrityViolationException.constraintName(): String? =
    (cause as? ConstraintViolationException)?.constraintName
