package tv.spring.doc.epub;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import tv.spring.doc.epub.model.nav.Node;
import tv.spring.doc.epub.parser.nav.NodeParser;

@Log4j2
public class SpringDocument {

    private final String baseUrl;
    private Document doc;
    private Element body;
    private final NodeParser nodeParser = new NodeParser();
    @Getter
    private Node nav;

    public SpringDocument(@NotNull String baseUrl) {
        this.baseUrl = baseUrl;
        this.doc = null;
        this.body = null;
        this.nav = null;
    }

    public void build() {
        doc = null;
        body = null;
        nav = null;
        var opt = Http.get(baseUrl);
        opt.ifPresent(document -> doc = document);
        if (doc != null) {
            body = doc.body();
        }
        var optNode = nodeParser.body(body);
        if (optNode.isEmpty()) {
            log.error("Unable to create a navigation tree node from: "+ baseUrl);
            return;
        }
        this.nav = optNode.get();
    }

    public Node getNavigation() {
        return nav;
    }
}
