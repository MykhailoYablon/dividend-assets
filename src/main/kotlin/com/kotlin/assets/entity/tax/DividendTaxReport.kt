package com.kotlin.assets.entity.tax

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate

@Entity
class DividendTaxReport(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "total_dividend_report_id", nullable = false)
    var totalDividendReport: TotalDividendReport? = null,

    @Column(nullable = false)
    var symbol: String,

    @Column(nullable = false)
    var date: LocalDate,

    @Column(nullable = false, precision = 10, scale = 4)
    var amount: BigDecimal,

    @Column(name = "nbu_rate", nullable = false, precision = 10, scale = 4)
    var nbuRate: BigDecimal,

    @Column(name = "ua_brutto", nullable = false, precision = 10, scale = 4)
    var uaBrutto: BigDecimal,

    @Column(name = "tax_9", nullable = false, precision = 10, scale = 4)
    var tax9: BigDecimal,

    @Column(name = "military_tax_5", nullable = false, precision = 10, scale = 4)
    var militaryTax5: BigDecimal,

    @Column(name = "tax_sum", nullable = false, precision = 10, scale = 4)
    var taxSum: BigDecimal
)
