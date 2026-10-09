package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun QrDisplayView(
    qrContent: String,
    modifier: Modifier = Modifier
) {
    val trimmed = qrContent.trim()

    // 1. Check if base64 encoded image
    val base64Bitmap = remember(trimmed) {
        if (trimmed.startsWith("data:image") || trimmed.length > 100 && !trimmed.startsWith("http") && !trimmed.startsWith("upi:")) {
            try {
                val cleanBase64 = if (trimmed.contains(",")) trimmed.substringAfter(",") else trimmed
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            base64Bitmap != null -> {
                Image(
                    bitmap = base64Bitmap.asImageBitmap(),
                    contentDescription = "Payment QR Code",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
                AsyncImage(
                    model = trimmed,
                    contentDescription = "Payment QR Code",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            else -> {
                // If it's a UPI string or text, generate a clean QR-like pattern visualizer with UPI details
                UpiQrVisualizer(content = trimmed)
            }
        }
    }
}

/**
 * Visual QR representation when string is a UPI intent or text
 */
@Composable
private fun UpiQrVisualizer(
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCode,
            contentDescription = "QR Code",
            modifier = Modifier.size(140.dp),
            tint = Color(0xFF1B5E20)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Scan to Pay with UPI",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF1B5E20)
        )
        if (content.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content.take(36) + if (content.length > 36) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
