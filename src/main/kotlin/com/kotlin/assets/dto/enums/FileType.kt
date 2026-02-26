package com.kotlin.assets.dto.enums

enum class FileType(vararg val mimeTypes: String) {
    XLSX(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/x-tika-ooxml"
    ),
    CSV("text/plain"),
    XML("application/xml", "text/xml");

    companion object {
        fun fromMimeType(mimeType: String): FileType =
            entries.firstOrNull { fileType -> mimeType in fileType.mimeTypes }
                ?: throw IllegalArgumentException("Unsupported file type: $mimeType")
    }
}