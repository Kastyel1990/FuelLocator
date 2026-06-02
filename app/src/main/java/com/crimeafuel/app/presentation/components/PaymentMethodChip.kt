@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.crimeafuel.app.presentation.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.crimeafuel.app.domain.model.PaymentMethod

@Composable
fun PaymentMethodChip(
    paymentMethod: PaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (paymentMethod) {
        PaymentMethod.CARDS -> "Талоны/Карты"
        PaymentMethod.CASHLESS -> "Безнал"
        PaymentMethod.CASH -> "Наличка"
    }

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        modifier = modifier,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    )
}
