package com.kotlin.assets.repository

import com.kotlin.assets.entity.TotalTaxReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TotalTaxReportRepository : JpaRepository<TotalTaxReport, Long> {
    fun findByYear(year: Short): TotalTaxReport?
}