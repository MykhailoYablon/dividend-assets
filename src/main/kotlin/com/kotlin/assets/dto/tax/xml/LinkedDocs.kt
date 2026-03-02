package com.kotlin.assets.dto.tax.xml

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class LinkedDocs(

    @XmlElement(name = "DOC")
    var docs: MutableList<Doc>? = null

)
