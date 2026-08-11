package com.sorsix.pawconnect.model

import jakarta.persistence.*
import java.time.LocalTime

@Entity
@Table(name = "business_hours")
class BusinessHours(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    var business: Business,

    @Column(name = "day_of_week", nullable = false)
    var dayOfWeek: Short,

    @Column(name = "open_time")
    var openTime: LocalTime? = null,

    @Column(name = "close_time")
    var closeTime: LocalTime? = null,

    @Column(nullable = false)
    var closed: Boolean = false,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}