package com.kotlin.assets.dto.tax.xml

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class DeclarHead(

    @XmlElement(name = "TIN")
    var tin: String? = null,

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

    @XmlElement(name = "C_REG")
    var cReg: String? = null,

    @XmlElement(name = "C_RAJ")
    var cRaj: String? = null,

    @XmlElement(name = "PERIOD_MONTH")
    var periodMonth: String? = null,

    @XmlElement(name = "PERIOD_TYPE")
    var periodType: String? = null,

    @XmlElement(name = "PERIOD_YEAR")
    var periodYear: String? = null,

    @XmlElement(name = "C_STI_ORIG")
    var cStiOrig: String? = null,

    @XmlElement(name = "C_DOC_STAN")
    var cDocStan: String? = null,

    @XmlElement(name = "LINKED_DOCS")
    var linkedDocs: LinkedDocs? = null,

    @XmlElement(name = "D_FILL")
    var dFill: String? = null,

    @XmlElement(name = "SOFTWARE")
    var software: String? = null

)