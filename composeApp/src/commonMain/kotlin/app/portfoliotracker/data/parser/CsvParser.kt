package app.portfoliotracker.data.parser

/**
 * Minimal RFC 4180 CSV parser for commonMain (kotlin-csv doesn't support wasmJs).
 * Handles quoted fields with embedded commas, newlines, and escaped quotes ("").
 */
object CsvParser {

    fun parse(content: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val field = StringBuilder()
        val currentRow = mutableListOf<String>()
        var inQuotes = false
        var i = 0

        while (i < content.length) {
            val c = content[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < content.length && content[i + 1] == '"') {
                            field.append('"')
                            i++ // skip escaped quote
                        } else {
                            inQuotes = false
                        }
                    } else {
                        field.append(c)
                    }
                }
                c == '"' -> inQuotes = true
                c == ',' -> {
                    currentRow.add(field.toString())
                    field.clear()
                }
                c == '\r' -> {
                    // skip \r, handle \n next
                }
                c == '\n' -> {
                    currentRow.add(field.toString())
                    field.clear()
                    if (currentRow.any { it.isNotEmpty() }) {
                        rows.add(currentRow.toList())
                    }
                    currentRow.clear()
                }
                else -> field.append(c)
            }
            i++
        }

        // last row (no trailing newline)
        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(field.toString())
            if (currentRow.any { it.isNotEmpty() }) {
                rows.add(currentRow.toList())
            }
        }

        return rows
    }
}
