// Part of the floating bottom bar from BakaSU (https://github.com/Baka-SU/BakaSU), GPL-3.0.
// BakaSU adapted it from the compose-miuix-ui example (https://github.com/compose-miuix-ui/miuix, Apache-2.0),
// which is based on Kyant0/AndroidLiquidGlass (https://github.com/Kyant0/AndroidLiquidGlass, Apache-2.0).
// Modified for RePixIV (package names and theme hooks only).


package com.repixiv.app.ui.bakasu.liquid

import top.yukonga.miuix.kmp.blur.BackdropEffectScope
import top.yukonga.miuix.kmp.blur.colorControls

fun BackdropEffectScope.vibrancy() {
    colorControls(
        brightness = 0f,
        contrast = 1f,
        saturation = 1.5f,
    )
}
