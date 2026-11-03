package io.github.andrearandazzo.papyrus.data.backup

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-protected backups: the ZIP is encrypted with AES-256-GCM, using only the encryption
 * built into Android (no extra library). The key comes from the password through PBKDF2-SHA256
 * with a random salt, so the same password never gives the same key twice.
 *
 * The file is cut into chunks of [CHUNK] bytes, each encrypted and authenticated on its own (the
 * "STREAM" construction): large backups never need to fit in memory, and any change, reordering or
 * cut-off is detected. Each chunk's nonce is a random prefix, the chunk number and a "last chunk"
 * flag; the header (salt, iterations, prefix) is authenticated with every chunk.
 *
 * File layout: header ([HEADER_SIZE] bytes) then chunks of ciphertext + 16-byte tag.
 */
object BackupCrypto {
    private val MAGIC = "PAPYRUS\u0001".toByteArray(Charsets.US_ASCII) // 8 bytes, never changes
    private const val VERSION: Byte = 1
    private const val SALT_SIZE = 16
    private const val PREFIX_SIZE = 7
    const val HEADER_SIZE = 8 + 1 + 4 + SALT_SIZE + PREFIX_SIZE
    private const val TAG_BITS = 128
    private const val TAG_SIZE = TAG_BITS / 8
    const val CHUNK = 64 * 1024

    /** Recommended for PBKDF2-SHA256 (OWASP); stored in each file, so it can grow in future versions. */
    const val ITERATIONS = 600_000

    /** A password-derived key, ready to encrypt or decrypt files that use [header]. */
    class Key internal constructor(internal val secret: SecretKey, internal val header: ByteArray)

    /** True if [start] (the first bytes of a file) is a password-protected backup. */
    fun isEncrypted(start: ByteArray): Boolean =
        start.size >= MAGIC.size && start.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    /** A new key for a new file: fresh salt and nonce prefix. */
    fun newKey(password: CharArray, iterations: Int = ITERATIONS): Key {
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
        val prefix = ByteArray(PREFIX_SIZE).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_SIZE).put(MAGIC).put(VERSION).putInt(iterations).put(salt).put(prefix).array()
        return Key(derive(password, salt, iterations), header)
    }

    /** The key for an existing file, from its [header]. Throws [InvalidBackupException] for unknown files. */
    fun keyFor(password: CharArray, header: ByteArray): Key {
        if (header.size != HEADER_SIZE || !isEncrypted(header)) throw InvalidBackupException("Not an encrypted backup")
        val buffer = ByteBuffer.wrap(header, MAGIC.size, HEADER_SIZE - MAGIC.size)
        if (buffer.get() != VERSION) throw InvalidBackupException("Encrypted with a newer version of Papyrus")
        val iterations = buffer.int
        if (iterations !in 1..10_000_000) throw InvalidBackupException("Damaged header")
        val salt = ByteArray(SALT_SIZE).also { buffer.get(it) }
        return Key(derive(password, salt, iterations), header.copyOf())
    }

    /** Reads the header of [input]: the first [HEADER_SIZE] bytes. */
    fun readHeader(input: InputStream): ByteArray = try {
        ByteArray(HEADER_SIZE).also { DataInputStream(input).readFully(it) }
    } catch (e: EOFException) {
        throw InvalidBackupException("Too short", e)
    }

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): SecretKey {
        val bytes = pbkdf2Sha256(password, salt, iterations)
        return SecretKeySpec(bytes, "AES").also { bytes.fill(0) }
    }

    /** See [Pbkdf2]. */
    internal fun pbkdf2Sha256(password: CharArray, salt: ByteArray, iterations: Int): ByteArray =
        Pbkdf2.sha256(password, salt, iterations)

    /** Writes the header, then everything written to the returned stream, encrypted. Close it to finish. */
    fun encrypting(output: OutputStream, key: Key): OutputStream {
        output.write(key.header)
        return EncryptingStream(output, key)
    }

    /**
     * Decrypts [input] (positioned right after the header). A wrong password is reported as
     * [WrongPasswordException] on the first chunk; a damaged or cut-off file as [InvalidBackupException].
     */
    fun decrypting(input: InputStream, key: Key): InputStream = DecryptingStream(input, key)

    private fun nonce(key: Key, counter: Int, last: Boolean): ByteArray =
        ByteBuffer.allocate(12).put(key.header, HEADER_SIZE - PREFIX_SIZE, PREFIX_SIZE).putInt(counter)
            .put(if (last) 1 else 0).array()

    private fun cipher(mode: Int, key: Key, counter: Int, last: Boolean): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, key.secret, GCMParameterSpec(TAG_BITS, nonce(key, counter, last)))
            updateAAD(key.header)
        }

    /** Buffers one chunk; a full chunk is written only once the next byte proves it isn't the last. */
    private class EncryptingStream(out: OutputStream, private val key: Key) : FilterOutputStream(out) {
        private val buffer = ByteArrayOutputStream(CHUNK)
        private var counter = 0
        private var closed = false

        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)

        override fun write(b: ByteArray, off: Int, len: Int) {
            var offset = off
            var left = len
            while (left > 0) {
                if (buffer.size() == CHUNK) flushChunk(last = false)
                val n = minOf(left, CHUNK - buffer.size())
                buffer.write(b, offset, n)
                offset += n
                left -= n
            }
        }

        private fun flushChunk(last: Boolean) {
            if (counter == Int.MAX_VALUE) throw IllegalStateException("Backup too large")
            out.write(cipher(Cipher.ENCRYPT_MODE, key, counter++, last).doFinal(buffer.toByteArray()))
            buffer.reset()
        }

        override fun flush() = out.flush()

        override fun close() {
            if (closed) return
            closed = true
            try {
                flushChunk(last = true) // always at least one chunk, even for an empty file
                out.flush()
            } finally {
                out.close()
            }
        }
    }

    private class DecryptingStream(private val input: InputStream, private val key: Key) : InputStream() {
        private val sealed = CHUNK + TAG_SIZE
        // One chunk read ahead, to know whether the current one is the last.
        private var next: ByteArray? = readSealed()
        private var plain = ByteArray(0)
        private var position = 0
        private var counter = 0
        private var finished = false

        private fun readSealed(): ByteArray? {
            val bytes = ByteArray(sealed)
            var n = 0
            while (n < sealed) {
                val read = input.read(bytes, n, sealed - n)
                if (read < 0) break
                n += read
            }
            return if (n == 0) null else bytes.copyOf(n)
        }

        private fun fill(): Boolean {
            if (finished) return false
            val current = next ?: throw InvalidBackupException("The backup is cut off")
            next = readSealed()
            val last = next == null
            plain = try {
                cipher(Cipher.DECRYPT_MODE, key, counter, last).doFinal(current)
            } catch (e: AEADBadTagException) {
                if (counter == 0) throw WrongPasswordException(e)
                throw InvalidBackupException("The backup is damaged", e)
            }
            counter++
            position = 0
            finished = last
            return true
        }

        override fun read(): Int {
            val one = ByteArray(1)
            return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xFF
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (len == 0) return 0
            while (position == plain.size) if (!fill()) return -1
            val n = minOf(len, plain.size - position)
            System.arraycopy(plain, position, b, off, n)
            position += n
            return n
        }

        override fun close() = input.close()
    }
}

/** The password doesn't open this backup (or, very rarely, its first bytes are damaged). */
class WrongPasswordException(cause: Throwable? = null) : Exception("Wrong password", cause)
