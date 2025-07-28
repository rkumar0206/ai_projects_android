package com.rtb.ai.projects.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID


object AppUtil {

    fun String.copyToClipboard(context: Context) {

        if (this.isNotEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Prompt Result", this)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Result copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper function to hide the keyboard
    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun TextView.displayMarkdownWithMarkwon(context: Context, markdownString: String) {
        // Create a Markwon instance
        val markwon = Markwon.builder(context)
            .usePlugin(HtmlPlugin.create()) // Example: enable HTML rendering
            //.usePlugin(ImagesPlugin.create()) // Example: enable image loading
            // Add other plugins as needed
            .build()

        // Set the Markdown text directly to the TextView
        markwon.setMarkdown(this, markdownString)
    }

    fun <T> T.convertToJsonString(): String? {

        return try {

            Gson().toJson(this)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun <T> String.convertJsonToObject(clazz: Class<T>): T? {

        return try {
            Gson().fromJson(this, clazz)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun ByteArray.saveByteArrayToInternalFile(
        context: Context,
        directoryName: String = "images", // Default subdirectory
        fileName: String? = null, // Optional: provide a specific name
        fileExtension: String = ".jpg" // Default extension
    ): String? {
        // 1. Get the directory for your app's private files
        val internalFilesDir = context.filesDir // Path: /data/user/0/your.package.name/files

        // 2. Create a subdirectory if it doesn't exist
        val imageSubDir = File(internalFilesDir, directoryName)
        if (!imageSubDir.exists()) {
            if (!imageSubDir.mkdirs()) {
                Log.e("FileSave", "Failed to create directory: ${imageSubDir.absolutePath}")
                return null // Failed to create directory
            }
        }

        // 3. Determine the final file name
        val finalFileName = fileName ?: "image_${UUID.randomUUID()}$fileExtension"
        val imageFile = File(imageSubDir, finalFileName)

        // 4. Write the ByteArray to the file using FileOutputStream
        try {
            FileOutputStream(imageFile).use { fos -> // 'use' will auto-close the stream
                fos.write(this)
                Log.d("FileSave", "byte array successfully saved to: ${imageFile.absolutePath}")
                return imageFile.absolutePath // Return the path of the saved file
            }
        } catch (e: IOException) {
            Log.e("FileSave", "Error saving image to file: ${e.message}", e)
            // Optionally, delete the partially written file if an error occurs
            if (imageFile.exists()) {
                imageFile.delete()
            }
            return null // Return null if saving failed
        } catch (e: SecurityException) {
            Log.e("FileSave", "Security exception while saving image: ${e.message}", e)
            return null
        }
    }

    /**
     * Retrieves an image file from internal storage as a ByteArray.
     *
     * @param filePath The absolute path to the image file in internal storage.
     * @return The ByteArray containing the image data, or null if reading failed or file not found.
     */
    fun retrieveImageAsByteArray(filePath: String?): ByteArray? {
        if (filePath == null) {
            Log.w("FileRetrieve", "File path is null, cannot retrieve image.")
            return null
        }

        val imageFile = File(filePath)

        if (!imageFile.exists()) {
            Log.w("FileRetrieve", "Image file does not exist at path: $filePath")
            return null
        }

        if (!imageFile.canRead()) {
            Log.w(
                "FileRetrieve",
                "Cannot read image file at path: $filePath (check permissions or file integrity)"
            )
            return null
        }

        try {
            FileInputStream(imageFile).use { fis -> // 'use' will auto-close the stream
                return fis.readBytes()
            }
        } catch (e: FileNotFoundException) {
            // This should theoretically be caught by imageFile.exists() but good to have as a safeguard
            Log.e(
                "FileRetrieve",
                "File not found during read (should have been caught earlier): ${e.message}",
                e
            )
            return null
        } catch (e: IOException) {
            Log.e("FileRetrieve", "Error reading image file to ByteArray: ${e.message}", e)
            return null
        } catch (e: SecurityException) {
            Log.e("FileRetrieve", "Security exception while reading image: ${e.message}", e)
            return null
        } catch (e: OutOfMemoryError) {
            Log.e(
                "FileRetrieve",
                "OutOfMemoryError while reading image into ByteArray. File might be too large: ${imageFile.length()} bytes",
                e
            )
            // Consider alternatives for very large files, like streaming or using BitmapFactory.decodeFile directly
            return null
        }
    }

}