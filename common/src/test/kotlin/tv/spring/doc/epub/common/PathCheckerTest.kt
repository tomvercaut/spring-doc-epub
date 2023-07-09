package tv.spring.doc.epub.common

import org.apache.commons.lang3.SystemUtils
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PathCheckerTest {

    @Test
    fun isExec() {
        var exec = "ls"
        if (SystemUtils.IS_OS_WINDOWS) {
            exec = "cmd.exe"
        }
        assertTrue(isExec(exec))
        assertFalse(isExec("abcdefghijklmnopqrstuvwxyz"))
    }
}