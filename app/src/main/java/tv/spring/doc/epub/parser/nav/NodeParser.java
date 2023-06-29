package tv.spring.doc.epub.parser.nav;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;
import tv.spring.doc.epub.model.nav.Node;

import java.util.Optional;

/**
 * The {@code NodeParser} class is responsible for parsing HTML elements and retrieving
 * an {@link Node} object representing a navigation tree.
 */
@Log4j2
public class NodeParser {

    /**
     * Retrieves a Node representing a navigation tree from an HTML body element.
     * <p>
     * Expected HTML structure:
     * <pre>{@code
     *     <body>
     *         ...
     *         <nav class="nav-menu">
     *             <ul class="nav-list">
     *                 <li class="nav-item" data-depth="0">
     *                     ...
     *                 </li>
     *             </ul>
     *         </nav>
     *     </body>
     * }</pre>
     * <p>
     * The function uses {@link NodeParser#item(Element)} to process the (nested) li elements inside the "nav-list" ul element.
     * @param body the HTML body element
     * @return An Optional containing an Index object if the required conditions are met, otherwise an empty Optional
     * Required conditions:
     * <ul>
     *     <li>input element is a body HTML element</li>
     *     <li>a nested nav element exists: &lt;nav class="nav-menu"&gt;</li>
     *     <li>a nested ul element exists: &lt;ul class="nav-list"&gt;</li>
     *     <li>a li element in the above required nav-list ul element</li>
     * </ul>
     */
    public Optional<Node> body(@NotNull Element body) {
        if (!body.tagName().equals("body")) {
            log.error(String.format("Expected a <body> HTML element but got %s", body.tagName()));
            return Optional.empty();
        }
        var nav = body.selectFirst("nav[class=nav-menu]");
        if (nav == null) {
            log.error("Expected a <nav> HTML element but got null.\n" +
                    "Unable to find nav-menu in navigation bar.");
            return Optional.empty();
        }
        var ul = nav.selectFirst("ul[class=nav-list]");
        if (ul == null) {
            log.error("Expected a <ul> HTML element but got null.\n" +
                    "Unable to find nav-list in navigation bar.");
            return Optional.empty();
        }
        var li = ul.selectFirst("li");
        if (li == null) {
            log.error("Expected a <li> HTML element but got null.\n" +
                    "Unable to find nav-list item in navigation bar.");
            return Optional.empty();
        }
        return item(li);
    }

    /**
     * Retrieves an Index representing a navigation tree/item from an HTML li element.
     * <p>
     * Expected HTML structure:
     * <pre>{@code
     *     <li class="nav-item" data-depth="0">
     *         <a class="nav-link" href="page.html">HTML page</a>
     *         <ul class="nav-list">
     *              <li class="nav-item" data-depth="1">
     *              <a class="nav-link" href="page.html">HTML page</a>
     *              <ul class="nav-list">
     *                  ...
     *              </ul>
     *         </ul>
     *     </li>
     * }</pre>
     *
     * @param li The li element to retrieve the navigation node.
     * @return An Optional containing the Index object if the li element matches the required conditions, otherwise an empty Optional.
     * Required conditions:
     * <ul>
     *     <li>input HTML element is an HTML li element</li>
     *     <li>input li element has a class attribute equal to "nav-item"</li>
     * </ul>
     */
    public Optional<Node> item(@NotNull Element li) {
        if (!li.tagName().equals("li")) {
            log.error(String.format("Expected a <li> HTML element but got %s", li.tagName()));
            return Optional.empty();
        }
        if (!li.attr("class").equals("nav-item")) {
            log.error("Expected a <li> HTML element to contain a class equal to 'nav-item'");
            return Optional.empty();
        }
        int depth = Integer.parseInt(li.attr("data-depth"), 10);
        var node = new Node();
        node.setDepth(depth);

        var a = li.selectFirst("> a[href]");
        if (a != null) {
            node.setName(a.text());
            node.setHref(a.attr("href"));
        }
        var uls = li.select("> ul[class=nav-list]");
        for (Element ul : uls) {
            var lis = ul.select("> li[class=nav-item]");
            for(Element tis : lis) {
                var opt = item(tis);
                if (opt.isEmpty()) {
                    return Optional.empty();
                }
                node.getChildren().add(opt.get());
            }
        }

        return Optional.of(node);
    }
}
