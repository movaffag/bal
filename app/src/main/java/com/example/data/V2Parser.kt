package com.example.data

import android.util.Base64
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object V2Parser {

    /**
     * Decode standard or URL-safe base64 string, correcting padding if needed.
     */
    fun decodeBase64(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        
        // Normalize characters for URL-safe base64
        var normalized = trimmed.replace('-', '+').replace('_', '/')
        
        // Add missing padding
        val r = normalized.length % 4
        if (r > 0) {
            normalized += "=".repeat(4 - r)
        }
        
        return try {
            val bytes = Base64.decode(normalized, Base64.DEFAULT)
            String(bytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            // Fallback for custom or corrupted base64
            try {
                val bytes = Base64.decode(trimmed, Base64.NO_WRAP or Base64.URL_SAFE)
                String(bytes, StandardCharsets.UTF_8)
            } catch (e2: Exception) {
                ""
            }
        }
    }

    /**
     * Parses a list of node URLs from a raw subscription body (could be base64 or plaintext).
     */
    fun parseSubscription(subscriptionId: Int, content: String): List<V2RayNodeEntity> {
        val decoded = decodeBase64(content)
        val textToParse = if (decoded.isNotEmpty() && (decoded.contains("://") || decoded.length > 20)) {
            decoded
        } else {
            content
        }

        val lines = textToParse.split(Regex("[\\r\\n]+"))
        val nodes = mutableListOf<V2RayNodeEntity>()

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            try {
                val node = parseNodeUrl(subscriptionId, trimmedLine)
                if (node != null) {
                    nodes.add(node)
                }
            } catch (e: Exception) {
                // Skip invalid configs
            }
        }

        return nodes
    }

    /**
     * Parses an individual V2Ray config URI.
     */
    fun parseNodeUrl(subscriptionId: Int, url: String): V2RayNodeEntity? {
        if (!url.contains("://")) return null

        val scheme = url.substringBefore("://").lowercase()
        return when (scheme) {
            "vmess" -> parseVmess(subscriptionId, url)
            "vless" -> parseUriProtocol(subscriptionId, "VLESS", url)
            "ss" -> parseShadowsocks(subscriptionId, url)
            "trojan" -> parseUriProtocol(subscriptionId, "TROJAN", url)
            else -> null
        }
    }

    private fun parseVmess(subscriptionId: Int, url: String): V2RayNodeEntity? {
        val rawBase64 = url.removePrefix("vmess://").trim()
        val decodedJson = decodeBase64(rawBase64)
        if (decodedJson.isEmpty()) return null

        return try {
            val json = JSONObject(decodedJson)
            val address = json.optString("add", "")
            val port = json.optInt("port", 0)
            val name = json.optString("ps", "VMESS Server")
            
            if (address.isNotEmpty() && port > 0) {
                V2RayNodeEntity(
                    subscriptionId = subscriptionId,
                    name = URLDecoder.decode(name, "UTF-8"),
                    protocol = "VMESS",
                    address = address,
                    port = port,
                    rawUrl = url
                )
            } else {
                null
            }
        } catch (e: Exception) {
            // Let's try parsing vmess as standard URI as fallback
            parseUriProtocol(subscriptionId, "VMESS", url)
        }
    }

    private fun parseShadowsocks(subscriptionId: Int, url: String): V2RayNodeEntity? {
        // ss://[base64_credentials]@host:port#Name
        val cleanUrl = url.removePrefix("ss://").trim()
        val hashIndex = cleanUrl.indexOf('#')
        val name = if (hashIndex != -1) {
            try {
                URLDecoder.decode(cleanUrl.substring(hashIndex + 1), "UTF-8")
            } catch (e: Exception) {
                cleanUrl.substring(hashIndex + 1)
            }
        } else {
            "Shadowsocks Server"
        }

        val mainPart = if (hashIndex != -1) cleanUrl.substring(0, hashIndex) else cleanUrl
        val atIndex = mainPart.lastIndexOf('@')
        
        if (atIndex == -1) {
            // Might be entirely base64-encoded userinfo and host info: ss://base64(method:password@host:port)
            val decoded = decodeBase64(mainPart)
            if (decoded.isNotEmpty() && decoded.contains("@")) {
                return parseShadowsocks(subscriptionId, "ss://$decoded#$name")
            }
            return null
        }

        val hostPortPart = mainPart.substring(atIndex + 1)
        val colonIndex = hostPortPart.indexOf(':')
        if (colonIndex == -1) return null

        val address = hostPortPart.substring(0, colonIndex)
        val portString = hostPortPart.substring(colonIndex + 1)
        val port = portString.toIntOrNull() ?: return null

        return V2RayNodeEntity(
            subscriptionId = subscriptionId,
            name = name,
            protocol = "SS",
            address = address,
            port = port,
            rawUrl = url
        )
    }

    private fun parseUriProtocol(subscriptionId: Int, protocol: String, url: String): V2RayNodeEntity? {
        // format: protocol://credentials@host:port?query#name
        // e.g. vless://91a7e78d@104.18.23.1:443?type=ws#US_Cloudflare
        val prefix = "${protocol.lowercase()}://"
        val cleanUrl = url.removePrefix(prefix).trim()

        val hashIndex = cleanUrl.indexOf('#')
        val name = if (hashIndex != -1) {
            try {
                URLDecoder.decode(cleanUrl.substring(hashIndex + 1), "UTF-8")
            } catch (e: Exception) {
                cleanUrl.substring(hashIndex + 1)
            }
        } else {
            "$protocol Server"
        }

        val mainPart = if (hashIndex != -1) cleanUrl.substring(0, hashIndex) else cleanUrl
        val queryIndex = mainPart.indexOf('?')
        val hostPortWithCreds = if (queryIndex != -1) mainPart.substring(0, queryIndex) else mainPart

        val atIndex = hostPortWithCreds.lastIndexOf('@')
        val hostPort = if (atIndex != -1) hostPortWithCreds.substring(atIndex + 1) else hostPortWithCreds

        val colonIndex = hostPort.lastIndexOf(':')
        if (colonIndex == -1) return null

        val address = hostPort.substring(0, colonIndex).trim()
        val portStr = hostPort.substring(colonIndex + 1).trim()
        val port = portStr.toIntOrNull() ?: return null

        if (address.isEmpty()) return null

        return V2RayNodeEntity(
            subscriptionId = subscriptionId,
            name = name,
            protocol = protocol,
            address = address,
            port = port,
            rawUrl = url
        )
    }
}
