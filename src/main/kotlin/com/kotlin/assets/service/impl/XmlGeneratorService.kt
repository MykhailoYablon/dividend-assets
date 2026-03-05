package com.kotlin.assets.service.impl

import com.kotlin.assets.dto.tax.xml.Declar
import com.kotlin.assets.dto.tax.xml.DeclarBody
import com.kotlin.assets.dto.tax.xml.DeclarBodyF1
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.Charset

@Service
class XmlGeneratorService {

    private val encoding = "windows-1251"

    fun saveXmlToFile(declar: Declar, filepath: String) {
        val context: JAXBContext =
            JAXBContext.newInstance(Declar::class.java, DeclarBody::class.java, DeclarBodyF1::class.java)
        val marshaller = createMarshaller(context)
        val file = File(filepath)
        marshaller.marshal(declar, file)
    }

    fun marshalToBytes(declar: Declar): ByteArray {
        val context = JAXBContext.newInstance(Declar::class.java, DeclarBody::class.java, DeclarBodyF1::class.java)
        val marshaller = createMarshaller(context)
        val baos = ByteArrayOutputStream()
        val writer = OutputStreamWriter(baos, Charset.forName(encoding))
        marshaller.marshal(declar, writer)
        return baos.toByteArray()
    }

    private fun createMarshaller(context: JAXBContext): Marshaller {
        return context.createMarshaller().apply {
            setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            setProperty(Marshaller.JAXB_ENCODING, encoding)
            setProperty(Marshaller.JAXB_FRAGMENT, false)
        }
    }
}
