package tv.spring.doc.epub;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AppOptions {
    private String springVersion = "latest";
    private String project = "spring-framework";
    private String outputDir = "output";
}
