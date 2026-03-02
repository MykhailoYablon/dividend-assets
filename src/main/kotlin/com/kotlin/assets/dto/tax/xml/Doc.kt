package com.kotlin.assets.dto.tax.xml

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class Doc(

    @XmlAttribute(name = "NUM")
    var num: String? = null,

    @XmlAttribute(name = "TYPE")
    var type: String? = null,

    @XmlElement(name = "C_DOC")
    var cDoc: String? = null,

    @XmlElement(name = "C_DOC_SUB")
    var cDocSub: String? = null,

    @XmlElement(name = "C_DOC_VER")
    var cDocVer: String? = null,

    @XmlElement(name = "C_DOC_TYPE")
    var cDocType: String? = null,

    @XmlElement(name = "C_DOC_CNT")
    var cDocCnt: String? = null,

    @XmlElement(name = "C_DOC_STAN")
    var cDocStan: String? = null,

    @XmlElement(name = "FILENAME")
    var filename: String? = null

)
