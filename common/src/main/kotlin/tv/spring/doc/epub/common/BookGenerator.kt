package tv.spring.doc.epub.common

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.tuple.ImmutablePair
import org.jsoup.nodes.Document
import org.jsoup.nodes.Node
import tv.spring.doc.epub.common.parser.NavTreeParser.fromUri
import tv.spring.doc.epub.model.Book
import tv.spring.doc.epub.model.NavItem
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.function.Consumer
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

object BookGenerator {
    private val log = KotlinLogging.logger {}

    @JvmStatic
    fun create(uri: URI, parallel: Boolean): Result<Book> {
        val resNav = fromUri(uri)
        if (resNav.isFailure) {
            return Result.failure(resNav.exceptionOrNull()!!)
        }
        val navTree = resNav.getOrThrow()
        val navTreeByHref = mapNavItems(navTree)
        val hrefs = getOrderedHrefs(navTree)
        val resDoc = getDocumentListFromNavigation(uri, hrefs, parallel)
        if (resDoc.isFailure) return Result.failure(resDoc.exceptionOrNull()!!)
        val docs = resDoc.getOrThrow()
        val resArticle = getArticles(docs)
        if (resArticle.isFailure) return Result.failure(resArticle.exceptionOrNull()!!)
        val articlesByHref = resArticle.getOrThrow()
        if (!checkMissingUrls(navTreeByHref, docs)) {
            return Result.failure(Throwable("Missing an URL from the navigation map used in the document map."))
        }
        shift(hrefs, navTreeByHref, articlesByHref)
        val book = Book()
        hrefs.stream().map { key: String? -> articlesByHref[key] }
            .forEach { node: Node? -> book.addSection(node!!) }
        return Result.success(book)
    }

    /**
     * Retrieves the articles from the given documents.
     *
     * @param docs a map of document URLs and their corresponding document objects
     * @return an optional map of document URLs and their corresponding article nodes, or an empty optional if no articles are found or multiple articles are found
     */
    private fun getArticles(docs: Map<String, Document>): Result<Map<String, Node>> {
        val articles: MutableMap<String, Node> = HashMap(docs.size)
        for ((href, doc) in docs) {
            val elements = doc.getElementsByTag("article")
            val n = elements.size
            if (n < 1) {
                val msg = "Unable to find the article tag inside the document [href=$href]."
                log.error(msg)
                return Result.failure(Throwable(msg))
            } else if (n > 1) {
                val msg = "Multiple article tags found inside the document [href=$href]. Unable to determine which one to extract."
                log.error(msg)
                return Result.failure(Throwable(msg))
            } else {
                val article = elements[0]
                articles[href] = article
            }
        }
        return Result.success(articles)
    }

    /**
     * Checks if any URLs in the document map are missing in the navigation map.
     *
     * @param navs The navigation map containing URL as keys and NavItem objects as values.
     * @param docs The document map containing URL as keys and Document objects as values.
     * @return Returns true if all URLs in the document map are present in the navigation map, otherwise false.
     */
    private fun checkMissingUrls(navs: Map<String, NavItem>, docs: Map<String, Document>): Boolean {
        for (href in docs.keys) {
            if (!navs.containsKey(href)) {
                log.error(String.format("Unable to find URL (%s) in the navigation index tree.", href))
                return false
            }
        }
        return true
    }

    /**
     * Maps a navigation item and its children recursively to a Map with their respective URLs as keys.
     *
     * @param item The navigation item to be mapped.
     * @return A Map containing the URLs of the navigation items as keys and the NavItems themselves as values.
     */
    private fun mapNavItems(item: NavItem): Map<String, NavItem> {
        val map: MutableMap<String, NavItem> = HashMap()
        mapNavItems(item, map)
        return map
    }

    /**
     * Maps the navigation items to a map by URL.
     *
     * @param item The navigation item to be mapped.
     * @param map  The map to contain the mapping of URLs and the navigation items.
     */
    private fun mapNavItems(item: NavItem, map: MutableMap<String, NavItem>) {
        if (item.href.isNotBlank()) {
            map[item.href] = item
        }
        item.children.forEach(Consumer { child: NavItem -> mapNavItems(child, map) })
    }

