package io.github.andrearandazzo.papyrus.data.backup

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * PBKDF2-HMAC-SHA256 (RFC 8018) for one 32-byte block, password as UTF-8.
 *
 * Android's own PBKDF2 is a slow pure-Java implementation, and calling the platform's native HMAC
 * once per round costs more in call overhead than in hashing (over 30 s for 600 000 rounds). So,
 * like fast implementations do, HMAC's two padded keys are hashed once and every round is just two
 * SHA-256 compressions on fixed-size blocks, with no allocation. Only the first round, which
 * includes the salt, uses the platform HMAC.
 */
internal object Pbkdf2 {
    fun sha256(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        require(iterations >= 1)
        val key = Charsets.UTF_8.encode(java.nio.CharBuffer.wrap(password)).let { b -> ByteArray(b.remaining()).also { b.get(it) } }
        try {
            // U1 = HMAC(P, S || INT(1)), with the platform HMAC.
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(key.takeIf { it.isNotEmpty() } ?: byteArrayOf(0), "HmacSHA256"))
            mac.update(salt)
            val u1 = mac.doFinal(byteArrayOf(0, 0, 0, 1))

            // The key padded to 64 bytes (hashed first if longer), xored with ipad / opad.
            val block = ByteArray(64)
            (if (key.size > 64) java.security.MessageDigest.getInstance("SHA-256").digest(key) else key).copyInto(block)
            val inner = IntArray(8)
            val outer = IntArray(8)
            val w = IntArray(64)
            initState(inner)
            initState(outer)
            compress(inner, toWords(ByteArray(64) { (block[it].toInt() xor 0x36).toByte() }), w)
            compress(outer, toWords(ByteArray(64) { (block[it].toInt() xor 0x5c).toByte() }), w)
            block.fill(0)

            // Each round hashes 32 bytes after the 64-byte key block: one block, fixed padding,
            // message length (64 + 32) * 8 = 768 bits.
            val u = IntArray(16)
            for (i in 0 until 8) u[i] = word(u1, i * 4)
            u[8] = 0x80000000.toInt()
            u[15] = 768
            val result = u.copyOf(8)
            val state = IntArray(8)
            repeat(iterations - 1) {
                inner.copyInto(state)
                compress(state, u, w)
                state.copyInto(u, 0, 0, 8)
                outer.copyInto(state)
                compress(state, u, w)
                state.copyInto(u, 0, 0, 8)
                for (i in 0 until 8) result[i] = result[i] xor u[i]
            }
            return ByteArray(32).also { out -> for (i in 0 until 8) putWord(out, i * 4, result[i]) }
        } finally {
            key.fill(0)
        }
    }

    private val K = intArrayOf(
        0x428a2f98, 0x71374491, -0x4a3f0431, -0x164a245b, 0x3956c25b, 0x59f111f1, -0x6dc07d5c, -0x54e3a12b,
        -0x27f85568, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, -0x7f214e02, -0x6423f959, -0x3e640e8c,
        -0x1b64963f, -0x1041b87a, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
        -0x67c1aeae, -0x57ce3993, -0x4ffcd838, -0x40a68039, -0x391ff40d, -0x2a586eb9, 0x06ca6351, 0x14292967,
        0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, -0x7e3d36d2, -0x6d8dd37b,
        -0x5d40175f, -0x57e599b5, -0x3db47490, -0x3893ae5d, -0x2e6d17e7, -0x2966f9dc, -0xbf1ca7b, 0x106aa070,
        0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
        0x748f82ee, 0x78a5636f, -0x7b3787ec, -0x7338fdf8, -0x6f410006, -0x5baf9315, -0x41065c09, -0x398e870e,
    )

    private fun initState(s: IntArray) {
        s[0] = 0x6a09e667; s[1] = -0x4498517b; s[2] = 0x3c6ef372; s[3] = -0x5ab00ac6
        s[4] = 0x510e527f; s[5] = -0x64fa9774; s[6] = 0x1f83d9ab; s[7] = 0x5be0cd19
    }

    private fun word(b: ByteArray, i: Int) =
        (b[i].toInt() shl 24) or ((b[i + 1].toInt() and 0xff) shl 16) or ((b[i + 2].toInt() and 0xff) shl 8) or (b[i + 3].toInt() and 0xff)

    private fun putWord(b: ByteArray, i: Int, v: Int) {
        b[i] = (v ushr 24).toByte(); b[i + 1] = (v ushr 16).toByte(); b[i + 2] = (v ushr 8).toByte(); b[i + 3] = v.toByte()
    }

    private fun toWords(block: ByteArray): IntArray =
        IntArray(16) { word(block, it * 4) }.also { block.fill(0) }

    /** One SHA-256 compression of the 16-word [block] into [s]; [w] is scratch space. */
    private fun compress(s: IntArray, block: IntArray, w: IntArray) {
        for (t in 0 until 16) w[t] = block[t]
        for (t in 16 until 64) {
            val a = w[t - 15]
            val b = w[t - 2]
            val s0 = Integer.rotateRight(a, 7) xor Integer.rotateRight(a, 18) xor (a ushr 3)
            val s1 = Integer.rotateRight(b, 17) xor Integer.rotateRight(b, 19) xor (b ushr 10)
            w[t] = w[t - 16] + s0 + w[t - 7] + s1
        }
        var a = s[0]; var b = s[1]; var c = s[2]; var d = s[3]
        var e = s[4]; var f = s[5]; var g = s[6]; var h = s[7]
        for (t in 0 until 64) {
            val t1 = h + (Integer.rotateRight(e, 6) xor Integer.rotateRight(e, 11) xor Integer.rotateRight(e, 25)) +
                ((e and f) xor (e.inv() and g)) + K[t] + w[t]
            val t2 = (Integer.rotateRight(a, 2) xor Integer.rotateRight(a, 13) xor Integer.rotateRight(a, 22)) +
                ((a and b) xor (a and c) xor (b and c))
            h = g; g = f; f = e; e = d + t1; d = c; c = b; b = a; a = t1 + t2
        }
        s[0] += a; s[1] += b; s[2] += c; s[3] += d; s[4] += e; s[5] += f; s[6] += g; s[7] += h
    }
}
