package com.crimeafuel.app.domain.model

enum class PaymentMethod(val displayName: String) {
    CARDS("Талоны / Топл. карты"),
    CASHLESS("Безнал"),
    CASH("Наличка");
}
