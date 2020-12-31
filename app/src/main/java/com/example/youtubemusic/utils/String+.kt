package com.example.youtubemusic.utils

fun String.toFileName() = this.replace("/", "_")
    .replace("\"", "\'")
    .replace("?", "_")
    .replace(":", "_")
    .replace("*", "_")
    .replace("<", "_")
    .replace(">", "_")
    .replace("|", "_")