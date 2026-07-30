package com.mediaplayer.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The XLSX side of playlist import. An .xlsx is a ZIP of XML, and this
 * walks it by hand rather than pulling in 16 MB of Apache POI — which means
 * the format's quirks are this file's problem: a string table that cells
 * point into by index, cells that carry their spreadsheet reference rather
 * than their position, and rows that simply omit empty cells.
 *
 * That last one is the trap. A row with cells in A and D is four columns
 * wide, not two, and getting it wrong shifts every field left so the
 * importer reads artists out of the title column.
 */
class XlsxRowReaderTest {

    /** Build a workbook from raw sheet XML, and optionally a string table. */
    private fun workbook(sheetXml: String, sharedStringsXml: String? = null): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            if (sharedStringsXml != null) {
                zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
                zip.write(sharedStringsXml.toByteArray())
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetXml.toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    private fun sharedStrings(vararg values: String): String = buildString {
        append("""<?xml version="1.0"?><sst>""")
        values.forEach { append("<si><t>").append(it).append("</t></si>") }
        append("</sst>")
    }

    private fun read(bytes: ByteArray): List<List<String>> =
        XlsxRowReader.read(ByteArrayInputStream(bytes))

    @Test
    fun `cells resolve through the shared string table`() {
        val rows = read(
            workbook(
                sheetXml = """
                    <worksheet><sheetData>
                      <row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c></row>
                      <row r="2"><c r="A2" t="s"><v>2</v></c><c r="B2" t="s"><v>3</v></c></row>
                    </sheetData></worksheet>
                """.trimIndent(),
                sharedStringsXml = sharedStrings("Track Name", "Artist Name", "Breed", "Nirvana"),
            ),
        )

        assertEquals(
            listOf(
                listOf("Track Name", "Artist Name"),
                listOf("Breed", "Nirvana"),
            ),
            rows,
        )
    }

    /**
     * A gap in the middle of a row must become an empty cell, not a missing
     * one — otherwise every column after it reads one field to the left.
     */
    @Test
    fun `a sparse row is padded to its real width`() {
        val rows = read(
            workbook(
                sheetXml = """
                    <worksheet><sheetData>
                      <row r="1"><c r="A1" t="s"><v>0</v></c><c r="D1" t="s"><v>1</v></c></row>
                    </sheetData></worksheet>
                """.trimIndent(),
                sharedStringsXml = sharedStrings("Breed", "Nevermind"),
            ),
        )

        assertEquals(listOf(listOf("Breed", "", "", "Nevermind")), rows)
    }

    @Test
    fun `a row that starts away from column A is padded at the front`() {
        val rows = read(
            workbook(
                sheetXml = """
                    <worksheet><sheetData>
                      <row r="1"><c r="C1" t="s"><v>0</v></c></row>
                    </sheetData></worksheet>
                """.trimIndent(),
                sharedStringsXml = sharedStrings("Breed"),
            ),
        )

        assertEquals(listOf(listOf("", "", "Breed")), rows)
    }

    /** Columns past Z carry two-letter references. */
    @Test
    fun `a two-letter column reference decodes to the right index`() {
        val rows = read(
            workbook(
                sheetXml = """
                    <worksheet><sheetData>
                      <row r="1"><c r="AA1" t="s"><v>0</v></c></row>
                    </sheetData></worksheet>
                """.trimIndent(),
                sharedStringsXml = sharedStrings("Breed"),
            ),
        )

        // AA is the 27th column, so 26 empties come first.
        assertEquals(27, rows.single().size)
        assertEquals("Breed", rows.single().last())
    }

    @Test
    fun `inline strings are read from the cell itself`() {
        val rows = read(
            workbook(
                sheetXml = """
                    <worksheet><sheetData>
                      <row r="1"><c r="A1" t="inlineStr"><is><t>Breed</t></is></c></row>
                    </sheetData></worksheet>
                """.trimIndent(),
            ),
        )

        assertEquals(listOf(listOf("Breed")), rows)
    }

    @Test
    fun `a formula result is taken as its string value`() {
        val rows = read(
            workbook(
                sheetXml = """
                    <worksheet><sheetData>
                      <row r="1"><c r="A1" t="str"><v>Breed</v></c></row>
                    </sheetData></worksheet>
                """.trimIndent(),
            ),
        )

        assertEquals(listOf(listOf("Breed")), rows)
    }

    @Test
    fun `numeric cells pass through as their raw text`() {
        val rows = read(
            workbook(
                sheetXml = """
                    <worksheet><sheetData>
                      <row r="1"><c r="A1"><v>200000</v></c></row>
                    </sheetData></worksheet>
                """.trimIndent(),
            ),
        )

        assertEquals(listOf(listOf("200000")), rows)
    }

    @Test
    fun `boolean cells render as words rather than digits`() {
        val rows = read(
            workbook(
                sheetXml = """
                    <worksheet><sheetData>
                      <row r="1"><c r="A1" t="b"><v>1</v></c><c r="B1" t="b"><v>0</v></c></row>
                    </sheetData></worksheet>
                """.trimIndent(),
            ),
        )

        assertEquals(listOf(listOf("TRUE", "FALSE")), rows)
    }

    /** Rich text is one string split across runs; a reader must rejoin it. */
    @Test
    fun `rich text runs are joined back into one string`() {
        val rows = read(
            workbook(
                sheetXml = """
                    <worksheet><sheetData>
                      <row r="1"><c r="A1" t="s"><v>0</v></c></row>
                    </sheetData></worksheet>
                """.trimIndent(),
                sharedStringsXml = """
                    <?xml version="1.0"?><sst>
                      <si><r><t>Smells Like </t></r><r><t>Teen Spirit</t></r></si>
                    </sst>
                """.trimIndent(),
            ),
        )

        assertEquals(listOf(listOf("Smells Like Teen Spirit")), rows)
    }

    /**
     * A malformed reference used to land the cell in column 0 and shift the
     * whole row left. Dropping it keeps every other column where it belongs.
     */
    @Test
    fun `a cell with an unreadable reference is dropped, not misplaced`() {
        val rows = read(
            workbook(
                sheetXml = """
                    <worksheet><sheetData>
                      <row r="1"><c r="??" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c></row>
                    </sheetData></worksheet>
                """.trimIndent(),
                sharedStringsXml = sharedStrings("junk", "Nirvana"),
            ),
        )

        assertEquals(listOf(listOf("", "Nirvana")), rows.map { it })
    }

    @Test
    fun `an index past the end of the string table reads as empty`() {
        val rows = read(
            workbook(
                sheetXml = """
                    <worksheet><sheetData>
                      <row r="1"><c r="A1" t="s"><v>9</v></c></row>
                    </sheetData></worksheet>
                """.trimIndent(),
                sharedStringsXml = sharedStrings("Breed"),
            ),
        )

        assertEquals(listOf(listOf("")), rows)
    }

    @Test
    fun `a workbook with no string table still reads`() {
        val rows = read(
            workbook(
                sheetXml = """
                    <worksheet><sheetData>
                      <row r="1"><c r="A1"><v>42</v></c></row>
                    </sheetData></worksheet>
                """.trimIndent(),
            ),
        )

        assertEquals(listOf(listOf("42")), rows)
    }

    /**
     * The message goes straight to the import screen, so it has to say what
     * was wrong with the file and what to do about it.
     */
    @Test
    fun `a workbook without a sheet is rejected with a reason`() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zip.write(sharedStrings("Breed").toByteArray())
            zip.closeEntry()
        }

        val error = runCatching { read(out.toByteArray()) }.exceptionOrNull()

        assertTrue("was $error", error is XlsxRowReader.XlsxReadException)
        assertTrue(error!!.message!!.contains("sheet1.xml"))
        assertTrue(error.message!!.contains("CSV"))
    }

    @Test
    fun `something that is not a workbook at all is rejected`() {
        val error = runCatching { read("not a zip".toByteArray()) }.exceptionOrNull()

        assertTrue("was $error", error != null)
    }

    @Test
    fun `an empty sheet yields no rows rather than an error`() {
        val rows = read(
            workbook(sheetXml = "<worksheet><sheetData></sheetData></worksheet>"),
        )

        assertTrue(rows.isEmpty())
    }
}
