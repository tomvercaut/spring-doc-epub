package tv.spring.doc.epub.common.parser

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tv.spring.doc.epub.model.NavItem
import tv.spring.doc.epub.service.DocumentationRetrievalService
import tv.spring.doc.epub.service.DownloadService
import java.io.IOException
import java.net.URI

internal class NavItemParserTest {
    private var doc: Document? = null
    @BeforeEach
    @Throws(IOException::class)
    fun readFragment() {
        val stream = this.javaClass
            .getClassLoader()
            .getResourceAsStream("fragment_index.html")!!
        doc = Jsoup.parse(stream, "UTF-8", "")
    }

    @Test
    fun nestedNavItems() {
        val navTreeParser = NavTreeParser(DocumentationRetrievalService(DownloadService()))
        val liDepth0 = doc!!.selectFirst("ul>li")
        Assertions.assertNotNull(liDepth0)
        Assertions.assertEquals(liDepth0!!.attr("data-depth"), "0")
        val result = navTreeParser.item(liDepth0, URI.create(""))
        Assertions.assertNotNull(result)
        Assertions.assertTrue(result.isSuccess)
        val index = result.getOrThrow()
        Assertions.assertNotNull(index)
        val expected = NavItem(
            0, "", "", mutableListOf(
                NavItem(1, "overview.html", "Overview", ArrayList()),
                NavItem(
                    1, "core.html", "Core Technologies", mutableListOf(
                        NavItem(
                            2, "core/beans.html", "The IoC Container", mutableListOf(
                                NavItem(3, "core/beans/introduction.html", "Introduction", mutableListOf()),
                                NavItem(3, "core/beans/dependencies.html", "Dependencies", mutableListOf())
                            )
                        ),
                        NavItem(2, "core/resources.html", "Resources", mutableListOf())
                    )
                ),
                NavItem(
                    1, "testing.html", "Testing", mutableListOf(
                        NavItem(2, "testing/introduction.html", "Introduction to Spring Testing", mutableListOf()),
                        NavItem(2, "testing/unit.html", "Unit Testing", mutableListOf())
                    )
                )
            )
        )
        Assertions.assertEquals(expected, index)
    }
}