package com.lagradost.cloudstream3.ui

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceFragmentCompat
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.settings.SettingsFragment.Companion.setSystemBarsPadding
import com.lagradost.cloudstream3.utils.txt
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding

/**
 * A base Fragment class that simplifies ViewBinding usage and handles view inflation safely.
 *
 * This class allows two modes of creating ViewBinding:
 * 1. Inflate: Using the standard `inflate()` method provided by generated ViewBinding classes.
 * 2. Bind: Using `bind()` on an existing root view.
 *
 * It also provides hooks for:
 * - Safe initialization of the binding (`onBindingCreated`)
 * - Automatic padding adjustment for system bars (`fixPadding`)
 * - Optional layout resource selection via `pickLayout()`
 *
 * @param T The type of ViewBinding for this Fragment.
 * @param bindingCreator The strategy used to create the binding instance.
 */
private interface BaseFragmentHelper<T : ViewBinding> {
    val bindingCreator: BaseFragment.BindingCreator<T>

    var _binding: T?
    val binding: T? get() = _binding

	companion object {
        const val TAG = "BaseFragment"
	}

    fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Try to reuse a cached root view first
        val cachedRoot = BaseFragmentPool.acquire(javaClass.name)

        val layoutId = pickLayout()
        val root: View? = cachedRoot ?: layoutId?.let {
            inflater.inflate(it, container, false)
        }

        _binding = try {
            when (val creator = bindingCreator) {
                is BaseFragment.BindingCreator.Inflate -> {
                    if (cachedRoot == null) creator.fn(inflater, container, false)
                    else creator.fn(LayoutInflater.from(root?.context), container, false).also {
                        Log.d(TAG, "Binding recreated from cached root for ${javaClass.name}")
                    }
                }
                is BaseFragment.BindingCreator.Bind -> {
                    if (root != null) creator.fn(root)
                    else throw IllegalStateException("Root view is null for bind()")
                }
            }
        } catch (t: Throwable) {
            showToast(
                txt(R.string.unable_to_inflate, t.message ?: ""),
                Toast.LENGTH_LONG
            )
            logError(t)
            null
        }

        return _binding?.root ?: root
    }

    /**
     * Called after the fragment's view has been created.
     *
     * This method is `final` to ensure that the binding is properly initialized and
     * system bar padding adjustments are applied before any subclass logic runs.
     * Subclasses should use [onBindingCreated] instead of overriding this method directly.
     */
    fun onViewReady(view: View, savedInstanceState: Bundle?) {
        fixPadding(view)
        binding?.let { onBindingCreated(it, savedInstanceState) }
    }

    /**
     * Called when the binding is safely created and view is ready.
     * Can be overridden to provide fragment-specific initialization.
     *
     * @param binding The safely created ViewBinding.
     * @param savedInstanceState Saved state bundle or null.
     */
    fun onBindingCreated(binding: T, savedInstanceState: Bundle?) {
        onBindingCreated(binding)
    }

    /**
     * Called when the binding is safely created and view is ready.
     * Overload without savedInstanceState for convenience.
     *
     * @param binding The safely created ViewBinding.
     */
    fun onBindingCreated(binding: T) {}

    /**
     * Pick a layout resource ID for the fragment.
     *
     * Return `null` by default. Override to provide a layout resource when using
     * `BindingCreator.Bind`. Not needed if using `BindingCreator.Inflate`.
     *
     * @return Layout resource ID or null.
     */
    @LayoutRes
    fun pickLayout(): Int? = null

    /**
     * Apply padding adjustments for system bars to the root view.
     *
     * `fixPadding` should be idempotent in respect to the configuration, as the function may be
     * called many times on the same view in case of configuration change like device orientation.
     *
     * @param view The root view to adjust.
     */
    fun fixPadding(view: View) {
        fixSystemBarsPadding(view)
    }

    /** Called by fragments when they’re destroyed, so the binding can be recycled. */
    fun recycleBindingOnDestroy() {
        _binding?.root?.let {
            BaseFragmentPool.release(javaClass.name, it)
            Log.d(TAG, "Root view released to pool for ${javaClass.name}")
        }
        _binding = null
    }
}

