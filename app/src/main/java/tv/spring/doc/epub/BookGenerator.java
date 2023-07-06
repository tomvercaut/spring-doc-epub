package tv.spring.doc.epub;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import tv.spring.doc.epub.model.Book;
import tv.spring.doc.epub.model.nav.NavItem;
import tv.spring.doc.epub.parser.nav.NavTreeParser;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Log4j2
public class BookGenerator {

    public static Optional<Book> create(@NotNull URI uri, boolean parallel) {
        var optNav = NavTreeParser.fromUri(uri);
        if (optNav.isEmpty()) {
            return Optional.empty();
        }
        var navTree = optNav.get();
        var navTreeByHref = mapNavItems(navTree);
        var hrefs = getOrderedHrefs(navTree);

        var optDocs = getDocumentListFromNavigation(uri, hrefs, parallel);
        if (optDocs.isEmpty()) return Optional.empty();
        var docs = optDocs.get();

        var optArticles = getArticles(docs);
        if (optArticles.isEmpty()) return Optional.empty();
        var articlesByHref = optArticles.get();

        if (!checkMissingUrls(navTreeByHref, docs)) {
            return Optional.empty();
        }

        HeaderShifter.shift(hrefs, navTreeByHref, articlesByHref);

        Book book = new Book();
        hrefs.stream().map(articlesByHref::get).forEach(book::addSection);
        return Optional.of(book);
    }

    /**
     * Retrieves the articles from the given documents.
     *
     * @param docs a map of document URLs and their corresponding document objects
     * @return an optional map of document URLs and their corresponding article nodes, or an empty optional if no articles are found or multiple articles are found
     */
    private static Optional<Map<String, Node>> getArticles(@NotNull Map<String, Document> docs) {
        Map<String, Node> articles = new HashMap<>(docs.size());
        for (var entry : docs.entrySet()) {
            final var href = entry.getKey();
            final var doc = entry.getValue();
            final var elements = doc.getElementsByTag("article");
            final int n = elements.size();
            if (n < 1) {
                log.error(String.format("Unable to find the article tag inside the document [href=%s].", href));
                return Optional.empty();
            } else if (n > 1) {
                log.error(String.format("Multiple article tags found inside the document [href=%s]. Unable to determine which one to extract.", href));
                return Optional.empty();
            } else {
                final var article = elements.get(0);
                articles.put(href, article);
            }
        }
        return Optional.of(articles);
    }

    /**
     * Checks if any URLs in the document map are missing in the navigation map.
     *
     * @param navs The navigation map containing URL as keys and NavItem objects as values.
     * @param docs The document map containing URL as keys and Document objects as values.
     * @return Returns true if all URLs in the document map are present in the navigation map, otherwise false.
     */
    private static boolean checkMissingUrls(@NotNull Map<String, NavItem> navs, @NotNull Map<String, Document> docs) {
        for (var href : docs.keySet()) {
            if (!navs.containsKey(href)) {
                log.error(String.format("Unable to find URL (%s) in the navigation index tree.", href));
                return false;
            }
        }
        return true;
    }

    /**
     * Maps a navigation item and its children recursively to a Map with their respective URLs as keys.
     *
     * @param item The navigation item to be mapped.
     * @return A Map containing the URLs of the navigation items as keys and the NavItems themselves as values.
     */
    private static Map<String, NavItem> mapNavItems(@NotNull NavItem item) {
        Map<String, NavItem> map = new HashMap<>();
        mapNavItems(item, map);
        return map;
    }

    /**
     * Maps the navigation items to a map by URL.
     *
     * @param item The navigation item to be mapped.
     * @param map  The map to contain the mapping of URLs and the navigation items.
     */
    private static void mapNavItems(@NotNull NavItem item, @NotNull Map<String, NavItem> map) {
        if (!item.getHref().isBlank()) {
            map.put(item.getHref(), item);
        }
        item.getChildren().forEach(child -> mapNavItems(child, map));
    }

    /**
     * Retrieves a list of documents from navigation based on the provided base URL and hrefs.
     *
     * @param baseUrl  The base URL for the hrefs
     * @param hrefs    The list of hrefs to retrieve documents from.
     * @param parallel Determines whether the document retrieval should be done in parallel or not.
     * @return An Optional containing a Map of documents, where the keys are the hrefs and the values are the corresponding documents.
     * Returns an empty Optional if any document retrieval fails or if there are null values in the resulting map of documents.
     * @throws RuntimeException If the provided base URL is malformed.
     */
    private static Optional<Map<String, Document>> getDocumentListFromNavigation(@NotNull URI baseUrl, @NotNull List<String> hrefs, boolean parallel) {
        final URL url;
        try {
            url = baseUrl.toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        Map<String, Document> docs = new HashMap<>();
        if (!parallel) {
            hrefs.stream()
                    .map(href -> retrieveDocumention(url, href))
                    .forEach(p -> docs.put(p.getKey(), p.getValue()));
        } else {
            int nproc = Runtime.getRuntime().availableProcessors();
            List<Future<ImmutablePair<String, Document>>> lf = new ArrayList<>();
            try (ExecutorService es = Executors.newFixedThreadPool(nproc)) {
                for (var href : hrefs) {
                    Future<ImmutablePair<String, Document>> f = es.submit(() -> retrieveDocumention(url, href));
                    lf.add(f);
                }
            }
            for (var f : lf) {
                try {
                    var p = f.get();
                    docs.put(p.getKey(), p.getValue());
                } catch (InterruptedException | ExecutionException e) {
                    log.error(String.format("Unable to retrieve documentation from future: %s", e));
                    return Optional.empty();
                }
            }
        }
        return docs.values().stream().anyMatch(Objects::isNull) ? Optional.empty() : Optional.of(docs);
    }

    /**
     * Retrieves the documentation for a given URL and href.
     *
     * @param url  The base URL for the href.
     * @param href The href to retrieve the documentation from.
     * @return An ImmutablePair containing the href as the key and the retrieved Document as the value.
     * If the retrieval fails, the value will be null.
     */
    @NotNull
    private static ImmutablePair<String, Document> retrieveDocumention(URL url, String href) {
        try {
            var turl = url.toURI().toString();
            if (!turl.endsWith("/")) {
                turl += '/';
            }
            var turi = URI.create(turl);
            turi = turi.resolve(href);
            var t = DocumentationRetriever.get(turi);
            return ImmutablePair.of(href, t.orElse(null));
        } catch (URISyntaxException e) {
            log.error(String.format("Unable to retrieve documentation for %s: %s", href, e));
            return ImmutablePair.of(href, null);
        }
    }

    /**
     * Retrieves the href values from the provided Node and its descendants in order.
     *
     * @param navItem The Node to retrieve href values from.
     * @return A List of href values in the order they appear in the Node and its descendants.
     */
    private static List<String> getOrderedHrefs(@NotNull NavItem navItem) {
        List<String> list = new ArrayList<>();
        getOrderedHrefs(navItem, list);
        return list;
    }

    /**
     * Retrieves hrefs in order from the provided node and adds them to the given list.
     *
     * @param navItem The node to retrieve hrefs from.
     * @param list    The list to add the retrieved hrefs to.
     */
    private static void getOrderedHrefs(@NotNull NavItem navItem, @NotNull List<String> list) {
        var href = navItem.getHref();
        if (!href.isBlank()) {
            list.add(href);
        }
        for (var child : navItem.getChildren()) {
            getOrderedHrefs(child, list);
        }
    }

}
