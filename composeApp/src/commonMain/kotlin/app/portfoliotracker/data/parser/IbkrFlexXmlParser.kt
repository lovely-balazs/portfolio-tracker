package app.portfoliotracker.data.parser

import kotlinx.datetime.LocalDate

class IbkrFlexXmlParser : BrokerParser {

    override val formatId = "ibkr-flex-xml"

    override fun canParse(content: String): Boolean {
        val start = content.take(1000)
        return start.contains("<FlexQueryResponse") || start.contains("<FlexStatements")
    }

    override fun parse(content: String): ParseResult {
        val transactions = mutableListOf<ImportedTransaction>()
        val warnings = mutableListOf<String>()
        var skipped = 0

        // Parse <Trade> elements
        val tradeRegex = Regex("""<Trade\s+([^>]+?)/>""", RegexOption.DOT_MATCHES_ALL)
        for (match in tradeRegex.findAll(content)) {
            try {
                val attrs = parseAttributes(match.groupValues[1])
                val levelOfDetail = attrs["levelOfDetail"] ?: ""
                if (levelOfDetail.equals("ORDER", ignoreCase = true)) {
                    continue // skip ORDER-level, only keep EXECUTION
                }
                val tx = tradeToTransaction(attrs)
                if (tx != null) transactions.add(tx) else skipped++
            } catch (e: Exception) {
                warnings.add("Trade element: ${e.message}")
                skipped++
            }
        }

        // Parse <CashTransaction> elements
        val cashRegex = Regex("""<CashTransaction\s+([^>]+?)/>""", RegexOption.DOT_MATCHES_ALL)
        for (match in cashRegex.findAll(content)) {
            try {
                val attrs = parseAttributes(match.groupValues[1])
                val tx = cashTransactionToTransaction(attrs)
                if (tx != null) transactions.add(tx) else skipped++
            } catch (e: Exception) {
                warnings.add("CashTransaction element: ${e.message}")
                skipped++
            }
        }

        return ParseResult(transactions, warnings, skipped)
    }

    private fun parseAttributes(attrString: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val regex = Regex("""(\w+)="([^"]*?)"""")
        for (match in regex.findAll(attrString)) {
            result[match.groupValues[1]] = match.groupValues[2]
        }
        return result
    }

    private fun tradeToTransaction(attrs: Map<String, String>): ImportedTransaction? {
        val symbol = attrs["symbol"] ?: return null
        val isin = attrs["isin"]?.takeIf { it.isNotBlank() }
        val description = attrs["description"]
        val dateTime = attrs["dateTime"] ?: attrs["tradeDate"] ?: return null
        val date = parseIbkrDate(dateTime) ?: return null
        val quantity = attrs["quantity"]?.toDoubleOrNull() ?: return null
        val price = attrs["tradePrice"]?.toDoubleOrNull() ?: return null
        val commission = attrs["ibCommission"]?.toDoubleOrNull() ?: 0.0
        val currency = attrs["currency"] ?: return null
        val buySell = attrs["buySell"] ?: ""

        val type = when {
            buySell.equals("BUY", ignoreCase = true) -> "BUY"
            buySell.equals("SELL", ignoreCase = true) -> "SELL"
            else -> buySell.uppercase()
        }

        val absQty = kotlin.math.abs(quantity)
        val totalAmount = absQty * price

        return ImportedTransaction(
            brokerSource = formatId,
            date = date,
            type = type,
            ticker = symbol,
            isin = isin,
            name = description,
            quantity = absQty,
            pricePerUnit = price,
            totalAmount = totalAmount,
            currency = currency,
            fee = kotlin.math.abs(commission),
            fxRate = attrs["fxRateToBase"]?.toDoubleOrNull(),
            notes = null,
        )
    }

    private fun cashTransactionToTransaction(attrs: Map<String, String>): ImportedTransaction? {
        val symbol = attrs["symbol"]?.takeIf { it.isNotBlank() }
        val isin = attrs["isin"]?.takeIf { it.isNotBlank() }
        val description = attrs["description"]
        val dateTime = attrs["dateTime"] ?: attrs["reportDate"] ?: return null
        val date = parseIbkrDate(dateTime) ?: return null
        val amount = attrs["amount"]?.toDoubleOrNull() ?: return null
        val currency = attrs["currency"] ?: return null
        val cashType = attrs["type"] ?: ""

        val type = mapCashTransactionType(cashType)

        return ImportedTransaction(
            brokerSource = formatId,
            date = date,
            type = type,
            ticker = symbol,
            isin = isin,
            name = description,
            quantity = null,
            pricePerUnit = null,
            totalAmount = kotlin.math.abs(amount),
            currency = currency,
            fee = 0.0,
            fxRate = attrs["fxRateToBase"]?.toDoubleOrNull(),
            notes = cashType.takeIf { it.isNotBlank() },
        )
    }

    private fun mapCashTransactionType(type: String): String = when {
        type.contains("Dividend", ignoreCase = true) -> "DIVIDEND"
        type.contains("Withholding", ignoreCase = true) -> "FEE"
        type.contains("Commission", ignoreCase = true) -> "FEE"
        type.contains("Interest", ignoreCase = true) -> "INTEREST"
        type.contains("Deposit", ignoreCase = true) -> "DEPOSIT"
        type.contains("Withdrawal", ignoreCase = true) -> "WITHDRAWAL"
        else -> "FEE"
    }

    companion object {
        fun parseIbkrDate(dateStr: String): LocalDate? {
            // IBKR formats: "YYYY-MM-DD;HH:MM:SS" or "YYYY-MM-DD" or "YYYYMMDD"
            val datePart = dateStr.split(";", "T", " ").first().trim()
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
