package com.lagradost.cloudstream3

/**
 * Deprecated alias for CloudStreamApp for backwards compatibility with plugins.
 * Use CloudStreamApp instead.
 */
// Deprecate after next stable
/*
@Deprecated(
    message = "Use CloudStreamApp instead",
    replaceWith = ReplaceWith("com.lagradost.cloudstream3.CloudStreamApp"),
    level = DeprecationLevel.WARNING
)*/
class AcraApplication {
	companion object {
		val context get() = CloudStreamApp.context

		fun removeKeys(folder: String): Int? =
			CloudStreamApp.removeKeys(folder)

		fun <T> setKey(path: String, value: T) =
			CloudStreamApp.setKey(path, value)

		fun <T> setKey(folder: String, path: String, value: T) =
			CloudStreamApp.setKey(folder, path, value)

		inline fun <reified T : Any> getKey(path: String, defVal: T?): T? =
			CloudStreamApp.getKey(path, defVal)

		inline fun <reified T : Any> getKey(path: String): T? =
			CloudStreamApp.getKey(path)

		inline fun <reified T : Any> getKey(folder: String, path: String): T? =
			CloudStreamApp.getKey(folder, path)

		inline fun <reified T : Any> getKey(folder: String, path: String, defVal: T?): T? =
			CloudStreamApp.getKey(folder, path, defVal)

		fun getKeys(folder: String): List<String>? =
			CloudStreamApp.getKeys(folder)

		fun removeKey(folder: String, path: String) =
			CloudStreamApp.removeKey(folder, path)

		fun removeKey(path: String) =
			CloudStreamApp.removeKey(path)
	}
}
