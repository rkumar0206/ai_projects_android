package com.rtb.ai.projects.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
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
import java.util.Random
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

    fun ByteArray.downloadImage(fileName: String, resolver: ContentResolver): Boolean {

        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val imageUri: Uri? =
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (imageUri != null) {
                resolver.openOutputStream(imageUri).use { outputStream ->
                    if (outputStream == null) {
                        throw Exception("Failed to get output stream for MediaStore URI.")
                    }
                    outputStream.write(this)
                }
            } else {
                throw Exception("MediaStore.Downloads.EXTERNAL_CONTENT_URI returned null.")
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }


    /**
     * Saves a ByteArray to a temporary file in the app's cache directory.
     *
     * @param context The application context.
     * @param prefix A prefix for the filename (e.g., "img_").
     * @param suffix A suffix/extension for the filename (e.g., ".jpg").
     * @return The absolute path to the saved file, or null if saving failed.
     */
    fun ByteArray.saveToCacheFile(
        context: Context,
        prefix: String = "cached_img_",
        suffix: String = ".tmp" // Use .tmp or a specific image extension
    ): String? {
        return try {
            // Create a unique file in the cache directory
            val tempFile = File.createTempFile(prefix, suffix, context.cacheDir)

            FileOutputStream(tempFile).use { fos ->
                fos.write(this)
            }

            tempFile.absolutePath // Return the path of the saved file
        } catch (e: IOException) {
            // Log the error or handle it appropriately
            e.printStackTrace()
            null // Return null if there was an error
        }
    }


    /**
     * Deletes all files and subdirectories within the application's cache directory.
     *
     * @param context The application context.
     * @return true if all deletions were successful, false otherwise.
     */
    fun clearApplicationCache(context: Context): Boolean {
        val cacheDir = context.cacheDir
        return if (cacheDir != null && cacheDir.isDirectory) {
            deleteFilesInDirectory(cacheDir)
        } else {
            false
        }
    }

    /**
     * Helper function to recursively delete files and subdirectories.
     */
    private fun deleteFilesInDirectory(dir: File): Boolean {
        var success = true
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (!deleteFilesInDirectory(file)) {
                    success = false
                }
            }
            if (!file.delete()) {
                // Log an error or handle the failure to delete
                println("Failed to delete ${file.absolutePath}")
                success = false
            }
        }
        // After deleting contents, try to delete the directory itself if it was not the main cache dir
        // For the main cacheDir, we only delete its contents.
        // If 'dir' could be a subdirectory you also want to remove, you might add dir.delete() here.
        // However, for context.cacheDir itself, you typically only clear its contents.
        return success
    }

//    fun clearOrphanedStoryImages(context: Context, prefix: String = "temp_", suffix: String = ".png") {
//        val cacheDir = context.cacheDir
//        cacheDir?.listFiles { file ->
//            file.isFile && file.name.startsWith(prefix) && file.name.endsWith(suffix)
//        }?.forEach { file ->
//            if (!file.delete()) {
//                println("Failed to delete orphaned file: ${file.absolutePath}")
//            }
//        }
//    }

    fun getRandomColor(): Int {
        val random = Random()
        // Generate reasonably bright and distinct random colors
        val r = random.nextInt(156) + 100 // 100-255
        val g = random.nextInt(156) + 100 // 100-255
        val b = random.nextInt(156) + 100 // 100-255
        return Color.rgb(r, g, b)
    }

    fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

}