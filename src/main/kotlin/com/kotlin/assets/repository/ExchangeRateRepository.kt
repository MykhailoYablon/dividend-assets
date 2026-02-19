package com.kotlin.assets.repository

import com.kotlin.assets.entity.ExchangeRate
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ExchangeRateRepository : JpaRepository<ExchangeRate, Long> {
    fun findByDate(date: LocalDate): ExchangeRate?
}