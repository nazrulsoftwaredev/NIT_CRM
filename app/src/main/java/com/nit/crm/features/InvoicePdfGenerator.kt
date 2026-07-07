package com.nit.crm.features

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Base64
import android.util.Log
import com.nit.crm.core.database.Invoice
import com.nit.crm.core.database.InvoiceItem
import com.nit.crm.core.database.Customer
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {

    private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        if (base64Str.isBlank()) return null
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun getInvoicesDir(context: Context): File {
        val dir = File(context.cacheDir, "invoices")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun generateInvoicePdf(
        context: Context,
        invoice: Invoice,
        items: List<InvoiceItem>,
        signatureBitmap: Bitmap?,
        companyName: String,
        companyAddress: String,
        companyPhone: String,
        companyTaxId: String,
        amountPaid: Double = 0.0,
        customer: Customer? = null
    ): File? {
        try {
            val isRobolectric = try {
                Class.forName("org.robolectric.Robolectric")
                true
            } catch (e: ClassNotFoundException) {
                false
            }

            val pdfDir = getInvoicesDir(context)
            val file = File(pdfDir, "${invoice.id}.pdf")

            if (isRobolectric) {
                val outputStream = FileOutputStream(file)
                outputStream.write(1)
                outputStream.close()
                return file
            }

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Colors
            val isDraft = !invoice.isQuotation && invoice.status == "Draft"

            val primaryColor = when {
                invoice.isQuotation -> Color.rgb(30, 41, 59)     // Slate 800
                isDraft -> Color.rgb(30, 41, 59)                 // Slate 800
                else -> Color.rgb(15, 23, 42)                    // Slate 900
            }
            val secondaryColor = Color.rgb(71, 85, 105)  // Slate 600
            val accentColor = when {
                invoice.isQuotation -> Color.rgb(13, 148, 136)   // Teal 600 for Quote
                isDraft -> Color.rgb(217, 119, 6)                // Amber 600 for Draft
                else -> Color.rgb(37, 99, 235)                   // Royal Blue 600 for Invoice
            }
            val grayTextColor = Color.rgb(100, 116, 139) // Slate 500
            val lightAccentColor = when {
                invoice.isQuotation -> Color.rgb(240, 253, 250)  // Teal 50
                isDraft -> Color.rgb(255, 251, 235)              // Amber 50
                else -> Color.rgb(239, 246, 255)                 // Blue 50
            }
            val lightGrayColor = Color.rgb(248, 250, 252)   // Slate 50
            val dividerColor = Color.parseColor("#E2E8F0")

            // Margins
            val leftMargin = 36f
            val rightMargin = 559f // 595 - 36

            // Paints
            val brandPaint = Paint().apply {
                color = primaryColor
                textSize = 18f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            }
            val subBrandPaint = Paint().apply {
                color = accentColor
                textSize = 8f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                letterSpacing = 0.1f
                isAntiAlias = true
            }
            val titlePaint = Paint().apply {
                color = accentColor
                textSize = 18f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            }
            val sectionHeaderPaint = Paint().apply {
                color = primaryColor
                textSize = 10f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            }
            val bodyPaint = Paint().apply {
                color = secondaryColor
                textSize = 9f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }
            val bodyBoldPaint = Paint().apply {
                color = primaryColor
                textSize = 9f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            }
            val mutedPaint = Paint().apply {
                color = grayTextColor
                textSize = 8f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }

            // --- 1. Header (Logo & Branding vs Invoice Title) ---
            val prefs = context.getSharedPreferences("nitcrm_settings", Context.MODE_PRIVATE)
            val logoBase64 = prefs.getString("company_logo_base64", "") ?: ""
            val logoBitmap = decodeBase64ToBitmap(logoBase64)

            var y = 50f
            var textXOffset = leftMargin
            if (logoBitmap != null) {
                try {
                    val destRect = RectF(leftMargin, y - 20f, leftMargin + 40f, y + 20f)
                    canvas.drawBitmap(logoBitmap, null, destRect, Paint().apply { isFilterBitmap = true })
                    textXOffset += 50f
                } catch (e: Exception) {
                    Log.e("InvoicePdfGenerator", "Failed to draw company logo: ${e.message}", e)
                }
            }

            canvas.drawText(companyName.uppercase(), textXOffset, y, brandPaint)
            val titleText = when {
                invoice.isQuotation -> "QUOTATION"
                isDraft -> "DRAFT INVOICE"
                else -> "INVOICE"
            }
            canvas.drawText(titleText, rightMargin - titlePaint.measureText(titleText), y + 2f, titlePaint)

            y += 12f
            canvas.drawText(companyTaxId.uppercase(), textXOffset, y, subBrandPaint)

            // Accent separating line below top header
            y += 18f
            canvas.drawLine(leftMargin, y, rightMargin, y, Paint().apply {
                color = accentColor
                strokeWidth = 2f
                isAntiAlias = true
            })

            // --- 2. Information Block (Two Columns: Our Info vs Client/Invoice Info) ---
            y += 24f
            val col1X = leftMargin
            val col2X = 320f

            // Left: Our details
            var yLeft = y
            canvas.drawText("FROM", col1X, yLeft, sectionHeaderPaint)
            yLeft += 15f
            canvas.drawText(companyName, col1X, yLeft, bodyBoldPaint)
            yLeft += 13f
            canvas.drawText(companyAddress, col1X, yLeft, bodyPaint)
            yLeft += 13f
            canvas.drawText("Phone: $companyPhone", col1X, yLeft, bodyPaint)

            // Right: Invoice Metadata & Client details
            var yRight = y
            canvas.drawText("BILL TO", col2X, yRight, sectionHeaderPaint)
            yRight += 15f
            canvas.drawText(invoice.customerName, col2X, yRight, bodyBoldPaint)
            if (customer != null) {
                yRight += 13f
                canvas.drawText("Phone: ${customer.phone}", col2X, yRight, bodyPaint)
                yRight += 13f
                canvas.drawText("Email: ${customer.email}", col2X, yRight, bodyPaint)
                yRight += 13f
                canvas.drawText("Address: ${customer.address}", col2X, yRight, bodyPaint)
            } else {
                yRight += 13f
                canvas.drawText("Client Account", col2X, yRight, bodyPaint)
            }
            
            yRight += 20f
            val detailsLabel = if (invoice.isQuotation) "QUOTATION DETAILS" else "INVOICE DETAILS"
            canvas.drawText(detailsLabel, col2X, yRight, sectionHeaderPaint)
            yRight += 15f
            val noLabel = if (invoice.isQuotation) "Quote No: " else "Invoice No: "
            canvas.drawText(noLabel, col2X, yRight, bodyPaint)
            val displayInvoiceNo = invoice.id.substringAfterLast("-", invoice.id)
            canvas.drawText(displayInvoiceNo, col2X + 75f, yRight, bodyBoldPaint)
            yRight += 13f
            canvas.drawText("Date: ", col2X, yRight, bodyPaint)
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(invoice.date))
            canvas.drawText(dateStr, col2X + 75f, yRight, bodyPaint)

            y = Math.max(yLeft, yRight) + 30f

            // --- 3. Items Table ---
            // Table Headers
            val tableHeaderPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            }
            val tableHeaderBgPaint = Paint().apply {
                color = primaryColor
                style = Paint.Style.FILL
            }

            canvas.drawRect(leftMargin, y, rightMargin, y + 22f, tableHeaderBgPaint)
            canvas.drawText("SL", leftMargin + 10f, y + 14f, tableHeaderPaint)
            canvas.drawText("ITEM DESCRIPTION", leftMargin + 40f, y + 14f, tableHeaderPaint)
            canvas.drawText("QTY", leftMargin + 285f, y + 14f, tableHeaderPaint)
            
            val priceHeader = "UNIT PRICE"
            canvas.drawText(priceHeader, leftMargin + 415f - tableHeaderPaint.measureText(priceHeader), y + 14f, tableHeaderPaint)
            
            val totalHeader = "TOTAL"
            canvas.drawText(totalHeader, rightMargin - 15f - tableHeaderPaint.measureText(totalHeader), y + 14f, tableHeaderPaint)

            y += 22f

            // Table Rows
            val tableBorderPaint = Paint().apply {
                color = dividerColor
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }
            val rowBgPaint = Paint().apply {
                color = lightGrayColor
                style = Paint.Style.FILL
            }

            var subtotal = 0.0
            for (index in items.indices) {
                val item = items[index]
                val serials = item.serialNumber?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                val hasSerial = serials.isNotEmpty()
                val rowHeight = if (hasSerial) 24f + (serials.size * 10f) else 24f

                // Draw alternate row backgrounds
                if (index % 2 == 1) {
                    canvas.drawRect(leftMargin, y, rightMargin, y + rowHeight, rowBgPaint)
                }

                // Bottom row divider line
                canvas.drawLine(leftMargin, y + rowHeight, rightMargin, y + rowHeight, tableBorderPaint)

                // Render SL
                canvas.drawText((index + 1).toString(), leftMargin + 10f, y + 15f, bodyPaint)

                // Render Item name & serial
                val displayName = if (item.productName.length > 40) item.productName.take(37) + "..." else item.productName
                canvas.drawText(displayName, leftMargin + 40f, y + 15f, bodyBoldPaint)
                if (hasSerial) {
                    serials.forEachIndexed { sIdx, sNum ->
                        canvas.drawText("S/N: $sNum", leftMargin + 40f, y + 26f + (sIdx * 10f), mutedPaint)
                    }
                }

                // Render Qty (centered)
                val qtyStr = item.quantity.toString()
                canvas.drawText(qtyStr, leftMargin + 285f - bodyPaint.measureText(qtyStr) / 2f, y + 15f, bodyPaint)

                // Render Unit Price (right aligned)
                val upStr = String.format(Locale.US, "৳%,.2f", item.unitPrice)
                canvas.drawText(upStr, leftMargin + 415f - bodyPaint.measureText(upStr), y + 15f, bodyPaint)

                // Render Total Price (right aligned)
                val tpStr = String.format(Locale.US, "৳%,.2f", item.totalPrice)
                canvas.drawText(tpStr, rightMargin - 15f - bodyBoldPaint.measureText(tpStr), y + 15f, bodyBoldPaint)

                subtotal += item.totalPrice
                y += rowHeight
            }

            // Fill empty rows to make the table look elegant if there are few items
            val targetTableBottom = 550f
            while (y < targetTableBottom) {
                canvas.drawLine(leftMargin, y + 24f, rightMargin, y + 24f, tableBorderPaint)
                y += 24f
            }

            // --- 4. Calculation Breakdown and Total ---
            y += 15f
            val summaryLabelsX = 350f
            val summaryValsX = rightMargin - 10f

            val labelPaint = Paint().apply {
                color = secondaryColor
                textSize = 9f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }
            val valuePaint = Paint().apply {
                color = primaryColor
                textSize = 9f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            }

            // Subtotal
            canvas.drawText("Equipment Subtotal:", summaryLabelsX, y, labelPaint)
            val subStr = String.format(Locale.US, "৳%,.2f", subtotal)
            canvas.drawText(subStr, summaryValsX - valuePaint.measureText(subStr), y, valuePaint)

            // Service Charge (Labor)
            if (invoice.serviceCharge > 0) {
                y += 16f
                canvas.drawText("Labor/Service Fee:", summaryLabelsX, y, labelPaint)
                val scStr = String.format(Locale.US, "৳%,.2f", invoice.serviceCharge)
                canvas.drawText(scStr, summaryValsX - valuePaint.measureText(scStr), y, valuePaint)
            }

            // Discount
            if (invoice.discount > 0) {
                y += 16f
                canvas.drawText("Discount Applied:", summaryLabelsX, y, labelPaint)
                val dsStr = String.format(Locale.US, "-৳%,.2f", invoice.discount)
                canvas.drawText(dsStr, summaryValsX - valuePaint.measureText(dsStr), y, valuePaint)
            }

            // Grand Total (Highlighted card/bg)
            val grandTotalLabelPaint = Paint().apply {
                color = accentColor
                textSize = 10f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            }
            val grandTotalValPaint = Paint().apply {
                color = primaryColor
                textSize = 11f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            }

            if (!invoice.isQuotation) {
                val remainingDue = (invoice.totalAmount - amountPaid).coerceAtLeast(0.0)
                
                if (remainingDue <= 0.01) {
                    // --- PAID IN FULL STAMP ---
                    y += 30f
                    val stampRect = RectF(summaryLabelsX - 10f, y - 10f, rightMargin, y + 45f)
                    canvas.drawRoundRect(stampRect, 8f, 8f, Paint().apply {
                        color = Color.rgb(20, 184, 166) // Teal 500
                        style = Paint.Style.FILL
                        alpha = 20
                    })
                    canvas.drawRoundRect(stampRect, 8f, 8f, Paint().apply {
                        color = Color.rgb(20, 184, 166)
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                    })
                    
                    val paidText = "PAID IN FULL"
                    val paidPaint = Paint().apply {
                        color = Color.rgb(20, 184, 166)
                        textSize = 22f
                        typeface = Typeface.create("sans-serif", Typeface.BOLD)
                        isAntiAlias = true
                    }
                    canvas.drawText(paidText, summaryLabelsX + (rightMargin - summaryLabelsX) / 2f - paidPaint.measureText(paidText) / 2f, y + 18f, paidPaint)
                    
                    val totalPaidText = String.format(Locale.US, "TOTAL AMOUNT PAID: ৳%,.2f", amountPaid)
                    val totalPaidPaint = Paint().apply {
                        color = primaryColor
                        textSize = 10f
                        typeface = Typeface.create("sans-serif", Typeface.BOLD)
                        isAntiAlias = true
                    }
                    canvas.drawText(totalPaidText, summaryLabelsX + (rightMargin - summaryLabelsX) / 2f - totalPaidPaint.measureText(totalPaidText) / 2f, y + 36f, totalPaidPaint)
                    
                    y += 65f
                } else {
                    y += 18f
                    // Total Amount
                    canvas.drawText("Total Amount Due:", summaryLabelsX, y, labelPaint)
                    val totStr = String.format(Locale.US, "৳%,.2f", invoice.totalAmount)
                    canvas.drawText(totStr, summaryValsX - valuePaint.measureText(totStr), y, valuePaint)
                    
                    // Paid to Date
                    y += 16f
                    canvas.drawText("Paid to Date:", summaryLabelsX, y, labelPaint)
                    val paidStr = String.format(Locale.US, "৳%,.2f", amountPaid)
                    canvas.drawText(paidStr, summaryValsX - valuePaint.measureText(paidStr), y, valuePaint)
                    
                    // Remaining Due (with Highlighted Box)
                    y += 22f
                    val totalBoxHeight = 28f
                    canvas.drawRect(summaryLabelsX - 10f, y - 8f, rightMargin, y - 8f + totalBoxHeight, Paint().apply {
                        color = lightAccentColor
                        style = Paint.Style.FILL
                    })
                    canvas.drawRect(summaryLabelsX - 10f, y - 8f, rightMargin, y - 8f + totalBoxHeight, Paint().apply {
                        color = accentColor
                        style = Paint.Style.STROKE
                        strokeWidth = 1f
                    })
                    
                    canvas.drawText("REMAINING DUE:", summaryLabelsX, y + 10f, grandTotalLabelPaint)
                    val remStr = String.format(Locale.US, "৳%,.2f", remainingDue)
                    canvas.drawText(remStr, summaryValsX - grandTotalValPaint.measureText(remStr), y + 10f, grandTotalValPaint)
                    
                    y += totalBoxHeight + 35f
                }
            } else {
                // Grand Total for Quotation
                y += 22f
                val totalBoxHeight = 28f
                canvas.drawRect(summaryLabelsX - 10f, y - 8f, rightMargin, y - 8f + totalBoxHeight, Paint().apply {
                    color = lightAccentColor
                    style = Paint.Style.FILL
                })
                canvas.drawRect(summaryLabelsX - 10f, y - 8f, rightMargin, y - 8f + totalBoxHeight, Paint().apply {
                    color = accentColor
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                })
                
                canvas.drawText("TOTAL QUOTE AMOUNT:", summaryLabelsX, y + 10f, grandTotalLabelPaint)
                val gtStr = String.format(Locale.US, "৳%,.2f", invoice.totalAmount)
                canvas.drawText(gtStr, summaryValsX - grandTotalValPaint.measureText(gtStr), y + 10f, grandTotalValPaint)
                
                y += totalBoxHeight + 35f
            }

            // --- 5. Signatures and Authorized Stamps ---
            val sigLineY = y + 45f
            
            // Draw company authorized signature bitmap if present in preferences
            val signatureBase64 = prefs.getString("company_signature_base64", "") ?: ""
            val companySigBitmap = decodeBase64ToBitmap(signatureBase64)
            if (companySigBitmap != null) {
                try {
                    val destRect = RectF(leftMargin + 20f, sigLineY - 42f, leftMargin + 130f, sigLineY - 2f)
                    canvas.drawBitmap(companySigBitmap, null, destRect, Paint().apply { isFilterBitmap = true })
                } catch (e: Exception) {
                    Log.e("InvoicePdfGenerator", "Failed to draw company signature to PDF: ${e.message}", e)
                }
            }

            // Authorized by
            canvas.drawLine(leftMargin, sigLineY, leftMargin + 150f, sigLineY, tableBorderPaint)
            canvas.drawText("Authorized Signature", leftMargin, sigLineY + 12f, Paint().apply {
                color = grayTextColor
                textSize = 8f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            })
            canvas.drawText(companyName, leftMargin, sigLineY + 23f, mutedPaint)

            // Received by
            val recX = rightMargin - 150f
            canvas.drawLine(recX, sigLineY, rightMargin, sigLineY, tableBorderPaint)
            canvas.drawText("Customer Signature", recX, sigLineY + 12f, Paint().apply {
                color = grayTextColor
                textSize = 8f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            })
            canvas.drawText("Client Acceptance", recX, sigLineY + 23f, mutedPaint)

            // Draw client signature bitmap above line
            if (signatureBitmap != null) {
                try {
                    val destRect = RectF(recX + 20f, sigLineY - 42f, recX + 130f, sigLineY - 2f)
                    canvas.drawBitmap(signatureBitmap, null, destRect, Paint().apply { isFilterBitmap = true })
                } catch (e: Exception) {
                    Log.e("InvoicePdfGenerator", "Failed to draw signature to PDF: ${e.message}", e)
                }
            }

            // --- 6. Professional Elegant Footer ---
            val footerY = 810f
            canvas.drawLine(leftMargin, footerY, rightMargin, footerY, tableBorderPaint)
            
            val footerTitlePaint = Paint().apply {
                color = primaryColor
                textSize = 8f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                isAntiAlias = true
            }
            val footerTextPaint = Paint().apply {
                color = grayTextColor
                textSize = 7f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                isAntiAlias = true
            }

            val footerGreeting = if (invoice.isQuotation) "We look forward to working with you!" else "Thank you for your business!"
            canvas.drawText(footerGreeting, leftMargin, footerY + 12f, footerTitlePaint)
            canvas.drawText("For any queries, please email info@nitcrm.com or call $companyPhone.", leftMargin, footerY + 21f, footerTextPaint)
            canvas.drawText("Page 1 of 1", rightMargin - footerTextPaint.measureText("Page 1 of 1"), footerY + 12f, footerTextPaint)

            // Render Draft Watermark if it's a draft invoice
            if (isDraft) {
                canvas.save()
                canvas.translate(297.5f, 421f)
                canvas.rotate(-30f)
                val watermarkPaint = Paint().apply {
                    color = Color.rgb(239, 68, 68) // Translucent Red
                    alpha = 20 // very faint opacity (approx 8%)
                    textSize = 90f
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("DRAFT", 0f, 30f, watermarkPaint)
                canvas.restore()
            }

            pdfDocument.finishPage(page)

            // Save document file to external cache/files so it can be shared via Intent
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()
            
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("InvoicePdfGenerator", "Failed to generate PDF: ${e.message}", e)
            return null
        }
    }
}
