package com.sorsix.pawconnect.util

import com.sorsix.pawconnect.model.Municipality

fun Municipality.geocodeQuery(): String {
    return listOfNotNull(name, city?.name, city?.country?.name).joinToString(", ")
}
