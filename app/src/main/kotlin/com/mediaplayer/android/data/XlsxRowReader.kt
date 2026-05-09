package com.mediaplayer.android.data

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory

/**
 * Minimal XLSX reader: extracts rows from the first worksheet of an
 * `.xlsx` workbook as `List<List<String>>`.
 *
 * Why not Apache POI? POI ships ~16 MB of dependencies (commons-collections4,
 * xmlbeans, log4j, …) for a use case where we only need flat string cells
 * out of a single sheet. The file format is just a ZIP of XML — `xl/sharedStrings.xml`
 * for the deduped string table and `xl/worksheets/sheet1.xml` for the cell
 * grid — so we walk both with SAX (built into Android) and resolve `t="s"`
 * cell types via lookup. Numeric / date cells are passed through as their
 * raw `<v>` text (we don't need date fidelity for a track-name column),
 * boolean cells render as `TRUE` / `FALSE`, inline strings as their `<t>`
 * content. Sparse rows are padded with empty cells based on the cell's
 * spreadsheet ref (`A1` / `B2` / `AA15`).
 *
 * Callers feed the produced rows into [CsvPlaylistParser.parseRows], which
 * does the same header-detection + alias matching as the CSV path.
 */
object XlsxRowReader {

    /**
     * Thrown when the provided stream isn't a valid XLSX (no zip header,
     * missing sheet1.xml, malformed XML). The message is surfaced as-is to
     * the import error screen so the user sees *why* the file was rejected.
     */
    class XlsxReadException(message: String) : Exception(message)

    fun read(input: InputStream): List<List<String>> {
        var sharedStringsBytes: ByteArray? = null
        var sheetBytes: ByteArray? = null

        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "xl/sharedStrings.xml" -> sharedStringsBytes = zip.readBytes()
                    "xl/worksheets/sheet1.xml" -> sheetBytes = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val sheet = sheetBytes
            ?: throw XlsxReadException(
                "Il file XLSX non contiene il foglio principale (xl/worksheets/sheet1.xml). " +
                    "Riprova esportando di nuovo la playlist o salva come CSV."
            )

        val sharedStrings = sharedStringsBytes?.let { parseSharedStrings(it) } ?: emptyList()
        return parseSheet(sheet, sharedStrings)
    }

    /**
     * Parses `xl/sharedStrings.xml` into a flat list indexed by `<si>` order.
     * Each `<si>` may contain a single `<t>` (plain) or several `<r><t>…</t></r>`
     * runs (rich text). We concatenate every `<t>` we see between `<si>` open
     * and close, which yields the same final string a spreadsheet renders.
     */
    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var depthSi = 0
        var inT = false

        val handler = object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes?) {
                when (qName) {
                    "si" -> {
                        depthSi++
                        if (depthSi == 1) current.setLength(0)
                    }
                    "t" -> if (depthSi > 0) inT = true
                }
            }
            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inT) current.append(ch, start, length)
            }
            override fun endElement(uri: String?, localName: String?, qName: String) {
                when (qName) {
                    "t" -> inT = false
                    "si" -> {
                        if (depthSi == 1) result.add(current.toString())
                        depthSi--
                    }
                }
            }
        }
        runSax(bytes, handler)
        return result
    }

    /**
     * Walks `<row>` / `<c>` / `<v>` elements building `List<List<String>>`.
     *
     * Cell type matrix (the `t` attribute on `<c>`):
     *  - `s` (default for shared strings) — `<v>` is an integer index into [strings].
     *  - `inlineStr` — string text lives inside `<is><t>…</t></is>` instead of `<v>`.
     *  - `str` — formula result string in `<v>` directly.
     *  - `b` — boolean; `<v>` is `0` / `1`, surfaced as `FALSE` / `TRUE`.
     *  - everything else (numeric, date) — `<v>` text passed through as-is.
     *
     * Sparse rows get padded: a row with cells `A1` and `D1` produces four
     * entries `[A1, "", "", D1]`. This is what the CSV parser expects so a
     * fixed column index always points at the right field.
     */
    private fun parseSheet(bytes: ByteArray, strings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var currentRow: MutableList<String>? = null
        var currentCellType = ""
        var currentCellRef = ""
        val valBuf = StringBuilder()
        var inV = false
        var inInlineT = false
        var lastColIndex = -1

        val handler = object : DefaultHandler() {
            override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes?) {
                when (qName) {
                    "row" -> {
                        currentRow = mutableListOf()
                        lastColIndex = -1
                    }
                    "c" -> {
                        currentCellType = attributes?.getValue("t") ?: ""
                        currentCellRef = attributes?.getValue("r") ?: ""
                        valBuf.setLength(0)
                        // Pad missing columns so column indices line up across rows.
                        val colIdx = colIndexOf(currentCellRef)
                        while (lastColIndex + 1 < colIdx) {
                            currentRow?.add("")
                            lastColIndex++
                        }
                    }
                    "v" -> inV = true
                    "t" -> if (currentCellType == "inlineStr") inInlineT = true
                }
            }
            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inV || inInlineT) valBuf.append(ch, start, length)
            }
            override fun endElement(uri: String?, localName: String?, qName: String) {
                when (qName) {
                    "v" -> inV = false
                    "t" -> if (currentCellType == "inlineStr") inInlineT = false
                    "c" -> {
                        val raw = valBuf.toString()
                        val resolved = when (currentCellType) {
                            "s" -> raw.toIntOrNull()?.let { strings.getOrNull(it) }.orEmpty()
                            "b" -> if (raw == "1") "TRUE" else "FALSE"
                            "inlineStr", "str" -> raw
                            else -> raw
                        }
                        currentRow?.add(resolved)
                        lastColIndex++
                    }
                    "row" -> {
                        currentRow?.let { rows.add(it) }
                        currentRow = null
                    }
                }
            }
        }
        runSax(bytes, handler)
        return rows
    }

    private fun runSax(bytes: ByteArray, handler: DefaultHandler) {
        try {
            val factory = SAXParserFactory.newInstance().apply {
                isNamespaceAware = false
                // Defuse XML external entity attacks — the parser should never
                // try to resolve DTDs or external references when reading
                // user-provided spreadsheets.
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            }
            factory.newSAXParser().parse(ByteArrayInputStream(bytes), handler)
        } catch (e: Exception) {
            throw XlsxReadException(
                "Errore durante la lettura del file XLSX: ${e.message ?: e.javaClass.simpleName}."
            )
        }
    }

    /**
     * Converts a spreadsheet cell ref like `A1`, `B2`, `AA15` into a 0-based
     * column index. Letters are read as base-26 with `A`=1; we subtract 1 at
     * the end so `A`→0. Returns -1 on a malformed ref (no leading letters)
     * which the caller treats as "no padding".
     */
    private fun colIndexOf(ref: String): Int {
        var col = 0
        var i = 0
        while (i < ref.length && ref[i].isLetter()) {
            col = col * 26 + (ref[i].uppercaseChar() - 'A' + 1)
            i++
        }
        return if (col == 0) -1 else col - 1
    }
}