    /**
     * Retrieves a list of documents from navigation based on the provided base URL and hrefs.
     *
     * @param baseUrl  The base URL for the hrefs
     * @param hrefs    The list of hrefs to retrieve documents from.
     * @param parallel Determines whether the document retrieval should be done in parallel or not.
     * @return A [Result] containing a [Map] of documents, where the keys are the hrefs and the values are the corresponding documents.
     * Returns a [kotlin.Result.Failure] if any document retrieval fails or if there are null values in the resulting map of documents.
     * @throws RuntimeException If the provided base URL is malformed.
     */
    private fun getDocumentListFromNavigation(
        baseUrl: URI,
        hrefs: List<String>,
        parallel: Boolean
    ): Result<Map<String, Document>> {
        val url: URL = try {
            baseUrl.toURL()
        } catch (e: MalformedURLException) {
            throw RuntimeException(e)
        }
        val docs: MutableMap<String, Result<Document>> = HashMap()
        if (!parallel) {
            hrefs.stream()
                .map { href: String -> retrieveDocumention(url, href) }
                .forEach { (key, value): ImmutablePair<String, Result<Document>> -> docs[key] = value }
        } else {
            val nproc = Runtime.getRuntime().availableProcessors()
            val lf: MutableList<Future<ImmutablePair<String, Result<Document>>>> = ArrayList()
            Executors.newFixedThreadPool(nproc).use { es ->
                for (href in hrefs) {
                    val f = es.submit<ImmutablePair<String, Result<Document>>> { retrieveDocumention(url, href) }
                    lf.add(f)
                }
            }
            for (f in lf) {
                try {
                    val (key, value) = f.get()
                    docs[key] = value
                } catch (e: InterruptedException) {
                    log.error(String.format("Unable to retrieve documentation from future: %s", e))
                    return Result.failure(e)
                } catch (e: ExecutionException) {
                    log.error(String.format("Unable to retrieve documentation from future: %s", e))
                    return Result.failure(e)
                }
            }
        }
        val map : MutableMap<String, Document> = mutableMapOf()
        for (href in docs.keys) {
            val r = docs[href] ?: return Result.failure(NullPointerException("Failed to get document for $href"))
            if (r.isFailure) {
                return Result.failure(r.exceptionOrNull()!!)
            }
            map[href] = r.getOrThrow()
        }
        return Result.success(map)
    }

    /**
     * Retrieves the documentation for a given URL and href.
     *
     * @param url  The base URL for the href.
     * @param href The href to retrieve the documentation from.
     * @return An ImmutablePair containing the href as the key and the [Result] of the retrieved Document as the value.
     * If the retrieval fails, the value will be null.
     */
    private fun retrieveDocumention(url: URL, href: String): ImmutablePair<String, Result<Document>> {
        return try {
            var turl = url.toURI().toString()
            if (!turl.endsWith("/")) {
                turl += '/'
            }
            var turi = URI.create(turl)
            turi = turi.resolve(href)
            val t: Result<Document> = DocumentationRetriever[turi]
            if (t.isFailure) {
                ImmutablePair<String, Result<Document>>(
                    href,
                    Result.failure(NullPointerException("Failed to retrieve documentation from $href"))
                )
            } else {
                ImmutablePair.of(href, Result.success(t.getOrThrow()))
            }
        } catch (e: URISyntaxException) {
            log.error { String.format("Unable to retrieve documentation for %s: %s", href, e) }
            ImmutablePair<String, Result<Document>>(
                href,
                Result.failure(NullPointerException("Failed to retrieve documentation from $href"))
            )
        }
    }

    /**
     * Retrieves the href values from the provided Node and its descendants in order.
     *
     * @param navItem The Node to retrieve href values from.
     * @return A List of href values in the order they appear in the Node and its descendants.
     */
    private fun getOrderedHrefs(navItem: NavItem): List<String> {
        val list: MutableList<String> = ArrayList()
        getOrderedHrefs(navItem, list)
        return list
    }

    /**
     * Retrieves hrefs in order from the provided node and adds them to the given list.
     *
     * @param navItem The node to retrieve hrefs from.
     * @param list    The list to add the retrieved hrefs to.
     */
    private fun getOrderedHrefs(navItem: NavItem, list: MutableList<String>) {
        val href = navItem.href
        if (href.isNotBlank()) {
            list.add(href)
        }
        for (child in navItem.children) {
            getOrderedHrefs(child, list)
        }
    }
}
