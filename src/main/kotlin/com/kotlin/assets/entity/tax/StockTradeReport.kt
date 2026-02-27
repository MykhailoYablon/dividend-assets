package com.kotlin.assets.entity.tax

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.math.BigDecimal
import java.time.LocalDate

@Entity
class StockTradeReport(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "total_stock_report_id", nullable = false)
    var totalStockReport: TotalStockReport? = null,

    @Column(nullable = false)
    var symbol: String,

    @Column(nullable = false)
    var sellQuantity: Int,

    @Column(nullable = false, precision = 10, scale = 4)
    var buyPriceUah: BigDecimal,

    @Column(nullable = false, precision = 10, scale = 4)
    var sellPriceUah: BigDecimal,

    @Column(nullable = false, precision = 10, scale = 4)
    var originalBuyPriceUsd: BigDecimal,

    var originalPL: BigDecimal,

    var ibCommission: BigDecimal,

    @Column(nullable = false)
    var buyDate: LocalDate,

    @Column(nullable = false)
    var sellDate: LocalDate,

    @Column(name = "buy_rate", nullable = false, precision = 10, scale = 4)
    var buyExchangeRate: BigDecimal,

    @Column(name = "sell_rate", nullable = false, precision = 10, scale = 4)
    var sellExchangeRate: BigDecimal,

    var netProfitUah: BigDecimal,

    var tax18: BigDecimal,

    var militaryTax5: BigDecimal
)