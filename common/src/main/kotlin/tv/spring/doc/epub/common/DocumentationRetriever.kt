package tv.spring.doc.epub.common

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.IOException
import java.net.URI


object DocumentationRetriever {

    /**
     * Retrieves the HTML document content for the given URL and returns a DOM [Document].
     *
     * @param uri the URL to retrieve the content from
     * @return A [Result] containing the [Document] if it could be retrieved, or an error otherwise.
     */
    @JvmStatic
    operator fun get(uri: URI): Result<Document> {
        val scheme = uri.scheme
        var doc: Document? = null
        try {
            if (scheme.equals("http", ignoreCase = true) ||
                scheme.equals("https", ignoreCase = true)
            ) {
                val connection = Jsoup.connect(uri.toURL().toString())
                connection.timeout(60000)
                doc = connection.get()
            } else if (scheme.equals("file", ignoreCase = true)) {
                doc = Jsoup.parse(File(uri))
            }
        } catch (e: IOException) {
            return Result.failure(e)
        }
        return if (doc == null) {
            Result.failure(Throwable("Unable to retrieve documentation from: $uri"))
        } else {
            Result.success(doc)
        }
    }
}
