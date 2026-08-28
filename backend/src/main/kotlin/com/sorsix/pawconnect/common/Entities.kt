package com.sorsix.pawconnect.common

import com.sorsix.pawconnect.domain.base.BaseEntity

fun BaseEntity.requireId(): Long =
    id ?: throw IllegalStateException("${javaClass.simpleName} has not been persisted yet")
