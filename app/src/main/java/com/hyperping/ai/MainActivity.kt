package com.hyperping.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    private val VPN_REQUEST_CODE = 1017
    private var isConnected = false
    private lateinit var connectBtn: Button
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("HyperPingPrefs", Context.MODE_PRIVATE)
        val isUnlocked = sharedPref.getBoolean("is_authenticated", false)

        if (!isUnlocked) {
            showPasscodeScreen(sharedPref)
        } else {
            showDashboardScreen()
        }
    }

    private fun showPasscodeScreen(sharedPref: android.content.SharedPreferences) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#08090C"))
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }

        val title = TextView(this).apply {
            text = "HYPERPING AI"
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "کد فعال‌سازی VIP را وارد کنید"
            setTextColor(Color.GRAY)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 40)
        }

        val input = EditText(this).apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.DKGRAY)
            hint = "رمز عبور..."
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#151821"))
            setPadding(30, 30, 30, 30)
        }

        val submitBtn = Button(this).apply {
            text = "ورود به سیستم"
            setBackgroundColor(Color.parseColor("#00FF88"))
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                if (input.text.toString().trim() == "1017") {
                    sharedPref.edit().putBoolean("is_authenticated", true).apply()
                    showDashboardScreen()
                } else {
                    Toast.makeText(this@MainActivity, "دسترسی غیرمجاز! کد امنیتی اشتباه است.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(input)
        layout.addView(Space(this).apply { minimumHeight = 40 })
        layout.addView(submitBtn)

        setContentView(layout)
    }

    private fun showDashboardScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#08090C"))
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }

        statusText = TextView(this).apply {
            text = "سیستم آماده اتصال"
            setTextColor(Color.WHITE)
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 60)
        }

        connectBtn = Button(this).apply {
            text = "اتصال سریع"
            setBackgroundColor(Color.parseColor("#00FF88"))
            setTextColor(Color.BLACK)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                if (!isConnected) {
                    val intent = VpnService.prepare(this@MainActivity)
                    if (intent != null) {
                        startActivityForResult(intent, VPN_REQUEST_CODE)
                    } else {
                        onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null)
                    }
                } else {
                    stopService(Intent(this@MainActivity, LocalDnsVpnService::class.java))
                    isConnected = false
                    text = "اتصال سریع"
                    setBackgroundColor(Color.parseColor("#00FF88"))
                    statusText.text = "قطع شد"
                    statusText.setTextColor(Color.WHITE)
                }
            }
        }

        layout.addView(statusText)
        layout.addView(connectBtn)

        setContentView(layout)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val serviceIntent = Intent(this, LocalDnsVpnService::class.java)
            startService(serviceIntent)
            isConnected = true
            connectBtn.text = "قطع اتصال"
            connectBtn.setBackgroundColor(Color.parseColor("#FF3366"))
            statusText.text = "محافظ DNS فعال است"
            statusText.setTextColor(Color.parseColor("#00FF88"))
            Toast.makeText(this, "محافظ DNS گیمینگ فعال شد!", Toast.LENGTH_SHORT).show()
        }
    }
}
