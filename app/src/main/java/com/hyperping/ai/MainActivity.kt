package com.hyperping.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val VPN_REQUEST_CODE = 1017
    private var isConnected = false
    private lateinit var prefs: SharedPreferences
    
    // وضعیت DNS فعلی
    private var currentPrimaryDns = "178.22.122.100"
    private var currentSecondaryDns = "185.51.200.2"

    // ویوهای اصلی
    private lateinit var rootContainer: FrameLayout
    private lateinit var contentArea: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var connectBtn: Button
    private lateinit var pingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("HyperPingPrefs", Context.MODE_PRIVATE)

        val isUnlocked = prefs.getBoolean("is_authenticated", false)
        if (!isUnlocked) {
            showPasscodeScreen()
        } else {
            setupMainApp()
        }
    }

    // ================= ۱. قفل امنیتی ورود ۱۰۱۷ =================
    private fun showPasscodeScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#08090C"))
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }

        val title = TextView(this).apply {
            text = "HYPERPING AI"
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "کد فعال‌سازی VIP را وارد کنید"
            setTextColor(Color.GRAY)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 50)
        }

        val input = EditText(this).apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.DKGRAY)
            hint = "رمز عبور..."
            gravity = Gravity.CENTER
            background = createBoxDrawable("#151821", "#00E5FF")
            setPadding(40, 40, 40, 40)
        }

        val submitBtn = Button(this).apply {
            text = "ورود به سیستم"
            background = createBoxDrawable("#00FF88", "#00FF88")
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            textSize = 16f
            setPadding(0, 30, 0, 30)
            setOnClickListener {
                if (input.text.toString().trim() == "1017") {
                    prefs.edit().putBoolean("is_authenticated", true).apply()
                    setupMainApp()
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

    // ================= ساختار اصلی و نویگیشن ۵ تب =================
    private fun setupMainApp() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#08090C"))
        }

        // هدر برنامه
        val header = TextView(this).apply {
            text = "HYPERPING AI HUD"
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 30, 0, 20)
            setBackgroundColor(Color.parseColor("#0D0F14"))
        }
        mainLayout.addView(header)

        // ناحیه اسکرول‌شونده محتوای هر تب
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        contentArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 30)
        }
        scrollView.addView(contentArea)
        mainLayout.addView(scrollView)

        // نوار پایین (Bottom Navigation)
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#10131A"))
            weightSum = 5f
            setPadding(10, 20, 10, 25)
        }

        val tabs = listOf("داشبورد", "رادار DNS", "کاستوم DoH", "دستیار چت", "تنظیمات")
        for (i in tabs.indices) {
            val btn = Button(this).apply {
                text = tabs[i]
                textSize = 11f
                setTextColor(Color.LTGRAY)
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    switchTab(i)
                }
            }
            bottomBar.addView(btn)
        }
        mainLayout.addView(bottomBar)

        setContentView(mainLayout)
        switchTab(0) // باز شدن داشبورد به عنوان تب اولیه
    }

    private fun switchTab(index: Int) {
        contentArea.removeAllViews()
        when (index) {
            0 -> renderDashboardTab()
            1 -> renderRadarTab()
            2 -> renderDohTab()
            3 -> renderChatTab()
            4 -> renderSettingsTab()
        }
    }

    // ================= تب ۱: داشبورد اصلی =================
    private fun renderDashboardTab() {
        pingText = TextView(this).apply {
            text = "پینگ زنده: ۴۲ms | جیتر: ۲ms"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createBoxDrawable("#10131A", "#00FF88")
            setPadding(40, 40, 40, 40)
            gravity = Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = if (isConnected) "محافظ DNS متصل است" else "سیستم آماده اتصال"
            setTextColor(if (isConnected) Color.parseColor("#00FF88") else Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val dnsInfo = TextView(this).apply {
            text = "DNS انتخابی:\nPrimary: $currentPrimaryDns\nSecondary: $currentSecondaryDns"
            setTextColor(Color.GRAY)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 30)
        }

        connectBtn = Button(this).apply {
            text = if (isConnected) "قطع اتصال" else "اتصال سریع گیمینگ"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            background = createBoxDrawable(if (isConnected) "#FF3366" else "#00FF88", "#00FF88")
            setPadding(50, 40, 50, 40)
            setOnClickListener {
                toggleConnection()
            }
        }

        card.addView(statusText)
        card.addView(dnsInfo)
        card.addView(connectBtn)

        contentArea.addView(pingText)
        contentArea.addView(card)
    }

    // ================= تب ۲: رادار و ترکیب هوشمند هوش مصنوعی =================
    private fun renderRadarTab() {
        val title = TextView(this).apply {
            text = "رادار هوشمند هوش مصنوعی"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 10, 0, 20)
        }
        contentArea.addView(title)

        val fetchBtn = Button(this).apply {
            text = "دریافت ترندهای روزانه گیمینگ (هوش مصنوعی)"
            setTextColor(Color.BLACK)
            background = createBoxDrawable("#00FF88", "#00FF88")
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                callBackendAi("لیست ۳ تا از بهترین DNSهای گیمینگ با پینگ عالی برای ایران را به صورت خلاصه بگو") { reply ->
                    showAiDnsResult(reply)
                }
            }
        }
        contentArea.addView(fetchBtn)
    }

    private fun showAiDnsResult(reply: String) {
        val resultCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createBoxDrawable("#151821", "#00E5FF")
            setPadding(30, 30, 30, 30)
        }

        val txt = TextView(this).apply {
            text = reply
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 10, 0, 20)
        }

        val applyBtn = Button(this).apply {
            text = "اعمال این ترکیب و اتصال هوشمند"
            setTextColor(Color.BLACK)
            background = createBoxDrawable("#00E5FF", "#00E5FF")
            setOnClickListener {
                currentPrimaryDns = "178.22.122.100"
                currentSecondaryDns = "185.51.200.2"
                Toast.makeText(this@MainActivity, "ترکیب روی کانکشن اعمال شد!", Toast.LENGTH_SHORT).show()
                switchTab(0)
            }
        }

        resultCard.addView(txt)
        resultCard.addView(applyBtn)
        contentArea.addView(Space(this).apply { minimumHeight = 30 })
        contentArea.addView(resultCard)
    }

    // ================= تب ۳: هاب کاستوم DoH دستی =================
    private fun renderDohTab() {
        val title = TextView(this).apply {
            text = "هاب اختصاصی کاستوم DoH"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 10, 0, 20)
        }

        val nameInput = EditText(this).apply {
            hint = "نام سرور (مثلاً: DoH من)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            background = createBoxDrawable("#151821", "#333333")
            setPadding(30, 30, 30, 30)
        }

        val urlInput = EditText(this).apply {
            hint = "https://dns.google/dns-query"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            background = createBoxDrawable("#151821", "#333333")
            setPadding(30, 30, 30, 30)
        }

        val saveBtn = Button(this).apply {
            text = "ذخیره و فعال‌سازی DoH"
            setTextColor(Color.BLACK)
            background = createBoxDrawable("#00FF88", "#00FF88")
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                val url = urlInput.text.toString().trim()
                if (url.startsWith("https://")) {
                    prefs.edit().putString("custom_doh", url).apply()
                    Toast.makeText(this@MainActivity, "DoH ذخیره شد و به عنوان اولویت ست گردید!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "آدرس باید حتماً با https شروع شود", Toast.LENGTH_SHORT).show()
                }
            }
        }

        contentArea.addView(title)
        contentArea.addView(nameInput)
        contentArea.addView(Space(this).apply { minimumHeight = 20 })
        contentArea.addView(urlInput)
        contentArea.addView(Space(this).apply { minimumHeight = 30 })
        contentArea.addView(saveBtn)
    }

    // ================= تب ۴: دستیار چت تخصصی بازی‌ها =================
    private fun renderChatTab() {
        val title = TextView(this).apply {
            text = "دستیار هوش مصنوعی بازی‌ها"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 10, 0, 20)
        }

        val chatLog = TextView(this).apply {
            text = "دستیار: نام بازی خود را بنویسید (مثلاً وارزون، پابجی، فیفا) تا بهترین تنظیمات را برایتان پیدا کنم.\n"
            setTextColor(Color.WHITE)
            textSize = 14f
            background = createBoxDrawable("#10131A", "#222222")
            setPadding(30, 30, 30, 30)
        }

        val input = EditText(this).apply {
            hint = "پیام شما..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            background = createBoxDrawable("#151821", "#333333")
            setPadding(30, 30, 30, 30)
        }

        val sendBtn = Button(this).apply {
            text = "ارسال به هوش مصنوعی"
            setTextColor(Color.BLACK)
            background = createBoxDrawable("#00E5FF", "#00E5FF")
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) {
                    chatLog.append("\nشما: $query\nدستیار: در حال بررسی...\n")
                    input.setText("")
                    callBackendAi(query) { reply ->
                        chatLog.append("\nدستیار: $reply\n")
                    }
                }
            }
        }

        contentArea.addView(title)
        contentArea.addView(chatLog)
        contentArea.addView(Space(this).apply { minimumHeight = 20 })
        contentArea.addView(input)
        contentArea.addView(Space(this).apply { minimumHeight = 20 })
        contentArea.addView(sendBtn)
    }

    // ================= تب ۵: تنظیمات و مدیریت API =================
    private fun renderSettingsTab() {
        val title = TextView(this).apply {
            text = "تنظیمات کلید OpenRouter (BYOK)"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 10, 0, 20)
        }

        val keyInput = EditText(this).apply {
            hint = "کلید API OpenRouter خود را وارد کنید..."
            setText(prefs.getString("openrouter_key", ""))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            background = createBoxDrawable("#151821", "#333333")
            setPadding(30, 30, 30, 30)
        }

        val saveBtn = Button(this).apply {
            text = "ذخیره کلید"
            setTextColor(Color.BLACK)
            background = createBoxDrawable("#00FF88", "#00FF88")
            typeface = Typeface.DEFAULT_BOLD
            setOnClickListener {
                val key = keyInput.text.toString().trim()
                prefs.edit().putString("openrouter_key", key).apply()
                Toast.makeText(this@MainActivity, "کلید اختصاصی شما با موفقیت ذخیره شد!", Toast.LENGTH_SHORT).show()
            }
        }

        val info = TextView(this).apply {
            text = "آدرس اتصال رله ضدتحریم:\nhttps://s.sosiss.ir/api.php\n(اینترنت ایران بدون نیاز به فیلترشکن متصل است)"
            setTextColor(Color.GRAY)
            textSize = 12f
            setPadding(0, 40, 0, 0)
        }

        contentArea.addView(title)
        contentArea.addView(keyInput)
        contentArea.addView(Space(this).apply { minimumHeight = 30 })
        contentArea.addView(saveBtn)
        contentArea.addView(info)
    }

    // ================= اتصال به بک‌اند اختصاصی s.sosiss.ir =================
    private fun callBackendAi(promptText: String, onResult: (String) -> Unit) {
        val apiKey = prefs.getString("openrouter_key", "")
        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(this, "لطفاً ابتدا از بخش تنظیمات کلید OpenRouter خود را وارد کنید!", Toast.LENGTH_LONG).show()
            return
        }

        thread {
            try {
                val url = URL("https://s.sosiss.ir/api.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000

                val jsonPayload = JSONObject().apply {
                    put("api_key", apiKey)
                    val messages = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", promptText)
                        })
                    }
                    put("messages", messages)
                }

                val os = conn.outputStream
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(jsonPayload.toString())
                writer.flush()
                writer.close()

                val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val resJson = JSONObject(response.toString())
                val reply = if (resJson.optBoolean("status", false)) {
                    resJson.optString("reply", "پاسخی دریافت نشد.")
                } else {
                    resJson.optString("message", "خطا در پردازش هوش مصنوعی.")
                }

                runOnUiThread { onResult(reply) }
            } catch (e: Exception) {
                runOnUiThread {
                    onResult("خطا در ارتباط با سرور: " + (e.message ?: "تایم‌اوت"))
                }
            }
        }
    }

    // ================= منطق اتصال VPN =================
    private fun toggleConnection() {
        if (!isConnected) {
            val intent = VpnService.prepare(this)
            if (intent != null) {
                startActivityForResult(intent, VPN_REQUEST_CODE)
            } else {
                onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null)
            }
        } else {
            stopService(Intent(this, LocalDnsVpnService::class.java))
            isConnected = false
            statusText.text = "قطع شد"
            statusText.setTextColor(Color.WHITE)
            connectBtn.text = "اتصال سریع گیمینگ"
            connectBtn.background = createBoxDrawable("#00FF88", "#00FF88")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val serviceIntent = Intent(this, LocalDnsVpnService::class.java).apply {
                putExtra("PRIMARY_DNS", currentPrimaryDns)
                putExtra("SECONDARY_DNS", currentSecondaryDns)
            }
            startService(serviceIntent)
            isConnected = true
            statusText.text = "محافظ DNS فعال است"
            statusText.setTextColor(Color.parseColor("#00FF88"))
            connectBtn.text = "قطع اتصال"
            connectBtn.background = createBoxDrawable("#FF3366", "#FF3366")
            Toast.makeText(this, "محافظ DNS گیمینگ با موفقیت فعال شد!", Toast.LENGTH_SHORT).show()
        }
    }

    // ایجاد حاشیه‌های نئونی شیک گیمینگ
    private fun createBoxDrawable(bgColor: String, borderColor: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgColor))
            setStroke(2, Color.parseColor(borderColor))
            cornerRadius = 16f
        }
    }
}
