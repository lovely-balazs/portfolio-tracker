package app.portfoliotracker.platform

import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader

actual fun pickFileAndRead(onResult: (String) -> Unit) {
    val input = document.createElement("input") as HTMLInputElement
    input.type = "file"
    input.accept = ".csv,.xml,.txt"
    input.onchange = {
        val file = input.files?.item(0)
        if (file != null) {
            val reader = FileReader()
            reader.onload = {
                val content = reader.result as? String
                if (content != null) {
                    onResult(content)
                }
                Unit
            }
            reader.readAsText(file)
        }
        Unit
    }
    input.click()
}
