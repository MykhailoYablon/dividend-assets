package com.kotlin.assets.repository

import com.kotlin.assets.entity.tax.TotalStockReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TotalStockReportRepository : JpaRepository<TotalStockReport, Long>{
    fun findByYear(year: Short): Optional<TotalStockReport>
}