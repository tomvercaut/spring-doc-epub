package tv.spring.doc.epub.common.writer

import tv.spring.doc.epub.model.Book
import java.io.IOException
import java.nio.file.Path

interface BookWriter {
    /**
     * Writes the contents of the [Book] to the specified [Path].
     *
     * @param path the path where the book should be written to. Must not be `null`.
     * @param book the book object containing the contents to be written. Must not be `null`.
     */
    @Throws(IOException::class)
    fun write(path: Path, book: Book)
}
