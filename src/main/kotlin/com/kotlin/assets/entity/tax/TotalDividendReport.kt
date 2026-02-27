package com.kotlin.assets.entity.tax

import com.fasterxml.jackson.annotation.JsonManagedReference
import com.kotlin.assets.dto.enums.ReportStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.util.function.Consumer

@Entity
class TotalDividendReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true)
    var year: Short,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReportStatus,

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    var totalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_ua_brutto", nullable = false, precision = 19, scale = 2)
    var totalUaBrutto: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_tax_9", nullable = false, precision = 19, scale = 2)
    var totalTax9: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_military_tax_5", nullable = false, precision = 19, scale = 2)
    var totalMilitaryTax5: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_tax_sum", nullable = false, precision = 19, scale = 2)
    var totalTaxSum: BigDecimal = BigDecimal.ZERO,

    @OneToMany(mappedBy = "totalTaxReport", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    var reports: MutableList<DividendTaxReport> = mutableListOf()

) {
    fun setTaxReports(reports: MutableList<DividendTaxReport>) {
        this.reports.clear()
        reports.forEach(Consumer { report: DividendTaxReport -> this.addTaxReport(report) }) // Add new ones
    }

    private fun addTaxReport(report: DividendTaxReport) {
        reports.add(report)
        report.totalDividendReport = this
    }
}
