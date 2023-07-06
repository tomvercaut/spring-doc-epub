package tv.spring.doc.epub;

import org.apache.commons.lang3.SystemUtils;

public class Pandoc {

    public static String executable() {
        return SystemUtils.IS_OS_WINDOWS ? "pandoc.exe" : "pandoc";
    }
}
