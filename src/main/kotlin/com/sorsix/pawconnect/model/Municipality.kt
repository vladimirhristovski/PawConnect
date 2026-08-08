package com.sorsix.pawconnect.model

import jakarta.persistence.*

@Entity
@Table(name = "municipalities")
class Municipality(
    @Column(nullable = false, unique = true)
    val code: String,

    @Column(nullable = false)
    val name: String,

    @Column(nullable = false)
    var city: String = "Skopje",
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}