package com.casty.music.backend.ui.utils

fun String.resize(width: Int, height: Int): String =
    if (contains("=w") || contains("-w")) this else "$this=w$width-h$height"
