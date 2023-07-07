package tv.spring.doc.epub.model

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node

/**
 * Represents a book with sections.
 *
 * This class provides methods to add sections to a book and retrieve the underlying document.
 */
class Book {
    private val document: Document = Document("")
    private val body: Element = document.body()

    /**
     * Adds a section to the body of the document.
     *
     * @param node The HTML node to be appended to the body.
     */
    fun addSection(node: Node) {
        body.appendChild(node)
    }

    /**
     * Returns the HTML document.
     *
     * @return The HTML document object.
     */
    fun getDocument(): Document {
        return document
    }
}