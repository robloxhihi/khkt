package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SpaceCardBorder
import com.example.ui.theme.SpaceCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

/**
 * Format decimal degrees to DMS string like 27°31'31.1"S 153°1...
 */
fun formatCoordinatesDMS(lat: Double, lng: Double): String {
    val latDirection = if (lat >= 0) "N" else "S"
    val absLat = Math.abs(lat)
    val latDeg = absLat.toInt()
    val latMin = ((absLat - latDeg) * 60).toInt()
    val latSec = ((absLat - latDeg - latMin / 60.0) * 3600)

    val lngDirection = if (lng >= 0) "E" else "W"
    val absLng = Math.abs(lng)
    val lngDeg = absLng.toInt()
    val lngMin = ((absLng - lngDeg) * 60).toInt()
    val lngSec = ((absLng - lngDeg - lngMin / 60.0) * 3600)

    return String.format(
        Locale.US,
        "%d°%02d'%04.1f\"%s %d°%02d'...",
        latDeg, latMin, latSec, latDirection, lngDeg, lngMin
    )
}

/**
 * Google Maps style interactive location card exactly matching the user's reference image
 */
@Composable
fun GoogleMapsStudentLocationCard(
    studentName: String,
    latitude: Double,
    longitude: Double,
    safeZoneName: String,
    batteryLevel: Int,
    isCharging: Boolean,
    speedKmh: Float,
    isPrivacyActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dmsText = formatCoordinatesDMS(latitude, longitude)

    val openGoogleMaps = {
        try {
            val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($studentName - $safeZoneName)")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFE8ECEF)) // Google Maps road base
            .border(1.5.dp, SpaceCardBorder, RoundedCornerShape(20.dp))
            .height(310.dp)
            .testTag("google_maps_student_location_card")
    ) {
        // Map Background Canvas (Streets, Water, Route Line)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Water / Park patches
            drawRect(
                color = Color(0xFFC7E6F8), // Water blue
                topLeft = Offset(w * 0.45f, 0f),
                size = androidx.compose.ui.geometry.Size(w * 0.55f, h * 0.35f)
            )
            drawRect(
                color = Color(0xFFD4EED8), // Park green
                topLeft = Offset(w * 0.65f, 0f),
                size = androidx.compose.ui.geometry.Size(w * 0.35f, h * 0.28f)
            )
            drawRect(
                color = Color(0xFFE4F5D9), // Park green bottom right
                topLeft = Offset(w * 0.7f, h * 0.58f),
                size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.35f)
            )

            // Grid Roads (Light grey)
            val roadColor = Color.White
            val mainRoadColor = Color(0xFFF9D67A) // Google Maps Yellow Major Road

            // Vertical & Horizontal Grid
            for (i in 1..6) {
                drawLine(
                    color = roadColor,
                    start = Offset(w * (i * 0.15f), 0f),
                    end = Offset(w * (i * 0.15f), h),
                    strokeWidth = 6f
                )
                drawLine(
                    color = roadColor,
                    start = Offset(0f, h * (i * 0.16f)),
                    end = Offset(w, h * (i * 0.16f)),
                    strokeWidth = 6f
                )
            }

            // Primary Arterial Road 22 (Horizontal Blue Highway)
            drawLine(
                color = Color(0xFF86A8D6),
                start = Offset(0f, h * 0.45f),
                end = Offset(w, h * 0.45f),
                strokeWidth = 14f
            )

            // Primary North-South Road 46
            drawLine(
                color = Color(0xFF86A8D6),
                start = Offset(w * 0.18f, 0f),
                end = Offset(w * 0.18f, h),
                strokeWidth = 14f
            )

            // Blue Transit Route Path (leading to student pin)
            val routeColor = Color(0xFF4285F4)
            val routePath = Path().apply {
                moveTo(0f, h * 0.45f)
                lineTo(w * 0.52f, h * 0.45f)
                lineTo(w * 0.52f, h * 0.72f)
                lineTo(w * 0.6f, h * 0.72f)
                lineTo(w * 0.6f, h * 0.98f)
            }
            drawPath(
                path = routePath,
                color = routeColor.copy(alpha = 0.5f),
                style = Stroke(width = 16f)
            )
            drawPath(
                path = routePath,
                color = Color(0xFF2979FF),
                style = Stroke(width = 8f)
            )
        }

        // Street & Place Labels (Overlaid on canvas)
        Box(modifier = Modifier.fillMaxSize()) {
            // "Delancey St" label
            Text(
                text = "Delancey St",
                fontSize = 11.sp,
                color = Color(0xFF4A5568),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp, top = 80.dp)
            )

            // Road 46 Badge
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 58.dp, top = 60.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1976D2))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(text = "46", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Road 22 Badge
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(end = 120.dp, bottom = 40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1976D2))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(text = "22", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Road 47 Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(end = 40.dp, bottom = 48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1976D2))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(text = "47", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Park label top right
            Text(
                text = "Raby Bay\nForeshore Park",
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 22.dp, end = 24.dp)
            )
        }

        // RED GOOGLE MAPS PIN MARKER (Teardrop pin right at destination)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Pin head with white student avatar inside
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFFEA4335)) // Google Maps Pin Red
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
                // Pin pointer triangle
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(8.dp)
                        .background(Color(0xFFB71C1C))
                )
                // Pin Ground Shadow
                Box(
                    modifier = Modifier
                        .size(14.dp, 6.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
        }

        // FLOATING TOP CARD (White box with Coordinates & "View larger map" matching Image 1)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .shadow(6.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
                .clickable { openGoogleMaps() }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = dmsText,
                    color = Color(0xFF202124),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View larger map",
                        color = Color(0xFF1A73E8), // Google Blue link
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Mở Google Maps",
                        tint = Color(0xFF1A73E8),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // SATELLITE THUMBNAIL BADGE (Bottom Left - matching Image 1)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 10.dp)
                .size(42.dp)
                .shadow(4.dp, RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF334E68))
                .border(2.dp, Color.White, RoundedCornerShape(6.dp))
                .clickable { openGoogleMaps() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = "Vệ tinh",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // FULLSCREEN EXPAND BUTTON (Bottom Right - matching Image 1)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 10.dp)
                .size(40.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { openGoogleMaps() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CropFree,
                contentDescription = "Toàn màn hình",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(22.dp)
            )
        }

        // Google Logo & Copyright Notice (Bottom Center)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Google",
                color = Color(0xFF5F6368),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Map data ©2026 Google",
                color = Color(0xFF5F6368),
                fontSize = 8.sp
            )
        }
    }
}
