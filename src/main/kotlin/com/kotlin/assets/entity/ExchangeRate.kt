package com.kotlin.assets.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
class ExchangeRate(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false, unique = true)
    var date: LocalDate,

    @Column(name = "nbu_rate", nullable = false, precision = 10, scale = 4)
    var nbuRate: BigDecimal,

    @Column(nullable = false)
    var currency: String = "USD"
)