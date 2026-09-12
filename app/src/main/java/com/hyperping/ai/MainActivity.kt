package com.hyperping.ai

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
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
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executors
import kotlin.concurrent.thread

// ═══════════════════════════════════════════════════════════════════
//  ⚡ HYPERPING — AURORA LUXE EDITION
//  تم «شبِ اورورا»: عمق کهکشانی، نورهای شفق، درخشش سه‌لایه
// ═══════════════════════════════════════════════════════════════════

class MainActivity : Activity() {

    private val PROXY_URL = "https://p.sosiss.ir/data_proxy.json"
    private val SS_URL    = "https://p.sosiss.ir/data_ss.json"
    private val VLESS_URL = "https://p.sosiss.ir/data_vless.json"

    // ─────────── پالت: کهکشان شب ───────────
    private val BG_DEEP     = "#05060E"   // عمیق‌ترین لایه
    private val BG_HEADER   = "#0A0E1C"   // هدر نیمه‌شفاف
    private val SURFACE     = "#0C1120"   // سطح کارت‌ها
    private val SURFACE_HI  = "#141B31"   // سطح برجسته
    private val SURFACE_LO  = "#090C18"   // سطح فرورفته
    private val STROKE_SOFT = "#1C2439"
    private val STROKE_HI   = "#2B3550"
    private val TXT_BRIGHT  = "#F5F8FF"
    private val TXT_BODY    = "#98A5C3"
    private val TXT_DIM     = "#5C6884"

    // ─────────── لهجه‌های تب‌ها ───────────
    private val ACC_CYAN   = "#22D3EE"    // تلگرام
    private val ACC_BLUE   = "#3B82F6"
    private val ACC_PURPLE = "#A855F7"    // شادوساکس
    private val ACC_PINK   = "#EC4899"
    private val ACC_GOLD   = "#FFC94A"    // VLESS (طلای لوکس)
    private val ACC_EMBER  = "#F97066"

    // ─────────── وضعیت پینگ ───────────
    private val STAT_HEALTHY = "#34D399"
    private val STAT_MEDIUM  = "#FBBF24"
    private val STAT_WEAK    = "#FB923C"
    private val STAT_DEAD    = "#FB4E6D"

    data class ProxyItem(val server: String, val port: Int, val secret: String, val tgLink: String, var ping: Int = -1, var index: Int = 0)
    data class ServerItem(val config: String, val protocol: String, val server: String, val port: Int, val remark: String, var ping: Int = -1, var index: Int = 0)

    private val proxyList = ArrayList<ProxyItem>()
    private val ssList    = ArrayList<ServerItem>()
    private val vlessList = ArrayList<ServerItem>()

    private var activeTab = 0 // 0=تلگرام, 1=شادوساکس, 2=VLESS

    private lateinit var contentListLayout: LinearLayout
    private lateinit var tabTgBtn: TextView
    private lateinit var tabSsBtn: TextView
    private lateinit var tabVlessBtn: TextView
    private lateinit var actionBtn: Button
    private lateinit var statusDescText: TextView
    private lateinit var progressTrack: FrameLayout
    private lateinit var progressFill: View
    private lateinit var ctaGlow: View
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

    // ═══════════════════════ ساخت صحنه ═══════════════════════

