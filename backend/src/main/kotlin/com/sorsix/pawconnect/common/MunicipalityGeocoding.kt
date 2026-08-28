package com.sorsix.pawconnect.common

import com.sorsix.pawconnect.domain.Municipality

fun Municipality.geocodeQuery(): String {
    return listOfNotNull(name, city?.name, city?.country?.name).joinToString(", ")
}
