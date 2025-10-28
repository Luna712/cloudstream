package com.lagradost.cloudstream3.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.viewbinding.ViewBinding
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.txt
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * A high-performance Fragment base class that provides:
 *  - Safe and automatic ViewBinding creation
 *  - Intelligent view recycling and caching across lifecycles
 *  - Seamless compatibility with Navigation Component and ViewPager
 *  - Optional layout inflation fallback via [pickLayout]
 *
 * This implementation minimizes unnecessary inflations while still
 * ensuring fragments can be garbage collected when memory is tight.
 */
abstract class BaseFragment<T : ViewBinding>(
	private val bindingCreator: BindingCreator<T>
) : Fragment() {

	private var _binding: T? = null
	protected val binding: T? get() = _binding

	private var recycledRoot: View? = null
	private var isFullyDestroyed = false

	companion object {
		/** Global binding cache (weak references to prevent memory leaks). */
		private val bindingCache = ConcurrentHashMap<String, WeakReference<ViewBinding>>()
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		val key = cacheKey()

		// Try cached binding first (if still valid)
		val cached = bindingCache[key]?.get() as? T
		if (cached != null && cached.root.parent == null) {
			_binding = cached
			recycledRoot = cached.root
			return recycledRoot
		}

		// Otherwise, inflate normally
		val layoutId = pickLayout()
		val root: View? = layoutId?.let { inflater.inflate(it, container, false) }

		_binding = try {
			when (bindingCreator) {
				is BindingCreator.Inflate -> bindingCreator.fn(inflater, container, false)
				is BindingCreator.Bind -> {
					if (root != null) bindingCreator.fn(root)
					else throw IllegalStateException("Root view is null for bind()")
				}
			}
		} catch (t: Throwable) {
			showToast(txt(R.string.unable_to_inflate, t.message ?: ""), Toast.LENGTH_LONG)
			logError(t)
			null
		}

		recycledRoot = _binding?.root ?: root
		_binding?.let { bindingCache[key] = WeakReference(it) }
		return recycledRoot
	}

	final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		fixPadding(view)
		binding?.let { onBindingCreated(it, savedInstanceState) }

		// Lifecycle watcher for cleanup
		viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_DESTROY) {
				isFullyDestroyed = shouldClearCache()
				if (isFullyDestroyed) {
					clearBindingCache()
				}
			}
		})
	}

	protected open fun onBindingCreated(binding: T, savedInstanceState: Bundle?) {
		onBindingCreated(binding)
	}

	protected open fun onBindingCreated(binding: T) {}

	override fun onConfigurationChanged(newConfig: Configuration) {
		binding?.apply { fixPadding(root) }
		super.onConfigurationChanged(newConfig)
	}

	/** Clears binding reference and removes it from global cache. */
	protected fun clearBindingCache() {
		_binding = null
		recycledRoot = null
		bindingCache.remove(cacheKey())
		isFullyDestroyed = false
	}

	override fun onDestroyView() {
		super.onDestroyView()
		// Keep binding cached; do not nullify immediately.
	}

	/**
	 * Determines whether the cache should be cleared.
	 * Default behavior:
	 *  - Clears cache when fragment is removed or activity is finishing
	 *  - Retains cache otherwise (e.g., ViewPager/tab fragments)
	 */
	protected open fun shouldClearCache(): Boolean {
		return isRemoving || activity?.isFinishing == true || !isAdded
	}

	/** Generates a unique cache key for this fragment type. */
	protected open fun cacheKey(): String = javaClass.name

	@LayoutRes
	protected open fun pickLayout(): Int? = null

	protected open fun fixPadding(view: View) {
		fixSystemBarsPadding(view)
	}

	sealed class BindingCreator<T : ViewBinding> {
		class Inflate<T : ViewBinding>(
			val fn: (LayoutInflater, ViewGroup?, Boolean) -> T
		) : BindingCreator<T>()

		class Bind<T : ViewBinding>(
			val fn: (View) -> T
		) : BindingCreator<T>()
	}
}
