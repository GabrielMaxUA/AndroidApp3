package com.trios2024aa.treasurehunt

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

// CustomInfoWindowAdapter.kt
class CustomInfoWindowAdapter(private val context: Context) : GoogleMap.InfoWindowAdapter {

    override fun getInfoWindow(marker: Marker): View? {
        // Return null to use the default info window frame
        return null
    }

    override fun getInfoContents(marker: Marker): View? {
        // Create a FrameLayout to host the ComposeView
        val frameLayout = FrameLayout(context)

        // Create a ComposeView and set the CustomInfoWindowContent composable
        val composeView = ComposeView(context).apply {
            setContent {
                CustomInfoWindowContent(
                    title = marker.title ?: "",
                    address = marker.snippet ?: ""
                )
            }
        }

        frameLayout.addView(composeView)
        return frameLayout
    }
}
