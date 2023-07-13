package tv.spring.doc.epub.common

import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tv.spring.doc.epub.common.parser.NavTreeParser
import tv.spring.doc.epub.common.service.BookGeneratorService
import tv.spring.doc.epub.model.Book
import tv.spring.doc.epub.service.DocumentationRetrievalService
import tv.spring.doc.epub.service.DownloadService
import java.net.URI
import java.net.URISyntaxException

internal class BookGeneratorTest {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Test
    @Throws(URISyntaxException::class)
    fun create() {
        val bookGeneratorService = BookGeneratorService(
            NavTreeParser(DocumentationRetrievalService(DownloadService())),
            DocumentationRetrievalService(
                DownloadService()
            )
        )
        val result = bookGeneratorService.create(
            URI(
                "https://docs.spring.io/spring-framework/reference"
            ),
            "output", true
        )
        Assertions.assertNotNull(result)
        if (result.isFailure) {
            log.error { result.exceptionOrNull() }
        }
        Assertions.assertTrue(result.isSuccess)
        val book: Book = result.getOrThrow()
        Assertions.assertNotNull(book)
    }
}