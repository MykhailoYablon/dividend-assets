package com.kotlin.assets.repository

import com.kotlin.assets.entity.tax.TotalDividendReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TotalDividendReportRepository : JpaRepository<TotalDividendReport, Long> {
    fun findByYear(year: Short): Optional<TotalDividendReport>
}