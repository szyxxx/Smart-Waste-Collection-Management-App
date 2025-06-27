package com.bluebin.ui.components

import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

object MapMarkerUtils {
    
    fun getDriverMarkerIcon(): BitmapDescriptor {
        // Create a custom truck icon bitmap for drivers instead of pinpoint
        return createTruckMarkerBitmap()
    }
    
    private fun createTruckMarkerBitmap(): BitmapDescriptor {
        // Create a custom truck marker using a truck emoji/icon
        // This creates a visually distinct marker from the pinpoint TPS markers
        val paint = android.graphics.Paint().apply {
            textSize = 60f
            color = android.graphics.Color.parseColor("#FF6B35") // Orange color
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        
        val text = "🚛" // Truck emoji
        val bounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        
        val bitmap = android.graphics.Bitmap.createBitmap(
            bounds.width() + 20, 
            bounds.height() + 20, 
            android.graphics.Bitmap.Config.ARGB_8888
        )
        
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawText(
            text, 
            bitmap.width / 2f, 
            bitmap.height / 2f + bounds.height() / 2f, 
            paint
        )
        
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
    
    fun getTpsMarkerIcon(isFull: Boolean = false): BitmapDescriptor {
        // Use red for full TPS, blue for normal TPS
        return if (isFull) {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        } else {
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
        }
    }
    
    fun getRouteStopMarkerIcon(isCompleted: Boolean, isCurrent: Boolean): BitmapDescriptor {
        return when {
            isCompleted -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            isCurrent -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
        }
    }
} 