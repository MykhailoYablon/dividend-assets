package com.kotlin.assets.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "dpi")
data class DpiEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "code", nullable = true)
    val code: Int?,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "region", nullable = false)
    val region: String
)