/**
 * A global pool for reusing [ViewBinding] instances across fragments to reduce
 * layout inflation overhead and improve navigation performance.
 *
 * This pool is intended for use with fragments that extend [BaseFragment],
 * [BaseDialogFragment], or [BaseBottomSheetDialogFragment] which support
 * recycling of their bindings.
 */

object BaseFragmentPool {
    private val pool = mutableMapOf<String, MutableList<View>>()

    /** Attempts to acquire a recycled root view from the pool. */
    fun acquire(key: String): View? {
        val list = pool[key] ?: return null
        val view = list.removeLastOrNull() ?: return null
        (view.parent as? ViewGroup)?.removeView(view)
        if (list.isEmpty()) pool.remove(key)
        return view
    }

    /** Releases a root view back to the pool for later reuse. */
    fun release(key: String, view: View) {
        (view.parent as? ViewGroup)?.removeView(view)
        pool.getOrPut(key) { mutableListOf() }.add(view)
    }

    /** Clears all cached views. */
    fun clearAll() {
        pool.values.flatten().forEach { (it.parent as? ViewGroup)?.removeView(it) }
        pool.clear()
    }
}

abstract class BaseFragment<T : ViewBinding>(
    override val bindingCreator: BindingCreator<T>
) : Fragment(), BaseFragmentHelper<T> {
    override var _binding: T? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = createBinding(inflater, container, savedInstanceState)

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewReady(view, savedInstanceState)
    }

    /**
     * Called when the device configuration changes (e.g., orientation).
     * Re-applies system bar padding fixes to the root view to ensure it
     * readjusts for orientation changes.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        view?.let { fixPadding(it) }
    }

    /** Cleans up the binding reference when the view is destroyed to avoid memory leaks. */
    override fun onDestroyView() {
        super.onDestroyView()
        recycleBindingOnDestroy()
    }

    /**
     * Sealed class representing the two strategies for creating a ViewBinding instance.
     */
    sealed class BindingCreator<T : ViewBinding> {

        /**
         * Use the standard inflate() method for creating the binding.
         *
         * @param fn Lambda that inflates the binding.
         */
        class Inflate<T : ViewBinding>(
            val fn: (LayoutInflater, ViewGroup?, Boolean) -> T
        ) : BindingCreator<T>()

        /**
         * Use bind() on an existing root view to create the binding. This should
         * be used if you are differing per device layouts, such as different
         * layouts for TV and Phone.
         *
         * @param fn Lambda that binds the root view.
         */
        class Bind<T : ViewBinding>(
            val fn: (View) -> T
        ) : BindingCreator<T>()
    }
}

abstract class BaseDialogFragment<T : ViewBinding>(
    override val bindingCreator: BaseFragment.BindingCreator<T>
) : DialogFragment(), BaseFragmentHelper<T> {
    override var _binding: T? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = createBinding(inflater, container, savedInstanceState)

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewReady(view, savedInstanceState)
    }

    /** @see [BaseFragment.onConfigurationChanged] for documentation. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        view?.let { fixPadding(it) }
    }

    /** Cleans up the binding reference when the view is destroyed to avoid memory leaks. */
    override fun onDestroyView() {
        super.onDestroyView()
        recycleBindingOnDestroy()
    }
}

abstract class BaseBottomSheetDialogFragment<T : ViewBinding>(
    override val bindingCreator: BaseFragment.BindingCreator<T>
) : BottomSheetDialogFragment(), BaseFragmentHelper<T> {
    override var _binding: T? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = createBinding(inflater, container, savedInstanceState)

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewReady(view, savedInstanceState)
    }

    /** @see [BaseFragment.onConfigurationChanged] for documentation. */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        view?.let { fixPadding(it) }
    }

    /** Cleans up the binding reference when the view is destroyed to avoid memory leaks. */
    override fun onDestroyView() {
        super.onDestroyView()
        recycleBindingOnDestroy()
    }
}

abstract class BasePreferenceFragmentCompat(): PreferenceFragmentCompat() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSystemBarsPadding()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setSystemBarsPadding()
    }
}
