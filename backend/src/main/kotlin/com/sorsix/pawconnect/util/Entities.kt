package com.sorsix.pawconnect.util

import com.sorsix.pawconnect.model.base.BaseEntity

fun BaseEntity.requireId(): Long =
    id ?: throw IllegalStateException("${javaClass.simpleName} has not been persisted yet")
