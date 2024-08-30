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
    var name: String,             // Name of the treasure
    var address: String,          // Address of the treasure
    var description: String,      // Description of the treasure
    var lat: Double = 0.0,        // Latitude of the treasure location (default 0.0)
    var lng: Double = 0.0,        // Longitude of the treasure location (default 0.0)
    var subItems: List<String> = emptyList(), // List of sub-items related to the treasure
    var isDone: Boolean = false,  // Tracks if the user has marked the task as done
    var hasReachedDestination: Boolean = false // Tracks if the user has reached the destination
)

// Main Composable function displaying the list of treasures and allowing map interaction
@Composable
fun TreasureList() {
    // Retrieve the current context
    val context = LocalContext.current
    // Access shared preferences to store and retrieve data
    val preferences = context.getSharedPreferences("treasure_prefs", Context.MODE_PRIVATE)
    // Create an editor to make changes to the shared preferences
    val editor = preferences.edit()
    // Create a Geocoder object to convert addresses to coordinates
    val geocoder = Geocoder(context)
    // Obtain a reference to the fused location provider client for accessing location services
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Predefined list of treasures
    val predefinedItems = listOf(
        TreasureItem(
            name = "Case of money",
            address = "300 George street",
            description = "It's hidden right below you."
        ),
        TreasureItem(
            name = "Diamond",
            address = "101 Charles Street east",
            description = "It's hidden right below you."
        ),
        TreasureItem(
            name = "Necklace",
            address = "279 Jarvis street",
            description = "It's hidden right below you."
        ),
        TreasureItem(
            name = "Chest of Gold",
            address = "Allan's Garden",
            description = "It's hidden right below you."
        ),
        TreasureItem(
            name = "Key from the bank deposit",
            address = "250 Dundas street",
            description = "It's hidden right below you."
        )
    )

    // Combine predefined items with those loaded from SharedPreferences, avoiding duplicates
    var tItems by remember {
        mutableStateOf(
            predefinedItems + loadItemsFromPreferences(preferences).filterNot { loadedItem ->
                predefinedItems.any { predefinedItem -> predefinedItem.name == loadedItem.name }
            }
        )
    }

    // State variables for managing UI and user interactions
    var showDialog by remember { mutableStateOf(false) } // Controls visibility of the "Add address" dialog
    var showMap by remember { mutableStateOf(false) }    // Controls visibility of the map
    var treasureName by remember { mutableStateOf("") }  // Holds the name of the treasure being added
    var treasureDescription by remember { mutableStateOf("") } // Holds the description of the treasure being added
    var address by remember { mutableStateOf("") }       // Holds the address of the treasure being added
    var showPermissionDialog by remember { mutableStateOf(false) } // Controls visibility of the location permission dialog
    var currentTreasureItem by remember { mutableStateOf<TreasureItem?>(null) } // Holds the currently selected treasure item
    var showTreasureFoundDialog by remember { mutableStateOf(false) } // Controls visibility of the "Treasure Found" dialog

    // Function to geocode an address into latitude and longitude coordinates
    fun geocodeAddress(address: String): Pair<Double, Double>? {
        return try {
            // Attempt to get location coordinates for the given address using Geocoder
            val results = geocoder.getFromLocationName(address, 1)
            if (results?.isNotEmpty() == true) {
                // If results are found, return the latitude and longitude
                Pair(results[0].latitude, results[0].longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Function to update the location of treasures by geocoding their addresses
    fun updateTreasureLocations(onLocationsUpdated: () -> Unit) {
        // Map through the treasure items and geocode their addresses if lat/lng is missing
        tItems = tItems.map { item ->
            val coordinates = geocodeAddress(item.address)
            if (coordinates != null) {
                item.copy(lat = coordinates.first, lng = coordinates.second)
            } else {
                item
            }
        }
        // Invoke the callback function to indicate that locations have been updated
        onLocationsUpdated()
    }

    // Permission launcher for requesting location access
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(), // Specifies the contract for requesting a single permission
        onResult = { isGranted ->
            if (isGranted) {
                // If location permission is granted, get the last known location
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        // If location is found, create an intent to open Google Maps with directions
                        currentTreasureItem?.let { item ->
                            val latitude = location.latitude
                            val longitude = location.longitude
                            val intentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${item.address},Toronto,Canada&origin=$latitude,$longitude")
                            val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)

                            // Simulate that the user reached the destination by setting hasReachedDestination to true
                            item.hasReachedDestination = true
                            // Save the updated list of treasure items to SharedPreferences
                            saveItemsToPreferences(tItems, editor)

                            // Show the treasure found dialog to the user
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

    // Function to handle the event when the user clicks the location icon on a treasure item
    fun handleLocationClick(item: TreasureItem) {
        currentTreasureItem = item // Set the clicked treasure item as the current treasure
        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED // Check if the location permission is already granted

        if (permissionGranted) {
            // If permission is granted, get the last known location and show directions
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val intentUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${item.address},Toronto,Canada&origin=$latitude,$longitude")
                    val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    context.startActivity(mapIntent)

                    // Simulate that the user reached the destination by setting hasReachedDestination to true
                    item.hasReachedDestination = true
                    // Save the updated list of treasure items to SharedPreferences
                    saveItemsToPreferences(tItems, editor)

                    // Show the treasure found dialog to the user
                    showTreasureFoundDialog = true
                } else {
                    Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // If permission is not granted, request it
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    context as Activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show a dialog explaining why the permission is needed
                showPermissionDialog = true
            } else {
                // Directly request the permission
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    if (showMap) {
        // Filter out treasure items that have already been found before displaying the map
        val unfoundItems = tItems.filter { !it.hasReachedDestination }

        // Geocode all addresses before displaying the map
        tItems = unfoundItems.map { item ->
            val coordinates = geocodeAddress(item.address)
            if (coordinates != null) {
                item.copy(lat = coordinates.first, lng = coordinates.second)
            } else {
                item
            }
        }

        // Show the map with unfound treasure locations pinned
        MapScreen(tItems = tItems, onBack = { showMap = false }) // Display the map screen with the treasures
    } else {
        // Composable UI layout for the main screen
        Column(
            modifier = Modifier.fillMaxSize(), // Fill the entire available space
            verticalArrangement = Arrangement.Top, // Align items to the top
            horizontalAlignment = Alignment.CenterHorizontally // Center items horizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceAround, // Space out the buttons evenly
                modifier = Modifier.fillMaxWidth() // Make the row fill the entire width of the screen
            ) {
                // Button to add a new treasure item
                Button(
                    onClick = { showDialog = true } // Show the dialog when clicked
                ) {
                    Text("Add address")
                }
                // Button to open the map with treasure locations
                Button(onClick = {
                    // Update locations and then show the map
                    updateTreasureLocations {
                        showMap = true
                    }
                }) {
                    Text("Open Map")
                }
            }

            // List of treasure items displayed in a LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize() // Fill the remaining space
                    .padding(16.dp) // Add padding around the list
            ) {
                items(tItems) { item ->
                    // For each item in the treasure list, display it using TreasureListItem composable
                    TreasureListItem(
                        item = item,
                        onLocationClick = { handleLocationClick(it) } // Handle location click event
                    )
                }
            }

            // Dialog for adding a new treasure item
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false }, // Dismiss the dialog when needed
                    confirmButton = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth() // Make the row fill the entire width of the screen
                                .padding(8.dp), // Add padding around the buttons
                            horizontalArrangement = Arrangement.SpaceBetween // Space out the buttons evenly
                        ) {
                            // "Add" button to add the treasure to the list
                            Button(onClick = {
                                if (treasureName.isNotBlank() || address.isNotBlank() || treasureDescription.isNotBlank()) {
                                    // Create a new treasure item with the entered details
                                    val newTreasure = TreasureItem(
                                        name = treasureName,
                                        address = address,
                                        description = treasureDescription
                                    )
                                    // Geocode the address to get latitude and longitude
                                    val coordinates = geocodeAddress(address)
                                    // If coordinates are found, update the treasure with them
                                    val updatedTreasure = if (coordinates != null) {
                                        newTreasure.copy(lat = coordinates.first, lng = coordinates.second)
                                    } else {
                                        newTreasure
                                    }
                                    // Add the new treasure to the list and save it to SharedPreferences
                                    tItems = tItems + updatedTreasure
                                    saveItemsToPreferences(tItems, editor)
                                    // Reset the input fields and close the dialog
                                    showDialog = false
                                    treasureName = ""
                                    address = ""
                                    treasureDescription = ""
                                }
                            }) {
                                Text(text = "Add")
                            }
                            // "Cancel" button to close the dialog without adding a treasure
                            Button(onClick = { showDialog = false }) {
                                Text(text = "Cancel")
                            }
                        }

                    },
                    title = { Text("Add a treasure Item") }, // Dialog title
                    text = {
                        // Column containing the input fields for the treasure details
                        Column {
                            // Text field for entering the treasure name
                            OutlinedTextField(
                                value = treasureName,
                                onValueChange = { treasureName = it }, // Update the state when the text changes
                                singleLine = true, // Ensure the input is single-line
                                placeholder = { Text(text = "What is your treasure?") }, // Placeholder text
                                modifier = Modifier
                                    .fillMaxWidth() // Fill the width of the dialog
                                    .padding(8.dp) // Add padding around the text field
                            )
                            // Text field for entering the address
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it }, // Update the state when the text changes
                                singleLine = true, // Ensure the input is single-line
                                placeholder = { Text(text = "What is the location of it?") }, // Placeholder text
                                modifier = Modifier
                                    .fillMaxWidth() // Fill the width of the dialog
                                    .padding(8.dp) // Add padding around the text field
                            )
                            // Text field for entering the description
                            OutlinedTextField(
                                value = treasureDescription,
                                onValueChange = { treasureDescription = it }, // Update the state when the text changes
                                singleLine = true, // Ensure the input is single-line
                                placeholder = { Text(text = "Describe your treasure") }, // Placeholder text
                                modifier = Modifier
                                    .fillMaxWidth() // Fill the width of the dialog
                                    .padding(8.dp) // Add padding around the text field
                            )
                        }

                    }
                )
            }

            // Dialog requesting location permission if needed
            if (showPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showPermissionDialog = false }, // Dismiss the dialog when needed
                    confirmButton = {
                        // Button to request location permission
                        Button(onClick = {
                            // Launch the permission request when the user clicks 'Allow'
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            showPermissionDialog = false // Close the dialog after the request
                        }) {
                            Text("Allow")
                        }
                    },
                    dismissButton = {
                        // Button to deny location permission
                        Button(onClick = { showPermissionDialog = false }) {
                            Text("Deny")
                        }
                    },
                    title = { Text("Permission Needed") }, // Dialog title
                    text = {
                        // Explanation text for why the permission is needed
                        Text("Location permission is required to show your current location on the map.")
                    }
                )
            }

            // Dialog showing that the treasure is found
            if (showTreasureFoundDialog) {
                AlertDialog(
                    onDismissRequest = { showTreasureFoundDialog = false }, // Dismiss the dialog when needed
                    confirmButton = {
                        // Button to acknowledge the found treasure
                        Button(onClick = {
                            showTreasureFoundDialog = false // Close the dialog
                        }) {
                            Text("OK")
                        }
                    },
                    title = { Text("Treasure Found!") }, // Dialog title
                    text = {
                        // Congratulatory text for finding the treasure
                        Text("Congratulations! You have found the treasure.")
                    }
                )
            }
        }
    }
}

