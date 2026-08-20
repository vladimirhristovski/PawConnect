package com.sorsix.pawconnect

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
    fromApplication<PawConnectBackendApplication>().with(TestcontainersConfiguration::class).run(*args)
}
