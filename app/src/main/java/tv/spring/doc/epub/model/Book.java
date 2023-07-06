package tv.spring.doc.epub.model;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

@Data
@Log4j2
public class Book {
    private Document document;
    private Element body;

    public Book() {
        document = new Document("");
        body = document.body();
    }

    public void addSection(@NotNull Node node) {
        body.appendChild(node);
    }

}
