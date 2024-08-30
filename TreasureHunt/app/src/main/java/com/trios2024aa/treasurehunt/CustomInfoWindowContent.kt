package com.trios2024aa.treasurehunt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CustomInfoWindowContent(
    title: String,
    address: String
) {
    Row(
        modifier = Modifier
            .background(Color.White)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Favorite, // Use a star icon as the favorite icon
            contentDescription = null,
            modifier = Modifier
                .height(50.dp) // Large size icon
                .padding(end = 8.dp) // Space between the icon and the text
        )

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = address,
                style = MaterialTheme.typography.displayMedium
            )
        }
    }
}