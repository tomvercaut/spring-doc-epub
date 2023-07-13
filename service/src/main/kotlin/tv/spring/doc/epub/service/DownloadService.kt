@file:JvmName("DownloadService")

package tv.spring.doc.epub.service

import com.fasterxml.uuid.Generators
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.temporal.ChronoUnit

class DownloadService {
    companion object {
        private val log = KotlinLogging.logger {}
        private val uuidGenerator = Generators.nameBasedGenerator()
    }

    /**
     * Retrieves the content of a web page at the specified URI by perfoming an HTTP GET request.
     *
     * @param uri The URI of the web page to retrieve.
     * @param timeoutMilliSeconds The timeout value in milliseconds for the connection. Default is 30_000 milliseconds.
     * @return A [Result] object containing the retrieved web page content as a [Document] on success,
     *         or an exception on failure. The [Result] object can be used to determine the outcome of the operation.
     */
    fun getDocument(uri: URI, timeoutMilliSeconds: Long = 30_000): Result<Document> {
        try {
            val result = get(uri, timeoutMilliSeconds)
            if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
            val response = result.getOrThrow()
            if (!MimeType.isHtml(response)) {
                return Result.failure(Throwable("Unable to create an HTML document from a ${response.contentType}"))
            }
            val text = response.body.toString(Charsets.UTF_8)
            val document = Jsoup.parse(text)
            return Result.success(document)
        } catch (ex: Exception) {
            return Result.failure(ex)
        }
    }

    /**
     * Downloads an image from the given URI and saves it to the specified output directory.
     *
     * The filename is encoded into a UUID based on the URL of the corresponding image.
     *
     * @param outputDir The path to the output directory where the image will be saved.
     * @param uri The URI of the image to download.
     * @param timeoutMilliSeconds The timeout duration for the download operation, in milliseconds. Default is 30,000 milliseconds.
     *
     * @return A Result object containing the path of the saved image if successful or if it already exists, or a failure if any error occurs during the download or save operation.
     */
    fun downloadImage(outputDir: String, uri: URI, timeoutMilliSeconds: Long = 30_000): Result<Path> {
        try {
            val result = get(uri, timeoutMilliSeconds)
            if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
            val response = result.getOrThrow()
            if (!MimeType.isImage(response)) {
                return Result.failure(Throwable("Unable to create an image from a ${response.contentType}"))
            }
            val path = uriToUuidPath(uri, outputDir)
            if (!Files.exists(path)) {
                val file = File(path.toString())
                file.writeBytes(response.body)
            }
            return Result.success(path)
        } catch (ex: Exception) {
            return Result.failure(ex)
        }
    }

    /**
     * Converts a URI to a file path where the filename is a UUID.
     *
     * @param uri the URI to convert
     * @param outputDir the output directory where the file path will be created
     * @return Path object representing the file path
     */
    private fun uriToUuidPath(uri: URI, outputDir: String): Path {
        val uuid = uuidGenerator.generate(uri.path)
        val path = Paths.get(outputDir, uuid.toString())
        return path
    }

    /**
     * Retrieves the content of a web page at the specified URI by perfoming an HTTP GET request.
     *
     * @param uri The URI to send the GET request to.
     * @param timeoutMilliSeconds The timeout value in milliseconds for each attempt. The default value is 30,000 milliseconds.
     * @param retry The number of times to retry the GET request in case of failure. The default value is 3.
     * @return A [Result] object containing the response [Document] if the request was successful, or an error message if the request failed.
     */
    fun get(
        uri: URI,
        timeoutMilliSeconds: Long = 30_000,
        retry: Int = 3
    ): Result<tv.spring.doc.epub.model.HttpResponse> {
        lateinit var request: HttpRequest
        try {
            request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.of(timeoutMilliSeconds, ChronoUnit.MILLIS))
                .build()
        } catch (ex: IllegalStateException) {
            log.error { ex }
        }
        for (i in 0 until retry) {
            val client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()
            try {
                val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
                val headers = response.headers().map()
                val contentType = headers["Content-Type"]?.let { if (it.isNotEmpty()) it[0].orEmpty() else "" } ?: ""
                val body = response.body()
                return Result.success(tv.spring.doc.epub.model.HttpResponse(headers, contentType, body))
            } catch (ex: IllegalStateException) {
                log.error { ex }
            } catch (ex: InterruptedException) {
                log.error { ex }
            } catch (ex: IOException) {
                log.error { ex }
            }

        }
        return Result.failure(Throwable("Unable to retrieve $uri after $retry attempts with a timeout $timeoutMilliSeconds ms."))
    }
}