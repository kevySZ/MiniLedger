package com.kevy.ledger.ui.main

import android.os.Bundle
import android.widget.Toast
import com.kevy.ledger.R
import com.kevy.ledger.databinding.ActivityAboutBinding
import com.kevy.ledger.ui.common.ThemedActivity

class AboutActivity : ThemedActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.title = getString(R.string.title_about)

        binding.textVersionValue.text = buildVersionText()
        binding.textUpdateNotesValue.text = getString(R.string.about_update_notes_content)
        binding.textAboutContent.text = getString(R.string.about_content)
        binding.buttonCheckUpdate.setOnClickListener {
            Toast.makeText(this, R.string.about_update_mock_hint, Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildVersionText(): String {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val versionName = packageInfo.versionName ?: "1.0"
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
        return getString(R.string.about_version_value, versionName, versionCode)
    }
}
