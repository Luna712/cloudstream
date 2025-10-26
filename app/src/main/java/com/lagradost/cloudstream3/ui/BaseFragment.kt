package com.lagradost.cloudstream3.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.lagradost.cloudstream3.ui.settings.Globals.EMULATOR
import com.lagradost.cloudstream3.ui.settings.Globals.TV
import com.lagradost.cloudstream3.ui.settings.Globals.isLandscape
import com.lagradost.cloudstream3.ui.settings.Globals.isLayout
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding

abstract class BaseFragment<T : ViewBinding>(
	private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> T
) : Fragment() {

	protected var _binding: T? = null
	protected val binding: T? get() = _binding

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View? {
		_binding = bindingInflater(inflater, container, false)
		return _binding?.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
        binding?.let {
			fixSystemBarsPadding(
				it.root,
				padBottom = isLandscape(),
				padLeft = isLayout(TV or EMULATOR)
			)
		}
		binding?.let { onBindingCreated(it, savedInstanceState) }
	}

	/** Called when binding has been safely created and view is ready. */
    protected open fun onBindingCreated(binding: T, savedInstanceState: Bundle?) {}

	override fun onConfigurationChanged(newConfig: Configuration) {
        binding?.let { ViewCompat.requestApplyInsets(it.root) }
		super.onConfigurationChanged(newConfig)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}
}