package tv.spring.doc.epub.model

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

class Book {
    private val document: Document = Document("")
    private val body: Element = document.body()

    fun addSection(node: Node) {
        body.appendChild(node)
    }
}