package app.portfoliotracker.data.parser

data class ParseResult(
    val transactions: List<ImportedTransaction>,
    val warnings: List<String>,
    val skippedRows: Int,
)
