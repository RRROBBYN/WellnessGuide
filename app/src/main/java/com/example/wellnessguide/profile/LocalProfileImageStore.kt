package com.example.wellnessguide.profile

import android.content.Context
import android.net.Uri
import java.io.File

object LocalProfileImageStore {

    fun imageFile(context: Context, userId: String): File {
        val folder = File(context.filesDir, "profile_images")

        if (!folder.exists()) {
            folder.mkdirs()
        }

        return File(folder, "$userId.jpg")
    }

    fun saveImage(
        context: Context,
        userId: String,
        uri: Uri
    ) {
        val targetFile = imageFile(context, userId)

        context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun deleteImage(
        context: Context,
        userId: String
    ) {
        val file = imageFile(context, userId)

        if (file.exists()) {
            file.delete()
        }
    }
}