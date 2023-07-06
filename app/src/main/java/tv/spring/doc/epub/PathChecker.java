package tv.spring.doc.epub;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Log4j2
public class PathChecker {
    /**
     * Checks if the specified name corresponds to an executable file in the system's PATH environment variable.
     *
     * @param name the name of the executable file to check
     * @return true if the executable file is found in the system's PATH and is both a regular file and set as executable, otherwise false
     */
    public static boolean isExec(@NotNull String name) {
        return Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator)))
                .map(Paths::get)
                .anyMatch(path -> {
                    var exec = path.resolve(name);
                    return Files.isRegularFile(exec) && Files.isExecutable(exec);
                });
    }
}
