package com.hyperping.ai

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.widget.*
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val PROXY_URL = "https://p.sosiss.ir/data_proxy.json"
    private val V2RAY_URL = "https://p.sosiss.ir/data_v2ray.json"

    // توکن‌های رنگی Aurora Dark
    private val BG_DARK = "#04050A"
    private val BG_HEADER = "#080B14"
    private val CARD_SURFACE = "#0E1220"
    private val CARD_SURFACE_SEC = "#131828"
    private val STROKE_PRI = "#1E2637"
    private val STROKE_BRIGHT = "#2A3448"
    private val TXT_PRI = "#F7FAFF"
    private val TXT_SEC = "#8593AC"
    private val TXT_MUTED = "#5A6780"

    // لهجه‌ها
    private val ACC_CYAN = "#22D3EE"
    private val ACC_BLUE = "#3B82F6"
    private val ACC_PURPLE = "#A855F7"
    private val ACC_PINK = "#EC4899"

    // وضعیت‌ها
    private val STAT_HEALTHY = "#34D399" // زیر 140
    private val STAT_MEDIUM = "#FBBF24"  // 141 - 280
    private val STAT_WEAK = "#FB923C"    // 281 - 500
    private val STAT_DEAD = "#FB4E6D"    // بالای 500 یا ناموفق

    data class ProxyItem(val server: String, val port: Int, val secret: String, val tgLink: String, var ping: Int = -1, var index: Int = 0)
    data class V2rayItem(val config: String, val protocol: String, val server: String, val port: Int, val remark: String, var ping: Int = -1, var index: Int = 0)

    private val allProxies = ArrayList<ProxyItem>()
    private val allV2rays = ArrayList<V2rayItem>()

    private var activeTab = 0 // 0 = Telegram, 1 = Shadowsocks
    private var activeFilter = 0 // 0=All, 1=Healthy, 2=Fast, 3=Dead
    private var searchQuery = ""

    private lateinit var contentListLayout: LinearLayout
    private lateinit var tabTgBtn: TextView
    private lateinit var tabV2Btn: TextView
    private lateinit var accentLine: View
    private lateinit var actionBtn: Button
    private lateinit var statusDescText: TextView
    private lateinit var loadingProgressBar: View
    private lateinit var filterChips: ArrayList<TextView>

    // شمارنده‌های اکشن‌کارت
    private lateinit var countTotalVal: TextView
    private lateinit var countHealthyVal: TextView
    private lateinit var countBestVal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor(BG_HEADER)
        window.navigationBarColor = Color.parseColor(BG_DARK)
        buildAuroraInterface()
        fetchCloudData()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun buildAuroraInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(BG_DARK))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        // ================= ۱. هدر لوکس با آیکون گرادینتی و چراغ نبض‌دار =================
        val header = RelativeLayout(this).apply {
            setBackgroundColor(Color.parseColor(BG_HEADER))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val brandBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val iconTile = TextView(this).apply {
            text = "H"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            val size = dp(34)
            layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(0, 0, dp(12), 0) }
            background = createGradient(ACC_CYAN, ACC_PURPLE, dp(10), 0, "")
        }

        val titleCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val appName = TextView(this).apply {
            text = "HYPER CORE"
            setTextColor(Color.parseColor(TXT_PRI))
            textSize = 16.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        val subTech = TextView(this).apply {
            text = "PROXY · SHADOWSOCKS · PING LAB"
            setTextColor(Color.parseColor(TXT_MUTED))
            textSize = 8.5f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.12f
            setPadding(0, dp(2), 0, 0)
        }
        titleCol.addView(appName)
        titleCol.addView(subTech)
        brandBox.addView(iconTile)
        brandBox.addView(titleCol)
        header.addView(brandBox)

        // قرص وضعیت ONLINE با انیمیشن آلفای بی‌نهایت
        val onlinePill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = createShape(CARD_SURFACE_SEC, STROKE_PRI, dp(20), 1)
            setPadding(dp(10), dp(5), dp(12), dp(5))
            val p = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            p.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            p.addRule(RelativeLayout.CENTER_VERTICAL)
            layoutParams = p
        }

        val pulseDot = View(this).apply {
            val s = dp(8)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { setMargins(0, 0, dp(6), 0) }
            background = createShape(STAT_HEALTHY, STAT_HEALTHY, dp(10), 0)
            val anim = AlphaAnimation(0.25f, 1.0f).apply {
                duration = 750
                repeatCount = Animation.INFINITE
                repeatMode = Animation.REVERSE
            }
            startAnimation(anim)
        }
        val onlineText = TextView(this).apply {
            text = "ONLINE"
            setTextColor(Color.parseColor(TXT_PRI))
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.1f
        }
        onlinePill.addView(pulseDot)
        onlinePill.addView(onlineText)
        header.addView(onlinePill)

        root.addView(header)

        // خط ۲dp گرادینت لهجه زیر هدر
        accentLine = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2))
            background = createGradient(ACC_CYAN, ACC_BLUE, 0, 0, "")
        }
        root.addView(accentLine)

        // ================= ۲. تب‌های قرصی شکل =================
        val tabContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createShape("#0A0D18", STROKE_PRI, dp(22), 1)
            setPadding(dp(5), dp(5), dp(5), dp(5))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
                setMargins(dp(16), dp(12), dp(16), dp(8))
            }
            weightSum = 2f
        }

        tabTgBtn = createTabButton("پروکسی تلگرام (MTProto)", 0)
        tabV2Btn = createTabButton("سرورهای شادوساکس (SS)", 1)
        tabContainer.addView(tabTgBtn)
        tabContainer.addView(tabV2Btn)
        root.addView(tabContainer)

        // ================= ۳. کارت اکشن و شمارنده‌های ۳ گانه =================
        val actionCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createShape(CARD_SURFACE, STROKE_PRI, dp(20), 1)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), dp(4), dp(16), dp(10))
            }
        }

        // ردیف آمار (تعداد کل | سالم | بهترین پینگ)
        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            setPadding(0, dp(4), 0, dp(12))
        }

        countTotalVal = createStatColumn(statsRow, "تعداد کل", "۰")
        statsRow.addView(createVerticalDivider())
        countHealthyVal = createStatColumn(statsRow, "سالم", "۰", STAT_HEALTHY)
        statsRow.addView(createVerticalDivider())
        countBestVal = createStatColumn(statsRow, "بهترین پینگ", "---", ACC_CYAN)
        actionCard.addView(statsRow)

        // نوار لودینگ باریک ۴dp
        loadingProgressBar = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(4)).apply {
                setMargins(0, dp(4), 0, dp(10))
            }
            background = createGradient(ACC_CYAN, ACC_BLUE, dp(2), 0, "")
            visibility = View.INVISIBLE
        }
        actionCard.addView(loadingProgressBar)

        statusDescText = TextView(this).apply {
            text = "مخزن در حال دریافت آخرین سرورها..."
            setTextColor(Color.parseColor(TXT_SEC))
            textSize = 12f
            setPadding(0, 0, 0, dp(10))
        }
        actionCard.addView(statusDescText)

        // دکمه اصلی ۵۰dp با انیمیشن ریپل و کلیک
        actionBtn = Button(this).apply {
            text = "⚡ تست و اعتبارسنجی دقیق پینگ"
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#04050A"))
            background = createGradient(ACC_CYAN, ACC_BLUE, dp(14), 0, "")
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50))
            setOnClickListener {
                animateClick(this)
                executeSmartPingTest()
            }
        }
        actionCard.addView(actionBtn)
        root.addView(actionCard)

        // ================= ۴. فیلد جستجو و چیپ‌های فیلتر =================
        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createShape(CARD_SURFACE_SEC, STROKE_PRI, dp(14), 1)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), 0, dp(16), dp(8))
            }
            gravity = Gravity.CENTER_VERTICAL
        }

        val searchInput = EditText(this).apply {
            hint = "جستجوی سرور، آی‌پی، موقعیت..."
            setHintTextColor(Color.parseColor(TXT_MUTED))
            setTextColor(Color.parseColor(TXT_PRI))
            textSize = 12.5f
            background = null
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s.toString().trim().lowercase()
                    renderFilteredList()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        searchBox.addView(searchInput)
        root.addView(searchBox)

        // چیپ‌های فیلتر ("همه", "سالم", "سریع", "قطع")
        val chipsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), 0, dp(16), dp(8))
        }

        filterChips = ArrayList()
        val chipNames = listOf("همه", "سالم", "سریع (<150ms)", "قطع")
        for (i in chipNames.indices) {
            val chip = TextView(this).apply {
                text = chipNames[i]
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(12), dp(6), dp(12), dp(6))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dp(8), 0)
                }
                setOnClickListener {
                    activeFilter = i
                    updateFilterChipsUI()
                    renderFilteredList()
                }
            }
            filterChips.add(chip)
            chipsRow.addView(chip)
        }
        root.addView(chipsRow)
        updateFilterChipsUI()

        // ================= ۵. لیست اسکرول آیتم‌ها =================
        val scroller = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
        }
        contentListLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(24))
        }
        scroller.addView(contentListLayout)
        root.addView(scroller)

        setContentView(root)
        updateActiveTabVisuals()
    }

    private fun createTabButton(title: String, tabIndex: Int): TextView {
        return TextView(this).apply {
            text = title
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener {
                if (activeTab != tabIndex) {
                    activeTab = tabIndex
                    updateActiveTabVisuals()
                    renderFilteredList()
                }
            }
        }
    }

    private fun updateActiveTabVisuals() {
        if (activeTab == 0) {
            tabTgBtn.setTextColor(Color.parseColor(TXT_PRI))
            tabTgBtn.background = createShape(CARD_SURFACE_SEC, ACC_CYAN, dp(18), 1)
            tabV2Btn.setTextColor(Color.parseColor(TXT_MUTED))
            tabV2Btn.background = null

            accentLine.background = createGradient(ACC_CYAN, ACC_BLUE, 0, 0, "")
            actionBtn.background = createGradient(ACC_CYAN, ACC_BLUE, dp(14), 0, "")
            countBestVal.setTextColor(Color.parseColor(ACC_CYAN))
        } else {
            tabV2Btn.setTextColor(Color.parseColor(TXT_PRI))
            tabV2Btn.background = createShape(CARD_SURFACE_SEC, ACC_PURPLE, dp(18), 1)
            tabTgBtn.setTextColor(Color.parseColor(TXT_MUTED))
            tabTgBtn.background = null

            accentLine.background = createGradient(ACC_PURPLE, ACC_PINK, 0, 0, "")
            actionBtn.background = createGradient(ACC_PURPLE, ACC_PINK, dp(14), 0, "")
            countBestVal.setTextColor(Color.parseColor(ACC_PURPLE))
        }
        updateStatsCounters()
    }

    private fun updateFilterChipsUI() {
        for (i in filterChips.indices) {
            val chip = filterChips[i]
            if (i == activeFilter) {
                chip.setTextColor(Color.parseColor(TXT_PRI))
                val color = if (activeTab == 0) ACC_CYAN else ACC_PURPLE
                chip.background = createShape(CARD_SURFACE_SEC, color, dp(20), 1)
            } else {
                chip.setTextColor(Color.parseColor(TXT_MUTED))
                chip.background = createShape("#090C16", STROKE_PRI, dp(20), 1)
            }
        }
    }

    private fun createStatColumn(parent: LinearLayout, label: String, initialVal: String, valColor: String = TXT_PRI): TextView {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val v = TextView(this).apply {
            text = initialVal
            setTextColor(Color.parseColor(valColor))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
        }
        val l = TextView(this).apply {
            text = label
            setTextColor(Color.parseColor(TXT_MUTED))
            textSize = 9.5f
            setPadding(0, dp(2), 0, 0)
        }
        col.addView(v)
        col.addView(l)
        parent.addView(col)
        return v
    }

    private fun createVerticalDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(1), dp(26)).apply { gravity = Gravity.CENTER_VERTICAL }
            setBackgroundColor(Color.parseColor(STROKE_PRI))
        }
    }

    // ================= دریافت اطلاعات از سرور =================
    private fun fetchCloudData() {
        thread {
            try {
                val pStr = httpGet(PROXY_URL)
                val pArr = JSONArray(pStr)
                allProxies.clear()
                for (i in 0 until pArr.length()) {
                    val o = pArr.getJSONObject(i)
                    allProxies.add(ProxyItem(o.optString("server"), o.optInt("port"), o.optString("secret"), o.optString("tg_link"), -1, i + 1))
                }

                val vStr = httpGet(V2RAY_URL)
                val vArr = JSONArray(vStr)
                allV2rays.clear()
                for (i in 0 until vArr.length()) {
                    val o = vArr.getJSONObject(i)
                    allV2rays.add(V2rayItem(o.optString("config"), o.optString("protocol", "SS"), o.optString("server"), o.optInt("port", 443), o.optString("remark", "Canada"), -1, i + 1))
                }

                runOnUiThread {
                    statusDescText.text = "مخزن بروز است. سرورها آماده تست و اتصال می‌باشند."
                    updateStatsCounters()
                    renderFilteredList()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusDescText.text = "خطا در اتصال به هاست: " + (e.message ?: "تایم‌اوت")
                }
            }
        }
    }

    private fun httpGet(urlStr: String): String {
        val c = URL(urlStr).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        val r = BufferedReader(InputStreamReader(c.inputStream, "UTF-8"))
        val b = StringBuilder()
        var l: String?
        while (r.readLine().also { l = it } != null) b.append(l)
        r.close()
        return b.toString()
    }

    private fun updateStatsCounters() {
        if (activeTab == 0) {
            countTotalVal.text = allProxies.size.toString()
            val healthy = allProxies.count { it.ping > 0 }
            countHealthyVal.text = healthy.toString()
            val best = allProxies.filter { it.ping > 0 }.minByOrNull { it.ping }
            countBestVal.text = if (best != null) "${best.ping}ms" else "---"
        } else {
            countTotalVal.text = allV2rays.size.toString()
            val healthy = allV2rays.count { it.ping > 0 }
            countHealthyVal.text = healthy.toString()
            val best = allV2rays.filter { it.ping > 0 }.minByOrNull { it.ping }
            countBestVal.text = if (best != null) "${best.ping}ms" else "---"
        }
    }

    // ================= فیلتر و رندر پله‌ای کارت‌ها =================
    private fun renderFilteredList() {
        contentListLayout.removeAllViews()

        val bestPing = if (activeTab == 0) {
            allProxies.filter { it.ping > 0 }.minByOrNull { it.ping }?.ping ?: -1
        } else {
            allV2rays.filter { it.ping > 0 }.minByOrNull { it.ping }?.ping ?: -1
        }

        if (activeTab == 0) {
            val filtered = allProxies.filter {
                val matchSearch = searchQuery.isEmpty() || it.server.contains(searchQuery) || it.port.toString().contains(searchQuery)
                val matchFilter = when (activeFilter) {
                    1 -> it.ping in 1..500
                    2 -> it.ping in 1..150
                    3 -> it.ping == -2
                    else -> true
                }
                matchSearch && matchFilter
            }

            for (i in filtered.indices) {
                val card = buildProxyCard(filtered[i], filtered[i].ping == bestPing && bestPing > 0)
                contentListLayout.addView(card)
                animateEntrance(card, i)
            }
        } else {
            val filtered = allV2rays.filter {
                val matchSearch = searchQuery.isEmpty() || it.server.contains(searchQuery) || it.remark.lowercase().contains(searchQuery)
                val matchFilter = when (activeFilter) {
                    1 -> it.ping in 1..500
                    2 -> it.ping in 1..150
                    3 -> it.ping == -2
                    else -> true
                }
                matchSearch && matchFilter
            }

            val count = Math.min(filtered.size, 100)
            for (i in 0 until count) {
                val card = buildV2rayCard(filtered[i], filtered[i].ping == bestPing && bestPing > 0)
                contentListLayout.addView(card)
                animateEntrance(card, i)
            }
        }
    }

    // ================= کارت پروکسی تلگرام =================
    private fun buildProxyCard(item: ProxyItem, isBest: Boolean): View {
        val isDead = item.ping == -2
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val borderColor = if (isBest) ACC_CYAN else if (item.ping > 0) STROKE_BRIGHT else if (isDead) STAT_DEAD else STROKE_PRI
            val bgCol = if (isBest) "#0F1626" else CARD_SURFACE
            background = createShape(bgCol, borderColor, dp(20), 1)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
            if (isDead) alpha = 0.65f
        }

        // ردیف بالا
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // چیپ شماره ۳۰x۳۰
        val numChip = TextView(this).apply {
            text = item.index.toString()
            setTextColor(Color.parseColor(TXT_SEC))
            textSize = 10f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            val s = dp(30)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { setMargins(0, 0, dp(12), 0) }
            background = createShape(CARD_SURFACE_SEC, STROKE_PRI, dp(10), 1)
        }

        // ستون اطلاعات
        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val ipText = TextView(this).apply {
            text = "${item.server}:${item.port}"
            setTextColor(Color.parseColor(TXT_PRI))
            textSize = 13.5f
            typeface = Typeface.MONOSPACE
        }
        val subProtocol = TextView(this).apply {
            text = "MTPROTO · TELEGRAM"
            setTextColor(Color.parseColor(TXT_MUTED))
            textSize = 9.5f
            letterSpacing = 0.1f
            setPadding(0, dp(2), 0, 0)
        }
        infoCol.addView(ipText)
        infoCol.addView(subProtocol)

        // ستون پینگ و سیگنال ۴ تایی
        val statusCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val pingBadge = TextView(this).apply {
            text = getPingText(item.ping, isBest)
            setTextColor(Color.parseColor(getPingColor(item.ping)))
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            background = createShape(CARD_SURFACE_SEC, getPingColor(item.ping), dp(20), 1)
            setPadding(dp(9), dp(3), dp(9), dp(3))
        }

        val signalBar = createSignalBar(item.ping)
        statusCol.addView(pingBadge)
        statusCol.addView(signalBar)

        topRow.addView(numChip)
        topRow.addView(infoCol)
        topRow.addView(statusCol)
        card.addView(topRow)

        // جداکننده فید گرادینتی
        card.addView(createDivider())

        // دکمه‌های پایین
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
            weightSum = 2f
        }

        val connBtn = Button(this).apply {
            text = "اتصال به تلگرام"
            setTextColor(Color.parseColor("#04050A"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = createGradient(ACC_CYAN, ACC_BLUE, dp(12), 0, "")
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1.25f).apply { setMargins(0, 0, dp(8), 0) }
            setOnClickListener {
                animateClick(this)
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.tgLink)))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "تلگرام نصب نیست!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val copyBtn = Button(this).apply {
            text = "کپی لینک"
            setTextColor(Color.parseColor(TXT_SEC))
            textSize = 11.5f
            background = createShape(CARD_SURFACE_SEC, STROKE_BRIGHT, dp(12), 1)
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 0.75f)
            setOnClickListener {
                animateClick(this)
                copyToClipboard(item.tgLink, "لینک پروکسی تلگرام کپی شد")
            }
        }

        btnRow.addView(connBtn)
        btnRow.addView(copyBtn)
        card.addView(btnRow)

        return card
    }

    // ================= کارت شادوساکس V2Ray =================
    private fun buildV2rayCard(item: V2rayItem, isBest: Boolean): View {
        val isDead = item.ping == -2
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val borderColor = if (isBest) ACC_PURPLE else if (item.ping > 0) STROKE_BRIGHT else if (isDead) STAT_DEAD else STROKE_PRI
            val bgCol = if (isBest) "#181026" else CARD_SURFACE
            background = createShape(bgCol, borderColor, dp(20), 1)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
            if (isDead) alpha = 0.65f
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val numChip = TextView(this).apply {
            text = item.index.toString()
            setTextColor(Color.parseColor(TXT_SEC))
            textSize = 10f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            val s = dp(30)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { setMargins(0, 0, dp(12), 0) }
            background = createShape(CARD_SURFACE_SEC, STROKE_PRI, dp(10), 1)
        }

        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val remarkText = TextView(this).apply {
            text = item.remark
            setTextColor(Color.parseColor(TXT_PRI))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
        }
        val ipPortText = TextView(this).apply {
            text = "${item.server}:${item.port}"
            setTextColor(Color.parseColor(TXT_MUTED))
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(2), 0, 0)
        }
        infoCol.addView(remarkText)
        infoCol.addView(ipPortText)

        val statusCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val pingBadge = TextView(this).apply {
            text = getPingText(item.ping, isBest)
            setTextColor(Color.parseColor(getPingColor(item.ping)))
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            background = createShape(CARD_SURFACE_SEC, getPingColor(item.ping), dp(20), 1)
            setPadding(dp(9), dp(3), dp(9), dp(3))
        }

        val signalBar = createSignalBar(item.ping)
        statusCol.addView(pingBadge)
        statusCol.addView(signalBar)

        topRow.addView(numChip)
        topRow.addView(infoCol)
        topRow.addView(statusCol)
        card.addView(topRow)

        card.addView(createDivider())

        // چیپ‌های فنی و دکمه کپی تمام‌عرض
        val tagsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }
        tagsRow.addView(createTagChip(item.protocol))
        tagsRow.addView(createTagChip("ICMP TEST"))
        card.addView(tagsRow)

        val copyBtn = Button(this).apply {
            text = "کپی کانفیگ شادوساکس"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = createGradient(ACC_PURPLE, ACC_PINK, dp(12), 0, "")
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42))
            setOnClickListener {
                animateClick(this)
                copyToClipboard(item.config, "کانفیگ شادوساکس کپی شد")
            }
        }
        card.addView(copyBtn)

        return card
    }

    // ================= نشانگر ۴ خانه‌ای سیگنال شبکه =================
    private fun createSignalBar(ping: Int): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)

            val activeBars = when {
                ping in 1..140 -> 4
                ping in 141..280 -> 3
                ping in 281..500 -> 2
                ping > 500 -> 1
                else -> 0
            }
            val activeColor = getPingColor(ping)

            for (b in 1..4) {
                val bar = View(this@MainActivity).apply {
                    val w = dp(5)
                    val h = dp(4 + b * 2)
                    layoutParams = LinearLayout.LayoutParams(w, h).apply {
                        setMargins(dp(1), 0, dp(1), 0)
                        gravity = Gravity.BOTTOM
                    }
                    val col = if (b <= activeBars) activeColor else "#1C2438"
                    background = createShape(col, col, dp(2), 0)
                }
                addView(bar)
            }
        }
    }

    private fun createTagChip(title: String): TextView {
        return TextView(this).apply {
            text = title
            setTextColor(Color.parseColor(TXT_MUTED))
            textSize = 9f
            typeface = Typeface.MONOSPACE
            background = createShape(CARD_SURFACE_SEC, STROKE_PRI, dp(6), 1)
            setPadding(dp(6), dp(2), dp(6), dp(2))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, dp(6), 0)
            }
        }
    }

    private fun createDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                setMargins(0, dp(10), 0, 0)
            }
            background = createGradient("#001E2637", STROKE_PRI, 0, 0, "")
        }
    }

    // ================= موتور تست پینگ =================
    private fun executeSmartPingTest() {
        actionBtn.isEnabled = false
        actionBtn.alpha = 0.65f
        actionBtn.text = "در حال تست و مرتب‌سازی..."
        loadingProgressBar.visibility = View.VISIBLE

        val executor = Executors.newFixedThreadPool(14)

        thread {
            if (activeTab == 0) {
                for (item in allProxies) {
                    executor.execute {
                        item.ping = probeTelegramPing(item.server, item.port)
                    }
                }
            } else {
                val limit = Math.min(allV2rays.size, 100)
                for (i in 0 until limit) {
                    val item = allV2rays[i]
                    executor.execute {
                        item.ping = linuxIcmpPing(item.server)
                    }
                }
            }

            executor.shutdown()
            while (!executor.isTerminated) {
                Thread.sleep(60)
            }

            // مرتب‌سازی هوشمند: سالم‌ترین‌ها در صدر
            if (activeTab == 0) {
                allProxies.sortBy { if (it.ping <= 0) 999999 else it.ping }
            } else {
                allV2rays.sortBy { if (it.ping <= 0) 999999 else it.ping }
            }

            runOnUiThread {
                actionBtn.isEnabled = true
                actionBtn.alpha = 1f
                actionBtn.text = "⚡ تست و اعتبارسنجی دقیق پینگ"
                loadingProgressBar.visibility = View.INVISIBLE
                statusDescText.text = "تست پایان یافت. سرورهای سالم مرتب شدند."
                updateStatsCounters()
                renderFilteredList()
            }
        }
    }

    private fun probeTelegramPing(host: String, port: Int): Int {
        if (host.isEmpty() || port <= 0) return -2
        var total = 0
        var ok = 0
        for (i in 1..2) {
            try {
                val sock = Socket()
                sock.tcpNoDelay = true
                val start = System.currentTimeMillis()
                sock.connect(InetSocketAddress(host, port), 1200)
                total += (System.currentTimeMillis() - start).toInt()
                sock.close()
                ok++
            } catch (e: Exception) {}
        }
        return if (ok > 0) total / ok else -2
    }

    private fun linuxIcmpPing(host: String): Int {
        if (host.isEmpty()) return -2
        return try {
            val cmd = "/system/bin/ping -c 2 -W 1 $host"
            val process = Runtime.getRuntime().exec(cmd)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var avgMs = -1

            while (reader.readLine().also { line = it } != null) {
                val l = line!!.lowercase()
                if (l.contains("min/avg") || (l.contains("rtt") && l.contains("/"))) {
                    val parts = l.substringAfter("=").trim().split("/")
                    if (parts.size >= 2) avgMs = parts[1].trim().toFloat().toInt()
                }
            }
            val exitCode = process.waitFor()
            if (exitCode == 0 && avgMs > 0) avgMs else -2
        } catch (e: Exception) {
            -2
        }
    }

    // ================= کمک‌کننده‌ها =================
    private fun getPingText(ping: Int, isBest: Boolean): String {
        return when {
            isBest -> "👑 ${ping}ms"
            ping > 0 -> "${ping}ms"
            ping == -2 -> "قطع"
            else -> "تست‌نشده"
        }
    }

    private fun getPingColor(ping: Int): String {
        return when {
            ping in 1..140 -> STAT_HEALTHY
            ping in 141..280 -> STAT_MEDIUM
            ping in 281..500 -> STAT_WEAK
            ping > 500 || ping == -2 -> STAT_DEAD
            else -> TXT_MUTED
        }
    }

    private fun animateEntrance(view: View, index: Int) {
        view.alpha = 0f
        view.translationY = dp(20).toFloat()
        val delay = Math.min(index * 32L, 480L)
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(320)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun animateClick(view: View) {
        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(70).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
    }

    private fun copyToClipboard(payload: String, toastMsg: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Payload", payload))
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
    }

    private fun createShape(bgColor: String, strokeColor: String, radius: Int, strokeWidth: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgColor))
            if (strokeWidth > 0) setStroke(strokeWidth, Color.parseColor(strokeColor))
            cornerRadius = radius.toFloat()
        }
    }

    private fun createGradient(startCol: String, endCol: String, radius: Int, strokeWidth: Int, strokeCol: String): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.parseColor(startCol), Color.parseColor(endCol))).apply {
            if (strokeWidth > 0 && strokeCol.isNotEmpty()) setStroke(strokeWidth, Color.parseColor(strokeCol))
            cornerRadius = radius.toFloat()
        }
    }
}
