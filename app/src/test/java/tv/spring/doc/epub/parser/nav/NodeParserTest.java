package tv.spring.doc.epub.parser.nav;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tv.spring.doc.epub.model.nav.Node;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NodeParserTest {

    Document doc;
    NodeParser nodeParser = new NodeParser();

    @BeforeEach
    public void readFragment() throws IOException {
        InputStream is = this.getClass()
                .getClassLoader()
                .getResourceAsStream("fragment_index.html");
        doc = Jsoup.parse(is, "UTF-8", "");
    }

    @Test
    void nestedNavItems() {
        var li_depth0 = doc.selectFirst("ul>li");
        assertNotNull(li_depth0);
        assertEquals(li_depth0.attr("data-depth"), "0");
        var opt = nodeParser.item(li_depth0);
        assertNotNull(opt);
        assertTrue(opt.isPresent());
        var index = opt.get();
        assertNotNull(index);

        var expected = new Node(0, "", "", List.of(
                new Node(1, "overview.html", "Overview", new ArrayList<>()),
                new Node(1, "core.html", "Core Technologies", List.of(
                        new Node(2, "core/beans.html", "The IoC Container", List.of(
                                new Node(3, "core/beans/introduction.html", "Introduction", List.of()),
                                new Node(3, "core/beans/dependencies.html", "Dependencies", List.of())
                        )),
                        new Node(2, "core/resources.html", "Resources", List.of())
                )),
                new Node(1, "testing.html", "Testing", List.of(
                        new Node(2, "testing/introduction.html", "Introduction to Spring Testing", List.of()),
                        new Node(2, "testing/unit.html", "Unit Testing", List.of())
                ))
        ));

        assertEquals(expected, index);

    }
}