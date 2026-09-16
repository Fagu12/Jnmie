package com.example.core.extension

import android.net.Uri
import com.example.domain.model.Extension
import com.example.domain.model.ExtensionCapability
import java.net.InetAddress
import java.net.URI

/**
 * Secure extension boundary and validation strategy.
 *
 * Security Principles:
 * 1. Zero Arbitrary Code Execution: Never loads external DEX, JAR, or dynamic bytecode.
 * 2. Strict URL & Network Boundary: Prevents SSRF attacks, intranet pivoting, and malicious schemes.
 * 3. Schema & Semantic Validation: Enforces strict data models and sanitization.
 * 4. Safe Sandboxed HTTP Pipeline: Operates with strict timeouts and header whitelisting.
 */
object ExtensionSecurityBoundary {

    private val BLOCKED_HOST_PREFIXES = listOf(
        "localhost",
        "127.",
        "10.",
        "192.168.",
        "172.16.",
        "172.17.",
        "172.18.",
        "172.19.",
        "172.2",
        "172.30.",
        "172.31.",
        "169.254.", // Link-local / Cloud metadata service
        "0.0.0.0",
        "[::1]",
        "fc00:",
        "fe80:"
    )

    private val ALLOWED_SCHEMES = setOf("https", "http")

    /**
     * Validates and normalizes repository URLs (including standard GitHub / GitLab repositories).
     */
    fun validateAndNormalizeRepoUrl(rawUrl: String): Result<String> {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Repository URL cannot be empty"))
        }

        val uri = try {
            val parsed = URI(trimmed)
            if (parsed.scheme == null || !ALLOWED_SCHEMES.contains(parsed.scheme.lowercase())) {
                return Result.failure(IllegalArgumentException("Repository URL must use https or http scheme"))
            }
            parsed
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Invalid URL syntax: ${e.message}"))
        }

        val host = uri.host ?: return Result.failure(IllegalArgumentException("Repository URL has no valid host"))

        // Prevent SSRF / local network access
        if (isBlockedHost(host)) {
            return Result.failure(SecurityException("Repository URL references a forbidden local or internal network address"))
        }

        // Normalize GitHub repository URLs to raw content JSON index if provided as browser repo URL
        var normalized = trimmed
        if (host.equals("github.com", ignoreCase = true)) {
            val path = uri.path?.trim('/') ?: ""
            val segments = path.split('/')
            if (segments.size >= 2 && !path.endsWith(".json")) {
                val owner = segments[0]
                val repo = segments[1].removeSuffix(".git")
                val branch = if (segments.size >= 4 && segments[2] == "tree") segments[3] else "main"
                normalized = "https://raw.githubusercontent.com/$owner/$repo/$branch/index.json"
            }
        } else if (host.equals("gitlab.com", ignoreCase = true)) {
            val path = uri.path?.trim('/') ?: ""
            val segments = path.split('/')
            if (segments.size >= 2 && !path.endsWith(".json")) {
                val owner = segments[0]
                val repo = segments[1].removeSuffix(".git")
                normalized = "https://gitlab.com/$owner/$repo/-/raw/main/index.json"
            }
        }

        return Result.success(normalized)
    }

    /**
     * Checks if a host points to a local or internal IP space.
     */
    private fun isBlockedHost(host: String): Boolean {
        val lowerHost = host.lowercase()
        if (BLOCKED_HOST_PREFIXES.any { lowerHost.startsWith(it) }) {
            return true
        }
        if (lowerHost == "metadata.google.internal" || lowerHost.contains("internal")) {
            return true
        }
        return false
    }

    /**
     * Validates extension manifest metadata against malicious or malformed configurations.
     */
    fun validateManifest(manifest: Extension): Result<Extension> {
        if (manifest.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Extension ID cannot be blank"))
        }
        if (!manifest.id.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            return Result.failure(IllegalArgumentException("Extension ID contains illegal characters: ${manifest.id}"))
        }
        if (manifest.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Extension name cannot be blank"))
        }
        if (manifest.baseUrl.isBlank()) {
            return Result.failure(IllegalArgumentException("Extension baseUrl cannot be blank"))
        }

        try {
            val baseUri = URI(manifest.baseUrl)
            if (baseUri.host != null && isBlockedHost(baseUri.host)) {
                return Result.failure(SecurityException("Extension baseUrl points to an untrusted internal host"))
            }
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Extension baseUrl is invalid: ${e.message}"))
        }

        // Sanitized and validated copy
        val validated = manifest.copy(
            id = manifest.id.trim(),
            name = manifest.name.trim(),
            version = manifest.version.trim().ifBlank { "1.0" },
            baseUrl = manifest.baseUrl.trim(),
            language = manifest.language.trim().uppercase().ifBlank { "ENGLISH" }
        )
        return Result.success(validated)
    }
}
