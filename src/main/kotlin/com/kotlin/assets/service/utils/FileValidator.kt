package com.kotlin.assets.service.utils

import com.kotlin.assets.dto.enums.FileType
import org.apache.tika.Tika
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class FileValidator {

    private val tika = Tika()

    fun validate(file: MultipartFile): FileType {
        // Always verify actual content with Tika — never trust the filename extension alone
        val detectedMime = tika.detect(file.bytes, file.originalFilename)
        val typeFromContent = runCatching { FileType.fromMimeType(detectedMime) }.getOrNull()

        // Cross-check extension against detected content type
        val filenameExt = file.originalFilename?.substringAfterLast(".")?.lowercase()
        val typeFromExtension = when (filenameExt) {
            "xml"  -> FileType.XML
            "csv"  -> FileType.CSV
            "xlsx" -> FileType.XLSX
            else   -> null
        }

        if (typeFromContent == null && typeFromExtension == null) {
            throw IllegalArgumentException("Unsupported file type: $detectedMime")
        }

        // If both are determined, they must agree
        if (typeFromContent != null && typeFromExtension != null && typeFromContent != typeFromExtension) {
            throw IllegalArgumentException("File content ($detectedMime) does not match extension (.$filenameExt)")
        }

        return typeFromContent ?: typeFromExtension!!
    }
}