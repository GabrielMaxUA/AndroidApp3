package com.trios2024aa.treasurehunt

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

// Data class representing a treasure item
data class TreasureItem(
    var name: String,
    var address: String,
    var description: String,
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var subItems: List<String> = emptyList(), // Array of sub-items
    var isDone: Boolean = false, // Tracks if the user has marked the task as done
    var hasReachedDestination: Boolean = false // Tracks if the user has reached the destination
)

@Composable
fun TreasureList() {
    val context = LocalContext.current
    val preferences = context.getSharedPreferences("treasure_prefs", Context.MODE_PRIVATE)
    val editor = preferences.edit()
    val geocoder = Geocoder(context)

    // Predefined list of treasures
    val predefinedItems = listOf(
        TreasureItem(
            name = "Case of money",
            address = "300 George street",
            description = "It's hidden right below you.",
            lat = 43.6629,
            lng = -79.3957
        ),
        TreasureItem(
            name = "Diamond",
            address = "101 Charles Street east",
            description = "It's hidden right below you.",
            lat = 43.6715,
            lng = -79.3837
        ),
        TreasureItem(
            name = "Necklace",
            address = "279 Jarvis street",
            description = "It's hidden right below you.",
            lat = 43.6604,
            lng = -79.3757
        ),
        TreasureItem(
            name = "Chest of Gold",
            address = "Allan's Garden",
            description = "It's hidden right below you.",
            lat = 43.6629,
            lng = -79.3729
        ),
        TreasureItem(
            name = "Key from the bank deposit",
            address = "250 Dundas street",
            description = "It's hidden right below you.",
            lat = 43.6568,
            lng = -79.3802
        )
    )

    // Load items from SharedPreferences and combine with predefined items, avoiding duplicates
    var tItems by remember {
        mutableStateOf(
            predefinedItems + loadItemsFromPreferences(preferences).filterNot { loadedItem ->
                predefinedItems.any { predefinedItem -> predefinedItem.name == loadedItem.name }
            }
        )
    }

    // State variables for managing UI
    var showDialog by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }
    var treasureName by remember { mutableStateOf("") }
    var treasureDescription by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var currentTreasureItem by remember { mutableStateOf<TreasureItem?>(null) }
    var showTreasureFoundDialog by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Function to geocode an address into lat/lng coordinates
    fun geocodeAddress(address: String): Pair<Double, Double>? {
        return try {
            val results = geocoder.getFromLocationName(address, 1)
            if (results?.isNotEmpty() == true) {
                Pair(results[0].latitude, results[0].longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Permission launcher for requesting location access
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        currentTreasureItem?.let { item ->
                            val latitude = location.latitude
                            val longitude = location.longitude
                            val intentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${item.address},Toronto,Canada&origin=$latitude,$longitude")
                            val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)

                            // Simulate that the user reached the destination
                            item.hasReachedDestination = true
                            saveItemsToPreferences(tItems, editor)

                            // Show the treasure found dialog
                            showTreasureFoundDialog = true
                        }
                    } else {
                        Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Function to handle the location click event
    fun handleLocationClick(item: TreasureItem) {
        currentTreasureItem = item
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val intentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${item.address},Toronto,Canada&origin=$latitude,$longitude")
                    val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    context.startActivity(mapIntent)

                    // Simulate that the user reached the destination
                    item.hasReachedDestination = true
                    saveItemsToPreferences(tItems, editor)

                    // Show the treasure found dialog
                    showTreasureFoundDialog = true
                } else {
                    Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                showPermissionDialog = true
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    if (showMap) {
        // Geocode all addresses before displaying the map
        tItems = tItems.map { item ->
            if (item.lat == 0.0 && item.lng == 0.0) {
                val coordinates = geocodeAddress(item.address)
                if (coordinates != null) {
                    item.copy(lat = coordinates.first, lng = coordinates.second)
                } else {
                    item
                }
            } else {
                item
            }
        }

        // Show the map with treasure locations pinned
        MapScreen(tItems = tItems, onBack = { showMap = false })
    } else {
        // Composable UI layout
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Button to add a new treasure item
                Button(
                    onClick = { showDialog = true }
                ) {
                    Text("Add address")
                }
                Button(onClick = { showMap = true }) {
                    Text("Open Map")
                }
            }

            // List of treasure items displayed in a LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(tItems) { item ->
                    TreasureListItem(
                        item = item,
                        onLocationClick = { handleLocationClick(it) }
                    )
                }
            }

            // Dialog for adding a new treasure item
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(onClick = {
                                if (treasureName.isNotBlank() || address.isNotBlank() || treasureDescription.isNotBlank()) {
                                    val newTreasure = TreasureItem(
                                        name = treasureName,
                                        address = address,
                                        description = treasureDescription
                                    )
                                    val coordinates = geocodeAddress(address)
                                    val updatedTreasure = if (coordinates != null) {
                                        newTreasure.copy(lat = coordinates.first, lng = coordinates.second)
                                    } else {
                                        newTreasure
                                    }
                                    tItems = tItems + updatedTreasure
                                    saveItemsToPreferences(tItems, editor)
                                    showDialog = false
                                    treasureName = ""
                                    address = ""
                                    treasureDescription = ""
                                }
                            }) {
                                Text(text = "Add")
                            }
                            Button(onClick = { showDialog = false }) {
                                Text(text = "Cancel")
                            }
                        }

                    },
                    title = { Text("Add a treasure Item") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = treasureName,
                                onValueChange = { treasureName = it },
                                singleLine = true,
                                placeholder = { Text(text = "What is your treasure?") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                singleLine = true,
                                placeholder = { Text(text = "What is the location of it?") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                            OutlinedTextField(
                                value = treasureDescription,
                                onValueChange = { treasureDescription = it },
                                singleLine = true,
                                placeholder = { Text(text = "Describe your treasure") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        }

                    }
                )
            }

            // Dialog requesting location permission if needed
            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    confirmButton = {
                        Button(onClick = {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            showPermissionDialog = false
                        }) {
                            Text("Allow")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showPermissionDialog = false }) {
                            Text("Deny")
                        }
                    },
                    title = { Text("Permission Needed") },
                    text = {
                        Text("Location permission is required to show your current location on the map.")
                    }
                )
            }

            // Dialog showing that the treasure is found
            if (showTreasureFoundDialog) {
                AlertDialog(
                    onDismissRequest = { showTreasureFoundDialog = false },
                    confirmButton = {
                        Button(onClick = {
                            showTreasureFoundDialog = false
                        }) {
                            Text("OK")
                        }
                    },
                    title = { Text("Treasure Found!") },
                    text = {
                        Text("Congratulations! You have found the treasure.")
                    }
                )
            }
        }
    }
}



