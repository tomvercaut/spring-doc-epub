package tv.spring.doc.epub.model

/**
 * Represents an HTTP response.
 *
 * @property headers The headers of the HTTP response.
 * @property contentType Content-Type
 * @property body The body of the HTTP response.
 */
data class HttpResponse(val headers: Map<String, List<String>>, val contentType: String, val body: ByteArray)
