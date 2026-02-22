package com.kotlin.assets.entity

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.Instant

@Entity
class SolarFileReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long? = null,

    var fileName: String,

    @OneToMany(mappedBy = "solarFileReport", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    var reports: MutableList<SolarReport> = mutableListOf(),

    @CreationTimestamp
    var createdAt: Instant = Instant.now()
)