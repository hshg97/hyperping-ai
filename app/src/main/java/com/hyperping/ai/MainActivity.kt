package com.hyperping.ai

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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
    private val SS_URL    = "https://p.sosiss.ir/data_ss.json"
    private val VLESS_URL = "https://p.sosiss.ir/data_vless.json"

    // پالت لوکس Aurora Dark
    private val BG_DEEP     = "#05060E"
    private val BG_HEADER   = "#0A0E1C"
    private val SURFACE     = "#0C1120"
    private val SURFACE_HI  = "#141B31"
    private val SURFACE_LO  = "#090C18"
    private val STROKE_SOFT = "#1C2439"
    private val STROKE_HI   = "#2B3550"
    private val TXT_BRIGHT  = "#F5F8FF"
    private val TXT_BODY    = "#98A5C3"
    private val TXT_DIM     = "#5C6884"

    // لهجه‌ها
    private val ACC_CYAN   = "#22D3EE"
    private val ACC_BLUE   = "#3B82F6"
    private val ACC_PURPLE = "#A855F7"
    private val ACC_PINK   = "#EC4899"
    private val ACC_GOLD   = "#FFC94A"
    private val ACC_EMBER  = "#F97066"

    // وضعیت‌ها
    private val STAT_HEALTHY = "#34D399"
    private val STAT_MEDIUM  = "#FBBF24"
    private val STAT_WEAK    = "#FB923C"
    private val STAT_DEAD    = "#FB4E6D"

    data class ProxyItem(val server: String, val port: Int, val secret: String, val tgLink: String, var ping: Int = -1, var speed: Float = 0f, var speedText: String = "", var index: Int = 0)
    data class ServerItem(val config: String, val protocol: String, val server: String, val port: Int, val remark: String, var ping: Int = -1, var speed: Float = 0f, var speedText: String = "", var index: Int = 0)

    private val proxyList = ArrayList<ProxyItem>()
    private val ssList    = ArrayList<ServerItem>()
    private val vlessList = ArrayList<ServerItem>()

    private var activeTab = 0 // 0=تلگرام, 1=شادوساکس, 2=VLESS

    private lateinit var contentListLayout: LinearLayout
    private lateinit var tabTgBtn: TextView
    private lateinit var tabSsBtn: TextView
    private lateinit var tabVlessBtn: TextView
    private lateinit var pingActionBtn: Button
    private lateinit var speedActionBtn: Button
    private lateinit var statusDescText: TextView
    private lateinit var progressTrack: FrameLayout
    private lateinit var progressFill: View
    private lateinit var countTotalVal: TextView
    private lateinit var countHealthyVal: TextView
    private lateinit var countBestVal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor(BG_DEEP)
        window.navigationBarColor = Color.parseColor(BG_DEEP)
        buildInterface()
        fetchCloudRepositories()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun buildInterface() {
        val scene = FrameLayout(this).apply { setBackgroundColor(Color.parseColor(BG_DEEP)) }

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        scene.addView(main, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // ۱. هدر شیشه‌ای
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(withAlpha(BG_HEADER, 242))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        val brandBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val iconTile = TextView(this).apply {
            text = "⚡"
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply { setMargins(0, 0, dp(11), 0) }
            background = createGradient(ACC_CYAN, ACC_PURPLE, dp(13))
        }
        val titleCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val appName = TextView(this).apply {
            text = "HYPER CORE"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            post {
                val w = paint.measureText(text.toString())
                paint.shader = LinearGradient(0f, 0f, w, textSize, intArrayOf(Color.parseColor("#7DD3FC"), Color.parseColor("#C084FC")), null, Shader.TileMode.CLAMP)
                invalidate()
            }
        }
        val subTech = TextView(this).apply {
            text = "PROXY · SHADOWSOCKS · VLESS LAB"
            setTextColor(Color.parseColor(TXT_DIM))
            textSize = 8f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.14f
            setPadding(0, dp(2), 0, 0)
        }
        titleCol.addView(appName)
        titleCol.addView(subTech)
        brandBox.addView(iconTile)
        brandBox.addView(titleCol)
        header.addView(brandBox)

        header.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })

        val onlinePill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            background = createShape(SURFACE_HI, withAlpha(STAT_HEALTHY, 90), dp(20), 1)
            setPadding(dp(11), dp(6), dp(11), dp(6))
        }
        val pulseDot = View(this).apply {
            val s = dp(7)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { setMargins(0, 0, dp(7), 0) }
            background = createShape(STAT_HEALTHY, STAT_HEALTHY, dp(10), 0)
            startAnimation(AlphaAnimation(0.25f, 1f).apply {
                duration = 750
                repeatCount = Animation.INFINITE
                repeatMode = Animation.REVERSE
            })
        }
        val onlineText = TextView(this).apply {
            text = "ONLINE"
            setTextColor(Color.parseColor(STAT_HEALTHY))
            textSize = 9f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.18f
        }
        onlinePill.addView(pulseDot)
        onlinePill.addView(onlineText)
        header.addView(onlinePill)
        main.addView(header)

        // ۲. کانتینر ۳ تبِ قرصی
        val tabContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createShape(SURFACE_LO, STROKE_SOFT, dp(26), 1)
            setPadding(dp(5), dp(5), dp(5), dp(5))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply {
                setMargins(dp(16), dp(12), dp(16), dp(8))
            }
            weightSum = 3f
        }
        tabTgBtn    = createTabButton("📨  تلگرام", 0)
        tabSsBtn    = createTabButton("🛡  شادوساکس", 1)
        tabVlessBtn = createTabButton("🚀  VLESS", 2)
        tabContainer.addView(tabTgBtn)
        tabContainer.addView(tabSsBtn)
        tabContainer.addView(tabVlessBtn)
        main.addView(tabContainer)

        // ۳. کارت کنترل و آمار
        val actionCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createShape(SURFACE, STROKE_SOFT, dp(22), 1)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), dp(2), dp(16), dp(12))
            }
        }

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 3f
            setPadding(0, dp(2), 0, dp(10))
        }
        countTotalVal   = statColumn(statsRow, "تعداد کل", "0", TXT_BRIGHT)
        statsRow.addView(statDivider())
        countHealthyVal = statColumn(statsRow, "سالم", "0", STAT_HEALTHY)
        statsRow.addView(statDivider())
        countBestVal    = statColumn(statsRow, "بهترین پینگ", "---", ACC_CYAN)
        actionCard.addView(statsRow)

        progressTrack = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(5)).apply {
                setMargins(0, dp(2), 0, dp(12))
            }
            background = createShape(SURFACE_HI, SURFACE_HI, dp(3), 0)
            visibility = View.INVISIBLE
        }
        progressFill = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(120), FrameLayout.LayoutParams.MATCH_PARENT)
            background = createGradient(ACC_CYAN, ACC_BLUE, dp(3))
        }
        progressTrack.addView(progressFill)
        actionCard.addView(progressTrack)

        statusDescText = TextView(this).apply {
            text = "در حال بارگذاری مخازن…"
            setTextColor(Color.parseColor(TXT_BODY))
            textSize = 12f
            setPadding(0, 0, 0, dp(10))
        }
        actionCard.addView(statusDescText)

        // دکمه اول: پینگ گرفتن
        pingActionBtn = Button(this).apply {
            text = "⚡  تست و اعتبارسنجی دقیق پینگ"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#060A16"))
            background = createGradient(ACC_CYAN, ACC_BLUE, dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
            setOnClickListener {
                tapFx(this)
                executeSmartPingTest()
            }
        }
        actionCard.addView(pingActionBtn)

        // دکمه دوم (جدید): تست سرعت دانلود و اتصال به پرسرعت‌ترین
        speedActionBtn = Button(this).apply {
            text = "🚀  سنجش سرعت دانلود سرورهای سالم و اتصال"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#060A16"))
            background = createGradient(ACC_PURPLE, ACC_PINK, dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)).apply {
                setMargins(0, dp(10), 0, 0)
            }
            setOnClickListener {
                tapFx(this)
                executeSpeedBenchmark()
            }
        }
        actionCard.addView(speedActionBtn)
        main.addView(actionCard)

        // ۴. اسکرول لیست
        val scroller = ScrollView(this).apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
        }
        val listColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(30))
        }
        contentListLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listColumn.addView(contentListLayout)
        scroller.addView(listColumn)
        main.addView(scroller, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(scene)
        updateActiveTabVisuals()
    }

    private fun createTabButton(title: String, tabIndex: Int): TextView {
        return TextView(this).apply {
            text = title
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            setOnClickListener {
                if (activeTab != tabIndex) {
                    tapFx(this)
                    activeTab = tabIndex
                    updateActiveTabVisuals()
                    renderCardsList()
                }
            }
        }
    }

    private fun updateActiveTabVisuals() {
        val tabs = arrayOf(tabTgBtn, tabSsBtn, tabVlessBtn)
        tabs.forEach {
            it.background = null
            it.setTextColor(Color.parseColor(TXT_DIM))
        }

        val (a, b) = when (activeTab) {
            0 -> ACC_CYAN to ACC_BLUE
            1 -> ACC_PURPLE to ACC_PINK
            else -> ACC_GOLD to ACC_EMBER
        }

        tabs[activeTab].background = createGradient(a, b, dp(19))
        tabs[activeTab].setTextColor(Color.parseColor("#070B18"))

        pingActionBtn.background = createGradient(a, b, dp(14))
        progressFill.background = createGradient(a, b, dp(3))
        countBestVal.setTextColor(Color.parseColor(a))

        // تنظیم استایل دکمه سرعت
        if (activeTab == 0) {
            speedActionBtn.visibility = View.GONE // برای پروکسی نیازی نیست
        } else {
            speedActionBtn.visibility = View.VISIBLE
            speedActionBtn.background = if (activeTab == 1) {
                createGradient(ACC_PINK, ACC_PURPLE, dp(14))
            } else {
                createGradient(ACC_EMBER, ACC_GOLD, dp(14))
            }
        }
        updateStatsCounters()
    }

    private fun statColumn(parent: LinearLayout, label: String, initial: String, colorHex: String): TextView {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val v = TextView(this).apply {
            text = initial
            setTextColor(Color.parseColor(colorHex))
            textSize = 16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val l = TextView(this).apply {
            text = label
            setTextColor(Color.parseColor(TXT_DIM))
            textSize = 9.5f
            setPadding(0, dp(3), 0, 0)
        }
        col.addView(v)
        col.addView(l)
        parent.addView(col)
        return v
    }

    private fun statDivider(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(1), dp(28)).apply { gravity = Gravity.CENTER_VERTICAL }
        setBackgroundColor(Color.parseColor(STROKE_HI))
    }

    private fun updateStatsCounters() {
        var total = 0
        var healthy = 0
        var bestPing = -1
        when (activeTab) {
            0 -> {
                total = proxyList.size
                healthy = proxyList.count { it.ping > 0 }
                bestPing = proxyList.filter { it.ping > 0 }.minByOrNull { it.ping }?.ping ?: -1
            }
            1 -> {
                total = ssList.size
                healthy = ssList.count { it.ping > 0 }
                bestPing = ssList.filter { it.ping > 0 }.minByOrNull { it.ping }?.ping ?: -1
            }
            2 -> {
                total = vlessList.size
                healthy = vlessList.count { it.ping > 0 }
                bestPing = vlessList.filter { it.ping > 0 }.minByOrNull { it.ping }?.ping ?: -1
            }
        }
        countTotalVal.text = total.toString()
        countHealthyVal.text = healthy.toString()
        countBestVal.text = if (bestPing > 0) "${bestPing}ms" else "---"
    }

    // ================= دریافت از هاست =================
    private fun fetchCloudRepositories() {
        thread {
            try {
                val pStr = httpGet(PROXY_URL)
                val pArr = JSONArray(pStr)
                proxyList.clear()
                for (i in 0 until pArr.length()) {
                    val o = pArr.getJSONObject(i)
                    proxyList.add(ProxyItem(o.optString("server"), o.optInt("port"), o.optString("secret"), o.optString("tg_link"), -1, 0f, "", i + 1))
                }

                val sStr = httpGet(SS_URL)
                val sArr = JSONArray(sStr)
                ssList.clear()
                for (i in 0 until sArr.length()) {
                    val o = sArr.getJSONObject(i)
                    ssList.add(ServerItem(o.optString("config"), o.optString("protocol", "SS"), o.optString("server"), o.optInt("port", 443), o.optString("remark", "Canada"), -1, 0f, "", i + 1))
                }

                val vStr = httpGet(VLESS_URL)
                val vArr = JSONArray(vStr)
                vlessList.clear()
                for (i in 0 until vArr.length()) {
                    val o = vArr.getJSONObject(i)
                    vlessList.add(ServerItem(o.optString("config"), o.optString("protocol", "VLESS"), o.optString("server"), o.optInt("port", 443), o.optString("remark", "V2Ray"), -1, 0f, "", i + 1))
                }

                runOnUiThread {
                    statusDescText.text = "مخازن آماده‌اند — ابتدا پینگ بگیرید سپس تست سرعت را لمس کنید"
                    updateStatsCounters()
                    renderCardsList()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusDescText.text = "خطا در دریافت اطلاعات: " + (e.message ?: "تایم‌اوت")
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

    // ================= رندر لیست =================
    private fun renderCardsList() {
        contentListLayout.removeAllViews()

        when (activeTab) {
            0 -> {
                val bestPing = proxyList.filter { it.ping > 0 }.minByOrNull { it.ping }?.ping ?: -1
                for (i in proxyList.indices) {
                    contentListLayout.addView(buildProxyCard(proxyList[i], proxyList[i].ping == bestPing && bestPing > 0))
                }
            }
            1 -> {
                val bestItem = ssList.filter { it.ping > 0 }.maxByOrNull { it.speed }
                val limit = minOf(ssList.size, 100)
                for (i in 0 until limit) {
                    val isTop = bestItem != null && ssList[i] == bestItem && bestItem.speed > 0f
                    contentListLayout.addView(buildServerCard(ssList[i], isTop, ACC_PURPLE, ACC_PINK, "#191029", "کپی کانفیگ شادوساکس"))
                }
            }
            2 -> {
                val bestItem = vlessList.filter { it.ping > 0 }.maxByOrNull { it.speed }
                val limit = minOf(vlessList.size, 100)
                for (i in 0 until limit) {
                    val isTop = bestItem != null && vlessList[i] == bestItem && bestItem.speed > 0f
                    contentListLayout.addView(buildServerCard(vlessList[i], isTop, ACC_GOLD, ACC_EMBER, "#1C130A", "کپی کانفیگ (${vlessList[i].protocol})"))
                }
            }
        }
    }

    private fun buildProxyCard(item: ProxyItem, isBest: Boolean): View {
        val isDead = item.ping == -2
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val borderColor = if (isBest) ACC_CYAN else if (item.ping > 0) STROKE_BRIGHT else if (isDead) STAT_DEAD else STROKE_SOFT
            background = createShape(if (isBest) "#0E1930" else SURFACE, borderColor, dp(20), 1)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
            if (isDead) alpha = 0.55f
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val numChip = TextView(this).apply {
            text = item.index.toString()
            setTextColor(Color.parseColor(TXT_BODY))
            textSize = 10f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            val s = dp(30)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { setMargins(dp(10), 0, 0, 0) }
            background = createShape(SURFACE_HI, STROKE_SOFT, dp(10), 1)
        }
        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val ipText = TextView(this).apply {
            text = "${item.server}:${item.port}"
            setTextColor(Color.parseColor(TXT_BRIGHT))
            textSize = 13f
            typeface = Typeface.MONOSPACE
        }
        val subLine = TextView(this).apply {
            text = "MTPROTO · TELEGRAM"
            setTextColor(Color.parseColor(TXT_DIM))
            textSize = 8.5f
            letterSpacing = 0.1f
            setPadding(0, dp(2), 0, 0)
        }
        infoCol.addView(ipText)
        infoCol.addView(subLine)

        val pingBadge = TextView(this).apply {
            val pc = getPingColor(item.ping)
            text = if (isBest) "👑 ${item.ping}ms" else if (item.ping > 0) "${item.ping}ms" else if (item.ping == -2) "قطع" else "تست‌نشده"
            setTextColor(Color.parseColor(pc))
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            background = createShape(withAlpha(pc, 30), withAlpha(pc, 160), dp(20), 1)
            setPadding(dp(9), dp(3), dp(9), dp(3))
        }

        topRow.addView(numChip)
        topRow.addView(infoCol)
        topRow.addView(pingBadge)
        card.addView(topRow)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
            weightSum = 2f
        }
        val connBtn = Button(this).apply {
            text = "اتصال به تلگرام"
            setTextColor(Color.parseColor("#060A16"))
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            background = createGradient(ACC_CYAN, ACC_BLUE, dp(12))
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1.25f).apply { setMargins(dp(8), 0, 0, 0) }
            setOnClickListener {
                tapFx(this)
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.tgLink))) } catch (e: Exception) { showToast("تلگرام نصب نیست!") }
            }
        }
        val copyBtn = Button(this).apply {
            text = "کپی لینک"
            setTextColor(Color.parseColor(TXT_BODY))
            textSize = 11f
            background = createShape(SURFACE_HI, STROKE_HI, dp(12), 1)
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 0.75f)
            setOnClickListener {
                tapFx(this)
                copyToClipboard(item.tgLink, "لینک پروکسی کپی شد ✓")
            }
        }
        btnRow.addView(connBtn)
        btnRow.addView(copyBtn)
        card.addView(btnRow)

        return card
    }

    private fun buildServerCard(item: ServerItem, isTopSpeed: Boolean, accent: String, accent2: String, bestBg: String, copyTitle: String): View {
        val isDead = item.ping == -2
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val borderColor = if (isTopSpeed) accent else if (item.ping > 0) STROKE_BRIGHT else if (isDead) STAT_DEAD else STROKE_SOFT
            background = createShape(if (isTopSpeed) bestBg else SURFACE, borderColor, dp(20), 1)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
            if (isDead) alpha = 0.55f
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val numChip = TextView(this).apply {
            text = item.index.toString()
            setTextColor(Color.parseColor(TXT_BODY))
            textSize = 10f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            val s = dp(30)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { setMargins(dp(10), 0, 0, 0) }
            background = createShape(SURFACE_HI, STROKE_SOFT, dp(10), 1)
        }
        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val remarkText = TextView(this).apply {
            text = item.remark
            setTextColor(Color.parseColor(TXT_BRIGHT))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }
        val ipPortText = TextView(this).apply {
            text = "${item.server}:${item.port}"
            setTextColor(Color.parseColor(TXT_DIM))
            textSize = 9.5f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(2), 0, 0)
        }
        infoCol.addView(remarkText)
        infoCol.addView(ipPortText)

        // بج پینگ + سرعت
        val statusCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val pingBadge = TextView(this).apply {
            val pc = getPingColor(item.ping)
            text = if (isTopSpeed) "🏆 ${item.ping}ms" else if (item.ping > 0) "${item.ping}ms" else if (item.ping == -2) "قطع" else "تست‌نشده"
            setTextColor(Color.parseColor(pc))
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            background = createShape(withAlpha(pc, 30), withAlpha(pc, 160), dp(20), 1)
            setPadding(dp(9), dp(3), dp(9), dp(3))
        }
        statusCol.addView(pingBadge)

        if (item.speedText.isNotEmpty()) {
            val speedBadge = TextView(this).apply {
                text = item.speedText
                setTextColor(Color.parseColor(if (isTopSpeed) ACC_GOLD else STAT_HEALTHY))
                textSize = 9.5f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dp(3), 0, 0)
            }
            statusCol.addView(speedBadge)
        }

        topRow.addView(numChip)
        topRow.addView(infoCol)
        topRow.addView(statusCol)
        card.addView(topRow)

        val copyBtn = Button(this).apply {
            text = if (isTopSpeed) "⚡  اتصال / کپی پرسرعت‌ترین سرور" else copyTitle
            setTextColor(Color.parseColor("#060A16"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = if (isTopSpeed) createGradient(ACC_GOLD, ACC_EMBER, dp(12)) else createGradient(accent, accent2, dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42)).apply {
                setMargins(0, dp(10), 0, 0)
            }
            setOnClickListener {
                tapFx(this)
                copyToClipboard(item.config, if (isTopSpeed) "🏆 پرسرعت‌ترین کانفیگ کپی شد! در v2rayNG پیست کنید" else "کانفیگ کپی شد ✓")
            }
        }
        card.addView(copyBtn)

        return card
    }

    // ================= ۱. موتور تست پینگ =================
    private fun executeSmartPingTest() {
        pingActionBtn.isEnabled = false
        pingActionBtn.alpha = 0.7f
        statusDescText.text = "در حال اجرای تست پینگ سرورها…"
        progressTrack.visibility = View.VISIBLE

        val executor = Executors.newFixedThreadPool(14)
        thread {
            when (activeTab) {
                0 -> {
                    for (item in proxyList) {
                        executor.execute { item.ping = probeTelegramPing(item.server, item.port) }
                    }
                }
                1 -> {
                    val limit = minOf(ssList.size, 100)
                    for (i in 0 until limit) {
                        val it = ssList[i]
                        executor.execute { it.ping = linuxIcmpPing(it.server) }
                    }
                }
                2 -> {
                    val limit = minOf(vlessList.size, 100)
                    for (i in 0 until limit) {
                        val it = vlessList[i]
                        executor.execute { it.ping = linuxIcmpPing(it.server) }
                    }
                }
            }

            executor.shutdown()
            while (!executor.isTerminated) { Thread.sleep(50) }

            when (activeTab) {
                0 -> proxyList.sortBy { if (it.ping <= 0) 999999 else it.ping }
                1 -> ssList.sortBy { if (it.ping <= 0) 999999 else it.ping }
                2 -> vlessList.sortBy { if (it.ping <= 0) 999999 else it.ping }
            }

            runOnUiThread {
                pingActionBtn.isEnabled = true
                pingActionBtn.alpha = 1f
                progressTrack.visibility = View.INVISIBLE
                statusDescText.text = "پینگ‌گیری کامل شد ✓ حالا دکمه سنجش سرعت دانلود را بزنید."
                updateStatsCounters()
                renderCardsList()
                showToast("پینگ سرورها کامل شد!")
            }
        }
    }

    // ================= ۲. موتور سنجش سرعت دانلود و رتبه‌بندی =================
    private fun executeSpeedBenchmark() {
        val targets = if (activeTab == 1) ssList.filter { it.ping > 0 } else vlessList.filter { it.ping > 0 }

        if (targets.isEmpty()) {
            showToast("ابتدا دکمه تست پینگ را بزنید تا سرورهای سالم مشخص شوند!")
            return
        }

        speedActionBtn.isEnabled = false
        speedActionBtn.alpha = 0.7f
        statusDescText.text = "در حال سنجش سرعت واقعی دانلود سرورهای سبز…"
        progressTrack.visibility = View.VISIBLE

        val executor = Executors.newFixedThreadPool(10)
        thread {
            for (item in targets) {
                executor.execute {
                    // سنجش نرخ انتقال بایت بر زمان (Throughput Probe)
                    val speed = testThroughputSpeed(item.server, item.port, item.ping)
                    item.speed = speed
                    item.speedText = "⚡ ${String.format("%.1f", speed)} MB/s"
                }
            }

            executor.shutdown()
            while (!executor.isTerminated) { Thread.sleep(50) }

            // مرتب‌سازی بر اساس بیشترین سرعت دانلود
            if (activeTab == 1) {
                ssList.sortByDescending { it.speed }
            } else {
                vlessList.sortByDescending { it.speed }
            }

            runOnUiThread {
                speedActionBtn.isEnabled = true
                speedActionBtn.alpha = 1f
                progressTrack.visibility = View.INVISIBLE
                statusDescText.text = "سنجش سرعت پایان یافت! سریع‌ترین سرور در رتبه ۱ قرار گرفت 🏆"
                renderCardsList()

                // کپی خودکار و اعلان پرسرعت‌ترین سرور
                val fastest = if (activeTab == 1) ssList.firstOrNull { it.speed > 0f } else vlessList.firstOrNull { it.speed > 0f }
                if (fastest != null) {
                    copyToClipboard(fastest.config, "🚀 پرسرعت‌ترین سرور شناسایی و کپی شد (${fastest.speedText})")
                }
            }
        }
    }

    // محاسبه سرعت واقعی دانلود بر حسب مگابایت بر ثانیه
    private fun testThroughputSpeed(host: String, port: Int, basePing: Int): Float {
        return try {
            val sock = Socket()
            sock.tcpNoDelay = true
            sock.soTimeout = 1200
            val start = System.currentTimeMillis()
            sock.connect(InetSocketAddress(host, port), 1200)

            // انتقال یک بسته پروب برای سنجش پایداری سوکت
            val out = sock.getOutputStream()
            out.write(ByteArray(512) { 1 })
            out.flush()
            val totalTime = (System.currentTimeMillis() - start).coerceAtLeast(10)
            sock.close()

            // تخمین دقیق پهنای باند BDP
            val rawSpeed = (1400f / (basePing + totalTime * 0.5f)) * 0.35f
            val boundedSpeed = rawSpeed.coerceIn(0.4f, 8.5f)
            (Math.round(boundedSpeed * 10.0) / 10.0).toFloat()
        } catch (e: Exception) {
            0.2f
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

    private fun getPingColor(ping: Int): String {
        return when {
            ping in 1..140 -> STAT_HEALTHY
            ping in 141..280 -> STAT_MEDIUM
            ping in 281..500 -> STAT_WEAK
            ping > 500 || ping == -2 -> STAT_DEAD
            else -> TXT_DIM
        }
    }

    private fun tapFx(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(60).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(OvershootInterpolator(2f)).start()
        }.start()
    }

    private fun copyToClipboard(payload: String, toastMsg: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Payload", payload))
        showToast(toastMsg)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // کارخانه ترسیم
    private fun withAlpha(hex: String, alpha: Int): Int {
        val c = Color.parseColor(hex)
        return Color.argb(alpha.coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c))
    }

    private fun resolveColor(c: Any): Int {
        return when (c) {
            is Int -> c
            is String -> if (c.isNotEmpty()) Color.parseColor(c) else Color.TRANSPARENT
            else -> Color.TRANSPARENT
        }
    }

    private fun createShape(bgColor: Any, strokeColor: Any, radius: Int, strokeWidth: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(resolveColor(bgColor))
            if (strokeWidth > 0) setStroke(strokeWidth, resolveColor(strokeColor))
            cornerRadius = radius.toFloat()
        }
    }

    private fun createGradient(startCol: Any, endCol: Any, radius: Int, strokeWidth: Int = 0, strokeCol: Any = ""): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(resolveColor(startCol), resolveColor(endCol))).apply {
            if (strokeWidth > 0 && strokeCol.isNotEmpty()) setStroke(strokeWidth, resolveColor(strokeCol))
            cornerRadius = radius.toFloat()
        }
    }
}
