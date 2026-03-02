package com.kotlin.assets.dto.tax.xml

import jakarta.xml.bind.annotation.adapters.XmlAdapter
import java.math.BigDecimal

open class BigDecimalAdapter : XmlAdapter<String, BigDecimal>() {
    override fun unmarshal(v: String?): BigDecimal? {
        return if (v == null) null else BigDecimal(v);
    }

    override fun marshal(v: BigDecimal?): String? {
        return v?.toPlainString()
    }
}