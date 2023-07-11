package tv.spring.doc.epub.common.parser

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import tv.spring.doc.epub.common.DocumentationRetriever
import tv.spring.doc.epub.model.NavItem
import java.net.URI

/**
 * The `NodeParser` class is responsible for parsing HTML elements and retrieving
 * an [NavItem] object representing a navigation tree.
 */
object NavTreeParser {
    private val log = KotlinLogging.logger {  }

    /**
     * Retrieves the documentation for the given URI and returns it as an Optional Node.
     *
     * @param uri the URI of the documentation to retrieve
     * @return An Optional Node representing the retrieved navigation tree, or Optional.empty() if not found
     */
    @JvmStatic
    fun fromUri(uri: URI): Result<NavItem> {
        val result: Result<Document> = DocumentationRetriever[uri]
        if (result.isFailure) {
            return Result.failure(result.exceptionOrNull()!!)
        }
        val doc = result.getOrThrow()
        val resBody = body(doc.body(), uri)
        if (resBody.isFailure) {
            return Result.failure(resBody.exceptionOrNull()!!)
        }
        return Result.success(resBody.getOrThrow())
    }

    /**
     * Retrieves a Node representing a navigation tree from an HTML body element.
     *
     *
     * Expected HTML structure:
     * <pre>`<body>
     * ...
     * <nav class="nav-menu">
     * <ul class="nav-list">
     * <li class="nav-item" data-depth="0">
     * ...
     * </li>
     * </ul>
     * </nav>
     * </body>
    `</pre> *
     *
     *
     * The function uses [NavTreeParser.item] to process the (nested) li elements inside the "nav-list" ul element.
     * @param body the HTML body element
     * @param baseUri URI to verify a navigation node is in the same domain as the input domain.
     * @return An Optional containing an Index object if the required conditions are met, otherwise an empty Optional
     * Required conditions:
     *
     *  * input element is a body HTML element
     *  * a nested nav element exists: &lt;nav class="nav-menu"&gt;
     *  * a nested ul element exists: &lt;ul class="nav-list"&gt;
     *  * a li element in the above required nav-list ul element
     *
     */
    fun body(body: Element, baseUri: URI): Result<NavItem> {
        if (body.tagName() != "body") {
            val msg = "Expected a <body> HTML element but got ${body.tagName()}"
            return Result.failure(Throwable(msg))
        }
        val nav = body.selectFirst("nav[class=nav-menu]")
        if (nav == null) {
            val msg =
                """
    Expected a <nav> HTML element but got null.
    Unable to find nav-menu in navigation bar.
    """.trimIndent()
            return Result.failure(Throwable(msg))
        }
        val ul = nav.selectFirst("ul[class=nav-list]")
        if (ul == null) {
            val msg =
                """
    Expected a <ul> HTML element but got null.
    Unable to find nav-list in navigation bar.
    """.trimIndent()
            return Result.failure(Throwable(msg))
        }
        val li = ul.selectFirst("li")
        if (li == null) {
            val msg =
                """
    Expected a <li> HTML element but got null.
    Unable to find nav-list item in navigation bar.
    """.trimIndent()
            return Result.failure(Throwable(msg))
        }
        return item(li, baseUri)
    }

    /**
     * Retrieves an Index representing a navigation tree/item from an HTML li element.
     *
     *
     * Expected HTML structure:
     * <pre>`<li class="nav-item" data-depth="0">
     * <a class="nav-link" href="page.html">HTML page</a>
     * <ul class="nav-list">
     * <li class="nav-item" data-depth="1">
     * <a class="nav-link" href="page.html">HTML page</a>
     * <ul class="nav-list">
     * ...
     * </ul>
     * </ul>
     * </li>
    `</pre> *
     *
     * @param li The li element to retrieve the navigation node.
     * @param baseUri URI to verify a navigation node is in the same domain as the input domain.
     * @return An Optional containing the Index object if the li element matches the required conditions, otherwise an empty Optional.
     * Required conditions:
     *
     *  * input HTML element is an HTML li element
     *  * input li element has a class attribute equal to "nav-item"
     *
     */
    @JvmStatic
    fun item(li: Element, baseUri: URI): Result<NavItem> {
        if (li.tagName() != "li") {
            return Result.failure(Throwable("Expected a <li> HTML element but got ${li.tagName()}"))
        }
        if (li.attr("class") != "nav-item") {
            return Result.failure(Throwable("Expected a <li> HTML element to contain a class equal to 'nav-item'"))
        }
        val depth = li.attr("data-depth").toInt(10)
        val node = NavItem()
        node.depth = depth
        val a = li.selectFirst("> a[href]")
        if (a != null) {
            node.name = a.text()
            node.href = a.attr("href")
            val uri = URI.create(node.href)
            if (uri.isAbsolute && !node.href.startsWith(baseUri.toString())) {
                log.warn { "URI [${node.href}] is out of the documentation domain [$baseUri]." }
                return Result.success(NavItem())
//                return Result.failure(Throwable("URI [${node.href}] is out of the documentation domain [$baseUri]."))
            }
        }
        val uls = li.select("> ul[class=nav-list]")
        for (ul in uls) {
            val lis = ul.select("> li[class=nav-item]")
            for (tis in lis) {
                val opt: Result<NavItem> = item(tis, baseUri)
                if (opt.isFailure) {
                    return opt
                }
                val t: NavItem = opt.getOrThrow()
                if (!t.isEmpty()) {
                    node.children.add(opt.getOrThrow())
                }
            }
        }
        return Result.success(node)
    }
}
