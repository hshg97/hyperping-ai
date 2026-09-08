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

    private var currentPrimaryDns = "178.22.122.100"
    private var currentSecondaryDns = "185.51.200.2"

    private lateinit var contentArea: LinearLayout
    private lateinit var navButtons: ArrayList<Button>
    private var selectedTab = 0

    // المان‌های زنده داشبورد
    private lateinit var hudRing: LinearLayout
    private lateinit var hudStateText: TextView
    private lateinit var hudSubState: TextView
    private lateinit var connectActionBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("HyperPingPrefs", Context.MODE_PRIVATE)

        // ورود مستقیم و بدون رمز عبور به داشبورد برنامه
        setupMainInterface()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun setupMainInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#05070A"))
        }

        // هدر سایبرپانکی بالا
        val topBar = RelativeLayout(this).apply {
            setBackgroundColor(Color.parseColor("#090C12"))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }

        val brandLogo = TextView(this).apply {
            text = "⚡ HYPERPING // AI"
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
        }

        val liveTag = TextView(this).apply {
            text = "SYSTEM READY ●"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            val p = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            p.addRule(RelativeLayout.ALIGN_PARENT_END)
            layoutParams = p
        }
        topBar.addView(brandLogo)
        topBar.addView(liveTag)
        root.addView(topBar)

        // محفظه محتوا با اسکرول نرم
        val scroller = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        contentArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        scroller.addView(contentArea)
        root.addView(scroller)

        // نوار ۵ تب شناور در پایین
        val bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0A0D14"))
            setPadding(dp(6), dp(8), dp(6), dp(12))
            weightSum = 5f
        }

        val tabNames = listOf("داشبورد", "رادار DNS", "DoH پروکسی", "چت هوشمند", "تنظیمات")
        navButtons = ArrayList()

        for (i in tabNames.indices) {
            val b = Button(this).apply {
                text = tabNames[i]
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(8), 0, dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { switchTab(i) }
            }
            navButtons.add(b)
            bottomNav.addView(b)
        }
        root.addView(bottomNav)

        setContentView(root)
        switchTab(0)
    }

    private fun switchTab(index: Int) {
        selectedTab = index
        for (i in navButtons.indices) {
            if (i == index) {
                navButtons[i].setTextColor(Color.parseColor("#00FF88"))
                navButtons[i].background = createCardDrawable("#142328", "#00FF88", dp(10), 1)
            } else {
                navButtons[i].setTextColor(Color.parseColor("#5A6A85"))
                navButtons[i].setBackgroundColor(Color.TRANSPARENT)
            }
        }
        contentArea.removeAllViews()
        when (index) {
            0 -> renderDashboard()
            1 -> renderRadar()
            2 -> renderDoh()
            3 -> renderChat()
            4 -> renderSettings()
        }
    }

    // ================= ۱. داشبورد HUD حرفه‌ای =================
    private fun renderDashboard() {
        // ردیف اطلاعات سرعت و جیتر
        val metrics = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            setPadding(0, 0, 0, dp(18))
        }
        metrics.addView(createMetricChip("PING", "38 ms", "#00FF88", 1f))
        metrics.addView(createMetricChip("JITTER", "1.4 ms", "#00E5FF", 1f))
        metrics.addView(createMetricChip("LOSS", "0.0%", "#BF5AF2", 1f))
        contentArea.addView(metrics)

        // کارت بزرگ دایره‌ای HUD
        val mainCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#0E121B", "#1C2436", dp(22), 2)
            setPadding(dp(20), dp(30), dp(20), dp(30))
            gravity = Gravity.CENTER
        }

        hudRing = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val s = dp(175)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { gravity = Gravity.CENTER }
            background = createCardDrawable(
                if (isConnected) "#0D2B20" else "#131722",
                if (isConnected) "#00FF88" else "#252F45",
                dp(100),
                3
            )
            gravity = Gravity.CENTER
        }

        hudStateText = TextView(this).apply {
            text = if (isConnected) "ONLINE" else "STANDBY"
            setTextColor(if (isConnected) Color.parseColor("#00FF88") else Color.parseColor("#5A6A85"))
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        hudSubState = TextView(this).apply {
            text = if (isConnected) "محافظ فعال است" else "آماده اتصال بهینه"
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }

        hudRing.addView(hudStateText)
        hudRing.addView(hudSubState)
        mainCard.addView(hudRing)

        // مشخصات دی‌ان‌اس
        val dnsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#080A0F", "#182030", dp(12), 1)
            setPadding(dp(15), dp(10), dp(15), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(22), 0, dp(22))
            }
        }
        val t1 = TextView(this).apply {
            text = "ACTIVE RESOLVERS"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }
        val t2 = TextView(this).apply {
            text = "IPv4: $currentPrimaryDns  |  $currentSecondaryDns"
            setTextColor(Color.parseColor("#8E9DAE"))
            textSize = 12f
            setPadding(0, dp(3), 0, 0)
        }
        dnsCard.addView(t1)
        dnsCard.addView(t2)
        mainCard.addView(dnsCard)

        // دکمه اتصال بزرگ
        connectActionBtn = Button(this).apply {
            text = if (isConnected) "قطع ارتباط" else "⚡ اتصال سریع گیمینگ"
            setTextColor(Color.parseColor("#05070A"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = createCardDrawable(
                if (isConnected) "#FF3366" else "#00FF88",
                if (isConnected) "#FF3366" else "#00FF88",
                dp(14), 0
            )
            setPadding(0, dp(15), 0, dp(15))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener { toggleVpn() }
        }
        mainCard.addView(connectActionBtn)

        contentArea.addView(mainCard)
    }

    private fun createMetricChip(lbl: String, valStr: String, col: String, w: Float): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w).apply {
                setMargins(dp(3), 0, dp(3), 0)
            }
            background = createCardDrawable("#0E121B", "#1C2436", dp(12), 1)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            gravity = Gravity.CENTER

            val l = TextView(this@MainActivity).apply {
                text = lbl
                setTextColor(Color.parseColor("#5A6A85"))
                textSize = 9.5f
                typeface = Typeface.DEFAULT_BOLD
            }
            val v = TextView(this@MainActivity).apply {
                text = valStr
                setTextColor(Color.parseColor(col))
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(2), 0, 0)
            }
            addView(l)
            addView(v)
        }
    }

    // ================= ۲. رادار هوشمند هوش مصنوعی =================
    private fun renderRadar() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#0E121B", "#00E5FF", dp(18), 1)
            setPadding(dp(18), dp(20), dp(18), dp(20))
        }

        val title = TextView(this).apply {
            text = "AI RADAR SCANNER"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }
        val desc = TextView(this).apply {
            text = "دریافت ترندترین DNSهای گیمینگ با اتصال خودکار هوش مصنوعی از طریق سرور ضدتحریم:"
            setTextColor(Color.parseColor("#8E9DAE"))
            textSize = 12f
            setPadding(0, dp(4), 0, dp(15))
        }

        val scanBtn = Button(this).apply {
            text = "🔍 دریافت و استخراج ترندهای گیمینگ"
            setTextColor(Color.parseColor("#05070A"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            background = createCardDrawable("#00FF88", "#00FF88", dp(12), 0)
            setPadding(0, dp(12), 0, dp(12))
            setOnClickListener {
                callBackendApi("لیست ۳ تا از بهترین DNSهای گیمینگ با پینگ عالی برای بازی‌های آنلاین را به صورت خلاصه معرفی کن") { reply ->
                    showRadarResult(reply)
                }
            }
        }

        card.addView(title)
        card.addView(desc)
        card.addView(scanBtn)
        contentArea.addView(card)
    }

    private fun showRadarResult(reply: String) {
        val res = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#121724", "#00E5FF", dp(14), 1)
            setPadding(dp(15), dp(15), dp(15), dp(15))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(15), 0, 0)
            }
        }
        val t = TextView(this).apply {
            text = "ترکیب پیشنهادی رادار هوش مصنوعی:"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        val b = TextView(this).apply {
            text = reply
            setTextColor(Color.WHITE)
            textSize = 12.5f
            setPadding(0, dp(6), 0, dp(12))
        }
        val applyBtn = Button(this).apply {
            text = "اعمال این ترکیب روی کانکشن"
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            background = createCardDrawable("#00E5FF", "#00E5FF", dp(10), 0)
            setOnClickListener {
                currentPrimaryDns = "178.22.122.100"
                currentSecondaryDns = "185.51.200.2"
                Toast.makeText(this@MainActivity, "DNS روی داشبورد اعمال شد!", Toast.LENGTH_SHORT).show()
                switchTab(0)
            }
        }
        res.addView(t)
        res.addView(b)
        res.addView(applyBtn)
        contentArea.addView(res)
    }

    // ================= ۳. هاب کاستوم DoH =================
    private fun renderDoh() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#0E121B", "#1C2436", dp(18), 1)
            setPadding(dp(18), dp(20), dp(18), dp(20))
        }

        val title = TextView(this).apply {
            text = "CUSTOM DOH PROXY"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }
        val sub = TextView(this).apply {
            text = "آدرس اختصاصی پروتکل DoH را جهت فعال‌سازی مستقیم وارد کنید:"
            setTextColor(Color.parseColor("#8E9DAE"))
            textSize = 12f
            setPadding(0, dp(4), 0, dp(14))
        }

        val nameIn = EditText(this).apply {
            hint = "نام دلخواه سرور (مثال: DNS اختصاصی)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#4B5563"))
            background = createCardDrawable("#131722", "#252F45", dp(10), 1)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val urlIn = EditText(this).apply {
            hint = "https://dns.google/dns-query"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#4B5563"))
            background = createCardDrawable("#131722", "#252F45", dp(10), 1)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(10), 0, dp(16))
            }
        }

        val saveBtn = Button(this).apply {
            text = "ذخیره و آماده‌سازی اتصال DoH"
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            background = createCardDrawable("#00FF88", "#00FF88", dp(12), 0)
            setPadding(0, dp(12), 0, dp(12))
            setOnClickListener {
                val u = urlIn.text.toString().trim()
                if (u.startsWith("https://")) {
                    prefs.edit().putString("custom_doh", u).apply()
                    Toast.makeText(this@MainActivity, "DoH ذخیره شد!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "آدرس باید با https شروع شود", Toast.LENGTH_SHORT).show()
                }
            }
        }

        card.addView(title)
        card.addView(sub)
        card.addView(nameIn)
        card.addView(urlIn)
        card.addView(saveBtn)
        contentArea.addView(card)
    }

    // ================= ۴. دستیار چت بازی‌ها با حباب‌های مدرن =================
    private fun renderChat() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#0E121B", "#1C2436", dp(18), 1)
            setPadding(dp(15), dp(15), dp(15), dp(15))
        }

        val title = TextView(this).apply {
            text = "GAME AI STRATEGIST"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(10))
        }
        card.addView(title)

        val chatStream = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#080A0F", "#182030", dp(12), 1)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        val welcomeBubble = TextView(this).apply {
            text = "دستیار هوشمند: نام بازی خود را بفرستید (مثلاً وارزون، فیفا، کالاف) تا بهترین DNS برای پینگ و رفع تحریم را به شما بگویم."
            setTextColor(Color.parseColor("#8E9DAE"))
            textSize = 12.5f
            background = createCardDrawable("#131722", "#131722", dp(8), 0)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        chatStream.addView(welcomeBubble)
        card.addView(chatStream)

        val input = EditText(this).apply {
            hint = "سوال درباره بازی مورد نظر..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#4B5563"))
            background = createCardDrawable("#131722", "#252F45", dp(10), 1)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(12), 0, dp(8))
            }
        }
        card.addView(input)

        val sendBtn = Button(this).apply {
            text = "ارسال پرسش به هوش مصنوعی"
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            background = createCardDrawable("#00E5FF", "#00E5FF", dp(10), 0)
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener {
                val q = input.text.toString().trim()
                if (q.isNotEmpty()) {
                    val userBubble = TextView(this@MainActivity).apply {
                        text = q
                        setTextColor(Color.WHITE)
                        textSize = 12.5f
                        background = createCardDrawable("#005072", "#00E5FF", dp(8), 1)
                        setPadding(dp(10), dp(6), dp(10), dp(6))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            gravity = Gravity.END
                            setMargins(0, dp(6), 0, dp(6))
                        }
                    }
                    chatStream.addView(userBubble)
                    input.setText("")

                    callBackendApi(q) { ans ->
                        val botBubble = TextView(this@MainActivity).apply {
                            text = ans
                            setTextColor(Color.WHITE)
                            textSize = 12.5f
                            background = createCardDrawable("#131722", "#1C2436", dp(8), 0)
                            setPadding(dp(10), dp(8), dp(10), dp(8))
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                setMargins(0, dp(4), 0, dp(4))
                            }
                        }
                        chatStream.addView(botBubble)
                    }
                }
            }
        }
        card.addView(sendBtn)
        contentArea.addView(card)
    }

    // ================= ۵. تنظیمات کلید OpenRouter =================
    private fun renderSettings() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#0E121B", "#1C2436", dp(18), 1)
            setPadding(dp(18), dp(20), dp(18), dp(20))
        }

        val title = TextView(this).apply {
            text = "OPENROUTER API KEY"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }
        val sub = TextView(this).apply {
            text = "کلید تاییدشده OpenRouter خود را در کادر زیر وارد و ذخیره کنید:"
            setTextColor(Color.parseColor("#8E9DAE"))
            textSize = 12f
            setPadding(0, dp(4), 0, dp(14))
        }

        val keyIn = EditText(this).apply {
            hint = "کلید API شما..."
            setText(prefs.getString("openrouter_key", ""))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#4B5563"))
            background = createCardDrawable("#131722", "#252F45", dp(10), 1)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(16))
            }
        }

        val saveBtn = Button(this).apply {
            text = "ذخیره کلید امنیتی"
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            background = createCardDrawable("#00FF88", "#00FF88", dp(12), 0)
            setPadding(0, dp(12), 0, dp(12))
            setOnClickListener {
                val k = keyIn.text.toString().trim()
                prefs.edit().putString("openrouter_key", k).apply()
                Toast.makeText(this@MainActivity, "کلید با موفقیت ذخیره شد!", Toast.LENGTH_SHORT).show()
            }
        }

        val relayInfo = TextView(this).apply {
            text = "پل ارتباطی هاست: https://s.sosiss.ir/api.php\nمدل فعال: انتخاب خودکار (openrouter/auto)"
            setTextColor(Color.parseColor("#5A6A85"))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, dp(22), 0, 0)
        }

        card.addView(title)
        card.addView(sub)
        card.addView(keyIn)
        card.addView(saveBtn)
        card.addView(relayInfo)
        contentArea.addView(card)
    }

    // ================= ارتباط با هاست s.sosiss.ir =================
    private fun callBackendApi(promptText: String, onResult: (String) -> Unit) {
        val apiKey = prefs.getString("openrouter_key", "")
        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(this, "لطفاً ابتدا از تب تنظیمات کلید خود را وارد کنید!", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "در حال اتصال به هوش مصنوعی...", Toast.LENGTH_SHORT).show()

        thread {
            try {
                val url = URL("https://s.sosiss.ir/api.php")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 20000
                conn.readTimeout = 20000

                val jsonPayload = JSONObject().apply {
                    put("api_key", apiKey)
                    val msgs = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", promptText)
                        })
                    }
                    put("messages", msgs)
                }

                val os = conn.outputStream
                val writer = OutputStreamWriter(os, "UTF-8")
                writer.write(jsonPayload.toString())
                writer.flush()
                writer.close()

                val stream = if (conn.responseCode == 200) conn.inputStream else conn.errorStream
                val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
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
                    resJson.optString("message", "خطا در پردازش.")
                }

                runOnUiThread { onResult(reply) }
            } catch (e: Exception) {
                runOnUiThread {
                    onResult("خطا در شبکه: " + (e.message ?: "تایم‌اوت"))
                }
            }
        }
    }

    // ================= اتصال و کنترل VPN =================
    private fun toggleVpn() {
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
            updateDashboardUI()
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
            updateDashboardUI()
            Toast.makeText(this, "محافظ DNS گیمینگ فعال شد!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDashboardUI() {
        if (selectedTab == 0) {
            hudStateText.text = if (isConnected) "ONLINE" else "STANDBY"
            hudStateText.setTextColor(if (isConnected) Color.parseColor("#00FF88") else Color.parseColor("#5A6A85"))
            hudSubState.text = if (isConnected) "محافظ فعال است" else "آماده اتصال بهینه"
            hudRing.background = createCardDrawable(
                if (isConnected) "#0D2B20" else "#131722",
                if (isConnected) "#00FF88" else "#252F45",
                dp(100), 3
            )
            connectActionBtn.text = if (isConnected) "قطع ارتباط" else "⚡ اتصال سریع گیمینگ"
            connectActionBtn.background = createCardDrawable(
                if (isConnected) "#FF3366" else "#00FF88",
                if (isConnected) "#FF3366" else "#00FF88",
                dp(14), 0
            )
        }
    }

    private fun createCardDrawable(bgColor: String, borderColor: String, radius: Int, strokeWidth: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgColor))
            if (strokeWidth > 0) {
                setStroke(strokeWidth, Color.parseColor(borderColor))
            }
            cornerRadius = radius.toFloat()
        }
    }
}
