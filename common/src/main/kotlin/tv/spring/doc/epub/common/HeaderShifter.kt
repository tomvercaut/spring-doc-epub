@file:JvmName("HeaderShifter")
package tv.spring.doc.epub.common

import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import tv.spring.doc.epub.model.NavItem
import java.util.regex.Matcher
import java.util.regex.Pattern

private val patternHtmlH = Pattern.compile("h(\\d)")

/**
 * Shifts HTML headers (h1, h2, ...) in the articles based on the corresponding depth of the navigation item for the URL of the article.
 *
 * @param hrefs          The list of hrefs corresponding to the articles to shift headers in.
 * @param navTreeByHref  The map of hrefs to nav items, used to determine the depth of each article.
 * @param articlesByHref The map of hrefs to articles, used to access the articles to shift headers in.
 */
fun shift(
    hrefs: List<String?>,
    navTreeByHref: Map<String, NavItem>,
    articlesByHref: Map<String, Node>
) {
    for (href in hrefs) {
        val navItem: NavItem = navTreeByHref[href]!!
        val article: Node = articlesByHref[href]!!
        shift(article, navItem.depth)
    }
}

/**
 * Shifts HTML headers (h1, h2, ...) in the provided node and its children by the specified value.
 *
 * @param node  The node to shift headers in.
 * @param depth The depth by which to shift the headers (1 based).
 * @throws NumberFormatException If the header level cannot be parsed as an integer.
 */
@Throws(NumberFormatException::class)
private fun shift(node: Node, depth: Int) {
    if (node is Element) {
        val name: String = node.tagName()
        val matcher: Matcher = patternHtmlH.matcher(name)
        if (matcher.matches()) {
            val level = matcher.group(1).toInt() + depth - 1
            node.tagName("h$level")
        }
    }
    for (child in node.childNodes()) {
        shift(child, depth)
    }
}