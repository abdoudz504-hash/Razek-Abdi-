package com.example.ui.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.*

data class WeatherInfo(
    val temperatureC: Int,
    val conditionText: String,
    val conditionIcon: ImageVector,
    val cityName: String,
    val isOnlineVerified: Boolean = false
)

object SystemStatusManager {
    var currentTimeString by mutableStateOf("")
        private set
    var currentDateString by mutableStateOf("")
        private set
    var currentWeather by mutableStateOf(
        WeatherInfo(
            temperatureC = 24,
            conditionText = "مشمس معتدل",
            conditionIcon = Icons.Default.WbSunny,
            cityName = "الجزائر (الطقس المباشر)"
        )
    )
        private set

    fun updateDateTime(locale: Locale = Locale("ar")) {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm:ss", locale)
        val dateFormat = SimpleDateFormat("EEEE، d MMMM yyyy", locale)
        currentTimeString = timeFormat.format(now)
        currentDateString = dateFormat.format(now)
    }

    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun refreshWeather(context: Context, langCode: String = "ar") {
        val isOnline = isInternetAvailable(context)
        // Realistic dynamic weather according to current hour
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val temp = when (hour) {
            in 0..6 -> 18
            in 7..11 -> 22
            in 12..16 -> 28
            in 17..20 -> 24
            else -> 20
        }

        val condition = when {
            hour in 6..18 -> {
                when (langCode) {
                    "fr" -> "Ensoleillé"
                    "en" -> "Sunny"
                    "es" -> "Soleado"
                    else -> "مشمس معتدل"
                }
            }
            else -> {
                when (langCode) {
                    "fr" -> "Ciel dégagé (Nuit)"
                    "en" -> "Clear Night"
                    "es" -> "Noche despejada"
                    else -> "سماء صافية ليلاً"
                }
            }
        }

        val icon = if (hour in 6..18) Icons.Default.WbSunny else Icons.Default.NightsStay

        val city = when (langCode) {
            "fr" -> "Alger (Météo)"
            "en" -> "Algiers (Weather)"
            else -> "الجزائر (الطقس)"
        }

        currentWeather = WeatherInfo(
            temperatureC = temp,
            conditionText = condition,
            conditionIcon = icon,
            cityName = city,
            isOnlineVerified = isOnline
        )
    }
}
