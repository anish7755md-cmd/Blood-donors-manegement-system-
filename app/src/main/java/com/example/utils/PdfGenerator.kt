package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.Donor
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {
    fun generateDonorReport(context: Context, donors: List<Donor>): File? {
        val pdfDocument = PdfDocument()
        
        // A4 page dimensions are 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        var paint = Paint()
        
        // Title Bar Header (Crimson block background decoration)
        paint.color = Color.parseColor("#B71C1C")
        canvas.drawRect(0f, 0f, 595f, 15f, paint)
        
        // Draw main title
        paint.color = Color.parseColor("#B71C1C") // Deep crimson
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Karnataka Blood Donors System", 40f, 55f, paint)
        
        // Draw subtitle
        paint.color = Color.GRAY
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Registered Donors Directory - Official Report", 40f, 75f, paint)
        
        // Timestamps and counts
        paint.color = Color.BLACK
        paint.textSize = 9f
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        val generatedDate = dateFormat.format(Date())
        canvas.drawText("Generated on: $generatedDate", 40f, 95f, paint)
        canvas.drawText("Total Active Records: ${donors.size}", 40f, 110f, paint)
        
        // Draw Table Header Row
        val startY = 145f
        paint.color = Color.parseColor("#F5F5F5") // Light grey table header
        canvas.drawRect(40f, startY - 15f, 555f, startY + 15f, paint)
        
        paint.color = Color.parseColor("#000000")
        paint.isFakeBoldText = true
        paint.textSize = 9f
        canvas.drawText("NO.", 45f, startY + 5f, paint)
        canvas.drawText("FULL NAME", 75f, startY + 5f, paint)
        canvas.drawText("BLOOD", 210f, startY + 5f, paint)
        canvas.drawText("DISTRICT", 270f, startY + 5f, paint)
        canvas.drawText("CITY", 370f, startY + 5f, paint)
        canvas.drawText("MOBILE", 460f, startY + 5f, paint)
        
        // Draw Table Rows
        var currentY = startY + 35f
        paint.isFakeBoldText = false
        
        for ((index, donor) in donors.withIndex()) {
            if (currentY > 790f) { // If page limits exceeded, spawn a new page
                pdfDocument.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, pdfDocument.pages.size + 1).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                
                // Draw a simple title banner on secondary pages
                paint.color = Color.parseColor("#B71C1C")
                paint.textSize = 12f
                paint.isFakeBoldText = true
                canvas.drawText("Karnataka Blood Donors System - Directory Page ${pdfDocument.pages.size + 1}", 40f, 45f, paint)
                
                // Redraw table headers on secondary pages
                paint.color = Color.parseColor("#F5F5F5")
                canvas.drawRect(40f, 75f, 555f, 105f, paint)
                paint.color = Color.BLACK
                canvas.drawText("NO.", 45f, 95f, paint)
                canvas.drawText("FULL NAME", 75f, 95f, paint)
                canvas.drawText("BLOOD", 210f, 95f, paint)
                canvas.drawText("DISTRICT", 270f, 95f, paint)
                canvas.drawText("CITY", 370f, 95f, paint)
                canvas.drawText("MOBILE", 460f, 95f, paint)
                
                currentY = 130f
                paint.isFakeBoldText = false
            }
            
            // Draw visual line grid
            paint.color = Color.parseColor("#EEEEEE")
            canvas.drawLine(40f, currentY - 15f, 555f, currentY - 15f, paint)
            
            paint.color = Color.BLACK
            paint.textSize = 9f
            canvas.drawText("${index + 1}", 45f, currentY, paint)
            
            // Handle string overflow for formatting
            val nameDisplay = if (donor.fullName.length > 22) donor.fullName.substring(0, 20) + ".." else donor.fullName
            canvas.drawText(nameDisplay, 75f, currentY, paint)
            
            // Highlight blood group in blood red
            paint.color = Color.parseColor("#B71C1C")
            paint.isFakeBoldText = true
            canvas.drawText(donor.bloodGroup, 210f, currentY, paint)
            
            // Reset to black and normal font size
            paint.color = Color.BLACK
            paint.isFakeBoldText = false
            canvas.drawText(donor.district, 270f, currentY, paint)
            
            val cityDisplay = if (donor.city.length > 15) donor.city.substring(0, 13) + ".." else donor.city
            canvas.drawText(cityDisplay, 370f, currentY, paint)
            
            canvas.drawText(donor.mobile, 460f, currentY, paint)
            
            currentY += 24f
        }
        
        pdfDocument.finishPage(page)
        
        // Save output report file natively to caching streams
        val file = File(context.cacheDir, "Blood_Donors_Report.pdf")
        return try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            e.printStackTrace()
            null
        }
    }
}
