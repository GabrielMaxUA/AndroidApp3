package com.trios2024aa.treasurehunt

import android.content.Context
import android.location.Geocoder
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class MapHandler(
    private val context: Context,
    private val googleMap: GoogleMap
) {

    fun setupMap(treasures: List<TreasureItem>) {
        treasures.forEach { treasure ->
            val position = getLocationFromAddress(context, treasure.address)
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(treasure.name)
            )
            marker?.tag = treasure
        }

        googleMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoContents(marker: Marker): View? {
                val treasure = marker.tag as? TreasureItem ?: return null

                // Create the LinearLayout for the InfoWindow
                val infoWindow = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(16, 16, 16, 16)
                }

                // Create the ImageView for the StreetView image
                val imageView = ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(300, 300) // Set appropriate dimensions
                }

                // Load the image using Glide
                val imageUrl = "https://maps.googleapis.com/maps/api/streetview?size=600x300&location=${getLocationFromAddress(context, treasure.address).latitude},${getLocationFromAddress(context, treasure.address).longitude}&key=AIzaSyCj56qDhghVxSU3GMwpzQiwX2ksLpF_qSE"
                Glide.with(context)
                    .load(imageUrl)
                    .into(imageView)

                // Add the ImageView to the InfoWindow layout
                infoWindow.addView(imageView)

                // Create the LinearLayout for the text content (title and address)
                val textLayout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 0, 0, 0)
                }

                // Create the TextView for the title
                val titleView = TextView(context).apply {
                    text = treasure.name
                    textSize = 16f
                    setTextColor(context.getColor(android.R.color.black))
                }

                // Create the TextView for the address
                val addressView = TextView(context).apply {
                    text = treasure.address
                    textSize = 14f
                    setTextColor(context.getColor(android.R.color.darker_gray))
                }

                // Add the TextViews to the text layout
                textLayout.addView(titleView)
                textLayout.addView(addressView)

                // Add the text layout to the InfoWindow layout
                infoWindow.addView(textLayout)

                return infoWindow
            }

            override fun getInfoWindow(marker: Marker): View? {
                return null // Use the default frame
            }
        })
    }

    private fun getLocationFromAddress(context: Context, strAddress: String): LatLng {
        return try {
            val coder = Geocoder(context, Locale.getDefault())
            val address = coder.getFromLocationName(strAddress, 1)
            if (address != null && address.isNotEmpty()) {
                LatLng(address[0].latitude, address[0].longitude)
            } else {
                LatLng(0.0, 0.0) // Default location if not found
            }
        } catch (e: Exception) {
            e.printStackTrace()
            LatLng(0.0, 0.0) // Default location if an error occurs
        }
    }
}
