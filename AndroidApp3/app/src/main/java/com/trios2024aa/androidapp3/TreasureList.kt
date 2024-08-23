package com.trios2024aa.androidapp3

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

// Data class representing a treasure item
data class TreasureItem(
    var name: String,
    var address: String,
    var subItems: List<String> = emptyList(), // Array of sub-items
    var isDone: Boolean = false, // Tracks if the user has marked the task as done
    var hasReachedDestination: Boolean = false // Tracks if the user has reached the destination
)

@Composable
fun TreasureList() {
    val context = LocalContext.current
    val preferences = context.getSharedPreferences("treasure_prefs", Context.MODE_PRIVATE)
    val editor = preferences.edit()

    // Predefined list of treasures
    val predefinedItems = listOf(
        TreasureItem(name = "Case of money", address = "300 George street"),
        TreasureItem(name = "Diamond", address = "101 Charles Street east"),
        TreasureItem(name = "Necklace", address = "279 Jarvis street"),
        TreasureItem(name = "Chest of Gold", address = "Allan's Garden"),
        TreasureItem(name = "Key from the bank deposit", address = "250 Dundas street")
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
    var treasureName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var currentTreasureItem by remember { mutableStateOf<TreasureItem?>(null) }
    var showTreasureFoundDialog by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

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

    // Composable UI layout
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Button to add a new treasure item
        Button(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Add address")
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
                            if (treasureName.isNotBlank() || address.isNotBlank()) {
                                val newTreasure = TreasureItem(
                                    name = treasureName,
                                    address = address
                                )
                                tItems = tItems + newTreasure
                                saveItemsToPreferences(tItems, editor)
                                showDialog = false
                                treasureName = ""
                                address = ""
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

// Function to save the list of treasure items to SharedPreferences
fun saveItemsToPreferences(items: List<TreasureItem>, editor: SharedPreferences.Editor) {
    val itemsSet = items.map {
        val subItemsString = it.subItems.joinToString("|") // Convert sub-items to a single string
        "${it.name}:${it.address}:${subItemsString}:${it.hasReachedDestination}"
    }.toSet()
    editor.putStringSet("treasure_items", itemsSet).apply()
}

// Function to load the list of treasure items from SharedPreferences
fun loadItemsFromPreferences(preferences: SharedPreferences): List<TreasureItem> {
    val itemsSet = preferences.getStringSet("treasure_items", emptySet()) ?: emptySet()
    return itemsSet.map {
        val parts = it.split(":")
        val subItems = parts[2].split("|") // Convert the string back to a list of sub-items
        TreasureItem(
            name = parts[0],
            address = parts[1],
            subItems = subItems,
            hasReachedDestination = parts[3].toBoolean()
        )
    }
}
