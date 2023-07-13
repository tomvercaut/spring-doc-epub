package tv.spring.doc.epub.common.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.commons.lang3.tuple.ImmutablePair
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import tv.spring.doc.epub.common.parser.NavTreeParser
import tv.spring.doc.epub.common.shift
import tv.spring.doc.epub.model.Book
import tv.spring.doc.epub.model.NavItem
import tv.spring.doc.epub.service.DocumentationRetrievalService
import tv.spring.doc.epub.service.DownloadService
import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.*
import java.util.function.Consumer
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class BookGeneratorService(
    private val navTreeParser: NavTreeParser,
    private val documentationRetrievalService: DocumentationRetrievalService
) {
    companion object {
        private val log = KotlinLogging.logger {}
        private val downloadExecutorService: ExecutorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
        private val downloadService = DownloadService()
    }

    fun create(uri: URI, outputPath: String, parallel: Boolean): Result<Book> {
        val resNav = navTreeParser.fromUri(uri)
        if (resNav.isFailure) {
            return Result.failure(resNav.exceptionOrNull()!!)
        }
        val navTree = resNav.getOrThrow()
        val navTreeByHref = mapNavItems(navTree)
        val hrefs = getOrderedHrefs(navTree)
        // Retrieve a map of the documentation by URL
        val resDoc = getDocumentListFromNavigation(uri, hrefs, parallel)
        if (resDoc.isFailure) {
            log.error("Unable to retrieve all documentation based on the documentation index.")
            return Result.failure(resDoc.exceptionOrNull()!!)
        }
        val docs = resDoc.getOrThrow()

        // Extract a map of the actual content by URL
        val resArticle = getArticles(docs)
        if (resArticle.isFailure) return Result.failure(resArticle.exceptionOrNull()!!)
        val articlesByHref = resArticle.getOrThrow()
        if (!checkMissingUrls(navTreeByHref, docs)) {
            return Result.failure(Throwable("Missing an URL from the navigation map used in the document map."))
        }

        // Retrieve and store the images used in the articles
        val imgPath = Paths.get(outputPath, "img")
        if (!Files.exists(imgPath)) Files.createDirectory(imgPath)
        val resImages = getImages(articlesByHref, uri, imgPath.toString())
        if (resImages.isFailure) {
            log.error("Unable to retrieve all images.")
            return Result.failure(resImages.exceptionOrNull()!!)
        }
        val imageSrcsPerHref = resImages.getOrThrow()

        // Modify the image links in the articles
        relinkImgTags(imageSrcsPerHref, imgPath.toString())

        shift(hrefs, navTreeByHref, articlesByHref)
        val book = Book()
        hrefs.stream().map { key: String? -> articlesByHref[key] }
            .forEach { node: Node? -> book.addSection(node!!) }
        return Result.success(book)
    }

    /**
     * Relinks the img tags in the HTML document with new relative paths.
     *
     * @param map A mutable map that contains the href as the key and a list of pairs of img [Node]s and [Path]s as the value.
     * @param outputPath The path of the output HTML file.
     */
    private fun relinkImgTags(map: MutableMap<String, List<Pair<Node, Path>>>, outputPath: String) {
        val opath = Paths.get(outputPath)
        for (href in map.keys) {
            val list = map[href]!!
            for (pair in list) {
                val imgNode = pair.first
                val relPath = opath.relativize(pair.second)
                imgNode.attr("src", relPath.toString())
            }
        }
    }

    /**
     *
     * Retrieves images for each article in the provided map of hrefs and nodes.
     *
     * Each node contains a list of pairs containing the img node and the corresponding filepath of the downloaded image.
     *
     * @param articlesByHref A map containing href values as keys and corresponding Node objects as values.
     * @param uri base URI for the project documentation site
     * @param outputPath The path where the retrieved images will be saved.
     *
     * @return A Result object containing a mutable map with href values as keys
     *         and a list of pairs containing the corresponding Node object and the path to the saved image as values.
     *         If the retrieval of images fails, an error message will be logged and a Result object with the failure
     *         exception will be returned.
     */
    private fun getImages(articlesByHref: Map<String, Node>, uri: URI, outputPath: String): Result<MutableMap<String, List<Pair<Node, Path>>>> {
        val map = mutableMapOf<String, List<Pair<Node, Path>>>()
        for (href in articlesByHref.keys) {
            val article = articlesByHref[href] ?: continue
            val result = getImages(href, article, uri, outputPath)
            if (result.isFailure) {
                log.error("Unable to retrieve all images for: $uri")
                return Result.failure(result.exceptionOrNull()!!)
            }
            map[href] = result.getOrThrow()
        }
        return Result.success(map)
    }

    /**
     * Retrieves the images from the given article node and downloads them to the specified output path.
     *
     * @param href The URL of the article.
     * @param article The article node containing the images to download.
     * @param uri base URI for the project documentation site
     * @param outputPath The path where the images will be saved.
     * @return A Result object containing a list of pairs. Each pair consists of an image node and its corresponding downloaded path.
     *         If an error occurs during the retrieval or download process, a failure result is returned with the corresponding exception.
     */
    private fun getImages(href: String, article: Node, uri: URI, outputPath: String): Result<MutableList<Pair<Node, Path>>> {
        if (article !is Element) {
            return Result.failure(Throwable("The article Node is not an Element. Unable to find any img children tags."))
        }
        val imgTags = article.select("img")
        val tasks = mutableListOf<Future<Result<Pair<Node, Path>>>>()
        imgTags.forEach { img ->
            val src = img.attr("src")
            if (src.isNotBlank()) {
                val imgHref: URL = if (src.startsWith("../")) {
                    val uriPage = uri.resolve(href)
                    val imgUri = uriPage.resolve(src)
                    imgUri.toURL()
//                    URL(URL(href), src)
                } else {
                    URL(src)
                }
                val future: Future<Result<Pair<Node, Path>>> = downloadExecutorService.submit(
                    Callable {
                        val rp = downloadService.downloadImage(outputPath, imgHref.toURI())
                        if (rp.isFailure) {
                            Result.failure(rp.exceptionOrNull()!!)
                        } else {
                            Result.success(Pair(img, rp.getOrThrow()))
                        }
                    }
                )
                tasks.add(future)

            }
        }
        val pairs = mutableListOf<Pair<Node,Path>>()
        for (task in tasks) {
            val result = task.get()
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull()!!)
            }
            val pair = result.getOrThrow()
            pairs.add(pair)
        }
        return Result.success(pairs)
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
                val msg =
                    "Multiple article tags found inside the document [href=$href]. Unable to determine which one to extract."
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
        val map: MutableMap<String, Document> = mutableMapOf()
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
            val t: Result<Document> = documentationRetrievalService.get(turi)
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
