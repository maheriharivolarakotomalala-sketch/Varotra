package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.entity.SaleWithItems
import com.example.util.Formatters
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object InvoicePdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 standard height

    fun generateInvoicePdf(context: Context, saleWithItems: SaleWithItems): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawInvoice(canvas, saleWithItems)

        document.finishPage(page)

        // Save to cache directory
        val invoiceDir = File(context.cacheDir, "invoices")
        if (!invoiceDir.exists()) {
            invoiceDir.mkdirs()
        }

        val fileName = "${saleWithItems.sale.invoiceNumber}.pdf"
        val file = File(invoiceDir, fileName)

        try {
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            document.close()
            return null
        }

        document.close()
        return file
    }

    private fun drawInvoice(canvas: Canvas, saleWithItems: SaleWithItems) {
        val sale = saleWithItems.sale
        val items = saleWithItems.items

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), paint)

        // Header Background Banner
        paint.color = Color.rgb(0, 109, 95) // Primary Teal #006D5F
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 95f, paint)

        // Brand Title
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 24f
        canvas.drawText("varotra.mg", 36f, 45f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        paint.color = Color.rgb(200, 240, 230)
        canvas.drawText("Commerce & Facturation Madagascar", 36f, 65f, paint)

        // Invoice Badge (Right Top)
        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 16f
        val invoiceTitle = "FACTURE"
        val invoiceTitleWidth = paint.measureText(invoiceTitle)
        canvas.drawText(invoiceTitle, PAGE_WIDTH - 36f - invoiceTitleWidth, 42f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        paint.color = Color.rgb(200, 240, 230)
        val numStr = "N° ${sale.invoiceNumber}"
        val numWidth = paint.measureText(numStr)
        canvas.drawText(numStr, PAGE_WIDTH - 36f - numWidth, 62f, paint)

        val dateStr = "Date: ${Formatters.formatDateTime(sale.timestamp)}"
        val dateWidth = paint.measureText(dateStr)
        canvas.drawText(dateStr, PAGE_WIDTH - 36f - dateWidth, 77f, paint)

        // Customer Info Card
        var currentY = 120f
        val cardRect = RectF(36f, currentY, PAGE_WIDTH - 36f, currentY + 70f)
        paint.color = Color.rgb(245, 248, 250)
        canvas.drawRoundRect(cardRect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(220, 230, 235)
        canvas.drawRoundRect(cardRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        canvas.drawText("CLIENT / DESTINATAIRE :", 50f, currentY + 22f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText(sale.customerName, 50f, currentY + 42f, paint)

        if (sale.customerPhone.isNotBlank()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 10f
            paint.color = Color.rgb(71, 85, 105)
            canvas.drawText("Tél: ${sale.customerPhone}", 50f, currentY + 58f, paint)
        }

        // Payment status badge
        val statusText = if (sale.isPaid) "PAYÉ (${sale.paymentMethod})" else "À CRÉDIT"
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val statusWidth = paint.measureText(statusText)
        val badgeRect = RectF(PAGE_WIDTH - 50f - statusWidth - 16f, currentY + 25f, PAGE_WIDTH - 50f, currentY + 48f)
        paint.color = if (sale.isPaid) Color.rgb(220, 252, 231) else Color.rgb(254, 226, 226)
        canvas.drawRoundRect(badgeRect, 6f, 6f, paint)

        paint.color = if (sale.isPaid) Color.rgb(22, 101, 52) else Color.rgb(153, 27, 27)
        canvas.drawText(statusText, badgeRect.left + 8f, badgeRect.top + 16f, paint)

        // Items Table Header
        currentY = 210f
        val tableLeft = 36f
        val tableRight = PAGE_WIDTH - 36f

        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + 26f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f

        canvas.drawText("N°", tableLeft + 10f, currentY + 17f, paint)
        canvas.drawText("DÉSIGNATION / ARTICLE", tableLeft + 40f, currentY + 17f, paint)
        canvas.drawText("QTÉ", tableLeft + 280f, currentY + 17f, paint)
        canvas.drawText("PRIX UNIT.", tableLeft + 350f, currentY + 17f, paint)
        val totalHeader = "TOTAL (Ar)"
        val totalHWidth = paint.measureText(totalHeader)
        canvas.drawText(totalHeader, tableRight - 10f - totalHWidth, currentY + 17f, paint)

        currentY += 26f

        // Table Rows
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f

        var index = 1
        for (item in items) {
            val rowHeight = 24f
            // Alternate background
            if (index % 2 == 0) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paint)
            }

            paint.color = Color.rgb(30, 41, 59)
            canvas.drawText("$index", tableLeft + 10f, currentY + 16f, paint)

            // Product name (truncated if too long)
            var pName = item.productName
            if (paint.measureText(pName) > 220f) {
                while (paint.measureText("$pName...") > 220f && pName.length > 5) {
                    pName = pName.substring(0, pName.length - 1)
                }
                pName = "$pName..."
            }
            canvas.drawText(pName, tableLeft + 40f, currentY + 16f, paint)

            val qteStr = "${item.quantity} ${item.unit}"
            canvas.drawText(qteStr, tableLeft + 280f, currentY + 16f, paint)

            val priceStr = Formatters.formatAriary(item.unitPrice)
            canvas.drawText(priceStr, tableLeft + 350f, currentY + 16f, paint)

            val totalStr = Formatters.formatAriary(item.totalPrice)
            val totalStrWidth = paint.measureText(totalStr)
            canvas.drawText(totalStr, tableRight - 10f - totalStrWidth, currentY + 16f, paint)

            // Bottom subtle row line
            paint.color = Color.rgb(226, 232, 240)
            paint.strokeWidth = 0.5f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(tableLeft, currentY + rowHeight, tableRight, currentY + rowHeight, paint)
            paint.style = Paint.Style.FILL

            currentY += rowHeight
            index++
        }

        // Total Section Card
        currentY += 20f
        val totalCardLeft = PAGE_WIDTH - 250f
        val totalCardRect = RectF(totalCardLeft, currentY, tableRight, currentY + 75f)

        paint.color = Color.rgb(0, 109, 95)
        canvas.drawRoundRect(totalCardRect, 8f, 8f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        canvas.drawText("TOTAL À PAYER", totalCardLeft + 16f, currentY + 28f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        val grandTotalStr = Formatters.formatAriary(sale.totalAmount)
        canvas.drawText(grandTotalStr, totalCardLeft + 16f, currentY + 54f, paint)

        // Notes section if any
        if (sale.notes.isNotBlank()) {
            paint.color = Color.rgb(100, 116, 139)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            paint.textSize = 9f
            canvas.drawText("Note: ${sale.notes}", tableLeft, currentY + 30f, paint)
        }

        // Footer
        val footerY = PAGE_HEIGHT - 45f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(tableLeft, footerY - 15f, tableRight, footerY - 15f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        val footerText = "Merci pour votre confiance ! • Facture émise avec l'application varotra.mg"
        val footerWidth = paint.measureText(footerText)
        canvas.drawText(footerText, (PAGE_WIDTH - footerWidth) / 2f, footerY, paint)
    }

    fun shareInvoicePdf(context: Context, pdfFile: File) {
        val authority = "${context.packageName}.fileprovider"
        try {
            val contentUri = FileProvider.getUriForFile(context, authority, pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Facture ${pdfFile.nameWithoutExtension} - varotra.mg")
                putExtra(Intent.EXTRA_TEXT, "Veuillez trouver ci-joint votre facture ${pdfFile.nameWithoutExtension} émise par varotra.mg.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Partager la facture PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erreur lors du partage de la facture", Toast.LENGTH_SHORT).show()
        }
    }

    fun viewInvoicePdf(context: Context, pdfFile: File) {
        val authority = "${context.packageName}.fileprovider"
        try {
            val contentUri = FileProvider.getUriForFile(context, authority, pdfFile)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(viewIntent, "Ouvrir la facture PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            // If no PDF reader app, fallback to share
            shareInvoicePdf(context, pdfFile)
        }
    }
}
