package com.ahsan.kotlinpractice.Utilities

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import org.ocpsoft.prettytime.PrettyTime
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


object Utils {
    var vibrantLightColorList = arrayOf(
        ColorDrawable(Color.parseColor("#ffeead")),
        ColorDrawable(Color.parseColor("#93cfb3")),
        ColorDrawable(Color.parseColor("#fd7a7a")),
        ColorDrawable(Color.parseColor("#faca5f")),
        ColorDrawable(Color.parseColor("#1ba798")),
        ColorDrawable(Color.parseColor("#6aa9ae")),
        ColorDrawable(Color.parseColor("#ffbf27")),
        ColorDrawable(Color.parseColor("#d93947"))
    )
    val randomDrawbleColor: ColorDrawable
        get() {
            val idx: Int = Random().nextInt(vibrantLightColorList.size)
            return vibrantLightColorList.get(idx)
        }

    fun DateToTimeFormat(oldstringDate: String?): String? {
        val p = PrettyTime(Locale(country))
        var isTime: String? = null
        try {
            val sdf = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.ENGLISH
            )
            val date: Date = sdf.parse(oldstringDate)
            isTime = p.format(date)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return isTime
    }

    fun DateFormat(oldstringDate: String?): String? {
        val newDate: String?
        val dateFormat = SimpleDateFormat("E, dd MMM YYYY", Locale(country))
        newDate = try {
            val date: Date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(oldstringDate)
            dateFormat.format(date)
        } catch (e: ParseException) {
            e.printStackTrace()
            oldstringDate
        }
        return newDate
    }

    fun formatDate(inputDate: String?): String? {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

        try {
            val date = inputFormat.parse(inputDate)
            return outputFormat.format(date)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return inputDate // Return the original date if parsing fails
    }

    val country: String
        get() {
            val locale: Locale = Locale.getDefault()
            val country: String = java.lang.String.valueOf(locale.getCountry())
            return country.lowercase(Locale.getDefault())
        }
    val language: String
        get() {
            val locale: Locale = Locale.getDefault()
            val country: String = java.lang.String.valueOf(locale.getLanguage())
            return country.lowercase(Locale.getDefault())
        }
}