    private fun buildInterface() {
        // ─── صحنه: فریم اصلی برای لایه‌های نور شفق ───
        val scene = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor(BG_DEEP))
        }

        // هاله‌های شفق — نورهای زنده‌ی پس‌زمینه
        scene.addView(auroraBlob(ACC_CYAN,   330, -dp(120), -dp(150), 0,      0,      65, 4200))
        scene.addView(auroraBlob(ACC_PURPLE, 300,  0,      -dp(130), -dp(120), 0,      55, 5200))
        scene.addView(auroraBlob(ACC_BLUE,   240,  dp(40),  dp(110),  dp(40),  0,      35, 6400))

        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        scene.addView(main, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // ─────────── ۱. هدر شیشه‌ای ───────────
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

        // لوگو با هاله‌ی تپنده
        val logoGlow = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL
            colors = intArrayOf(withAlpha(ACC_CYAN, 110), Color.TRANSPARENT)
            gradientRadius = dp(30).toFloat()
        }
        val logoTileBg = LayerDrawable(arrayOf(logoGlow, createGradient(ACC_CYAN, ACC_PURPLE, dp(13)))).apply {
            setLayerInset(0, -dp(9), -dp(9), -dp(9), -dp(9))
        }
        val iconTile = TextView(this).apply {
            text = "⚡"
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply { setMargins(0, 0, dp(11), 0) }
            background = logoTileBg
        }
        pulseScale(iconTile, 1.06f, 2400)

        val titleCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val appName = TextView(this).apply {
            text = "HYPER CORE"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.06f
            // گرادیان متنی لوکس
            post {
                val w = paint.measureText(text.toString())
                paint.shader = LinearGradient(
                    0f, 0f, w, textSize,
                    intArrayOf(Color.parseColor("#7DD3FC"), Color.parseColor("#C084FC")),
                    null, Shader.TileMode.CLAMP
                )
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

        // قرص وضعیت آنلاین
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

        // ─────────── خط سه‌رنگ با برق متحرک ───────────
        val accentLine = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3))
        }
        val lineBase = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor(ACC_CYAN), Color.parseColor(ACC_PURPLE), Color.parseColor(ACC_GOLD))
            )
        }
        val lineShine = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(64), FrameLayout.LayoutParams.MATCH_PARENT)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, withAlpha("#FFFFFF", 95), Color.TRANSPARENT)
            )
            translationX = -dp(64).toFloat()
        }
        accentLine.addView(lineBase)
        accentLine.addView(lineShine)
        main.addView(accentLine)
        loopShine(lineShine)

        // ─────────── ۲. کانتینر تب‌های قرصی ───────────
        val tabContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createShape(SURFACE_LO, STROKE_SOFT, dp(26), 1)
            setPadding(dp(5), dp(5), dp(5), dp(5))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply {
                setMargins(dp(16), dp(14), dp(16), dp(10))
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

        // ─────────── ۳. کارت کنترل مرکزی ───────────
        val actionCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createShape(SURFACE, STROKE_SOFT, dp(22), 1)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), dp(2), dp(16), dp(12))
            }
        }

        val monitorLabel = TextView(this).apply {
            text = "LIVE · NETWORK MONITOR"
            setTextColor(Color.parseColor(TXT_DIM))
            textSize = 8f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.26f
            setPadding(0, 0, 0, dp(12))
        }
        actionCard.addView(monitorLabel)

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

        // نوار پیشرفت نامعین با حرکت پیوسته
        progressTrack = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(5)).apply {
                setMargins(0, dp(2), 0, dp(12))
            }
            background = createShape(SURFACE_HI, SURFACE_HI, dp(3), 0)
            clipChildren = true
            visibility = View.INVISIBLE
        }
        progressFill = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(110), FrameLayout.LayoutParams.MATCH_PARENT)
            background = createGradient(ACC_CYAN, ACC_BLUE, dp(3))
        }
        progressTrack.addView(progressFill)
        actionCard.addView(progressTrack)
        progressTrack.post {
            val w = progressTrack.width
            ObjectAnimator.ofFloat(progressFill, View.TRANSLATION_X, -dp(110).toFloat(), (w + dp(110)).toFloat()).apply {
                duration = 950
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.RESTART
                interpolator = LinearInterpolator()
                start()
            }
        }

        statusDescText = TextView(this).apply {
            text = "در حال اتصال به مخازن سه‌گانه…"
            setTextColor(Color.parseColor(TXT_BODY))
            textSize = 12f
            setPadding(0, 0, 0, dp(12))
        }
        actionCard.addView(statusDescText)

        // دکمه‌ی CTA با هاله‌ی تنفسی + برق عبوری
        val ctaWrap = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54))
        }
        ctaGlow = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                setMargins(-dp(7), -dp(7), -dp(7), -dp(7))
            }
        }
        breathe(ctaGlow, 0.45f, 0.95f, 2600)

        actionBtn = Button(this).apply {
            text = "⚡  تست و اعتبارسنجی دقیق پینگ"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            stateListAnimator = null
            setTextColor(Color.parseColor("#060A16"))
            background = createGradient(ACC_CYAN, ACC_BLUE, dp(16))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setOnClickListener {
                tapFx(this)
                executeSmartPingTest()
            }
        }

        val ctaShine = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(dp(56), dp(92), Gravity.CENTER_VERTICAL)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, withAlpha("#FFFFFF", 70), Color.TRANSPARENT)
            )
            rotation = 14f
            translationX = -dp(56).toFloat()
            isClickable = false
            isFocusable = false
        }

        ctaWrap.addView(ctaGlow)
        ctaWrap.addView(actionBtn)
        ctaWrap.addView(ctaShine)
        actionCard.addView(ctaWrap)
        loopShine(ctaShine)
        main.addView(actionCard)

        // ─────────── ۴. لیست با محوشدگی لبه‌ها ───────────
        val scroller = object : ScrollView(this) {
            override fun getSolidColor(): Int = Color.parseColor(BG_DEEP)
        }.apply {
            overScrollMode = View.OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            isVerticalFadingEdgeEnabled = true
            setFadingEdgeLength(dp(42))
        }

        val listColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(6), dp(16), dp(30))
        }
        contentListLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val footer = TextView(this).apply {
            text = "HYPERPING · AURORA EDITION"
            setTextColor(Color.parseColor(TXT_DIM))
            textSize = 8f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.3f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(18), 0, 0)
            }
        }

        listColumn.addView(contentListLayout)
        listColumn.addView(footer)
        scroller.addView(listColumn)
        main.addView(scroller, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(scene)
        updateActiveTabVisuals()
    }

    // ═══════════════════════ تب‌ها ═══════════════════════

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

    private fun activeTabBg(a: String, b: String): Drawable {
        val halo = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(dp(5), withAlpha(a, 70))
            cornerRadius = dp(23).toFloat()
        }
        val pill = createGradient(a, b, dp(19))
        return LayerDrawable(arrayOf(halo, pill)).apply {
            setLayerInset(0, -dp(4), -dp(4), -dp(4), -dp(4))
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

        val active = tabs[activeTab]
        active.background = activeTabBg(a, b)
        active.setTextColor(Color.parseColor("#070B18"))

        actionBtn.background = createGradient(a, b, dp(16))
        progressFill.background = createGradient(a, b, dp(3))
        ctaGlow.background = glowBackdrop(a)
        countBestVal.setTextColor(Color.parseColor(a))

        updateStatsCounters()
    }

    // ═══════════════════════ آمار ═══════════════════════

    private fun statColumn(parent: LinearLayout, label: String, initial: String, colorHex: String): TextView {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val v = TextView(this).apply {
            text = initial
            setTextColor(Color.parseColor(colorHex))
            textSize = 16.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val l = TextView(this).apply {
            text = label
            setTextColor(Color.parseColor(TXT_DIM))
            textSize = 9.5f
            letterSpacing = 0.04f
            setPadding(0, dp(3), 0, 0)
        }
        col.addView(v)
        col.addView(l)
        parent.addView(col)
        return v
    }

    private fun statDivider(): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(1), dp(30)).apply { gravity = Gravity.CENTER_VERTICAL }
        background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.TRANSPARENT, Color.parseColor(STROKE_HI), Color.TRANSPARENT)
        )
    }

    private fun animateNumber(tv: TextView, target: Int, suffix: String = "") {
        val current = tv.text.toString().filter { it.isDigit() }.toIntOrNull() ?: 0
        if (current == target) { tv.text = "$target$suffix"; return }
        ValueAnimator.ofInt(current, target).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
            addUpdateListener { a -> tv.text = "${a.animatedValue as Int}$suffix" }
            start()
        }
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
        animateNumber(countTotalVal, total)
        animateNumber(countHealthyVal, healthy)
        if (bestPing > 0) animateNumber(countBestVal, bestPing, "ms") else countBestVal.text = "---"
    }

    // ═══════════════════════ دریافت مخازن ═══════════════════════

    private fun fetchCloudRepositories() {
        thread {
            runOnUiThread {
                showSkeleton()
                statusDescText.text = "در حال دریافت مخازن سه‌گانه از کلاود…"
            }
            try {
                // ۱. تلگرام
                val pStr = httpGet(PROXY_URL)
                val pArr = JSONArray(pStr)
                proxyList.clear()
                for (i in 0 until pArr.length()) {
                    val o = pArr.getJSONObject(i)
                    proxyList.add(ProxyItem(o.optString("server"), o.optInt("port"), o.optString("secret"), o.optString("tg_link"), -1, i + 1))
                }

                // ۲. شادوساکس
                val sStr = httpGet(SS_URL)
                val sArr = JSONArray(sStr)
                ssList.clear()
                for (i in 0 until sArr.length()) {
                    val o = sArr.getJSONObject(i)
                    ssList.add(ServerItem(o.optString("config"), o.optString("protocol", "SS"), o.optString("server"), o.optInt("port", 443), o.optString("remark", "Canada"), -1, i + 1))
                }

                // ۳. VLESS
                val vStr = httpGet(VLESS_URL)
                val vArr = JSONArray(vStr)
                vlessList.clear()
                for (i in 0 until vArr.length()) {
                    val o = vArr.getJSONObject(i)
                    vlessList.add(ServerItem(o.optString("config"), o.optString("protocol", "VLESS"), o.optString("server"), o.optInt("port", 443), o.optString("remark", "V2Ray"), -1, i + 1))
                }

                runOnUiThread {
                    statusDescText.text = "مخازن آماده‌اند — برای سنجش دقیق پینگ، دکمه را لمس کن"
                    updateStatsCounters()
                    renderCardsList()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    contentListLayout.removeAllViews()
                    contentListLayout.addView(emptyState("📡", "اتصال برقرار نشد", "خطا: " + (e.message ?: "تایم‌اوت"), STAT_DEAD))
                    statusDescText.text = "خطا در دریافت اطلاعات — اتصال شبکه را بررسی کن"
                    showFancyToast("دریافت مخازن ناموفق بود", STAT_DEAD)
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

    // ═══════════════════════ رندر لیست ═══════════════════════

    private fun renderCardsList() {
        contentListLayout.removeAllViews()

        when (activeTab) {
            0 -> {
                contentListLayout.addView(sectionHeader("مخزن تلگرام — MTProto", proxyList.size, ACC_CYAN))
                if (proxyList.isEmpty()) {
                    contentListLayout.addView(emptyState("📨", "مخزن تلگرام خالی است", "هنوز سروری برای نمایش دریافت نشده", ACC_CYAN))
                    return
                }
                val best = proxyList.filter { it.ping > 0 }.minByOrNull { it.ping }?.ping ?: -1
                for (i in proxyList.indices) {
                    val card = buildProxyCard(proxyList[i], proxyList[i].ping == best && best > 0)
                    contentListLayout.addView(card)
                    animateEntrance(card, i)
                }
            }
            1 -> {
                contentListLayout.addView(sectionHeader("مخزن شادوساکس — Shadowsocks", ssList.size, ACC_PURPLE))
                if (ssList.isEmpty()) {
                    contentListLayout.addView(emptyState("🛡", "مخزن شادوساکس خالی است", "هنوز سروری برای نمایش دریافت نشده", ACC_PURPLE))
                    return
                }
                val limit = minOf(ssList.size, 100)
                val best = ssList.filter { it.ping > 0 }.minByOrNull { it.ping }?.ping ?: -1
                for (i in 0 until limit) {
                    val card = buildServerCard(ssList[i], ssList[i].ping == best && best > 0, ACC_PURPLE, ACC_PINK, "#191029", "کپی کانفیگ شادوساکس")
                    contentListLayout.addView(card)
                    animateEntrance(card, i)
                }
            }
            2 -> {
                contentListLayout.addView(sectionHeader("مخزن VLESS — V2Ray", vlessList.size, ACC_GOLD))
                if (vlessList.isEmpty()) {
                    contentListLayout.addView(emptyState("🚀", "مخزن VLESS خالی است", "هنوز سروری برای نمایش دریافت نشده", ACC_GOLD))
                    return
                }
                val limit = minOf(vlessList.size, 100)
                val best = vlessList.filter { it.ping > 0 }.minByOrNull { it.ping }?.ping ?: -1
                for (i in 0 until limit) {
                    val card = buildServerCard(vlessList[i], vlessList[i].ping == best && best > 0, ACC_GOLD, ACC_EMBER, "#1C130A", "کپی کانفیگ V2Ray (${vlessList[i].protocol})")
                    contentListLayout.addView(card)
                    animateEntrance(card, i)
                }
            }
        }
    }

    private fun sectionHeader(title: String, count: Int, accent: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(4), 0, dp(14))
            }
        }
        val dot = View(this).apply {
            val s = dp(9)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { setMargins(dp(2), 0, dp(8), 0) }
            background = createShape(accent, accent, dp(5), 0)
        }
        val t = TextView(this).apply {
            text = title
            setTextColor(Color.parseColor(TXT_BRIGHT))
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        val line = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, dp(1), 1f).apply { setMargins(dp(10), 0, dp(10), 0) }
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, Color.parseColor(STROKE_HI), Color.TRANSPARENT)
            )
        }
        val chip = TextView(this).apply {
            text = "$count مورد"
            setTextColor(Color.parseColor(TXT_BODY))
            textSize = 9.5f
            typeface = Typeface.MONOSPACE
            background = createShape(SURFACE_HI, STROKE_SOFT, dp(20), 1)
            setPadding(dp(9), dp(3), dp(9), dp(3))
        }
        row.addView(dot)
        row.addView(t)
        row.addView(line)
        row.addView(chip)
        return row
    }

    private fun emptyState(icon: String, title: String, sub: String, accent: String): View {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(48), dp(24), dp(48))
        }
        val circle = TextView(this).apply {
            text = icon
            textSize = 26f
            gravity = Gravity.CENTER
            val s = dp(72)
            layoutParams = LinearLayout.LayoutParams(s, s)
            background = createShape(SURFACE_HI, withAlpha(accent, 130), dp(36), 1)
        }
        val t = TextView(this).apply {
            text = title
            setTextColor(Color.parseColor(TXT_BRIGHT))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(4))
        }
        val s = TextView(this).apply {
            text = sub
            setTextColor(Color.parseColor(TXT_DIM))
            textSize = 11f
            gravity = Gravity.CENTER
        }
        col.addView(circle)
        col.addView(t)
        col.addView(s)
        breathe(col, 0.72f, 1f, 2000)
        return col
    }

    // ═══════════════════════ کارت‌ها ═══════════════════════

    private fun buildProxyCard(item: ProxyItem, isBest: Boolean): View {
        val isDead = item.ping == -2
        val isTested = item.ping > 0

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
            isClickable = true
            if (isDead) alpha = 0.55f
        }
        val bg: Drawable = when {
            isBest   -> glowCardBg("#0E1930", ACC_CYAN, dp(20))
            isDead   -> createShape("#140B12", withAlpha(STAT_DEAD, 110), dp(20), 1)
            isTested -> createShape(SURFACE, STROKE_HI, dp(20), 1)
            else     -> createShape(SURFACE, STROKE_SOFT, dp(20), 1)
        }
        card.background = RippleDrawable(ColorStateList.valueOf(withAlpha(ACC_CYAN, 40)), bg, null)

        // ردیف بالا
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val numChip = TextView(this).apply {
            text = item.index.toString()
            setTextColor(if (isBest) Color.parseColor("#060A16") else Color.parseColor(TXT_BODY))
            textSize = 10.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            val s = dp(32)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { setMargins(dp(12), 0, 0, 0) }
            background = if (isBest) createGradient(ACC_CYAN, ACC_BLUE, dp(11)) else createShape(SURFACE_HI, STROKE_SOFT, dp(11), 1)
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
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MIDDLE
        }
        val subLine = TextView(this).apply {
            text = "MTPROTO · TELEGRAM"
            setTextColor(Color.parseColor(TXT_DIM))
            textSize = 8.5f
            letterSpacing = 0.14f
            setPadding(0, dp(3), 0, 0)
        }
        infoCol.addView(ipText)
        infoCol.addView(subLine)

        val statusCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val pingBadge = TextView(this).apply {
            val pc = getPingColor(item.ping)
            text = getPingText(item.ping, isBest)
            setTextColor(Color.parseColor(pc))
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            background = createShape(withAlpha(pc, 30), withAlpha(pc, 160), dp(20), 1)
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }
        statusCol.addView(pingBadge)
        statusCol.addView(createSignalBar(item.ping))

        topRow.addView(numChip)
        topRow.addView(infoCol)
        topRow.addView(statusCol)
        card.addView(topRow)
        card.addView(createDivider())

        // دکمه‌ها
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 2f
            setPadding(0, dp(11), 0, 0)
        }
        val connBtn = Button(this).apply {
            text = "اتصال به تلگرام"
            setTextColor(Color.parseColor("#060A16"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            stateListAnimator = null
            background = createGradient(ACC_CYAN, ACC_BLUE, dp(13))
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1.25f).apply { setMargins(dp(8), 0, 0, 0) }
            setOnClickListener {
                tapFx(this)
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.tgLink)))
                } catch (e: Exception) {
                    showFancyToast("تلگرام نصب نیست!", STAT_DEAD)
                }
            }
        }
        val copyBtn = Button(this).apply {
            text = "کپی لینک"
            setTextColor(Color.parseColor(TXT_BODY))
            textSize = 11.5f
            isAllCaps = false
            stateListAnimator = null
            background = createShape(SURFACE_HI, STROKE_HI, dp(13), 1)
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 0.75f)
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

    private fun buildServerCard(item: ServerItem, isBest: Boolean, accent: String, accent2: String, bestBg: String, copyTitle: String): View {
        val isDead = item.ping == -2
        val isTested = item.ping > 0

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
            isClickable = true
            if (isDead) alpha = 0.55f
        }
        val bg: Drawable = when {
            isBest   -> glowCardBg(bestBg, accent, dp(20))
            isDead   -> createShape("#140B12", withAlpha(STAT_DEAD, 110), dp(20), 1)
            isTested -> createShape(SURFACE, STROKE_HI, dp(20), 1)
            else     -> createShape(SURFACE, STROKE_SOFT, dp(20), 1)
        }
        card.background = RippleDrawable(ColorStateList.valueOf(withAlpha(accent, 40)), bg, null)

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val numChip = TextView(this).apply {
            text = item.index.toString()
            setTextColor(if (isBest) Color.parseColor("#060A16") else Color.parseColor(TXT_BODY))
            textSize = 10.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER
            val s = dp(32)
            layoutParams = LinearLayout.LayoutParams(s, s).apply { setMargins(dp(12), 0, 0, 0) }
            background = if (isBest) createGradient(accent, accent2, dp(11)) else createShape(SURFACE_HI, STROKE_SOFT, dp(11), 1)
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
            setPadding(0, dp(3), 0, 0)
        }
        infoCol.addView(remarkText)
        infoCol.addView(ipPortText)

        val statusCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val pingBadge = TextView(this).apply {
            val pc = getPingColor(item.ping)
            text = getPingText(item.ping, isBest)
            setTextColor(Color.parseColor(pc))
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            background = createShape(withAlpha(pc, 30), withAlpha(pc, 160), dp(20), 1)
            setPadding(dp(10), dp(4), dp(10), dp(4))
        }
        statusCol.addView(pingBadge)
        statusCol.addView(createSignalBar(item.ping))

        topRow.addView(numChip)
        topRow.addView(infoCol)
        topRow.addView(statusCol)
        card.addView(topRow)
        card.addView(createDivider())

        val tagsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(9), 0, dp(9))
        }
        tagsRow.addView(tagChip(item.protocol))
        tagsRow.addView(tagChip("ICMP · PING TEST"))
        card.addView(tagsRow)

        val copyBtn = Button(this).apply {
            text = copyTitle
            setTextColor(Color.parseColor("#060A16"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            stateListAnimator = null
            background = createGradient(accent, accent2, dp(13))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44))
            setOnClickListener {
                tapFx(this)
                copyToClipboard(item.config, "کانفیگ کپی شد ✓")
            }
        }
        card.addView(copyBtn)

        return card
    }

    // ═══════════════════════ اجزای کوچک ═══════════════════════

    private fun createSignalBar(ping: Int): LinearLayout {
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, 0)
        }
        val activeBars = when {
            ping in 1..140 -> 4
            ping in 141..280 -> 3
            ping in 281..500 -> 2
            ping > 500 -> 1
            else -> 0
        }
        val activeColor = getPingColor(ping)

        for (b in 1..4) {
            val hPx = dp(4 + b * 2)
            val col = if (b <= activeBars) activeColor else "#222B44"
            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(5), hPx).apply {
                    setMargins(dp(1), 0, dp(1), 0)
                }
                background = createShape(col, col, dp(2), 0)
                pivotY = hPx.toFloat()   // رشد از پایین
                scaleY = 0f
                postDelayed({
                    animate().scaleY(1f).setDuration(300).setInterpolator(DecelerateInterpolator()).start()
                }, 120L + b * 60L)
            }
            wrap.addView(bar)
        }
        return wrap
    }

    private fun tagChip(title: String): TextView {
        return TextView(this).apply {
            text = title
            setTextColor(Color.parseColor(TXT_DIM))
            textSize = 8.5f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.08f
            background = createShape(SURFACE_HI, STROKE_SOFT, dp(7), 1)
            setPadding(dp(8), dp(3), dp(8), dp(3))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, dp(6), 0)
            }
        }
    }

    private fun createDivider(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
                setMargins(0, dp(12), 0, dp(2))
            }
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.TRANSPARENT, withAlpha(STROKE_SOFT, 170), Color.TRANSPARENT)
            )
        }
    }

    // ═══════════════════════ موتور تست پینگ ═══════════════════════

    private fun executeSmartPingTest() {
        actionBtn.isEnabled = false
        actionBtn.alpha = 0.7f
        actionBtn.text = "در حال تست و مرتب‌سازی…"
        progressTrack.visibility = View.VISIBLE
        statusDescText.text = "پروب‌های موازی در حال اندازه‌گیری هستند…"

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
                        val item = ssList[i]
                        executor.execute { item.ping = linuxIcmpPing(item.server) }
                    }
                }
                2 -> {
                    val limit = minOf(vlessList.size, 100)
                    for (i in 0 until limit) {
                        val item = vlessList[i]
                        executor.execute { item.ping = linuxIcmpPing(item.server) }
                    }
                }
            }

            executor.shutdown()
            while (!executor.isTerminated) { Thread.sleep(60) }

            // مرتب‌سازی
            when (activeTab) {
                0 -> proxyList.sortBy { if (it.ping <= 0) 999999 else it.ping }
                1 -> ssList.sortBy { if (it.ping <= 0) 999999 else it.ping }
                2 -> vlessList.sortBy { if (it.ping <= 0) 999999 else it.ping }
            }

            runOnUiThread {
                actionBtn.isEnabled = true
                actionBtn.alpha = 1f
                actionBtn.text = "⚡  تست و اعتبارسنجی دقیق پینگ"
                progressTrack.visibility = View.INVISIBLE
                val healthy = when (activeTab) {
                    0 -> proxyList.count { it.ping > 0 }
                    1 -> ssList.count { it.ping > 0 }
                    else -> vlessList.count { it.ping > 0 }
                }
                statusDescText.text = "تست کامل شد ✓  $healthy سرور سالم شناسایی و مرتب شد"
                updateStatsCounters()
                renderCardsList()
                showFancyToast("تست پینگ کامل شد ✓", STAT_HEALTHY)
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
            else -> TXT_DIM
        }
    }

    // ═══════════════════════ انیمیشن‌ها ═══════════════════════

    /** هاله‌های شفق پس‌زمینه */
    private fun auroraBlob(color: String, sizeDp: Int, ml: Int, mt: Int, mr: Int, mb: Int, alpha: Int, period: Long): View {
        val s = dp(sizeDp)
        val v = View(this)
        v.layoutParams = FrameLayout.LayoutParams(s, s).apply { setMargins(ml, mt, mr, mb) }
        v.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(withAlpha(color, alpha), Color.TRANSPARENT)
        ).apply {
            gradientType = GradientDrawable.RADIAL
            gradientRadius = s * 0.8f
        }
        breathe(v, 0.55f, 1f, period)
        return v
    }

    /** تنفس آرام آلفا — برای نورها */
    private fun breathe(view: View, from: Float, to: Float, period: Long) {
        view.alpha = from
        ValueAnimator.ofFloat(from, to).apply {
            duration = period
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { view.alpha = it.animatedValue as Float }
            start()
        }
    }

    /** تپش ملایم مقیاس — برای لوگو */
    private fun pulseScale(view: View, max: Float, period: Long) {
        ValueAnimator.ofFloat(1f, max).apply {
            duration = period
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { a ->
                val s = a.animatedValue as Float
                view.scaleX = s
                view.scaleY = s
            }
            start()
        }
    }

    /** برق عبوری بی‌پایان روی سطح‌های درخشان */
    private fun loopShine(shine: View) {
        shine.post {
            val host = shine.parent as? View ?: return@post
            val w = shine.layoutParams.width
            shine.translationX = (-w).toFloat()
            shine.animate()
                .translationX((host.width + w).toFloat())
                .setDuration(1600)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    shine.postDelayed({ loopShine(shine) }, 2400)
                }
                .start()
        }
    }

    /** ورود سینمایی کارت‌ها */
    private fun animateEntrance(view: View, index: Int) {
        view.alpha = 0f
        view.translationY = dp(26).toFloat()
        view.scaleX = 0.96f
        view.scaleY = 0.96f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(minOf(index * 26L, 400L))
            .setDuration(340)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
    }

    /** فشردن فنری + بازخورد لمسی */
    private fun tapFx(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(60).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(160)
                .setInterpolator(OvershootInterpolator(2f)).start()
        }.start()
    }

    /** شیمر اسکلتون‌ها */
    private fun startShimmer(view: View, delay: Long = 0) {
        view.startAnimation(AlphaAnimation(0.35f, 0.9f).apply {
            duration = 850
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
            startOffset = delay
        })
    }

    private fun showSkeleton(count: Int = 4) {
        contentListLayout.removeAllViews()
        repeat(count) { i ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = createShape(SURFACE, STROKE_SOFT, dp(20), 1)
                setPadding(dp(16), dp(16), dp(16), dp(16))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, 0, dp(12))
                }
            }
            val top = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val circle = View(this).apply {
                val s = dp(34)
                layoutParams = LinearLayout.LayoutParams(s, s)
                background = createShape(SURFACE_HI, SURFACE_HI, dp(11), 0)
                startShimmer(this, i * 120L)
            }
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(12), 0, dp(12), 0)
                }
            }
            val l1 = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(120), dp(12)).apply { setMargins(0, 0, 0, dp(7)) }
                background = createShape(SURFACE_HI, SURFACE_HI, dp(6), 0)
                startShimmer(this, i * 120L + 80)
            }
            val l2 = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dp(80), dp(9))
                background = createShape(SURFACE_HI, SURFACE_HI, dp(5), 0)
                startShimmer(this, i * 120L + 160)
            }
            col.addView(l1)
            col.addView(l2)
            top.addView(circle)
            top.addView(col)
            card.addView(top)

            val wide = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(34)).apply {
                    setMargins(0, dp(14), 0, 0)
                }
                background = createShape(SURFACE_HI, SURFACE_HI, dp(12), 0)
                startShimmer(this, i * 120L + 240)
            }
            card.addView(wide)
            contentListLayout.addView(card)
        }
    }

    // ═══════════════════════ ابزارها ═══════════════════════

    private fun copyToClipboard(payload: String, toastMsg: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Payload", payload))
        showFancyToast(toastMsg, STAT_HEALTHY)
    }

    private fun showFancyToast(message: String, accent: String) {
        try {
            val box = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setPadding(dp(16), dp(11), dp(16), dp(11))
                background = glowCardBg("#10162B", accent, dp(18))
            }
            val dot = View(this).apply {
                val s = dp(8)
                layoutParams = LinearLayout.LayoutParams(s, s).apply { setMargins(dp(9), 0, 0, 0) }
                background = createShape(accent, accent, dp(4), 0)
            }
            val txt = TextView(this).apply {
                text = message
                setTextColor(Color.parseColor(TXT_BRIGHT))
                textSize = 12.5f
            }
            box.addView(dot)
            box.addView(txt)
            Toast(this).apply {
                view = box
                duration = Toast.LENGTH_SHORT
                setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, dp(140))
                show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────── کارخانه‌ی ترسیم ───────────

    private fun withAlpha(hex: String, alpha: Int): Int {
        val c = Color.parseColor(hex)
        return Color.argb(alpha.coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c))
    }

    private fun createShape(bgColor: String, strokeColor: String, radius: Int, strokeWidth: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgColor))
            if (strokeWidth > 0) setStroke(strokeWidth, Color.parseColor(strokeColor))
            cornerRadius = radius.toFloat()
        }
    }

    private fun createGradient(startCol: String, endCol: String, radius: Int, strokeWidth: Int = 0, strokeCol: String = ""): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.parseColor(startCol), Color.parseColor(endCol))
        ).apply {
            if (strokeWidth > 0 && strokeCol.isNotEmpty()) setStroke(strokeWidth, Color.parseColor(strokeCol))
            cornerRadius = radius.toFloat()
        }
    }

    /** هاله‌ی سه‌لایه برای کارت برترین سرور */
    private fun glowCardBg(bgColor: String, accent: String, radius: Int): Drawable {
        val halo = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(dp(7), withAlpha(accent, 45))
            cornerRadius = (radius + dp(6)).toFloat()
        }
        val mid = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(dp(3), withAlpha(accent, 95))
            cornerRadius = (radius + dp(3)).toFloat()
        }
        val core = createShape(bgColor, withAlpha(accent, 210), radius, 1)
        return LayerDrawable(arrayOf(halo, mid, core)).apply {
            setLayerInset(1, dp(3), dp(3), dp(3), dp(3))
            setLayerInset(2, dp(6), dp(6), dp(6), dp(6))
        }
    }

    /** هاله‌ی رادیال پشت دکمه‌ی CTA */
    private fun glowBackdrop(accent: String): GradientDrawable = GradientDrawable().apply {
        gradientType = GradientDrawable.RADIAL
        colors = intArrayOf(withAlpha(accent, 90), Color.TRANSPARENT)
        gradientRadius = dp(130).toFloat()
    }
}
