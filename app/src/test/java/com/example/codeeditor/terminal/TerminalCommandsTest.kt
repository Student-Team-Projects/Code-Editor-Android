package com.example.codeeditor.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TerminalCommandsTest {
    @Test
    fun parsesBareCd() {
        assertEquals(TerminalLine.Cd(""), TerminalLine.parse("cd"))
        assertEquals(TerminalLine.Cd("build"), TerminalLine.parse("  cd   build "))
        assertEquals(TerminalLine.Cd("'My Project'"), TerminalLine.parse("cd 'My Project'"))
        assertEquals(TerminalLine.Cd("\$PREFIX/bin"), TerminalLine.parse("cd \$PREFIX/bin"))
    }

    @Test
    fun cdWithOperatorsIsAnOrdinaryCommand() {
        for (line in listOf("cd build && make", "cd x; ls", "cd \$(pwd)", "cd `pwd`", "cd x | cat", "cdx", "echo cd")) {
            assertEquals(line, TerminalLine.Command(line), TerminalLine.parse(line))
        }
    }

    @Test
    fun parsesClear() {
        assertEquals(TerminalLine.Clear, TerminalLine.parse(" clear "))
        assertEquals(TerminalLine.Command("clear x"), TerminalLine.parse("clear x"))
    }

    @Test
    fun cdCommandPrintsTheNewDirectory() {
        assertEquals("cd && pwd", cdCommand(""))
        assertEquals("cd build && pwd", cdCommand("build"))
    }

    @Test
    fun terminalCommandDisablesPythonBuffering() {
        assertEquals("export PYTHONUNBUFFERED=1\nls", terminalCommand("ls"))
    }

    @Test
    fun stripsAnsiEscapes() {
        assertEquals("red plain", stripAnsi("\u001B[1;31mred\u001B[0m plain"))
        assertEquals("text", stripAnsi("\u001B]0;title\u0007text"))
        assertEquals("ab", stripAnsi("a\u001B[2Kb"))
        assertEquals("no escapes", stripAnsi("no escapes"))
    }

    @Test
    fun displaysHomeAsTilde() {
        assertEquals("~", displayPath(null))
        assertEquals("~", displayPath(TERMUX_HOME))
        assertEquals("~/proj", displayPath("$TERMUX_HOME/proj"))
        assertEquals("/storage/emulated/0/p", displayPath("/storage/emulated/0/p"))
        assertEquals("$TERMUX_HOME-other", displayPath("$TERMUX_HOME-other"))
        assertEquals("~/proj $", promptFor("$TERMUX_HOME/proj"))
    }

    @Test
    fun mapsPickedFoldersToPaths() {
        val ext = "com.android.externalstorage.documents"
        assertEquals("/storage/emulated/0/Documents/proj", externalStoragePath(ext, "primary:Documents/proj"))
        assertEquals("/storage/emulated/0", externalStoragePath(ext, "primary:"))
        assertEquals("/storage/1A2B-3C4D/code", externalStoragePath(ext, "1A2B-3C4D:code"))
        assertNull(externalStoragePath("com.google.android.apps.docs.storage", "primary:x"))
        assertNull(externalStoragePath(ext, "no-volume"))
        assertNull(externalStoragePath(null, "primary:x"))
    }
}
