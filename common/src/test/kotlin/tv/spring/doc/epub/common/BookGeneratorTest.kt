package tv.spring.doc.epub.common

import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tv.spring.doc.epub.common.BookGenerator.create
import tv.spring.doc.epub.model.Book
import java.net.URI
import java.net.URISyntaxException

internal class BookGeneratorTest {
    companion object {
        private val log = KotlinLogging.logger {}
    }
    @Test
    @Throws(URISyntaxException::class)
    fun create() {
        val result = create(URI("https://docs.spring.io/spring-framework/reference"), true)
        Assertions.assertNotNull(result)
        if (result.isFailure) {
            log.error { result.exceptionOrNull() }
        }
        Assertions.assertTrue(result.isSuccess)
        val book: Book = result.getOrThrow()
        Assertions.assertNotNull(book)
    }
}