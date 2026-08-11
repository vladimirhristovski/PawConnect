package com.sorsix.pawconnect.model

import jakarta.persistence.*

@Entity
@Table(name = "roles")
class Role(
    @Column(nullable = false, unique = true)
    val name: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}