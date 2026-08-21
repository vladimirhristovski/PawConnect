package com.sorsix.pawconnect.model

import com.sorsix.pawconnect.model.base.SoftDeletableEntity
import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "listings")
@SQLRestriction("deleted_at IS NULL")
class Listing(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    var pet: Pet,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posted_by", nullable = false)
    var postedBy: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "municipality_id", nullable = false)
    var municipality: Municipality,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    var status: ListingStatus,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_id")
    var business: Business? = null,

    var title: String? = null,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(name = "adoption_fee", nullable = false)
    var adoptionFee: BigDecimal = BigDecimal.ZERO,

    var latitude: BigDecimal? = null,

    var longitude: BigDecimal? = null,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    ) : SoftDeletableEntity()