@Composable
fun MapScreen(tItems: List<TreasureItem>, onBack: () -> Unit) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(43.651070, -79.347015), 12f)
    }

    var selectedTreasure by remember { mutableStateOf<TreasureItem?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapClick = { selectedTreasure = null } // Deselect the treasure when the map is clicked
        ) {
            tItems.forEach { treasure ->
                Marker(
                    state = rememberMarkerState(position = LatLng(treasure.lat, treasure.lng)),
                    title = treasure.name,
                    snippet = treasure.address,
                    onClick = {
                        selectedTreasure = treasure
                        true
                    }
                )
            }
        }

        selectedTreasure?.let { treasure ->
            val imageUrl = "https://maps.googleapis.com/maps/api/streetview?size=400x400&location=${treasure.lat},${treasure.lng}&key=AIzaSyCj56qDhghVxSU3GMwpzQiwX2ksLpF_qSE"
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                CustomInfoWindowContent(
                    title = treasure.name,
                    address = treasure.address,
                    description = treasure.description,
                    imageUrl = imageUrl
                )
            }
        }

        Button(
            onClick = { onBack() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Back to List")
        }
    }
}

@Composable
fun CustomInfoWindowContent(
    title: String,
    address: String,
    description: String,
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color.White)
            .border(1.dp, Color.Gray)
            .padding(8.dp)
    ) {
        Image(
            painter = rememberAsyncImagePainter(imageUrl),
            contentDescription = "Street View Image",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Light)
            )
        }
    }
}

@Composable
fun TreasureListItem(
    item: TreasureItem,
    onLocationClick: (TreasureItem) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .border(
                border = BorderStroke(2.dp, Color(0XFF018786)),
                shape = RoundedCornerShape(20)
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = item.name, modifier = Modifier.padding(bottom = 4.dp))

            // Display sub-items
            item.subItems.forEach { subItem ->
                Text(text = subItem, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
            }
        }

        Row(modifier = Modifier.padding(8.dp)) {
            if (!item.hasReachedDestination) {
                IconButton(onClick = { onLocationClick(item) }) {
                    Icon(imageVector = Icons.Default.Place, contentDescription = null)
                }
            }
            if (item.hasReachedDestination) {
                IconButton(onClick = { /* Handle done action */ }) {
                    Icon(imageVector = Icons.Default.Done, contentDescription = null)
                }
            }
        }
    }
}

fun saveItemsToPreferences(items: List<TreasureItem>, editor: SharedPreferences.Editor) {
    val itemsSet = items.map {
        val subItemsString = it.subItems.joinToString("|") // Convert sub-items to a single string
        "${it.name}:${it.address}:${it.description}:${it.lat}:${it.lng}:${subItemsString}:${it.hasReachedDestination}"
    }.toSet()
    editor.putStringSet("treasure_items", itemsSet).apply()
}

fun loadItemsFromPreferences(preferences: SharedPreferences): List<TreasureItem> {
    val itemsSet = preferences.getStringSet("treasure_items", emptySet()) ?: emptySet()
    return itemsSet.mapNotNull {
        val parts = it.split(":")
        if (parts.size >= 7) {
            val subItems = parts[5].split("|").filter { it.isNotBlank() } // Ensure subItems are non-empty
            TreasureItem(
                name = parts[0],
                address = parts[1],
                description = parts[2],
                lat = parts[3].toDoubleOrNull() ?: 0.0,  // Default to 0.0 if conversion fails
                lng = parts[4].toDoubleOrNull() ?: 0.0,  // Default to 0.0 if conversion fails
                subItems = subItems,
                hasReachedDestination = parts[6].toBoolean()
            )
        } else {
            null
        }
    }
}