// Composable function displaying the map with treasure markers
@Composable
fun MapScreen(tItems: List<TreasureItem>, onBack: () -> Unit) {
    // Create a CameraPositionState to control the camera position on the map
    val cameraPositionState = rememberCameraPositionState {
        // Set initial camera position over Toronto
        position = CameraPosition.fromLatLngZoom(LatLng(43.651070, -79.347015), 12f)
    }

    // State variable to track the currently selected treasure
    var selectedTreasure by remember { mutableStateOf<TreasureItem?>(null) }

    // Box composable to contain the map and additional UI elements
    Box(modifier = Modifier.fillMaxSize()) {
        // Display the Google Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(), // Fill the available space
            cameraPositionState = cameraPositionState, // Use the defined camera position state
            onMapClick = { selectedTreasure = null } // Deselect the treasure when the map is clicked
        ) {
            // Loop through each treasure item and add a marker on the map
            tItems.forEach { treasure ->
                Marker(
                    state = rememberMarkerState(position = LatLng(treasure.lat, treasure.lng)), // Set the marker position
                    title = treasure.name, // Set the marker title
                    snippet = treasure.address, // Set the marker snippet (short description)
                    onClick = {
                        // Set the selected treasure when a marker is clicked
                        selectedTreasure = treasure
                        true
                    }
                )
            }
        }

        // Display the custom info window if a treasure is selected
        selectedTreasure?.let { treasure ->
            // Construct the URL for the street view image of the location
            val imageUrl = "https://maps.googleapis.com/maps/api/streetview?size=400x400&location=${treasure.lat},${treasure.lng}&key=YOUR_API_KEY"
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter) // Align the info window at the top center of the map
                    .padding(16.dp) // Add padding around the info window
            ) {
                // Display the custom info window content
                CustomInfoWindowContent(
                    title = treasure.name,
                    address = treasure.address,
                    description = treasure.description,
                    imageUrl = imageUrl
                )
            }
        }

        // Back button to return to the list of treasures
        Button(
            onClick = { onBack() }, // Go back to the treasure list when clicked
            modifier = Modifier
                .align(Alignment.BottomCenter) // Align the button at the bottom center of the map
                .padding(16.dp) // Add padding around the button
        ) {
            Text("Back to List")
        }
    }
}

