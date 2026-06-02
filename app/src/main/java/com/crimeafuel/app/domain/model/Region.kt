package com.crimeafuel.app.domain.model

enum class Region(val displayName: String) {
    SIMFEROPOL("г. Симферополь"),
    SIMFEROPOL_GO("г.о. Симферополь"),
    SIMFEROPOL_DISTRICT("Симферопольский район"),
    SEVASTOPOL("г. Севастополь"),
    YALTA("г. Ялта"),
    YALTA_GO("г.о. Ялта"),
    ALUSHTA("г. Алушта"),
    ALUSHTA_GO("г.о. Алушта"),
    EVPATORIA("г. Евпатория"),
    KERCH("г. Керчь"),
    FEODOSIA("г. Феодосия"),
    DZHANKOY("г. Джанкой"),
    BAKHCHISARAY("г. Бахчисарай"),
    BAKHCHISARAY_DISTRICT("Бахчисарайский район"),
    BELOGORSK_DISTRICT("Белогорский район"),
    KIROVSKY_DISTRICT("Кировский район"),
    KRASNOGVARDEYSKY_DISTRICT("Красногвардейский район"),
    LENINSKY_DISTRICT("Ленинский район"),
    NIZHNEGORSKY_DISTRICT("Нижнегорский район"),
    RAZDOLNENSKY_DISTRICT("Раздольненский район"),
    SAKSKY_DISTRICT("Сакский район"),
    SAKI("г. Саки"),
    CHERNOMORSKY_DISTRICT("Черноморский район"),
    ARMYANSK("г. Армянск"),
    KRASNOPEREKOPSK("г. Красноперекопск"),
    SUDAK("г. Судак"),
    MASSANDRA("пгт. Массандра"),
    GENICHESKY_DISTRICT("Гениченский район"),
    SALKOVO("с. Сальково");

    companion object {
        fun fromDisplayName(name: String): Region {
            return entries.find {
                it.displayName.equals(name.trim(), ignoreCase = true)
            } ?: run {
                // Try partial match
                val normalized = name.trim().lowercase()
                entries.find { normalized.contains(it.displayName.lowercase()) }
                    ?: entries.find { it.displayName.lowercase().contains(normalized) }
                    ?: SIMFEROPOL // fallback
            }
        }
    }
}
