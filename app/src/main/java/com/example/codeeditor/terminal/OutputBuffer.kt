package com.example.codeeditor.terminal

/** How a piece of the transcript is shown. */
enum class ChunkStyle { OUTPUT, ERROR, COMMAND, INFO }

data class OutputChunk(val style: ChunkStyle, val text: String)

/**
    Terminal transcript that keeps only the most recent [maxChars] characters,
    so a program that prints forever can't run the app out of memory. Thread-safe.
 */
class OutputBuffer(private val maxChars: Int) {
    private val chunks = ArrayDeque<OutputChunk>()
    private var size = 0
    var truncated = false
        private set
    /** Changes on every modification, so callers can skip unchanged snapshots. */
    @Volatile
    var version = 0L
        private set

    @Synchronized
    fun append(style: ChunkStyle, text: String) {
        if (text.isEmpty()) return
        // Chunks are stored as they arrive and only merged in snapshot(), so trimming stays cheap.
        chunks.addLast(OutputChunk(style, text))
        size += text.length
        version++
        trim()
    }

    @Synchronized
    fun snapshot(): List<OutputChunk> {
        val merged = ArrayList<OutputChunk>()
        val text = StringBuilder()
        var style: ChunkStyle? = null
        for (chunk in chunks) {
            if (chunk.style != style && style != null) {
                merged.add(OutputChunk(style, text.toString()))
                text.setLength(0)
            }
            style = chunk.style
            text.append(chunk.text)
        }
        if (style != null) merged.add(OutputChunk(style, text.toString()))
        return merged
    }

    /** True when empty, so the caller knows whether the next line needs a line break first. */
    @Synchronized
    fun endsWithNewline(): Boolean = chunks.lastOrNull()?.text?.endsWith('\n') ?: true

    @Synchronized
    fun clear() {
        chunks.clear()
        size = 0
        truncated = false
        version++
    }

    private fun trim() {
        while (size > maxChars) {
            truncated = true
            val first = chunks.first()
            val excess = size - maxChars
            if (first.text.length <= excess) {
                chunks.removeFirst()
                size -= first.text.length
            } else {
                chunks[0] = OutputChunk(first.style, first.text.substring(excess))
                size -= excess
            }
        }
    }
}
