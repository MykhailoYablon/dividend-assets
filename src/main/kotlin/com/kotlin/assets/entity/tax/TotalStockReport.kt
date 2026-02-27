package com.kotlin.assets.entity.tax

import com.fasterxml.jackson.annotation.JsonManagedReference
import com.kotlin.assets.dto.enums.ReportStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.function.Consumer

@Entity
class TotalStockReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var year: Short,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReportStatus,

    @Column(name = "total_profit", nullable = false, precision = 19, scale = 2)
    var totalUaBrutto: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_ua_brutto", nullable = false, precision = 19, scale = 2)
    var totalUaNetto: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_tax_9", nullable = false, precision = 19, scale = 2)
    var totalTax18: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_military_tax_5", nullable = false, precision = 19, scale = 2)
    var totalMilitaryTax5: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_tax_sum", nullable = false, precision = 19, scale = 2)
    var totalTaxSum: BigDecimal = BigDecimal.ZERO,

    @OneToMany(mappedBy = "totalTaxReport", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    var reports: MutableList<StockTradeReport> = mutableListOf()

) {
    fun setTaxReports(reports: MutableList<StockTradeReport>) {
        this.reports.clear()
        reports.forEach(Consumer { report: StockTradeReport -> this.addTaxReport(report) }) // Add new ones
    }

    private fun addTaxReport(report: StockTradeReport) {
        reports.add(report)
        report.totalStockReport = this
    }
}
