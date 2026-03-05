package com.kotlin.assets.parser

import com.kotlin.assets.entity.DpiEntity
import com.kotlin.assets.repository.DpiRepository
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class DpiExcelParserServiceTest {

    private val dpiRepository: DpiRepository = mock()
    private val service = DpiExcelParserService(dpiRepository)

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds an in-memory .xlsx stream from the given row specs.
     * Each element is a Pair<code: Any?, name: String?> representing one row.
     * Row 0 is always a header and is added automatically before the provided rows.
     */
    private fun buildXlsx(vararg rows: Pair<Any?, String?>): ByteArrayInputStream {
        val workbook = WorkbookFactory.create(true)
        val sheet = workbook.createSheet()

        // header row (row 0 — always skipped by the parser)
        sheet.createRow(0).createCell(0).setCellValue("Code")

        rows.forEachIndexed { i, (code, name) ->
            val row = sheet.createRow(i + 1)
            when (code) {
                null -> { /* leave cell 0 blank */ }
                is Int, is Long -> row.createCell(0).setCellValue((code as Number).toDouble())
                is String -> row.createCell(0).setCellValue(code)
            }
            if (name != null) row.createCell(1).setCellValue(name)
        }

        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()
        return ByteArrayInputStream(out.toByteArray())
    }

    private fun capturedRecords(): List<DpiEntity> {
        val captor = argumentCaptor<List<DpiEntity>>()
        verify(dpiRepository).saveAll(captor.capture())
        return captor.firstValue
    }

    // ─── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `parseAndSave - skips header row and saves data rows`() {
        val stream = buildXlsx(
            Pair(915, "Центральне управління ДПС")
        )

        service.parseAndSave(stream)

        val records = capturedRecords()
        assertEquals(1, records.size)
        assertEquals(915, records[0].code)
        assertEquals("Центральне управління ДПС", records[0].name)
    }

    @Test
    fun `parseAndSave - region header row sets currentRegion for subsequent data rows`() {
        val stream = buildXlsx(
            Pair(null, "Київ"),          // region header: no code
            Pair(101, "ДПС Шевченківська")
        )

        service.parseAndSave(stream)

        val records = capturedRecords()
        assertEquals(1, records.size)
        assertEquals("Київ", records[0].region)
    }

    @Test
    fun `parseAndSave - data rows before any region header have empty region`() {
        val stream = buildXlsx(
            Pair(200, "Перша ДПС")
        )

        service.parseAndSave(stream)

        val records = capturedRecords()
        assertEquals("", records[0].region)
    }

    @Test
    fun `parseAndSave - region changes correctly between multiple regions`() {
        val stream = buildXlsx(
            Pair(null, "Регіон А"),
            Pair(1, "ДПС А1"),
            Pair(null, "Регіон Б"),
            Pair(2, "ДПС Б1"),
            Pair(3, "ДПС Б2")
        )

        service.parseAndSave(stream)

        val records = capturedRecords()
        assertEquals(3, records.size)
        assertEquals("Регіон А", records[0].region)
        assertEquals("Регіон Б", records[1].region)
        assertEquals("Регіон Б", records[2].region)
    }

    @Test
    fun `parseAndSave - numeric code cell is parsed as Int`() {
        val stream = buildXlsx(
            Pair(42, "Тестова ДПС")
        )

        service.parseAndSave(stream)

        assertEquals(42, capturedRecords()[0].code)
    }

    @Test
    fun `parseAndSave - string code cell is parsed as Int`() {
        val stream = buildXlsx(
            Pair("99", "Тестова ДПС")
        )

        service.parseAndSave(stream)

        assertEquals(99, capturedRecords()[0].code)
    }

    @Test
    fun `parseAndSave - non-numeric string code results in null code`() {
        val stream = buildXlsx(
            Pair("ABC", "Тестова ДПС")
        )

        service.parseAndSave(stream)

        assertNull(capturedRecords()[0].code)
    }

    @Test
    fun `parseAndSave - row with blank name is skipped`() {
        val stream = buildXlsx(
            Pair(1, "  "),           // blank (whitespace only)
            Pair(2, "Валідна ДПС")
        )

        service.parseAndSave(stream)

        val records = capturedRecords()
        assertEquals(1, records.size)
        assertEquals(2, records[0].code)
    }

    @Test
    fun `parseAndSave - row with missing name cell is skipped`() {
        val stream = buildXlsx(
            Pair(1, null),           // name cell not created
            Pair(2, "Валідна ДПС")
        )

        service.parseAndSave(stream)

        val records = capturedRecords()
        assertEquals(1, records.size)
        assertEquals(2, records[0].code)
    }

    @Test
    fun `parseAndSave - empty sheet saves no records`() {
        val stream = buildXlsx()   // only header row

        service.parseAndSave(stream)

        assertEquals(emptyList<DpiEntity>(), capturedRecords())
    }

    @Test
    fun `parseAndSave - saves all parsed records in a single saveAll call`() {
        val stream = buildXlsx(
            Pair(null, "Регіон"),
            Pair(1, "ДПС 1"),
            Pair(2, "ДПС 2"),
            Pair(3, "ДПС 3")
        )

        service.parseAndSave(stream)

        val records = capturedRecords()
        assertEquals(3, records.size)
    }
}
