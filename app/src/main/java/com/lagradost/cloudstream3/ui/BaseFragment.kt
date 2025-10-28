package com.lagradost.cloudstream3.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.txt
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import java.lang.reflect.Method

abstract class BaseFragment<T : ViewBinding>(
    private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> T,
    @LayoutRes private val baseLayout: Int? = null,
    @LayoutRes private val tvLayout: Int? = null
) : Fragment() {

    private var _binding: T? = null
    protected val binding: T? get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = pickLayout()
        if (layoutId != null) {
            val root = inflater.inflate(layoutId, container, false)
            _binding = try {
                @Suppress("UNCHECKED_CAST")
                getBindFunction()?.invoke(null, root) as? T
            } catch (t: Throwable) {
                showToast(
                    txt(R.string.unable_to_inflate, t.message ?: ""),
                    Toast.LENGTH_LONG
                )
                logError(t)
                null
            }
            return root
        }

        _binding = bindingInflater(inflater, container, false)
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fixPadding(view)
        binding?.let { onBindingCreated(it, savedInstanceState) }
    }

    /** Called when binding has been safely created and view is ready. */
    protected open fun onBindingCreated(binding: T, savedInstanceState: Bundle?) {}

    override fun onConfigurationChanged(newConfig: Configuration) {
        fixPadding(binding?.root)
        super.onConfigurationChanged(newConfig)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun pickLayout(): Int? = when {
        isLayout(TV or EMULATOR) && tvLayout != null -> tvLayout
        else -> baseLayout
    }

    private fun fixPadding(view: View?) {
        fixSystemBarsPadding(
            view,
            padBottom = isLandscape(),
            padLeft = isLayout(TV or EMULATOR)
        )
    }

    private fun getBindFunction(): Method? {
        val clazz = bindingInflater::class.java.enclosingClass ?: return null
        return try {
            clazz.getMethod("bind", View::class.java)
        } catch (_: Exception) {
            null
        }
    }
}