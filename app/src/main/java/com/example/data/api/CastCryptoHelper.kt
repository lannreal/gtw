package com.example.data.api

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CastCryptoHelper {

    private fun bufferToBase64Url(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
    
    private fun base64UrlToBuffer(str: String): ByteArray {
        return Base64.decode(str, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun toFixedLength(bytes: ByteArray, length: Int): ByteArray {
        if (bytes.size == length) return bytes
        if (bytes.size > length) return bytes.copyOfRange(bytes.size - length, bytes.size)
        val padded = ByteArray(length)
        System.arraycopy(bytes, 0, padded, length - bytes.size, bytes.size)
        return padded
    }

    // Convert Java DER ASN.1 Signature to Raw 64-byte IEEE P1363
    private fun derToRawSignature(der: ByteArray): ByteArray {
        var offset = 2
        if (der[1] < 0) offset += (der[1].toInt() and 0x7F)
        
        offset++
        val rLen = der[offset++].toInt()
        val rBytes = der.copyOfRange(offset, offset + rLen)
        offset += rLen
        
        offset++
        val sLen = der[offset++].toInt()
        val sBytes = der.copyOfRange(offset, offset + sLen)
        
        val raw = ByteArray(64)
        val rStart = if (rBytes.size > 32) rBytes.size - 32 else 0
        val rCopyLen = Math.min(32, rBytes.size)
        System.arraycopy(rBytes, rStart, raw, 32 - rCopyLen, rCopyLen)
        
        val sStart = if (sBytes.size > 32) sBytes.size - 32 else 0
        val sCopyLen = Math.min(32, sBytes.size)
        System.arraycopy(sBytes, sStart, raw, 64 - sCopyLen, sCopyLen)
        
        return raw
    }

    fun generateAttestationPayload(challengeId: String, nonce: String): JSONObject {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = kpg.generateKeyPair()
        val pubKey = keyPair.public as ECPublicKey

        val x = bufferToBase64Url(toFixedLength(pubKey.w.affineX.toByteArray(), 32))
        val y = bufferToBase64Url(toFixedLength(pubKey.w.affineY.toByteArray(), 32))
        val jwk = JSONObject().apply {
            put("kty", "EC")
            put("crv", "P-256")
            put("x", x)
            put("y", y)
            put("ext", true)
        }

        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(keyPair.private)
        sig.update(nonce.toByteArray(Charsets.UTF_8))
        val derSignature = sig.sign()
        val rawSignature = derToRawSignature(derSignature)
        val signatureB64 = bufferToBase64Url(rawSignature)

        val client = JSONObject().apply {
            put("user_agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            put("pixel_ratio", 1)
            put("screen_width", 1920)
            put("screen_height", 1080)
            put("color_depth", 24)
            put("languages", JSONArray(listOf("en-US", "en", "id")))
            put("platform", "Win32")
            put("hardware_concurrency", 8)
            put("device_memory", 8)
            put("touch_support", false)
        }

        return JSONObject().apply {
            put("viewer_id", "")
            put("device_id", "")
            put("challenge_id", challengeId)
            put("nonce", nonce)
            put("signature", signatureB64)
            put("public_key", jwk)
            put("client", client)
            put("storage", JSONObject())
            put("attributes", JSONObject().put("entropy", "user_agent,languages,platform,screen,color_depth,hardware_concurrency,device_memory"))
        }
    }

    private val be = 512
    private val lt = be - 1
    private val dr = 2
    private val lr = 2654435761u
    private val hr = 2246822519u

    private fun re(t: UInt, e: Int): UInt = (t shl e) or (t shr (32 - e))
    private fun ht(t: UInt, e: UInt): UInt = t * e

    private fun ye(t: UIntArray) {
        t[0] = t[0] + t[1]; t[3] = re(t[3] xor t[0], 16)
        t[2] = t[2] + t[3]; t[1] = re(t[1] xor t[2], 12)
        t[0] = t[0] + t[1]; t[3] = re(t[3] xor t[0], 8)
        t[2] = t[2] + t[3]; t[1] = re(t[1] xor t[2], 7)
    }

    private fun gr(t: ByteArray): UIntArray {
        val e = uintArrayOf(1779033703u, 3144134277u, 1013904242u, 2773480762u)
        for (i in t.indices) {
            e[0] = e[0] + (t[i].toUInt() and 255u)
            e[0] = re(e[0], 7)
            ye(e)
        }
        for (i in 0 until 8) ye(e)
        val r = UIntArray(be)
        for (i in 0 until be) {
            ye(e)
            r[i] = e[0] xor e[2]
        }
        for (i in 0 until dr) {
            for (s in 0 until be) {
                val a = (r[s] and lt.toUInt()).toInt()
                var c = r[s] + r[a]
                c = re(c, 13)
                c = c xor ht(r[(s + 1) and lt], lr)
                r[s] = c
                e[0] = e[0] xor c
                ye(e)
            }
        }
        val n = UIntArray(8)
        val o = be / 8
        for (i in 0 until 8) {
            ye(e)
            var s = e[0]
            val a = i * o
            for (c in 0 until o) {
                val d = r[a + c]
                s = s + d
                s = re(s, 5)
                s = s xor ht(d, hr)
            }
            n[i] = s xor e[2]
        }
        return n
    }

    private fun wr(t: UIntArray): Int {
        var e = 0
        for (r in t.indices) {
            val n = t[r]
            if (n == 0u) { e += 32; continue }
            return e + Integer.numberOfLeadingZeros(n.toInt())
        }
        return e
    }

    fun solvePoW(nonce: String, difficulty: Int, timeoutMs: Long = 20000L): String? {
        if (difficulty <= 0) return "0"
        val prefix = "$nonce:"
        val start = System.currentTimeMillis()
        var s = 0
        while (true) {
            for (c in 0 until 1024) {
                val input = (prefix + s).toByteArray(Charsets.UTF_8)
                val d = gr(input)
                if (wr(d) >= difficulty) return s.toString()
                s++
            }
            if (System.currentTimeMillis() - start > timeoutMs) return null
        }
    }

    private fun getQa(): Map<String, IntArray> {
        val map = mutableMapOf<String, IntArray>()
        for (n in 1..20) {
            map[n.toString()] = intArrayOf(n xor 0, (31 - n) xor 0)
        }
        return map
    }

    private fun getEa(version: String, len: Int): IntArray {
        val r = version.trim()
        val o = getQa()[r] ?: return intArrayOf()
        val a = o[0]
        val i = o[1]
        if (a < 1 || i < 1 || a > len || i > len) return intArrayOf()
        return intArrayOf(a, i)
    }

    private fun ws(keyParts: List<String>, version: String): List<String> {
        val r = getEa(version, keyParts.size)
        if (r.isEmpty()) return keyParts
        val n = r.map { keyParts[it - 1] }
        return if (n.isNotEmpty()) n else keyParts
    }

    fun decryptPlaybackPayload(playback: JSONObject): JSONObject {
        val keyPartsArr = playback.optJSONArray("key_parts")
        val keyParts = mutableListOf<String>()
        if (keyPartsArr != null) {
            for (i in 0 until keyPartsArr.length()) {
                keyParts.add(keyPartsArr.getString(i))
            }
        }
        val version = playback.optString("version", "")
        
        val validParts = ws(keyParts, version)
        var keyBytes = ByteArray(0)
        for (part in validParts) {
            val buf = base64UrlToBuffer(part)
            val newArr = ByteArray(keyBytes.size + buf.size)
            System.arraycopy(keyBytes, 0, newArr, 0, keyBytes.size)
            System.arraycopy(buf, 0, newArr, keyBytes.size, buf.size)
            keyBytes = newArr
        }
        
        val iv = base64UrlToBuffer(playback.getString("iv"))
        val encryptedData = base64UrlToBuffer(playback.getString("payload"))
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        val secretKey = SecretKeySpec(keyBytes, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        
        val decrypted = cipher.doFinal(encryptedData)
        return JSONObject(String(decrypted))
    }
}
