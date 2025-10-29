package com.lagradost.cloudstream3.ui.setup

import android.widget.AbsListView
import android.widget.ArrayAdapter
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentSetupLayoutBinding
import com.lagradost.cloudstream3.mvvm.safe
import com.lagradost.cloudstream3.ui.BaseFragment

class SetupFragmentLayout : BaseFragment<FragmentSetupLayoutBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentSetupLayoutBinding::inflate)
) {
    override fun onBindingCreated(binding: FragmentSetupLayoutBinding) {
        safe {
            val ctx = context ?: return@safe

            val settingsManager = PreferenceManager.getDefaultSharedPreferences(ctx)

            val prefNames = resources.getStringArray(R.array.app_layout)
            val prefValues = resources.getIntArray(R.array.app_layout_values)

            val currentLayout =
                settingsManager.getInt(getString(R.string.app_layout_key), -1)

            val arrayAdapter =
                ArrayAdapter<String>(ctx, R.layout.sort_bottom_single_choice)

            arrayAdapter.addAll(prefNames.toList())
            binding.apply {
                listview1.adapter = arrayAdapter
                listview1.choiceMode = AbsListView.CHOICE_MODE_SINGLE
                listview1.setItemChecked(
                    prefValues.indexOf(currentLayout), true
                )

                listview1.setOnItemClickListener { _, _, position, _ ->
                    settingsManager.edit()
                        .putInt(getString(R.string.app_layout_key), prefValues[position])
                        .apply()
                    activity?.recreate()
                }

                nextBtt.setOnClickListener {
                    setKey(HAS_DONE_SETUP_KEY, true)
                    findNavController().navigate(R.id.navigation_home)
                }

                prevBtt.setOnClickListener {
                    findNavController().popBackStack()
                }
            }
        }
    }
}
