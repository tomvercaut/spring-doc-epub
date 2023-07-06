package tv.spring.doc.epub;

import org.jetbrains.annotations.NotNull;
import tv.spring.doc.epub.model.Book;

import java.io.IOException;
import java.nio.file.Path;

public interface BookWriter {

    /**
     * Writes the contents of the {@link Book} to the specified {@link Path}.
     *
     * @param path the path where the book should be written to. Must not be {@code null}.
     * @param book the book object containing the contents to be written. Must not be {@code null}.
     */
    void write(@NotNull Path path, @NotNull Book book) throws IOException;
}
