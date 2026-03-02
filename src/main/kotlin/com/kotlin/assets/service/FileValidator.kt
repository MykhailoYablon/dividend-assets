package com.kotlin.assets.service

import com.kotlin.assets.dto.enums.FileType
import org.apache.tika.Tika
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class FileValidator {

    private val tika = Tika()

    fun validate(file: MultipartFile): FileType {
        val filenameType = file.originalFilename
            ?.substringAfterLast(".")
            ?.lowercase()

        // First try to detect from filename extension
        val typeFromExtension = when (filenameType) {
            "xml"  -> FileType.XML
            "csv"  -> FileType.CSV
            "xlsx" -> FileType.XLSX
            else   -> null
        }

        if (typeFromExtension != null) return typeFromExtension

        // Fall back to content sniffing with Tika
        val detectedMime = tika.detect(file.bytes, file.originalFilename)
        return FileType.fromMimeType(detectedMime)
    }
}