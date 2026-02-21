package com.kotlin.assets.repository

import com.kotlin.assets.entity.SolarReport
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SolarRepository : JpaRepository<SolarReport, Long> {
}