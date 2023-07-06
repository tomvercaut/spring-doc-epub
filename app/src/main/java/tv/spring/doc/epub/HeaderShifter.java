package tv.spring.doc.epub;

import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import tv.spring.doc.epub.model.nav.NavItem;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class HeaderShifter {

    private static final Pattern patternHtmlH = Pattern.compile("h(\\d)");

    /**
     * Shifts HTML headers (h1, h2, ...) in the articles based on the corresponding depth of the navigation item for the URL of the article.
     *
     * @param hrefs          The list of hrefs corresponding to the articles to shift headers in.
     * @param navTreeByHref  The map of hrefs to nav items, used to determine the depth of each article.
     * @param articlesByHref The map of hrefs to articles, used to access the articles to shift headers in.
     */
    public static void shift(@NotNull List<String> hrefs, @NotNull Map<String, NavItem> navTreeByHref, @NotNull Map<String, Node> articlesByHref) {
        for (var href : hrefs) {
            final var navItem = navTreeByHref.get(href);
            var article = articlesByHref.get(href);
            shift(article, navItem.getDepth());
        }
    }

    /**
     * Shifts HTML headers (h1, h2, ...) in the provided node and its children by the specified value.
     *
     * @param node  The node to shift headers in.
     * @param depth The depth by which to shift the headers (1 based).
     * @throws NumberFormatException If the header level cannot be parsed as an integer.
     */
    private static void shift(@NotNull Node node, int depth) throws NumberFormatException {
        if (node instanceof Element element) {
            var name = element.tagName();
            var matcher = patternHtmlH.matcher(name);
            if (matcher.matches()) {
                int level = Integer.parseInt(matcher.group(1)) + depth - 1;
                element.tagName("h" + level);
            }
        }
        for (var child : node.childNodes()) {
            shift(child, depth);
        }
    }
}
