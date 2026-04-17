package app.portfoliotracker.data.parser

import kotlinx.datetime.LocalDate

class LightyearCsvParser : BrokerParser {

    override val formatId = "lightyear-csv"

    private val expectedHeaders = listOf(
        "Date", "Type", "Ticker", "Name", "Shares",
        "Price per share", "Total amount", "Currency",
    )

    override fun canParse(content: String): Boolean {
        val firstLine = content.lineSequence().firstOrNull()?.trim() ?: return false
        return expectedHeaders.all { header -> firstLine.contains(header, ignoreCase = true) }
    }

    override fun parse(content: String): ParseResult {
        val rows = CsvParser.parse(content)
        if (rows.isEmpty()) return ParseResult(emptyList(), emptyList(), 0)

        val header = rows.first().map { it.trim() }
        val colIndex = header.withIndex().associate { (i, name) -> name to i }

        val transactions = mutableListOf<ImportedTransaction>()
        val warnings = mutableListOf<String>()
        var skipped = 0

        for (rowIdx in 1 until rows.size) {
            val row = rows[rowIdx]
            try {
                val tx = parseRow(row, colIndex, rowIdx + 1)
                if (tx != null) {
                    transactions.add(tx)
                } else {
                    skipped++
                }
            } catch (e: Exception) {
                warnings.add("Row ${rowIdx + 1}: ${e.message}")
                skipped++
            }
        }

        return ParseResult(transactions, warnings, skipped)
    }

    private fun parseRow(
        row: List<String>,
        colIndex: Map<String, Int>,
        rowNumber: Int,
    ): ImportedTransaction? {
        fun col(name: String): String? = colIndex[name]?.let { row.getOrNull(it)?.trim() }

        val dateStr = col("Date") ?: throw IllegalArgumentException("missing Date")
        val type = col("Type") ?: throw IllegalArgumentException("missing Type")
        val ticker = col("Ticker")?.takeIf { it.isNotBlank() }
        val name = col("Name")?.takeIf { it.isNotBlank() }
        val sharesStr = col("Shares")?.takeIf { it.isNotBlank() }
        val priceStr = col("Price per share")?.takeIf { it.isNotBlank() }
        val totalStr = col("Total amount")?.takeIf { it.isNotBlank() }
        val currency = col("Currency") ?: throw IllegalArgumentException("missing Currency")
        val fxRateStr = col("FX rate")?.takeIf { it.isNotBlank() }
        val notes = col("Notes")?.takeIf { it.isNotBlank() }

        val date = parseDdMmYyyy(dateStr)
            ?: throw IllegalArgumentException("invalid date: $dateStr")

        val mappedType = mapType(type)

        return ImportedTransaction(
            brokerSource = formatId,
            date = date,
            type = mappedType,
            ticker = ticker,
            isin = null,
            name = name,
            quantity = sharesStr?.toDoubleOrNull(),
            pricePerUnit = priceStr?.toDoubleOrNull(),
            totalAmount = totalStr?.toDoubleOrNull(),
            currency = currency,
            fee = 0.0, // Lightyear doesn't break out fees in CSV
            fxRate = fxRateStr?.toDoubleOrNull(),
            notes = notes,
        )
    }

    private fun mapType(type: String): String = when (type.uppercase()) {
        "BUY" -> "BUY"
        "SELL" -> "SELL"
        "DIVIDEND" -> "DIVIDEND"
        "CUSTODY_FEE", "FX_FEE" -> "FEE"
        "DEPOSIT" -> "DEPOSIT"
        "WITHDRAWAL" -> "WITHDRAWAL"
        "INTEREST" -> "INTEREST"
        else -> type.uppercase()
    }

    companion object {
        fun parseDdMmYyyy(s: String): LocalDate? {
            val parts = s.split("/")
            if (parts.size != 3) return null
            val day = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            val year = parts[2].toIntOrNull() ?: return null
            if (month < 1 || month > 12 || day < 1 || day > 31) return null
            return try {
                LocalDate(year, month, day)
            } catch (_: Exception) {
                null
            }
        }
    }
}
