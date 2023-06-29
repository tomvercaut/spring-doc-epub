package tv.spring.doc.epub;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.Optional;

public class Http {
    /**
     * Retrieves the HTML document content for the given URL and returns a DOM {@link Document}.
     *
     * @param url the URL to retrieve the content from
     * @return An Optional containing the {@link Document} if it could be retrieved, or an empty Optional otherwise.
     */
    public static Optional<Document> get(String url) {
        try {
            var doc = Jsoup.connect(url).get();
            return Optional.of(doc);
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
