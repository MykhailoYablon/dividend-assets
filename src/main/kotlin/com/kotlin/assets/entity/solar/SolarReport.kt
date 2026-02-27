package com.kotlin.assets.entity.solar

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(
    name = "solar_report",
    uniqueConstraints = [UniqueConstraint(columnNames = ["date"])]
)
class SolarReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solar_file_report_id", nullable = false)
    var solarFileReport: SolarFileReport? = null,

    @Column(name = "date")
    var date: LocalDate = LocalDate.now(),

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal = BigDecimal.ZERO,

    var year: Int = 0,

    var exchangeRate: BigDecimal = BigDecimal.ZERO,

    var usdValue: BigDecimal = BigDecimal.ZERO
)