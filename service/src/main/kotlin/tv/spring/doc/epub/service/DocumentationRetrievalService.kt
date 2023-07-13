package tv.spring.doc.epub.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.IOException
import java.net.URI


class DocumentationRetrievalService(private val downloadService: DownloadService) {
        private val log = KotlinLogging.logger {}

        /**
         * Retrieves the HTML document content for the given URL and returns a DOM [Document].
         *
         * @param uri the URL to retrieve the content from
         * @return A [Result] containing the [Document] if it could be retrieved, or an error otherwise.
         */
        fun get(uri: URI): Result<Document> {
            val scheme = uri.scheme
            var doc: Document? = null
            try {
                if (scheme.equals("http", ignoreCase = true) ||
                    scheme.equals("https", ignoreCase = true)
                ) {
                    val result = downloadService.getDocument(uri, 60_000)
                    if (result.isFailure) return result
                    doc = result.getOrNull()
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
