package com.raj.kotlinmusic.ui.theme

import androidx.compose.ui.graphics.Color

data class ColorPalette(
    val id: String,
    val name: String,
    val background: Color,
    val surface: Color,
    val text: Color,
    val mutedText: Color,
    val accent1: Color,
    val accent2: Color,
    val accent3: Color,
    val accent4: Color
)

object ThemePalettes {
    val RetroTech = ColorPalette(
        id = "retro_tech",
        name = "Retro Tech",
        background = Color(0xFF161616),
        surface = Color(0xFF2C2C2C),
        text = Color(0xFFFFFFFF),
        mutedText = Color(0xFF9E9E9E),
        accent1 = Color(0xFFEE6557), // Coral
        accent2 = Color(0xFFD3E382), // Lime
        accent3 = Color(0xFFF5C754), // Yellow
        accent4 = Color(0xFFFF8A65)  // Light Coral
    )

    val MatchaMilk = ColorPalette(
        id = "matcha_milk",
        name = "Matcha Milk",
        background = Color(0xFFF5F5EE),
        surface = Color(0xFFECEDE4),
        text = Color(0xFF1E2218),
        mutedText = Color(0xFF7D8570),
        accent1 = Color(0xFF4A7C59), // Forest
        accent2 = Color(0xFFA3C77A), // Sage
        accent3 = Color(0xFFD4A85A), // Honey
        accent4 = Color(0xFF8FA8B5)  // Slate Blue
    )

    val VolcanicAsh = ColorPalette(
        id = "volcanic_ash",
        name = "Volcanic Ash",
        background = Color(0xFF1A1410),
        surface = Color(0xFF2B2420),
        text = Color(0xFFF0EAE4),
        mutedText = Color(0xFFA09088),
        accent1 = Color(0xFFE8622A), // Ember
        accent2 = Color(0xFFF0BF60), // Gold
        accent3 = Color(0xFFC44F6A), // Magma Pink
        accent4 = Color(0xFF7AB8A0)  // Seafoam
    )

    val MidnightJazz = ColorPalette(
        id = "midnight_jazz",
        name = "Midnight Jazz",
        background = Color(0xFF0C1220),
        surface = Color(0xFF16213A),
        text = Color(0xFFF4EDD8),
        mutedText = Color(0xFF8A9AB5),
        accent1 = Color(0xFFC8A050), // Brass
        accent2 = Color(0xFF5B9BD5), // Steel Blue
        accent3 = Color(0xFFD4727A), // Rouge
        accent4 = Color(0xFF68C8A0)  // Mint
    )

    val PolarBloom = ColorPalette(
        id = "polar_bloom",
        name = "Polar Bloom",
        background = Color(0xFFF0F4F8),
        surface = Color(0xFFE3EBF2),
        text = Color(0xFF0E1C2E),
        mutedText = Color(0xFF6E8AA0),
        accent1 = Color(0xFF2A6FA8), // Arctic Blue
        accent2 = Color(0xFF5CC8B8), // Glacier
        accent3 = Color(0xFFE8855A), // Coral Berry
        accent4 = Color(0xFFB47DC4)  // Lavender
    )

    val TravelAgency = ColorPalette(
        id = "travel_agency",
        name = "Travel Agency",
        background = Color(0xFF12212E),
        surface = Color(0xFF223647),
        text = Color(0xFFFFFFFF),
        mutedText = Color(0xFFECE7DC),
        accent1 = Color(0xFFEA9940), // Orange
        accent2 = Color(0xFF307082), // Teal
        accent3 = Color(0xFF6CA3A2), // Light Teal
        accent4 = Color(0xFFECE7DC)  // Sand/Cream
    )

    val Honeydew = ColorPalette(
        id = "honeydew",
        name = "Honeydew",
        background = Color(0xFF282900),
        surface = Color(0xFF3B3C08),
        text = Color(0xFFE6F5E2),
        mutedText = Color(0xFFA4E3A4),
        accent1 = Color(0xFF337738), // Fern
        accent2 = Color(0xFFB4D44D), // Yellow Green
        accent3 = Color(0xFFA4E3A4), // Celadon
        accent4 = Color(0xFFE6F5E2)  // Honeydew
    )

