package com.kotlin.assets.dto.tax.xml

import jakarta.xml.bind.annotation.*
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import java.math.BigDecimal

@XmlRootElement(name = "DECLARBODY")
@XmlAccessorType(XmlAccessType.FIELD)
data class DeclarBody(

    @XmlAttribute(name = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    var type: String = "DECLARBODY_MAIN",

    @XmlElement(name = "H01")
    var h01: String? = null,

    @XmlElement(name = "H03")
    var h03: String? = null,

    @XmlElement(name = "H05")
    var h05: String? = null,

    @XmlElement(name = "HBOS")
    var hbos: String? = null,

    @XmlElement(name = "HCITY")
    var hcity: String? = null,

    @XmlElement(name = "HD1")
    var hd1: String? = null,

    @XmlElement(name = "HFILL")
    var hfill: String? = null,

    @XmlElement(name = "HNAME")
    var hname: String? = null,

    @XmlElement(name = "HSTI")
    var hsti: String? = null,

    @XmlElement(name = "HSTREET")
    var hstreet: String? = null,

    @XmlElement(name = "HTIN")
    var htin: String? = null,

    @XmlElement(name = "HZ")
    var hz: String? = null,

    @XmlElement(name = "HZY")
    var hzy: String? = null,

    @XmlElement(name = "R0104G3")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r0104g3: BigDecimal? = null,

    @XmlElement(name = "R0104G6")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r0104g6: BigDecimal? = null,

    @XmlElement(name = "R0104G7")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r0104g7: BigDecimal? = null,

    @XmlElement(name = "R0108G3")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r0108g3: BigDecimal? = null,

    @XmlElement(name = "R0108G6")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r0108g6: BigDecimal? = null,

    @XmlElement(name = "R0108G7")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r0108g7: BigDecimal? = null,

    @XmlElement(name = "R01010G2S")
    var r01010g2s: String? = null,

    @XmlElement(name = "R010G3")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r010g3: BigDecimal? = null,

    @XmlElement(name = "R010G6")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r010g6: BigDecimal? = null,

    @XmlElement(name = "R010G7")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r010g7: BigDecimal? = null,

    @XmlElement(name = "R012G3")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r012g3: BigDecimal? = null,

    @XmlElement(name = "R013G3")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r013g3: BigDecimal? = null,

    @XmlElement(name = "R018G3")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r018g3: BigDecimal? = null,

    @XmlElement(name = "R0201G3")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r0201g3: BigDecimal? = null,

    @XmlElement(name = "R0211G3")
    @XmlJavaTypeAdapter(BigDecimalAdapter::class)
    var r0211g3: BigDecimal? = null

)
