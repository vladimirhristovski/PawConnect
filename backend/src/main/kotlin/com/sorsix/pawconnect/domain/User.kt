package com.sorsix.pawconnect.domain

import com.sorsix.pawconnect.domain.base.SoftDeletableEntity
import jakarta.persistence.*
import org.hibernate.annotations.SQLRestriction

@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
class User(
    @Column(nullable = false, unique = true)
    var username: String,

    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var password: String,

    @Column(name = "first_name")
    var firstName: String? = null,

    @Column(name = "last_name")
    var lastName: String? = null,

    var phone: String? = null,

    ) : SoftDeletableEntity() {

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    var roles: MutableSet<Role> = mutableSetOf()

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    fun hasRole(roleName: String): Boolean = roles.any { it.name.equals(roleName, ignoreCase = true) }
    fun isAdmin(): Boolean = hasRole("ADMIN")

}