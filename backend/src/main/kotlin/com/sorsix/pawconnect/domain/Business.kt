package com.sorsix.pawconnect.domain

import com.sorsix.pawconnect.domain.base.SoftDeletableEntity
import jakarta.persistence.*
import java.math.BigDecimal
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "businesses")
@SQLRestriction("deleted_at IS NULL")
class Business(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    var type: BusinessType,

    @Column(nullable = false)
    var name: String,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(nullable = false)
    var phone: String,

    var email: String? = null,

    @Column(nullable = false)
    var address: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "municipality_id", nullable = false)
    var municipality: Municipality,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    var owner: User? = null,

    var latitude: BigDecimal? = null,

    var longitude: BigDecimal? = null,

    @Column(name = "address_geocoded", nullable = false)
    var addressGeocoded: Boolean = false,

    ) : SoftDeletableEntity()