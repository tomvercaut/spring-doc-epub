package tv.spring.doc.epub;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookGeneratorTest {

    @Test
    void create() throws URISyntaxException {
        var optionalBook = BookGenerator.create(new URI("https://docs.spring.io/spring-framework/reference"), true);
        assertNotNull(optionalBook);
        assertTrue(optionalBook.isPresent());
        var book = optionalBook.get();
        assertNotNull(book);
    }
}