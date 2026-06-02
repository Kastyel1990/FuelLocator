package com.crimeafuel.app.presentation.filter

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.crimeafuel.app.domain.model.FilterState
import com.crimeafuel.app.domain.model.PaymentMethod
import com.crimeafuel.app.domain.model.Region
import com.crimeafuel.app.presentation.components.PaymentMethodChip
import com.crimeafuel.app.presentation.components.RegionSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    filterState: FilterState,
    onToggleRegion: (Region) -> Unit,
    onTogglePayment: (PaymentMethod) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Фильтры",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Region selector
            Text(
                text = "Район / Город",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            RegionSelector(
                selectedRegions = filterState.regions,
                onRegionToggle = onToggleRegion
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Payment method chips
            Text(
                text = "Вид отпуска топлива",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Allow wrapping of chips if there are many
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PaymentMethod.entries.forEach { method ->
                    PaymentMethodChip(
                        paymentMethod = method,
                        isSelected = method in filterState.paymentMethods,
                        onClick = { onTogglePayment(method) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!filterState.isDefault) {
                Button(
                    onClick = {
                        onClearFilters()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Сбросить фильтры")
                }
            }
        }
    }
}
