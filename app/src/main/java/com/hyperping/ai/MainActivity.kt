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

    private var currentPrimaryDns = "178.22.122.100"
    private var currentSecondaryDns = "185.51.200.2"

    private lateinit var contentArea: LinearLayout
    private lateinit var navButtons: ArrayList<Button>
    private var selectedTab = 0

    // ویوهای داینامیک داشبورد
    private lateinit var hudCircle: LinearLayout
    private lateinit var hudPingVal: TextView
    private lateinit var hudStatus: TextView
    private lateinit var connectBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("HyperPingPrefs", Context.MODE_PRIVATE)

        if (!prefs.getBoolean("is_authenticated", false)) {
            showPasscodeScreen()
        } else {
            setupMainApp()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    // ================= ۱. صفحه ورود VIP با استایل سایبرپانکی =================
    private fun showPasscodeScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#060709"))
            gravity = Gravity.CENTER
            setPadding(dp(30), dp(30), dp(30), dp(30))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#0E1118", "#00E5FF", dp(20), 2)
            setPadding(dp(25), dp(35), dp(25), dp(35))
            gravity = Gravity.CENTER
        }

        val logoBadge = TextView(this).apply {
            text = "⚡ HYPERPING AI"
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        val subText = TextView(this).apply {
            text = "سیستم بهینه‌ساز شبکه گیمینگ VIP\nبرای احراز هویت کد امنیتی را وارد کنید"
            setTextColor(Color.parseColor("#8A99AD"))
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(25))
        }

        val input = EditText(this).apply {
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#4A5568"))
            hint = "کد دسترسی (۱۰۱۷)"
            gravity = Gravity.CENTER
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            background = createCardDrawable("#161B26", "#2D3748", dp(12), 2)
            setPadding(dp(15), dp(15), dp(15), dp(15))
        }

        val btn = Button(this).apply {
            text = "تایید و ورود به سیستم"
            setTextColor(Color.parseColor("#060709"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = createCardDrawable("#00FF88", "#00FF88", dp(12), 0)
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener {
                if (input.text.toString().trim() == "1017") {
                    prefs.edit().putBoolean("is_authenticated", true).apply()
                    setupMainApp()
                } else {
                    Toast.makeText(this@MainActivity, "دسترسی غیرمجاز! کد امنیتی اشتباه است.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        card.addView(logoBadge)
        card.addView(subText)
        card.addView(input)
        card.addView(Space(this).apply { minimumHeight = dp(20) })
        card.addView(btn)

        root.addView(card)
        setContentView(root)
    }

    // ================= ساختار هدر و نویگیشن ۵ تب =================
    private fun setupMainApp() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#08090C"))
        }

        // هدر مدرن بالا
        val topBar = RelativeLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0C0E14"))
            setPadding(dp(20), dp(15), dp(20), dp(15))
        }

        val brandTitle = TextView(this).apply {
            text = "HYPERPING // AI HUD"
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }

        val engineStatus = TextView(this).apply {
            text = "LIVE ENGINE ●"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            params.addRule(RelativeLayout.ALIGN_PARENT_END)
            layoutParams = params
        }
        topBar.addView(brandTitle)
        topBar.addView(engineStatus)
        root.addView(topBar)

        // فضای محتوای تب‌ها
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        contentArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        scroll.addView(contentArea)
        root.addView(scroll)

        // نوار شناور نویگیشن زیر صفحه
        val navContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0D1017"))
            setPadding(dp(6), dp(10), dp(6), dp(10))
            weightSum = 5f
        }

        val tabTitles = listOf("داشبورد", "رادار هوش", "کاستوم DoH", "دستیار چت", "تنظیمات")
        navButtons = ArrayList()

        for (i in tabTitles.indices) {
            val navBtn = Button(this).apply {
                text = tabTitles[i]
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(2), dp(8), dp(2), dp(8))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { switchTab(i) }
            }
            navButtons.add(navBtn)
            navContainer.addView(navBtn)
        }
        root.addView(navContainer)

        setContentView(root)
        switchTab(0)
    }

    private fun switchTab(index: Int) {
        selectedTab = index
        for (i in navButtons.indices) {
            if (i == index) {
                navButtons[i].setTextColor(Color.parseColor("#00FF88"))
                navButtons[i].background = createCardDrawable("#151F26", "#00FF88", dp(10), 1)
            } else {
                navButtons[i].setTextColor(Color.parseColor("#64748B"))
                navButtons[i].setBackgroundColor(Color.TRANSPARENT)
            }
        }
        contentArea.removeAllViews()
        when (index) {
            0 -> buildDashboardScreen()
            1 -> buildRadarScreen()
            2 -> buildDohScreen()
            3 -> buildChatScreen()
            4 -> buildSettingsScreen()
        }
    }

    // ================= ۱. داشبورد خیره‌کننده گیمینگ =================
    private fun buildDashboardScreen() {
        // ۱. ردیف کارت‌های ۳ گانه اطلاعات شبکه
        val metricsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            setPadding(0, 0, 0, dp(20))
        }

        metricsRow.addView(createMetricBadge("PING", "34 ms", "#00FF88", 1f))
        metricsRow.addView(createMetricBadge("JITTER", "1 ms", "#00E5FF", 1f))
        metricsRow.addView(createMetricBadge("LOSS", "0.0 %", "#A855F7", 1f))
        contentArea.addView(metricsRow)

        // ۲. حلقه HUD بزرگ مرکزی
        val hudCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#0F131D", "#1E2638", dp(24), 2)
            setPadding(dp(20), dp(35), dp(20), dp(35))
            gravity = Gravity.CENTER
        }

        hudCircle = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val size = dp(170)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER
            }
            background = createCardDrawable(
                if (isConnected) "#0D251C" else "#151821",
                if (isConnected) "#00FF88" else "#2D3748",
                dp(100),
                3
            )
            gravity = Gravity.CENTER
        }

        hudPingVal = TextView(this).apply {
            text = if (isConnected) "ON" else "OFF"
            setTextColor(if (isConnected) Color.parseColor("#00FF88") else Color.parseColor("#64748B"))
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        hudStatus = TextView(this).apply {
            text = if (isConnected) "محافظ فعال است" else "سرویس قطع است"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
        }

        hudCircle.addView(hudPingVal)
        hudCircle.addView(hudStatus)
        hudCard.addView(hudCircle)

        // جزئیات دی‌ان‌اس فعال
        val detailsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#090B10", "#1E2638", dp(14), 1)
            setPadding(dp(15), dp(12), dp(15), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(20), 0, dp(25))
            }
        }

        val dnsTitle = TextView(this).apply {
            text = "TARGET RESOLVERS (ACTIVE COMBO)"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
        }
        val dnsBody = TextView(this).apply {
            text = "Primary: $currentPrimaryDns  |  Secondary: $currentSecondaryDns"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
            setPadding(0, dp(4), 0, 0)
        }
        detailsCard.addView(dnsTitle)
        detailsCard.addView(dnsBody)
        hudCard.addView(detailsCard)

        // دکمه بزرگ اتصال
        connectBtn = Button(this).apply {
            text = if (isConnected) "قطع ارتباط محافظ" else "اتصال سریع و بهینه‌سازی"
            setTextColor(Color.parseColor("#060709"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = createCardDrawable(
                if (isConnected) "#FF3366" else "#00FF88",
                if (isConnected) "#FF3366" else "#00FF88",
                dp(16),
                0
            )
            setPadding(0, dp(16), 0, dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener { toggleConnection() }
        }
        hudCard.addView(connectBtn)

        contentArea.addView(hudCard)
    }

    private fun createMetricBadge(label: String, value: String, colorHex: String, weight: Float): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply {
                setMargins(dp(4), 0, dp(4), 0)
            }
            background = createCardDrawable("#0F131D", "#1E2638", dp(14), 1)
            setPadding(dp(10), dp(12), dp(10), dp(12))
            gravity = Gravity.CENTER

            val l = TextView(this@MainActivity).apply {
                text = label
                setTextColor(Color.parseColor("#64748B"))
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
            }
            val v = TextView(this@MainActivity).apply {
                text = value
                setTextColor(Color.parseColor(colorHex))
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(2), 0, 0)
            }
            addView(l)
            addView(v)
        }
    }

    // ================= ۲. تب رادار هوشمند DNS =================
    private fun buildRadarScreen() {
        val banner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#0F131D", "#00E5FF", dp(18), 1)
            setPadding(dp(18), dp(20), dp(18), dp(20))
        }

        val title = TextView(this).apply {
            text = "AI RADAR SCANNER"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
        }
        val desc = TextView(this).apply {
            text = "هوش مصنوعی از طریق هاست ضدتحریم، بهترین DNSهای پینگ پایین دنیا را برای شما استخراج و جفت‌سازی می‌کند."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
            setPadding(0, dp(6), 0, dp(15))
        }

        val scanBtn = Button(this).apply {
            text = "🔍 اسکن و دریافت ترندهای گیمینگ"
            setTextColor(Color.parseColor("#060709"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            background = createCardDrawable("#00FF88", "#00FF88", dp(12), 0)
            setPadding(0, dp(12), 0, dp(12))
            setOnClickListener {
                callBackendAi("لیست ۳ تا از بهترین DNSهای گیمینگ با پینگ عالی برای بازی‌های آنلاین را به صورت خلاصه بگو") { reply ->
                    showRadarResult(reply)
                }
            }
        }

        banner.addView(title)
        banner.addView(desc)
        banner.addView(scanBtn)
        contentArea.addView(banner)
    }

    private fun showRadarResult(reply: String) {
        val resCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#131826", "#00E5FF", dp(16), 1)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(16), 0, 0)
            }
        }

        val t = TextView(this).apply {
            text = "پاسخ رادار هوشمند:"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        val r = TextView(this).apply {
            text = reply
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(0, dp(8), 0, dp(14))
        }

        val applyBtn = Button(this).apply {
            text = "اعمال این ترکیب روی داشبورد"
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            background = createCardDrawable("#00E5FF", "#00E5FF", dp(10), 0)
            setOnClickListener {
                currentPrimaryDns = "178.22.122.100"
                currentSecondaryDns = "185.51.200.2"
                Toast.makeText(this@MainActivity, "ترکیب اعمال شد!", Toast.LENGTH_SHORT).show()
                switchTab(0)
            }
        }

        resCard.addView(t)
        resCard.addView(r)
        resCard.addView(applyBtn)
        contentArea.addView(resCard)
    }

    // ================= ۳. هاب کاستوم DoH =================
    private fun buildDohScreen() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#0F131D", "#1E2638", dp(18), 1)
            setPadding(dp(18), dp(20), dp(18), dp(20))
        }

        val title = TextView(this).apply {
            text = "CUSTOM DOH RESOLVER"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }
        val sub = TextView(this).apply {
            text = "آدرس اختصاصی پروتکل DNS-Over-HTTPS را وارد نمایید:"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
            setPadding(0, dp(4), 0, dp(15))
        }

        val nameIn = EditText(this).apply {
            hint = "عنوان سرور (مثال: Cloudflare Gaming)"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#4A5568"))
            background = createCardDrawable("#161B26", "#2D3748", dp(10), 1)
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        val urlIn = EditText(this).apply {
            hint = "https://dns.google/dns-query"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#4A5568"))
            background = createCardDrawable("#161B26", "#2D3748", dp(10), 1)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(10), 0, dp(18))
            }
        }

        val saveBtn = Button(this).apply {
            text = "ذخیره و فعال‌سازی پروتکل"
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            background = createCardDrawable("#00FF88", "#00FF88", dp(12), 0)
            setPadding(0, dp(12), 0, dp(12))
            setOnClickListener {
                val url = urlIn.text.toString().trim()
                if (url.startsWith("https://")) {
                    prefs.edit().putString("custom_doh", url).apply()
                    Toast.makeText(this@MainActivity, "DoH ذخیره شد!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "آدرس باید با https شروع شود!", Toast.LENGTH_SHORT).show()
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
    private fun buildChatScreen() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#0F131D", "#1E2638", dp(18), 1)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val title = TextView(this).apply {
            text = "GAME AI STRATEGIST"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(12))
        }
        card.addView(title)

        val chatBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#090B10", "#1E2638", dp(12), 1)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val botMsg = TextView(this).apply {
            text = "دستیار هوش مصنوعی: نام بازی و اپراتور خود را بنویسید (مثلاً وارزون روی همراه اول) تا بهترین تنظیمات پینگ را تحلیل کنم."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12.5f
            background = createCardDrawable("#161C27", "#161C27", dp(10), 0)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        chatBox.addView(botMsg)
        card.addView(chatBox)

        val input = EditText(this).apply {
            hint = "سوال درباره بازی مورد نظر..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#4A5568"))
            background = createCardDrawable("#161B26", "#2D3748", dp(10), 1)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(14), 0, dp(10))
            }
        }
        card.addView(input)

        val sendBtn = Button(this).apply {
            text = "ارسال پرسش به هوش مصنوعی"
            setTextColor(Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
            background = createCardDrawable("#00E5FF", "#00E5FF", dp(12), 0)
            setPadding(0, dp(12), 0, dp(12))
            setOnClickListener {
                val q = input.text.toString().trim()
                if (q.isNotEmpty()) {
                    val userBubble = TextView(this@MainActivity).apply {
                        text = q
                        setTextColor(Color.WHITE)
                        textSize = 12.5f
                        background = createCardDrawable("#004C6D", "#00E5FF", dp(10), 1)
                        setPadding(dp(12), dp(8), dp(12), dp(8))
                        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                            gravity = Gravity.END
                            setMargins(0, dp(8), 0, dp(8))
                        }
                    }
                    chatBox.addView(userBubble)
                    input.setText("")

                    callBackendAi(q) { ans ->
                        val botBubble = TextView(this@MainActivity).apply {
                            text = ans
                            setTextColor(Color.WHITE)
                            textSize = 12.5f
                            background = createCardDrawable("#161C27", "#1E2638", dp(10), 0)
                            setPadding(dp(12), dp(10), dp(12), dp(10))
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                setMargins(0, dp(4), 0, dp(4))
                            }
                        }
                        chatBox.addView(botBubble)
                    }
                }
            }
        }
        card.addView(sendBtn)
        contentArea.addView(card)
    }

    // ================= ۵. تنظیمات کلید OpenRouter =================
    private fun buildSettingsScreen() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCardDrawable("#0F131D", "#1E2638", dp(18), 1)
            setPadding(dp(18), dp(20), dp(18), dp(20))
        }

        val title = TextView(this).apply {
            text = "API KEY VAULT (OPENROUTER)"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }
        val sub = TextView(this).apply {
            text = "کلید API خود را جهت استفاده رایگان از مدل‌های هوش مصنوعی وارد کنید:"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
            setPadding(0, dp(4), 0, dp(15))
        }

        val keyIn = EditText(this).apply {
            hint = "sk-or-v1-..."
            setText(prefs.getString("openrouter_key", ""))
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#4A5568"))
            background = createCardDrawable("#161B26", "#2D3748", dp(10), 1)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(18))
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
            text = "پل ارتباطی هاست: https://s.sosiss.ir/api.php\n(ترافیک بین‌الملل بدون فیلتر هدایت می‌شود)"
            setTextColor(Color.parseColor("#64748B"))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, dp(25), 0, 0)
        }

        card.addView(title)
        card.addView(sub)
        card.addView(keyIn)
        card.addView(saveBtn)
        card.addView(relayInfo)
        contentArea.addView(card)
    }

    // ================= اتصال به بک‌اند اختصاصی s.sosiss.ir =================
    private fun callBackendAi(promptText: String, onResult: (String) -> Unit) {
        val apiKey = prefs.getString("openrouter_key", "")
        if (apiKey.isNullOrEmpty()) {
            Toast.makeText(this, "لطفاً ابتدا از بخش تنظیمات کلید OpenRouter خود را وارد کنید!", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "در حال پردازش در هوش مصنوعی...", Toast.LENGTH_SHORT).show()

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
                    onResult("خطا در ارتباط با هاست: " + (e.message ?: "تایم‌اوت"))
                }
            }
        }
    }

    // ================= اتصال و قطع VPN =================
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
            updateConnectionUI()
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
            updateConnectionUI()
            Toast.makeText(this, "محافظ DNS گیمینگ با موفقیت فعال شد!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateConnectionUI() {
        if (selectedTab == 0) {
            hudPingVal.text = if (isConnected) "ON" else "OFF"
            hudPingVal.setTextColor(if (isConnected) Color.parseColor("#00FF88") else Color.parseColor("#64748B"))
            hudStatus.text = if (isConnected) "محافظ فعال است" else "سرویس قطع است"
            hudCircle.background = createCardDrawable(
                if (isConnected) "#0D251C" else "#151821",
                if (isConnected) "#00FF88" else "#2D3748",
                dp(100),
                3
            )
            connectBtn.text = if (isConnected) "قطع ارتباط محافظ" else "اتصال سریع و بهینه‌سازی"
            connectBtn.background = createCardDrawable(
                if (isConnected) "#FF3366" else "#00FF88",
                if (isConnected) "#FF3366" else "#00FF88",
                dp(16),
                0
            )
        }
    }

    // سازنده گرافیک نئونی و کارت‌های شیشه‌ای
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
