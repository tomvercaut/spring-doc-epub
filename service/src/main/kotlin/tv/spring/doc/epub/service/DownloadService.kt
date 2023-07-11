@file:JvmName("DownloadService")

package tv.spring.doc.epub.service

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.UnsupportedMimeTypeException
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL

/**
 * Retrieves the content of a web page at the specified URL.
 *
 * @param url The URL of the web page to retrieve.
 * @return A [Result] object containing the retrieved web page content as a [Document] on success,
 *         or an exception on failure.
 */
fun get(url: URL): Result<Document> {
    try {
        val connection = Jsoup.connect(url.toString())
        connection.timeout(60000)
        val doc = connection.get()
        return Result.success(doc)
    } catch (ex: SocketTimeoutException) {
        return Result.failure(ex)
    } catch (ex: MalformedURLException) {
        return Result.failure(ex)
    } catch (ex: HttpStatusException) {
        return Result.failure(ex)
    } catch (ex: UnsupportedMimeTypeException) {
        return Result.failure(ex)
    } catch (ex: IOException) {
        return Result.failure(ex)
    }
}