// Composable function displaying the custom info window content
@Composable
fun CustomInfoWindowContent(
    title: String,      // Title of the treasure
    address: String,    // Address of the treasure
    description: String, // Description of the treasure
    imageUrl: String,   // URL of the street view image
    modifier: Modifier = Modifier // Modifier to apply to the info window
) {
    // Row to arrange the image and text side by side
    Row(
        modifier = modifier
            .background(Color.White) // Set the background color to white
            .border(1.dp, Color.Gray) // Add a gray border around the info window
            .padding(8.dp) // Add padding around the contents
    ) {
        // Load and display the image using Coil library
        Image(
            painter = rememberAsyncImagePainter(imageUrl), // Load the image from the URL
            contentDescription = "Street View Image", // Content description for accessibility
            modifier = Modifier
                .size(100.dp) // Set the image size
                .clip(RoundedCornerShape(4.dp)), // Clip the image corners to make them rounded
            contentScale = ContentScale.Crop // Crop the image to fill the available space
        )
        // Column to arrange the text content vertically
        Column(
            modifier = Modifier.padding(start = 8.dp) // Add padding to the start of the column
        ) {
            // Display the title in bold
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            // Display the address
            Text(
                text = address,
                style = MaterialTheme.typography.bodyMedium
            )
            // Display the description in a lighter font
            Text(
                text = description,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Light)
            )
        }
    }
}

