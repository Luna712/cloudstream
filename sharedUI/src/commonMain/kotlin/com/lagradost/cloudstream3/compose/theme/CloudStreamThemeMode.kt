package com.lagradost.cloudstream3.shared.ui.theme

enum class CloudStreamThemeMode {
    /** "Black" standard dark, #111111 backgrounds */
    Dark,
    /** "Amoled" / "AmoledLight" pure black (#000000) */
    Amoled,
    /** "Light" white/gray backgrounds, dark text */
    Light,
    /** "Dracula" */
    Dracula,
    /** "Lavender" */
    Lavender,
    /** "SilentBlue" */
    SilentBlue,
    /** "System" resolved on each platform via [isSystemInDarkTheme] */
    FollowSystem,
}
