package tv.spring.doc.epub.common.writer

import io.github.oshai.kotlinlogging.KotlinLogging
import tv.spring.doc.epub.common.Pandoc
import tv.spring.doc.epub.common.isExec
import tv.spring.doc.epub.model.Book
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class EpubBookWriter : BookWriter {
    @Throws(IOException::class)
    override fun write(path: Path, book: Book) {
        val exec: String = Pandoc.executable()
        if (!isExec(exec)) {
            throw RuntimeException(String.format("%s is not an executable on PATH", exec))
        }
        val doc = book.getDocument()
        var tmpFile: File? = null
        try {
            // Write a temporary HTML file
            tmpFile = Files.createTempFile("ebook-", null).toFile()
            BufferedWriter(FileWriter(tmpFile!!)).use { writer -> writer.write(doc.outerHtml()) }
            val builder = ProcessBuilder(
                "pandoc",
                "-s",
                "-f",
                "html",
                "-t",
                "epub",
                "-o",
                path.toFile().getCanonicalPath(),
                tmpFile.absolutePath
            )
            builder.inheritIO()
            val process = builder.start()
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw RuntimeException(
                    String.format(
                        "Something went wrong while running pandoc: %s",
                        java.lang.String.join(" ", builder.command())
                    )
                )
            }
        } catch (e: IOException) {
            deleteFile(tmpFile)
        } catch (e: InterruptedException) {
            deleteFile(tmpFile)
        }
        deleteFile(tmpFile)
    }

    companion object {
        private val log = KotlinLogging.logger{}
        private fun deleteFile(tmpFile: File?) {
            if (tmpFile != null) {
                if (!tmpFile.delete()) {
                    log.error(String.format("Unable to remove tempoary file: %s", tmpFile.path))
                }
            }
        }
    }
}
