package com.kotlin.assets.service.client

import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.time.LocalDate

class NbuDataClient(val nbuClient: RestClient) {

    val nbuUrl: String = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchangenew"

    fun getExchangeRate(date: LocalDate): BigDecimal {
        val requestDate: String = date.toString().replace("-", "")
        val exchangeRates = nbuClient.get().uri(
            nbuUrl + "?json"
                    + "&valcode=" + "USD"
                    + "&date=" + requestDate
        )
            .retrieve()
            .body<MutableList<NbuExchangeRate?>?>(object :
                ParameterizedTypeReference<MutableList<NbuExchangeRate?>?>() {
            })

        return BigDecimal(exchangeRates?.first()?.rate)

    }
}

data class NbuExchangeRate(val rate: String)