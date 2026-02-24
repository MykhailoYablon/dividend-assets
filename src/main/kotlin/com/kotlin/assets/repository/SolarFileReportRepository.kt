package com.kotlin.assets.repository

import com.kotlin.assets.entity.SolarFileReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SolarFileReportRepository : JpaRepository<SolarFileReport, Long> {
    fun findFirstByUserIdOrderByCreatedAtDesc(userId: Long): Optional<SolarFileReport>
}