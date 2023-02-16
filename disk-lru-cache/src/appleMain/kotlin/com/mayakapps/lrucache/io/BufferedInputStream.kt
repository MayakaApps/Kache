package com.mayakapps.lrucache.io


internal actual class BufferedInputStream actual constructor(
    base: InputStream,
    size: Int,
) : InputStream() {

    init {
        check(size > 0) { "Buffer size <= 0" }
    }

    private var base: InputStream? = base
    private var buffer: ByteArray? = ByteArray(size)

    private var pos = 0
    private var count = 0

    private fun getBaseIfOpen() = base ?: throw IOException("Stream closed")

    private fun getBufferIfOpen() = buffer ?: throw IOException("Stream closed")

    private fun fill() {
        pos = 0
        count = getBaseIfOpen().read(getBufferIfOpen())
    }

    override fun read(): Int {
        if (pos >= count) {
            fill()
            if (pos >= count) return -1
        }

        return getBufferIfOpen()[pos++].toInt() and 0xff
    }

    private fun read1(buffer: ByteArray, offset: Int, length: Int): Int {
        if (pos >= count) {
            // If the requested length is at least as large as the buffer, do not bother to copy the bytes into the
            // local buffer.  In this way buffered streams will cascade harmlessly.
            if (length >= getBufferIfOpen().size) {
                return getBaseIfOpen().read(buffer, offset, length)
            }

            fill()
            if (pos >= count) return -1
        }

        val avail = count - pos
        val cnt = if (avail < length) avail else length
        getBufferIfOpen().copyInto(buffer, offset, pos, pos + cnt)
        pos += cnt
        return cnt
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (this.buffer == null) throw IOException("Stream closed")
        if (offset < 0 || length < 0 || buffer.size - (offset + length) < 0) throw IndexOutOfBoundsException()
        if (length == 0) return 0

        var n = 0
        while (true) {
            val nread = read1(buffer, offset + n, length - n)
            if (nread <= 0) return if (n == 0) nread else n
            val reachedEOF = nread < length - n
            n += nread
            if (n >= length) return n
            // if not closed but no bytes available, return
            if (base != null && reachedEOF) return n
        }
    }

    override fun close() {
        buffer = null
        getBaseIfOpen().close()
        base = null
    }
}