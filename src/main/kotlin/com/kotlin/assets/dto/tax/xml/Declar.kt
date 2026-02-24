package com.kotlin.assets.dto.tax.xml

import jakarta.xml.bind.annotation.*

@XmlRootElement(name = "DECLAR")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlSeeAlso(DeclarBody::class, DeclarBodyF1::class)
data class Declar(

    @XmlAttribute(
        name = "noNamespaceSchemaLocation",
        namespace = "http://www.w3.org/2001/XMLSchema-instance"
    )
    var schemaLocation: String? = null,

    @XmlElement(name = "DECLARHEAD", required = true)
    var declarHead: DeclarHead? = null,

    @XmlElements(
        XmlElement(name = "DECLARBODY", type = DeclarBody::class),
        XmlElement(name = "DECLARBODY", type = DeclarBodyF1::class)
    )
    private var declarBody: Any? = null

) {
    fun getDeclarBodyMain(): DeclarBody? = declarBody as? DeclarBody

    fun getDeclarBodyF1(): DeclarBodyF1? = declarBody as? DeclarBodyF1

    fun setDeclarBody(body: DeclarBody) {
        declarBody = body
        schemaLocation = "F0100214.xsd"
    }

    fun setDeclarBody(body: DeclarBodyF1) {
        declarBody = body
        schemaLocation = "F0121214.xsd"
    }
}
