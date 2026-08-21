package com.sorsix.pawconnect.model

import com.sorsix.pawconnect.model.base.SoftDeletableEntity
import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction
import java.time.Instant

@Entity
@Table(name = "adoption_applications")
@SQLRestriction("deleted_at IS NULL")
class AdoptionApplication(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    var listing: Listing,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    var applicant: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    var status: ApplicationStatus,

    @Column(columnDefinition = "text")
    var message: String? = null,

    @Column(name = "contact_phone")
    var contactPhone: String? = null,

    @Column(name = "contact_email")
    var contactEmail: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    var reviewedBy: User? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,

    ) : SoftDeletableEntity()