    val AzureMist = ColorPalette(
        id = "azure_mist",
        name = "Azure Mist",
        background = Color(0xFF001B29),
        surface = Color(0xFF132F40),
        text = Color(0xFFE2F5F3),
        mutedText = Color(0xFFA4CBE3),
        accent1 = Color(0xFF4DD4CD), // Strong Cyan
        accent2 = Color(0xFF333C77), // Twilight Indigo
        accent3 = Color(0xFFA4CBE3), // Icy Blue
        accent4 = Color(0xFFE2F5F3)  // Azure Mist
    )

    val DarkJungleGreen = ColorPalette(
        id = "dark_jungle_green",
        name = "Dark Jungle Green",
        background = Color(0xFF1E201F),
        surface = Color(0xFF2C302F),
        text = Color(0xFFFFFFFF),
        mutedText = Color(0xFF5AA371),
        accent1 = Color(0xFF1D6C61), // Deep Turquoise
        accent2 = Color(0xFF3EB9A8), // Verdigris
        accent3 = Color(0xFF5AA371), // Forest Green
        accent4 = Color(0xFF193A31)  // Medium Jungle Green
    )

    val LiquidGlass = ColorPalette(
        id = "liquid_glass",
        name = "Liquid Glass",
        background = Color(0xFF0F0500),
        surface = Color(0x26FFFFFF), // Translucent White for glass effect
        text = Color(0xFFFFFFFF),
        mutedText = Color(0xFFE5E5EA),
        accent1 = Color(0xFFFFB300), // Sunset Gold
        accent2 = Color(0xFFFF4500), // Sunset Red/Orange
        accent3 = Color(0xFFFF8F00), // Amber Glow
        accent4 = Color(0xFFFFFFFF)  // Silver Light
    )

    val WhiteCandy = ColorPalette(
        id = "white_candy",
        name = "White Candy",
        background = Color(0xFFF3F4F6),
        surface = Color.White,
        text = Color(0xFF1E293B),
        mutedText = Color(0xFF64748B),
        accent1 = Color(0xFFFFA0D4),
        accent2 = Color(0xFFFFF080),
        accent3 = Color(0xFFFCF9F2),
        accent4 = Color(0xFFE2E8F0)
    )

    val NightSpice = ColorPalette(
        id = "night_spice",
        name = "Night Spice",
        background = Color(0xFF000000), // AMOLED Black
        surface = Color(0xFF212121), // Grey Bezels/Belt
        text = Color(0xFFF8FAFC),
        mutedText = Color(0xFF94A3B8),
        accent1 = Color(0xFFFF4500), // Fiery Orange/Red
        accent2 = Color(0xFFFF9800), // Saffron
        accent3 = Color(0xFF3A1F28), // Deep Maroon/Wine
        accent4 = Color(0xFF26141D)
    )

    val Caesar = ColorPalette(
        id = "caesar",
        name = "Caesar",
        background = Color(0xFF000000), // Black
        surface = Color(0xFF111111), // Very dark surface
        text = Color(0xFFFFFFFF), // White
        mutedText = Color(0xFFAAAAAA),
        accent1 = Color(0xFF6D001A), // Burgundy
        accent2 = Color(0xFF8B0021), // Lighter Burgundy
        accent3 = Color(0xFFFFFFFF), // White accent
        accent4 = Color(0xFF222222)
    )

    val list = listOf(
        RetroTech,
        MatchaMilk,
        VolcanicAsh,
        MidnightJazz,
        PolarBloom,
        TravelAgency,
        Honeydew,
        AzureMist,
        DarkJungleGreen,
        LiquidGlass,
        WhiteCandy,
        NightSpice,
        Caesar
    )
    
    fun getById(id: String): ColorPalette {
        return list.find { it.id == id } ?: WhiteCandy
    }
}
