package com.lagradost.cloudstream4.ui.theme

enum class CloudStreamThemeMode {
    /** "Black" standard dark, #111111 backgrounds */
    Dark,
    /** "Amoled" pure black (#000000) */
    Amoled,
    /** "AmoledLight" pure black (#000000) bg, lighter (#121213) surfaces */
    AmoledLight,
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
    /**
     * Uses platform dynamic color system, Material You on Android 12+,
     * falls back to [Dark] on unsupported platforms.
     */
    Dynamic,
}
