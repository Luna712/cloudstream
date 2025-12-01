package com.lagradost.cloudstream3

import android.content.Context

object GitInfo {
	fun hash(context: Context): String {
		return try {
			context.assets.open("git-hash.txt")
				.bufferedReader()
				.readText()
				.trim()
		} catch (_: Exception) {
			"unknown"
		}
	}
}
