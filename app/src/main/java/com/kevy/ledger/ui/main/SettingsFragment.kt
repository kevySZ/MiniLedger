package com.kevy.ledger.ui.main

import android.app.AlertDialog
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
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.title_about)
                .setMessage(R.string.about_content)
                .setPositiveButton(android.R.string.ok, null)
                .show()
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
    }
}
