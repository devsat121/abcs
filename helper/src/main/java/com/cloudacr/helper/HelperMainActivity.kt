package com.cloudacr.helper

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
// FIX #1 & #2: Import helper's own R class.
// Original code had "import com.cloudacr.app.R" — the main app's R class.
// The helper module has NO compile dependency on the app module, so that import
// causes "Unresolved reference: R" at compile time.
import com.cloudacr.helper.R
import com.cloudacr.helper.databinding.ActivityHelperMainBinding

class HelperMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelperMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelperMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEnableAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.btnOpenMainApp.setOnClickListener {
            packageManager.getLaunchIntentForPackage("com.cloudacr.app")
                ?.let { startActivity(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val enabled = isAccessibilityServiceEnabled()
        val running = ACRAccessibilityService.isRunning

        when {
            enabled && running -> {
                binding.statusIcon.setImageResource(R.drawable.ic_check)
                binding.statusTitle.text = "Helper is Active"
                binding.statusSubtitle.text = "Accessibility service running · Main app connected"
                binding.statusCard.setCardBackgroundColor(getColor(R.color.status_ok))
                binding.btnEnableAccessibility.visibility = View.GONE
            }
            enabled -> {
                binding.statusIcon.setImageResource(R.drawable.ic_check)
                binding.statusTitle.text = "Service Enabled"
                binding.statusSubtitle.text = "Accessibility enabled · Waiting for main app"
                binding.statusCard.setCardBackgroundColor(getColor(R.color.status_warn))
                binding.btnEnableAccessibility.visibility = View.GONE
            }
            else -> {
                binding.statusIcon.setImageResource(R.drawable.ic_warning)
                binding.statusTitle.text = "Setup Required"
                binding.statusSubtitle.text = "Enable the accessibility service to allow the helper to work"
                binding.statusCard.setCardBackgroundColor(getColor(R.color.status_error))
                binding.btnEnableAccessibility.visibility = View.VISIBLE
            }
        }

        val mainInstalled = try {
            packageManager.getPackageInfo("com.cloudacr.app", 0); true
        } catch (e: Exception) { false }

        binding.mainAppStatus.text = if (mainInstalled)
            "✓ Cloud ACR main app installed"
        else
            "✗ Cloud ACR main app not installed"
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "${packageName}/${ACRAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return TextUtils.SimpleStringSplitter(':').apply {
            setString(enabledServices)
        }.any { it.equals(service, ignoreCase = true) }
    }
}
