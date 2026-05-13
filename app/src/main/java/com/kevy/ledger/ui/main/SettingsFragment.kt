package com.kevy.ledger.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.kevy.ledger.R
import com.kevy.ledger.app.AppGraph
import com.kevy.ledger.databinding.FragmentSettingsBinding
import com.kevy.ledger.ui.account.AccountManagerActivity
import com.kevy.ledger.ui.backup.BackupRestoreActivity
import com.kevy.ledger.ui.book.BookManagerActivity
import com.kevy.ledger.ui.category.CategoryManagerActivity
import com.kevy.ledger.ui.common.AppThemeManager
import com.kevy.ledger.ui.common.AppThemeMode
import com.kevy.ledger.ui.common.Refreshable

class SettingsFragment : Fragment(R.layout.fragment_settings), Refreshable {
    private var binding: FragmentSettingsBinding? = null
    private val repository get() = AppGraph.repository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSettingsBinding.bind(view)

        binding?.buttonBooks?.setOnClickListener {
            startActivity(Intent(requireContext(), BookManagerActivity::class.java))
        }
        binding?.buttonAccounts?.setOnClickListener {
            startActivity(Intent(requireContext(), AccountManagerActivity::class.java))
        }
        binding?.buttonCategories?.setOnClickListener {
            startActivity(Intent(requireContext(), CategoryManagerActivity::class.java))
        }
        binding?.buttonBackup?.setOnClickListener {
            startActivity(Intent(requireContext(), BackupRestoreActivity::class.java))
        }
        binding?.buttonAbout?.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
        binding?.cardThemeWarm?.setOnClickListener {
            applyThemeMode(AppThemeMode.WARM)
        }
        binding?.cardThemeBarbie?.setOnClickListener {
            applyThemeMode(AppThemeMode.BARBIE)
        }

        refreshContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun refreshContent() {
        val bookName = repository.getCurrentBook()?.name ?: getString(R.string.empty_books)
        binding?.textCurrentBook?.text = getString(R.string.settings_current_book, bookName)
        refreshThemeSection()
    }

    private fun applyThemeMode(mode: AppThemeMode) {
        if (AppThemeManager.currentMode(requireContext()) == mode) return
        AppThemeManager.setMode(requireContext(), mode)
        requireActivity().recreate()
    }

    private fun refreshThemeSection() {
        val currentMode = AppThemeManager.currentMode(requireContext())
        val currentThemeName = if (currentMode == AppThemeMode.WARM) {
            getString(R.string.settings_theme_warm)
        } else {
            getString(R.string.settings_theme_barbie)
        }

        binding?.textThemeCurrent?.text = getString(R.string.settings_theme_current, currentThemeName)
        binding?.cardThemeWarm?.isChecked = currentMode == AppThemeMode.WARM
        binding?.cardThemeBarbie?.isChecked = currentMode == AppThemeMode.BARBIE
        binding?.cardThemeWarm?.strokeWidth =
            resources.getDimensionPixelSize(if (currentMode == AppThemeMode.WARM) R.dimen.theme_card_selected_stroke else R.dimen.theme_card_normal_stroke)
        binding?.cardThemeBarbie?.strokeWidth =
            resources.getDimensionPixelSize(if (currentMode == AppThemeMode.BARBIE) R.dimen.theme_card_selected_stroke else R.dimen.theme_card_normal_stroke)
    }
}
