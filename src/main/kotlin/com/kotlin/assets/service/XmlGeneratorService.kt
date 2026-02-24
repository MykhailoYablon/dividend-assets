package com.kotlin.assets.service

import com.kotlin.assets.dto.tax.xml.Declar
import com.kotlin.assets.dto.tax.xml.DeclarBody
import com.kotlin.assets.dto.tax.xml.DeclarBodyF1
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import java.io.File

class XmlGeneratorService {

    private val encoding = "windows-1251"

    fun saveXmlToFile(declar: Declar, filepath: String) {
        val context: JAXBContext =
            JAXBContext.newInstance(Declar::class.java, DeclarBody::class.java, DeclarBodyF1::class.java)
        val marshaller = context.createMarshaller()


        // Configure marshaller
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding)
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, false)


        val file = File(filepath)
        marshaller.marshal(declar, file)
    }
}
