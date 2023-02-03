package com.mayakapps.lrucache.io


internal actual class BufferedOutputStream actual constructor(
    private val base: OutputStream,
    size: Int,
) : OutputStream() {

    private val buffer = ByteArray(size)

    private var count = 0

    /** Flush the internal buffer  */
    private fun flushBuffer() {
        if (count > 0) {
            base.write(buffer, 0, count)
            count = 0
        }
    }

    override fun write(byte: Int) {
        if (count >= buffer.size) flushBuffer()
        buffer[count++] = byte.toByte()
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        if (length >= this.buffer.size) {
            // If the request length exceeds the size of the output buffer, flush the output buffer and then write
            // the data directly. In this way buffered streams will cascade harmlessly.
            flushBuffer()
            base.write(buffer, offset, length)
            return
        }

        if (length > this.buffer.size - count) flushBuffer()

        buffer.copyInto(this.buffer, count, offset, offset + length)
        count += length
    }

    override fun close() {
        safeBase.close()
    }

    override fun flush() {
        flushBuffer()
        base.flush()
    }
}