package com.kevy.ledger.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kevy.ledger.R
import com.kevy.ledger.databinding.ActivityMainBinding
import com.kevy.ledger.ui.common.Refreshable

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.setOnItemSelectedListener {
            switchFragment(it.itemId)
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.navigation_home
        }
    }

    override fun onResume() {
        super.onResume()
        (supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? Refreshable)?.refreshContent()
    }

    private fun switchFragment(itemId: Int) {
        val fragment = when (itemId) {
            R.id.navigation_records -> RecordsFragment()
            R.id.navigation_stats -> StatsFragment()
            R.id.navigation_settings -> SettingsFragment()
            else -> HomeFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
