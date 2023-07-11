package tv.spring.doc.epub.common

import org.apache.commons.lang3.SystemUtils

object Pandoc {
    @JvmStatic
    fun executable(): String {
        return if (SystemUtils.IS_OS_WINDOWS) "pandoc.exe" else "pandoc"
    }
}
