package com.rtb.ai.projects.ui.feature_the_random_value.feature_random_story.util

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.core.graphics.scale
import androidx.core.graphics.withTranslation
import com.rtb.ai.projects.data.model.ContentItem
import com.rtb.ai.projects.util.AppUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object StoryPdfGenerator {

    private const val TAG = "StoryPdfGenerator"

    // Standard A4 page dimensions in points (1/72 inch)
    private const val PAGE_WIDTH_A4 = 595
    private const val PAGE_HEIGHT_A4 = 842
    private const val MARGIN = 40f // Margin in points

    private const val TEXT_SIZE_TITLE = 18f
    private const val TEXT_SIZE_BODY = 12f
    private const val LINE_SPACING_MULTIPLIER = 1.2f
    private const val IMAGE_SPACING = 6f // Space above and below images

    suspend fun generatePdf(
        storyTitle: String?,
        storyContent: List<ContentItem>,
        outputFileName: String = "${storyTitle ?: "story_${System.currentTimeMillis()}"}.pdf"
    ): Result<String>? {

        return withContext(Dispatchers.IO) {

            val pdfDocument = PdfDocument()
            var currentPageNumber = 0
            var page: PdfDocument.Page? = null
            var canvas: Canvas? = null
            var currentY = MARGIN

            val contentWidth = PAGE_WIDTH_A4 - 2 * MARGIN

            val titlePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = TEXT_SIZE_TITLE
                isAntiAlias = true
            }

            val bodyTextPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = TEXT_SIZE_BODY
                isAntiAlias = true
            }

            fun startNewPage() {
                page?.let {
                    pdfDocument.finishPage(it)
                }
                currentPageNumber++
                val pageInfo =
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH_A4, PAGE_HEIGHT_A4, currentPageNumber)
                        .create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page!!.canvas
                currentY = MARGIN

                // Optional: Draw page numbers or headers/footers here
            }

            startNewPage() // Start the first page

            // Draw Story Title
            if (!storyTitle.isNullOrEmpty()) {
                val titleStaticLayout = StaticLayout.Builder.obtain(
                    storyTitle,
                    0,
                    storyTitle.length,
                    titlePaint,
                    contentWidth.toInt()
                ).setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, LINE_SPACING_MULTIPLIER)
                    .setIncludePad(false)
                    .build()

                if (currentY + titleStaticLayout.height > PAGE_HEIGHT_A4 - MARGIN) {
                    startNewPage()
                }
                canvas?.withTranslation(MARGIN, currentY) {
                    titleStaticLayout.draw(this)
                }
                currentY += titleStaticLayout.height + TEXT_SIZE_TITLE // Extra space after title
            }

            for (item: ContentItem in storyContent) {

                if (item is ContentItem.TextContent) {
                    val textStaticLayout = StaticLayout.Builder.obtain(
                        item.text,
                        0,
                        item.text.length,
                        bodyTextPaint,
                        contentWidth.toInt()
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(0f, LINE_SPACING_MULTIPLIER)
                        .setIncludePad(false)
                        .build()

                    if (currentY + textStaticLayout.height > PAGE_HEIGHT_A4 - MARGIN) {
                        startNewPage()
                    }
                    canvas?.withTranslation(MARGIN, currentY) {
                        textStaticLayout.draw(this)
                    }
                    currentY += textStaticLayout.height + (TEXT_SIZE_BODY / 2) // Space after text block
                }

                if (item is ContentItem.ImageContent) {

                    item.imageFilePath?.let {
                        try {

                            val imageAsByteArray = AppUtil.retrieveImageAsByteArray(it)
                            if (imageAsByteArray != null) {
                                val originalBitmap = BitmapFactory.decodeByteArray(
                                    imageAsByteArray,
                                    0,
                                    imageAsByteArray.size
                                )
                                if (originalBitmap != null) {
                                    val aspectRatio =
                                        originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                                    var scaledWidth = contentWidth
                                    var scaledHeight = scaledWidth / aspectRatio

                                    if (scaledHeight > originalBitmap.height) { // Don't scale up beyond original
                                        scaledHeight = originalBitmap.height.toFloat()
                                        scaledWidth = scaledHeight * aspectRatio
                                    }

                                    if (currentY + scaledHeight + IMAGE_SPACING * 2 > PAGE_HEIGHT_A4 - MARGIN) {
                                        startNewPage()
                                    }

                                    currentY += IMAGE_SPACING
                                    val destX =
                                        MARGIN + (contentWidth - scaledWidth) / 2 // Center image
                                    canvas?.drawBitmap(
                                        originalBitmap.scale(
                                            scaledWidth.toInt(),
                                            scaledHeight.toInt()
                                        ),
                                        destX,
                                        currentY,
                                        null
                                    )
                                    originalBitmap.recycle() // Recycle bitmap to free memory
                                    currentY += scaledHeight + IMAGE_SPACING
                                } else {
                                    Log.w(
                                        TAG,
                                        "Failed to decode bitmap from byte array for path: $it"
                                    )
                                }
                            } else {
                                Log.w(TAG, "Failed to decode bitmap from path: $it")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading image for PDF: $it", e)
                        }
                    }
                }

            }
            page?.let {
                pdfDocument.finishPage(it)
            }

            // Save the file in the DOWNLOADS folder
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val outputFile = File(downloadsDir, outputFileName)

            return@withContext try {
                FileOutputStream(outputFile).use { fos ->
                    pdfDocument.writeTo(fos)
                }
                pdfDocument.close()
                Log.i(TAG, "PDF generated successfully at ${outputFile.absolutePath}")
                Result.success(outputFile.absolutePath)
            } catch (e: IOException) {
                Log.e(TAG, "Error writing PDF to file", e)
                pdfDocument.close()
                Result.failure(e)
            }
        }
    }
}