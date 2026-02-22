package com.kotlin.assets.repository

import com.kotlin.assets.entity.SolarFileReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SolarFileReportRepository : JpaRepository<SolarFileReport, Long>