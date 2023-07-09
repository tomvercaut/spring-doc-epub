@file:JvmName("PathChecker")

package tv.spring.doc.epub.common

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Checks if the specified executable file is present and executable in the system's PATH.
 *
 * @param name the name of the executable file to check
 * @return true if the executable file is found and is executable, false otherwise
 */
fun isExec(name: String): Boolean {
    return System.getenv("PATH")
        .split(File.pathSeparator)
        .map { p -> Paths.get(p) }
        .any { p ->
            val exec = p.resolve(name)
            Files.isRegularFile(exec) && Files.isExecutable(exec)
        }
}