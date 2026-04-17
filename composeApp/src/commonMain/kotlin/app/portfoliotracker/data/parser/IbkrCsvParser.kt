package app.portfoliotracker.data.parser

import kotlinx.datetime.LocalDate

class IbkrCsvParser : BrokerParser {

    override val formatId = "ibkr-csv"

    override fun canParse(content: String): Boolean {
        val firstLines = content.lineSequence().take(10).joinToString("\n")
        return firstLines.contains("Statement,Header") ||
            firstLines.contains("Trades,Header") ||
            firstLines.contains("Account Information,Header")
    }

    override fun parse(content: String): ParseResult {
        val transactions = mutableListOf<ImportedTransaction>()
        val warnings = mutableListOf<String>()
        var skipped = 0

        val rows = CsvParser.parse(content)

        var currentSection = ""
        var columnMap = emptyMap<String, Int>()
        val isinLookup = mutableMapOf<String, String>() // symbol → ISIN

        for ((rowIdx, row) in rows.withIndex()) {
            if (row.size < 2) continue

            val section = row[0].trim()
            val rowType = row[1].trim()

            when (rowType) {
                "Header" -> {
                    currentSection = section
                    columnMap = row.withIndex().associate { (i, name) -> name.trim() to i }
                }
                "Data" -> {
                    if (section != currentSection) {
                        currentSection = section
                    }
                    try {
                        when (currentSection) {
                            "Trades" -> {
                                val tx = parseTradeRow(row, columnMap, isinLookup)
                                if (tx != null) transactions.add(tx) else skipped++
                            }
                            "Dividends" -> {
                                val tx = parseDividendRow(row, columnMap)
                                if (tx != null) transactions.add(tx) else skipped++
                            }
                            "Withholding Tax" -> {
                                val tx = parseWithholdingRow(row, columnMap)
                                if (tx != null) transactions.add(tx) else skipped++
                            }
                            "Financial Instrument Information" -> {
                                parseInstrumentInfo(row, columnMap, isinLookup)
                            }
                        }
                    } catch (e: Exception) {
                        warnings.add("Row ${rowIdx + 1} ($currentSection): ${e.message}")
                        skipped++
                    }
                }
                // Skip Total, SubTotal, Notes rows
            }
        }

        // Backfill ISINs on transactions that were parsed before instrument info
        val backfilled = transactions.map { tx ->
            if (tx.isin == null && tx.ticker != null) {
                tx.copy(isin = isinLookup[tx.ticker])
            } else tx
        }

        return ParseResult(backfilled, warnings, skipped)
    }

    private fun parseTradeRow(
        row: List<String>,
        colMap: Map<String, Int>,
        isinLookup: Map<String, String>,
    ): ImportedTransaction? {
        fun col(name: String) = colMap[name]?.let { row.getOrNull(it)?.trim() }

        val discriminator = col("DataDiscriminator") ?: ""
        if (discriminator.equals("ClosedLot", ignoreCase = true) ||
            discriminator.equals("OpenLot", ignoreCase = true)) {
            return null // skip lot-level rows
        }

        val symbol = col("Symbol") ?: return null
        val dateStr = col("Date/Time") ?: col("TradeDate") ?: return null
        val date = parseIbkrCsvDate(dateStr) ?: return null
        val quantity = col("Quantity")?.toDoubleOrNull() ?: return null
        val price = col("T. Price")?.toDoubleOrNull()
            ?: col("TradePrice")?.toDoubleOrNull() ?: return null
        val commission = col("Comm/Fee")?.toDoubleOrNull()
            ?: col("IBCommission")?.toDoubleOrNull() ?: 0.0
        val currency = col("Currency") ?: return null
        val code = col("Code") ?: ""

        val type = if (quantity >= 0) "BUY" else "SELL"
        val absQty = kotlin.math.abs(quantity)
        val isin = isinLookup[symbol]

        return ImportedTransaction(
            brokerSource = formatId,
            date = date,
            type = type,
            ticker = symbol,
            isin = isin,
            name = null,
            quantity = absQty,
            pricePerUnit = price,
            totalAmount = absQty * price,
            currency = currency,
            fee = kotlin.math.abs(commission),
            fxRate = null,
            notes = code.takeIf { it.isNotBlank() },
        )
    }

    private fun parseDividendRow(
        row: List<String>,
        colMap: Map<String, Int>,
    ): ImportedTransaction? {
        fun col(name: String) = colMap[name]?.let { row.getOrNull(it)?.trim() }

        val symbol = extractSymbolFromDescription(col("Description") ?: "")
        val dateStr = col("Date") ?: return null
        val date = parseIbkrCsvDate(dateStr) ?: return null
        val amount = col("Amount")?.toDoubleOrNull() ?: return null
        val currency = col("Currency") ?: return null

        return ImportedTransaction(
            brokerSource = formatId,
            date = date,
            type = "DIVIDEND",
            ticker = symbol,
            isin = null,
            name = col("Description"),
            quantity = null,
            pricePerUnit = null,
            totalAmount = kotlin.math.abs(amount),
            currency = currency,
            fee = 0.0,
            fxRate = null,
            notes = null,
        )
    }

    private fun parseWithholdingRow(
        row: List<String>,
        colMap: Map<String, Int>,
    ): ImportedTransaction? {
        fun col(name: String) = colMap[name]?.let { row.getOrNull(it)?.trim() }

        val symbol = extractSymbolFromDescription(col("Description") ?: "")
        val dateStr = col("Date") ?: return null
        val date = parseIbkrCsvDate(dateStr) ?: return null
        val amount = col("Amount")?.toDoubleOrNull() ?: return null
        val currency = col("Currency") ?: return null

        return ImportedTransaction(
            brokerSource = formatId,
            date = date,
            type = "FEE",
            ticker = symbol,
            isin = null,
            name = col("Description"),
            quantity = null,
            pricePerUnit = null,
            totalAmount = kotlin.math.abs(amount),
            currency = currency,
            fee = kotlin.math.abs(amount),
            fxRate = null,
            notes = "Withholding Tax",
        )
    }

    private fun parseInstrumentInfo(
        row: List<String>,
        colMap: Map<String, Int>,
        isinLookup: MutableMap<String, String>,
    ) {
        fun col(name: String) = colMap[name]?.let { row.getOrNull(it)?.trim() }
        val symbol = col("Symbol") ?: return
        val isin = col("Security ID")?.takeIf { it.isNotBlank() } ?: return
        val idType = col("Security ID Type") ?: ""
        if (idType.equals("ISIN", ignoreCase = true) || idType.isBlank()) {
            isinLookup[symbol] = isin
        }
    }

    private fun extractSymbolFromDescription(desc: String): String? {
        // IBKR dividend description format: "AAPL(US0378331005) Cash Dividend..."
        val match = Regex("""^(\S+?)\(""").find(desc)
        return match?.groupValues?.get(1)?.takeIf { it.isNotBlank() }
    }

    companion object {
        fun parseIbkrCsvDate(dateStr: String): LocalDate? {
            // Formats: "2024-01-15, 09:30:00" or "2024-01-15" or "20240115"
            val datePart = dateStr.split(",", " ", "T", ";").first().trim()
            return try {
                if (datePart.contains("-")) {
                    LocalDate.parse(datePart)
                } else if (datePart.length == 8) {
                    val y = datePart.substring(0, 4).toInt()
                    val m = datePart.substring(4, 6).toInt()
                    val d = datePart.substring(6, 8).toInt()
                    LocalDate(y, m, d)
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }
}
