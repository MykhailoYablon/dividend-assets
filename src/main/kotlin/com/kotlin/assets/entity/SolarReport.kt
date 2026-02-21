package com.kotlin.assets.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigDecimal
import java.time.LocalDate

@Entity
class SolarReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "date")
    var date: LocalDate = LocalDate.now(),

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,

    var year: Int = 0,

    var exchangeRate: BigDecimal = BigDecimal.ZERO,

    var usdValue: BigDecimal = BigDecimal.ZERO
)