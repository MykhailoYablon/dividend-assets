package com.kotlin.assets.dto.tax.xml

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlValue

@XmlAccessorType(XmlAccessType.FIELD)
data class RowNumField(

    @XmlAttribute(name = "ROWNUM")
    var rowNum: String? = null,

    @XmlValue
    var value: String? = null

)