// Composable function displaying an individual treasure item in the list
@Composable
fun TreasureListItem(
    item: TreasureItem, // The treasure item to display
    onLocationClick: (TreasureItem) -> Unit // Function to call when the location icon is clicked
) {
    // Row to arrange the treasure name and icons horizontally
    Row(
        modifier = Modifier
            .padding(8.dp) // Add padding around the row
            .fillMaxWidth() // Make the row fill the entire width of the screen
            .border(
                border = BorderStroke(2.dp, Color(0XFF018786)), // Add a border around the row
                shape = RoundedCornerShape(20) // Round the corners of the border
            ),
        horizontalArrangement = Arrangement.SpaceBetween, // Space out the contents evenly
        verticalAlignment = Alignment.CenterVertically // Align the contents vertically in the center
    ) {
        // Column to display the treasure name and its sub-items
        Column(modifier = Modifier.padding(8.dp)) {
            // Display the name of the treasure
            Text(text = item.name, modifier = Modifier.padding(bottom = 4.dp))

            // Display each sub-item in the treasure item, if any
            item.subItems.forEach { subItem ->
                Text(text = subItem, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
            }
        }

        // Row to display the location and done icons
        Row(modifier = Modifier.padding(8.dp)) {
            // If the destination has not been reached, display the location icon
            if (!item.hasReachedDestination) {
                IconButton(onClick = { onLocationClick(item) }) { // Handle location click event
                    Icon(imageVector = Icons.Default.Place, contentDescription = null)
                }
            }
            // If the destination has been reached, display the done icon
            if (item.hasReachedDestination) {
                IconButton(onClick = { /* Handle done action */ }) {
                    Icon(imageVector = Icons.Default.Done, contentDescription = null)
                }
            }
        }
    }
}

// Function to save the treasure items to SharedPreferences
fun saveItemsToPreferences(items: List<TreasureItem>, editor: SharedPreferences.Editor) {
    // Convert each item to a string and save it as a Set in SharedPreferences
    val itemsSet = items.map {
        val subItemsString = it.subItems.joinToString("|") // Convert sub-items to a single string
        "${it.name}:${it.address}:${it.description}:${it.lat}:${it.lng}:${subItemsString}:${it.hasReachedDestination}"
    }.toSet()
    // Store the string set in SharedPreferences
    editor.putStringSet("treasure_items", itemsSet).apply()
}

// Function to load the treasure items from SharedPreferences
fun loadItemsFromPreferences(preferences: SharedPreferences): List<TreasureItem> {
    // Retrieve the stored string set from SharedPreferences
    val itemsSet = preferences.getStringSet("treasure_items", emptySet()) ?: emptySet()
    // Parse each stored string to recreate the TreasureItem object
    return itemsSet.mapNotNull {
        val parts = it.split(":") // Split the string into parts
        if (parts.size >= 7) { // Ensure there are enough parts to reconstruct the object
            val subItems = parts[5].split("|").filter { it.isNotBlank() } // Ensure subItems are non-empty
            TreasureItem(
                name = parts[0], // Name of the treasure
                address = parts[1], // Address of the treasure
                description = parts[2], // Description of the treasure
                lat = parts[3].toDoubleOrNull() ?: 0.0,  // Convert latitude to double, default to 0.0 if conversion fails
                lng = parts[4].toDoubleOrNull() ?: 0.0,  // Convert longitude to double, default to 0.0 if conversion fails
                subItems = subItems, // List of sub-items
                hasReachedDestination = parts[6].toBoolean() // Whether the destination has been reached
            )
        } else {
            null // Return null if the string cannot be parsed properly
        }
    }
}
