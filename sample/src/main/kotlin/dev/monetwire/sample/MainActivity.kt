package dev.monetwire.sample

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import dev.monetwire.MonetWire

class MainActivity : Activity() {

    private lateinit var banner: ViewGroup
    private lateinit var consentStatus: TextView
    private lateinit var actionCount: TextView
    private var actions = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        banner = findViewById(R.id.banner)
        consentStatus = findViewById(R.id.consent_status)
        actionCount = findViewById(R.id.action_count)
        keepClearOfSystemBars(findViewById(R.id.root))

        findViewById<Button>(R.id.next_item).setOnClickListener {
            actions++
            MonetWire.userAction()
            showCount()
        }
        findViewById<Button>(R.id.take_break).setOnClickListener {
            MonetWire.showInterstitialAtBreak(this) {
                Toast.makeText(this, R.string.break_done, Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.privacy_options).setOnClickListener {
            MonetWire.showPrivacyOptions(this) { onConsentChanged() }
        }
        findViewById<Button>(R.id.reset_consent).setOnClickListener {
            MonetWire.resetConsent()
            Toast.makeText(this, R.string.reset_done, Toast.LENGTH_LONG).show()
        }

        showCount()
        // Every launch, before any ad. On a returning user's phone this is quick and silent.
        MonetWire.gatherConsent(this) { onConsentChanged() }
    }

    override fun onDestroy() {
        MonetWire.releaseBanner(banner)
        super.onDestroy()
    }

    private fun onConsentChanged() {
        consentStatus.setText(if (MonetWire.canShowAds) R.string.consent_ok else R.string.consent_no)
        findViewById<View>(R.id.privacy_options).visibility =
            if (MonetWire.privacyOptionsRequired) View.VISIBLE else View.GONE
        MonetWire.showBanner(this, banner)
    }

    private fun showCount() {
        actionCount.text = getString(R.string.actions, actions)
    }

    /**
     * Android 16 draws every app edge to edge. Without this the banner sits under the gesture bar
     * and the text under the status bar.
     */
    private fun keepClearOfSystemBars(root: View) {
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                view.setPadding(
                    insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom,
                )
            }
            insets
        }
    }
}
