package com.crimeafuel.app.domain.model

enum class FuelType(val displayName: String, val shortName: String) {
    AI_92("АИ-92", "92"),
    AI_95("АИ-95", "95"),
    AI_95_PLUS("АИ-95+", "95+"),
    AI_100("АИ-100", "100"),
    DT("Дизельное топливо", "ДТ"),
    DT_PLUS("ДТ Премиум", "ДТ+"),
    GAS("Газ", "Газ");

    companion object {
        val GASOLINE = listOf(AI_92, AI_95, AI_95_PLUS, AI_100)
        val DIESEL = listOf(DT, DT_PLUS)
        val GAS_LIST = listOf(GAS)
    }
}
