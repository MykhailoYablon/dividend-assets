package com.kotlin.assets.service

import com.kotlin.assets.entity.ExchangeRate
import com.kotlin.assets.repository.ExchangeRateRepository
import com.kotlin.assets.service.client.NbuDataClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class ExchangeRateService(
    val repository: ExchangeRateRepository,
    private val nbuDataClient: NbuDataClient
) {
    @Transactional
    fun getRateForDate(date: LocalDate): BigDecimal {
        val exchangeRate = repository.findByDate(date) ?: fetchAndSaveRate(date)
        return exchangeRate.nbuRate
    }

    private fun fetchAndSaveRate(date: LocalDate): ExchangeRate {
        val exchangeRate: BigDecimal = nbuDataClient.getExchangeRate(date)
        return saveExchangeRate(date, exchangeRate)
    }

    private fun saveExchangeRate(
        date: LocalDate,
        exchangeRate: BigDecimal
    ): ExchangeRate {
        return repository.save(ExchangeRate(date = date, nbuRate = exchangeRate))
    }
}
