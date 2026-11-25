package com.example.codeeditor.termux

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellQuotingTest {
    @Test
    fun noCwdLeavesCommandAlone() {
        assertEquals("ls -l", withCwd("ls -l", null))
        assertEquals("ls -l", withCwd("ls -l", ""))
    }

    @Test
    fun cwdWithSpaces() {
        assertEquals("cd -- '/sdcard/My Project' && {\nmake\n}", withCwd("make", "/sdcard/My Project"))
    }

    @Test
    fun cwdWithSingleQuote() {
        assertEquals("'it'\\''s'", q("it's"))
        assertEquals("cd -- '/tmp/it'\\''s' && {\npwd\n}", withCwd("pwd", "/tmp/it's"))
    }

    @Test
    fun cwdWithShellMetacharactersIsLiteral() {
        assertEquals("'\$(rm -rf x); `y`'", q("\$(rm -rf x); `y`"))
    }

    @Test
    fun tildeExpandsToHome() {
        assertEquals("cd -- \"\$HOME\" && {\npwd\n}", withCwd("pwd", "~"))
        assertEquals("cd -- \"\$HOME\"/'my dir' && {\npwd\n}", withCwd("pwd", "~/my dir"))
    }

    @Test
    fun multiLineCommandIsGuardedByCd() {
        assertEquals("cd -- '/x' && {\necho a\necho b\n}", withCwd("echo a\necho b", "/x"))
    }
}
