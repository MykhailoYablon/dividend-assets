package com.kotlin.assets.repository

import com.kotlin.assets.entity.solar.SolarReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
interface SolarRepository : JpaRepository<SolarReport, Long> {

    @Modifying
    @Query(
        value = """
            INSERT INTO solar_report (date, amount, solar_file_report_id, year, exchange_rate, usd_value)
            VALUES (:date, :amount, :solarFileReportId, :year, :exchangeRate, :usdValue)
            ON CONFLICT (date) 
            DO UPDATE SET 
                amount = EXCLUDED.amount,
                year = EXCLUDED.year,
                exchange_rate = EXCLUDED.exchange_rate,
                usd_value = EXCLUDED.usd_value,
                solar_file_report_id = EXCLUDED.solar_file_report_id
        """, nativeQuery = true
    )
    fun upsert(
        @Param("date") date: LocalDate,
        @Param("amount") amount: BigDecimal,
        @Param("solarFileReportId") solarFileReportId: Long,
        @Param("year") year: Int,
        @Param("exchangeRate") exchangeRate: BigDecimal,
        @Param("usdValue") usdValue: BigDecimal
    ) : Long

    fun findAllBySolarFileReportId(fileId: Long?): List<SolarReport>
}