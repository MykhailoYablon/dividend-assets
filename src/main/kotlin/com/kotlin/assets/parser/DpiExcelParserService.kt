package com.kotlin.assets.parser

import com.kotlin.assets.entity.DpiEntity
import com.kotlin.assets.repository.DpiRepository
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class DpiExcelParserService(
    private val dpiRepository: DpiRepository
) {
    fun parseAndSave(inputStream: InputStream) {
        val workbook = WorkbookFactory.create(inputStream)
        val sheet = workbook.getSheetAt(0)

        val records = mutableListOf<DpiEntity>()
        var currentRegion = ""

        for (row in sheet) {
            if (row.rowNum == 0) continue // skip header row

            val codeCell = row.getCell(0)
            val nameCell = row.getCell(1) ?: continue

            val name = nameCell.stringCellValue.trim()
            if (name.isBlank()) continue

            // Detect region header rows (bold, no code)
            if (codeCell == null || codeCell.cellType == CellType.BLANK) {
                currentRegion = name
                continue
            }

            val code = when (codeCell.cellType) {
                CellType.NUMERIC -> codeCell.numericCellValue.toInt()
                CellType.STRING  -> codeCell.stringCellValue.trim().toIntOrNull()
                else -> null
            }

            records += DpiEntity(
                code   = code,
                name   = name,
                region = currentRegion
            )
        }

        workbook.close()
        dpiRepository.saveAll(records)
    }
}