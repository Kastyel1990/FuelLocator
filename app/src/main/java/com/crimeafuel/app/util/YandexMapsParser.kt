package com.crimeafuel.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

data class ImportedStationData(
    val name: String,
    val address: String,
    val lat: Double?,
    val lng: Double?
)

object YandexMapsParser {

    suspend fun parseSharedText(text: String): ImportedStationData? = withContext(Dispatchers.IO) {
        try {
            val lines = text.trim().split("\n").map { it.trim() }
            
            // Extract URL
            val urlString = lines.find { it.contains("yandex.ru/maps") || it.contains("yandex.ru/navi") } ?: return@withContext null
            
            // Extract Name and Address (assuming they are before the URL)
            var name = ""
            var address = ""
            
            val urlIndex = lines.indexOf(urlString)
            if (urlIndex >= 1) {
                name = lines[0]
            }
            if (urlIndex >= 2) {
                address = lines[1]
            }

            // If it's a short link, follow redirects
            var finalUrl = urlString
            if (urlString.contains("/-/")) {
                finalUrl = resolveShortUrl(urlString)
            }

            // Extract coordinates from ll=lon,lat or pt=lon,lat
            val coordinates = extractCoordinates(finalUrl)
            val lng = coordinates?.first
            val lat = coordinates?.second

            // Cleanup name if it contains generic stuff
            if (name.isBlank() || name.contains("yandex")) {
                name = "Новая заправка"
            }

            if (coordinates == null && name == "Новая заправка" && address.isBlank()) {
                return@withContext null
            }

            ImportedStationData(
                name = name,
                address = address,
                lat = lat,
                lng = lng
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resolveShortUrl(urlString: String): String {
        try {
            var url = URL(urlString)
            var connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connect()
            
            var responseCode = connection.responseCode
            var redirects = 0
            
            while ((responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308) && redirects < 5) {
                
                val newUrl = connection.getHeaderField("Location") ?: break
                url = URL(newUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connect()
                responseCode = connection.responseCode
                redirects++
            }
            return url.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return urlString
        }
    }

    private fun extractCoordinates(url: String): Pair<Double, Double>? {
        // Looking for ll=lon,lat or pt=lon,lat or coordinates=lon,lat
        // Example: ll=34.053805%2C44.919799
        val decodedUrl = java.net.URLDecoder.decode(url, "UTF-8")
        val pattern = Pattern.compile("(?:ll|pt)=([0-9.]+),([0-9.]+)")
        val matcher = pattern.matcher(decodedUrl)
        if (matcher.find()) {
            val lon = matcher.group(1)?.toDoubleOrNull()
            val lat = matcher.group(2)?.toDoubleOrNull()
            if (lon != null && lat != null) {
                return Pair(lon, lat)
            }
        }
        
        // Also sometimes it's like yandex.ru/maps/geo/.../?ll=...
        // Or yandex.ru/maps/?ll=33.99,44.99&z=...
        return null
    }
}
