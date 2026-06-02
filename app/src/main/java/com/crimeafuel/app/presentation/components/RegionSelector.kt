package com.crimeafuel.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crimeafuel.app.domain.model.Region

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionSelector(
    selectedRegions: List<Region>,
    onRegionToggle: (Region) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val displayText = when {
        selectedRegions.isEmpty() -> "Все регионы"
        selectedRegions.size == 1 -> selectedRegions[0].displayName
        else -> "${selectedRegions.size} регионов"
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Region.entries.forEach { region ->
                val isSelected = region in selectedRegions
                DropdownMenuItem(
                    text = {
                        Row {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(region.displayName)
                        }
                    },
                    onClick = { onRegionToggle(region) }
                )
            }
        }
    }
}
