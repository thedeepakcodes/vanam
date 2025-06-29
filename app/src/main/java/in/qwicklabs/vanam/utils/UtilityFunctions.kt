package `in`.qwicklabs.vanam.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UtilityFunctions {

    fun bitmapToBase64Image(bitmap: Bitmap): String {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    fun uriToBase64Image(uri: Uri, context: Context): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmapToBase64Image(bitmap)
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun base64ToTempUri(base64: String, context: Context): Uri {
        val imageBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        val tempFile = File.createTempFile("tree_image", ".jpg", context.cacheDir)
        tempFile.writeBytes(imageBytes)
        return tempFile.toUri()
    }

    fun formatDate(date: Date, pattern: String): String {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(date)
    }

    fun calculateAge(date: Date): String {
        val currentDate = Date()
        val timeDifference = currentDate.time - date.time
        val seconds = timeDifference / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours < 24 -> "$hours hour${if (hours > 1) "s" else ""}"
            hours < 24 * 30 -> "${hours / 24} day${if (hours / 24 > 1) "s" else ""}"
            else -> "${hours / (24 * 30)} month${if (hours / (24 * 30) > 1) "s" else ""}"
        }
    }

    fun openMap(latitude: Double, longitude: Double, context: Context) {
        val uri = "geo:$latitude,$longitude".toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val browserUri =
                "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude".toUri()
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
            context.startActivity(browserIntent)
        }
    }

    fun formatNumberShort(input: String): String {
        val number = input.toLongOrNull() ?: return input

        val formatted = when {
            number >= 1_000_000_000 -> String.format("%.1fB", number / 1_000_000_000.0)
            number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
            number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
            else -> number.toString()
        }

        return formatted.removeSuffix(".0")
    }

}
