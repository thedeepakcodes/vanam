package `in`.qwicklabs.vanam.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import java.io.ByteArrayOutputStream
import java.io.File

object UtilityFunctions {

    fun bitmapToBase64Image(bitmap: Bitmap): String {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
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

}
