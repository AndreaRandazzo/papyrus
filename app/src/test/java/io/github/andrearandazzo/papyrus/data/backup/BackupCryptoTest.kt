package io.github.andrearandazzo.papyrus.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.random.Random

class BackupCryptoTest {
    // Few iterations keep the tests fast; the real value is stored in each file.
    private fun encrypt(data: ByteArray, password: String = "correct horse"): ByteArray {
        val out = ByteArrayOutputStream()
        BackupCrypto.encrypting(out, BackupCrypto.newKey(password.toCharArray(), iterations = 1000)).use { it.write(data) }
        return out.toByteArray()
    }

    private fun decrypt(file: ByteArray, password: String = "correct horse"): ByteArray {
        val input = ByteArrayInputStream(file)
        val key = BackupCrypto.keyFor(password.toCharArray(), BackupCrypto.readHeader(input))
        return BackupCrypto.decrypting(input, key).readBytes()
    }

    @Test
    fun roundTripsAnySize() {
        val chunk = BackupCrypto.CHUNK
        for (size in listOf(0, 1, chunk - 1, chunk, chunk + 1, 3 * chunk, 3 * chunk + 17)) {
            val data = Random(size).nextBytes(size)
            assertArrayEquals("size $size", data, decrypt(encrypt(data)))
        }
    }

    @Test
    fun recognizesEncryptedFiles() {
        assertTrue(BackupCrypto.isEncrypted(encrypt(byteArrayOf(1, 2, 3))))
        assertFalse(BackupCrypto.isEncrypted(byteArrayOf(0x50, 0x4B, 3, 4))) // a ZIP starts with "PK"
    }

    @Test
    fun hidesTheContent() {
        val text = "Codice fiscale RSSMRA88C52H501Z".repeat(100).toByteArray()
        val file = encrypt(text)
        assertFalse(String(file, Charsets.ISO_8859_1).contains("RSSMRA"))
        // Same data and password, different file: random salt and nonces.
        assertFalse(file.contentEquals(encrypt(text)))
    }

    @Test(expected = WrongPasswordException::class)
    fun wrongPasswordIsReported() {
        decrypt(encrypt(ByteArray(100)), password = "wrong")
    }

    @Test(expected = InvalidBackupException::class)
    fun changedByteIsDetected() {
        val file = encrypt(Random(1).nextBytes(3 * BackupCrypto.CHUNK))
        file[file.size - 100] = (file[file.size - 100] + 1).toByte()
        decrypt(file)
    }

    @Test(expected = InvalidBackupException::class)
    fun cutOffFileIsDetected() {
        val file = encrypt(Random(2).nextBytes(3 * BackupCrypto.CHUNK))
        // Drop the last chunk exactly: what's left looks complete but isn't marked as the end.
        decrypt(file.copyOf(BackupCrypto.HEADER_SIZE + 2 * (BackupCrypto.CHUNK + 16)))
    }

    @Test(expected = InvalidBackupException::class)
    fun changedHeaderIsDetected() {
        val file = encrypt(Random(3).nextBytes(500))
        file[BackupCrypto.HEADER_SIZE - 1] = (file[BackupCrypto.HEADER_SIZE - 1] + 1).toByte() // nonce prefix
        try {
            decrypt(file)
        } catch (e: WrongPasswordException) {
            throw InvalidBackupException("tampered header looks like a wrong password", e)
        }
    }

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    @Test
    fun pbkdf2MatchesTheStandard() {
        // Published PBKDF2-HMAC-SHA256 test vectors (password "password", salt "salt").
        val salt = "salt".toByteArray()
        assertEquals("120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b",
            hex(BackupCrypto.pbkdf2Sha256("password".toCharArray(), salt, 1)))
        assertEquals("ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43",
            hex(BackupCrypto.pbkdf2Sha256("password".toCharArray(), salt, 2)))
        assertEquals("c5e478d59288c841aa530db6845c4c8d962893a001ce4e11a4963873aa98134a",
            hex(BackupCrypto.pbkdf2Sha256("password".toCharArray(), salt, 4096)))
    }

    @Test
    fun pbkdf2MatchesJavaWithAccents() {
        // Same result as Java's own PBKDF2 (which also uses UTF-8), for a password with accents.
        val password = "perché città 1234".toCharArray()
        val salt = ByteArray(16) { it.toByte() }
        val expected = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(javax.crypto.spec.PBEKeySpec(password, salt, 1000, 256)).encoded
        assertArrayEquals(expected, BackupCrypto.pbkdf2Sha256(password, salt, 1000))
    }

    @Test
    fun pbkdf2HandlesLongAndEmptyPasswords() {
        val salt = ByteArray(16) { (it * 7).toByte() }
        for (password in listOf("", "x".repeat(64), "y".repeat(65), "z".repeat(200))) {
            val expected = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 50, 256)).encoded
            assertArrayEquals("length ${password.length}", expected, BackupCrypto.pbkdf2Sha256(password.toCharArray(), salt, 50))
        }
    }
}
