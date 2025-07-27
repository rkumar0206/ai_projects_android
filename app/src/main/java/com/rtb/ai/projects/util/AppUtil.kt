package com.rtb.ai.projects.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin // If using HTML plugin


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
}