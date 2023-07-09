package tv.spring.doc.epub;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import tv.spring.doc.epub.common.PathChecker;
import tv.spring.doc.epub.model.Book;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Log4j2
public class EpubBookWriter implements BookWriter {

    @Override
    public void write(@NotNull Path path, @NotNull Book book) throws IOException {
        final var exec = Pandoc.executable();
        if (!PathChecker.isExec(exec)) {
            throw new RuntimeException(String.format("%s is not an executable on PATH", exec));
        }
        final var doc = book.getDocument();
        File tmpFile = null;
        try {
            // Write a temporary HTML file
            tmpFile = Files.createTempFile("ebook-", null).toFile();
            try (var writer = new BufferedWriter(new FileWriter(tmpFile))) {
                writer.write(doc.outerHtml());
            }
            var builder = new ProcessBuilder("pandoc", "-s", "-f", "html", "-t", "epub", "-o", path.toFile().getCanonicalPath(), tmpFile.getAbsolutePath());
            builder.inheritIO();
            var process = builder.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException(String.format("Something went wrong while running pandoc: %s", String.join(" ", builder.command())));
            }
        } catch (IOException | InterruptedException e) {
            deleteFile(tmpFile);
        }
        deleteFile(tmpFile);
    }

    private static void deleteFile(File tmpFile) {
        if (tmpFile != null) {
            if (!tmpFile.delete()) {
                log.error(String.format("Unable to remove tempoary file: %s", tmpFile.getPath()));
            }
        }
    }

}
