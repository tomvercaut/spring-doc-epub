package tv.spring.doc.epub.service

import tv.spring.doc.epub.model.HttpResponse

internal object MimeType {
    /**
     * Checks if a given MIME type string represents HTML content.
     *
     * @param s The MIME type string to be checked.
     * @return True if the string represents HTML content, false otherwise.
     */
    @JvmStatic
    fun isHtml(s: String): Boolean {
        return s.lowercase().startsWith("text/html")
    }

    /**
     * Checks if the given HttpResponse contains HTML content.
     *
     * @param response The HttpResponse object to check.
     * @return true if the response contains HTML content, false otherwise.
     */
    @JvmStatic
    fun isHtml(response: HttpResponse): Boolean {
        return isHtml(response.contentType)
    }

    /**
     * Checks whether the given MIME type string identifies an image.
     *
     * @param s The MIME type string to check.
     * @return True if the string starts with "image" (case-insensitive), false otherwise.
     */
    @JvmStatic
    fun isImage(s: String): Boolean {
        return s.lowercase().startsWith("image")
    }

    /**
     * Check if the given HTTP response represents an image.
     *
     * @param response The HTTP response to check.
     * @return True if the response represents an image, false otherwise.
     */
    @JvmStatic
    fun isImage(response: HttpResponse): Boolean {
        return isImage(response.contentType)
    }
}