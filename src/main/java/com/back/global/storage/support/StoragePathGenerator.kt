package com.back.global.storage.support

import java.time.LocalDate
import java.util.UUID

object StoragePathGenerator {
    fun generate(pathPrefix: String, domain: String, originalFileName: String): String {
        val safeDomain = domain.lowercase().replace(Regex("[^a-z0-9_-]"), "-")
        val extension = extractExtension(originalFileName)
        val today = LocalDate.now()

        return listOf(
            pathPrefix.trim('/'),
            safeDomain,
            today.year.toString(),
            "%02d".format(today.monthValue),
            "%02d".format(today.dayOfMonth),
            "${UUID.randomUUID()}$extension"
        ).filter { it.isNotBlank() }
            .joinToString("/")
    }

    private fun extractExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex > 0 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex)
        } else {
            ""
        }
    }
}
