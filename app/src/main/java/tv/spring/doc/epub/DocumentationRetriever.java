package tv.spring.doc.epub;

import lombok.extern.log4j.Log4j2;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Log4j2
public class DocumentationRetriever {
    /**
     * Retrieves the HTML document content for the given URL and returns a DOM {@link Document}.
     *
     * @param uri the URL to retrieve the content from
     * @return An Optional containing the {@link Document} if it could be retrieved, or an empty Optional otherwise.
     */
    public static Optional<Document> get(URI uri) {
        var scheme = uri.getScheme();
        Document doc = null;
        try {
            if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
                var connection = Jsoup.connect(String.valueOf(uri.toURL()));
                connection.timeout(60000);
                doc = connection.get();
            } else if (scheme.equalsIgnoreCase("file")) {
                doc = Jsoup.parse(new File(uri));
            }
        } catch (IOException e) {
            log.error(e);
            log.error(String.format("Unable to retrieve documentation from: %s", uri));
            return Optional.empty();
        }
        if (doc == null) {
            log.error(String.format("Unable to retrieve documentation from: %s", uri));
            return Optional.empty();
        } else {
            return Optional.of(doc);
        }
    }
}
