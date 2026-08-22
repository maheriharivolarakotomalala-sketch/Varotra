package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {

    private val ariaryFormat: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale.FRENCH).apply {
            groupingSeparator = ' '
        }
        DecimalFormat("#,###", symbols)
    }

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
    private val dateTimeFormatter = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRENCH)
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.FRENCH)

    fun formatAriary(amount: Long): String {
        return "${ariaryFormat.format(amount)} Ar"
    }

    fun formatAriary(amount: Double): String {
        return "${ariaryFormat.format(amount.toLong())} Ar"
    }

    fun formatDate(timestamp: Long): String {
        return dateFormatter.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormatter.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormatter.format(Date(timestamp))
    }
}
