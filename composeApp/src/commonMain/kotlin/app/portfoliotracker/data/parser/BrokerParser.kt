package app.portfoliotracker.data.parser

interface BrokerParser {
    val formatId: String
    fun canParse(content: String): Boolean
    fun parse(content: String): ParseResult
}
