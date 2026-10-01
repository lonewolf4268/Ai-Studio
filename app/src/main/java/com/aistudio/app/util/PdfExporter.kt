package com.aistudio.app.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.aistudio.app.data.model.ChatMessage
import com.aistudio.app.data.model.MessageSender
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    fun generatePdf(
        context: Context,
        sessionTitle: String,
        messages: List<ChatMessage>
    ): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in points (1/72 inch)
        val pageHeight = 842 // A4 height in points
        val margin = 40f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Paints
        val titlePaint = Paint().apply {
            color = Color.rgb(26, 115, 232) // M3 Primary Blue
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val userSenderPaint = Paint().apply {
            color = Color.rgb(32, 33, 36)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val aiSenderPaint = Paint().apply {
            color = Color.rgb(103, 58, 183) // Purple for Gemini AI
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var yPosition = margin

        // Document Header
        canvas.drawText("AI Studio Chat Transcript", margin, yPosition, titlePaint)
        yPosition += 24f

        val dateStr = SimpleDateFormat("MMMM dd, yyyy • HH:mm", Locale.getDefault()).format(Date())
        val displayTitle = if (sessionTitle.length > 30) sessionTitle.take(30) + "..." else sessionTitle
        canvas.drawText("Title: $displayTitle   |   Exported: $dateStr", margin, yPosition, subtitlePaint)
        yPosition += 16f

        canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint)
        yPosition += 24f

        val contentWidth = (pageWidth - (margin * 2)).toInt()

        // Render messages
        for (message in messages) {
            val senderLabel = when (message.sender) {
                MessageSender.YOU -> "YOU [${message.timestamp}]"
                MessageSender.AI -> "GEMINI AI [${message.timestamp}]"
                MessageSender.APP -> "SYSTEM [${message.timestamp}]"
            }

            // Page break check before message header
            if (yPosition > pageHeight - margin - 40f) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPosition = margin
            }

            val currentSenderPaint = if (message.sender == MessageSender.AI) aiSenderPaint else userSenderPaint
            canvas.drawText(senderLabel, margin, yPosition, currentSenderPaint)
            yPosition += 18f

            // Wrap and render message text
            val lines = wrapText(message.message, textPaint, contentWidth)
            for (line in lines) {
                if (yPosition > pageHeight - margin - 20f) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = margin
                }
                canvas.drawText(line, margin, yPosition, textPaint)
                yPosition += 15f
            }

            yPosition += 14f // Spacing between messages
        }

        pdfDocument.finishPage(page)

        // Save PDF to cache exports folder
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val cleanTitle = sessionTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(20).ifBlank { "Chat" }
        val pdfFile = File(exportDir, "Transcript_${cleanTitle}_${System.currentTimeMillis()}.pdf")

        FileOutputStream(pdfFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return pdfFile
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.split("\n")

        for (paragraph in paragraphs) {
            if (paragraph.isBlank()) {
                result.add("")
                continue
            }

            val words = paragraph.split(" ")
            var currentLine = StringBuilder()

            for (word in words) {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine = StringBuilder(testLine)
                } else {
                    if (currentLine.isNotEmpty()) {
                        result.add(currentLine.toString())
                    }
                    currentLine = StringBuilder(word)
                }
            }

            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            }
        }

        return result
    }
}
