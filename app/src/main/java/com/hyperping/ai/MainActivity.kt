// ==================== IMPORTS ====================
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
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
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

private val PROXY_URL = "https://p.sosiss.ir/data_proxy.json"
private val V2RAY_URL = "https://p.sosiss.ir/data_v2ray.json"

// ==================== PALETTE (Aurora Dark 2025) ====================
private const val C_BG_DEEP = "#04050A"
private const val C_BG_TOP  = "#0A0E1B"
private const val C_NAV     = "#080B14"
private const val C_SURFACE = "#0E1220"
private const val C_SURF_2  = "#131828"
private const val C_STROKE  = "#1E2637"
private const val C_STROKE_2= "#2A3448"
private const val C_TEXT    = "#F7FAFF"
private const val C_MUTED   = "#8593AC"
private const val C_DIM     = "#5A6780"

private const val C_CYAN    = "#22D3EE"
private const val C_BLUE    = "#3B82F6"
private const val C_VIOLET  = "#A855F7"
private const val C_PINK    = "#EC4899"
private const val C_GREEN   = "#34D399"
private const val C_AMBER   = "#FBBF24"
private const val C_RED     = "#FB4E6D"

data class ProxyModel(val server: String, val port: Int, val secret: String, val tgLink: String, var ping: Int = -1, var stateText: String = "آماده تست")
data class V2rayModel(val config: String, val protocol: String, val server: String, val port: Int, val remark: String, var ping: Int = -1, var stateText: String = "آماده تست")

private val proxyItems = ArrayList<ProxyModel>()
private val v2rayItems = ArrayList<V2rayModel>()

private var activeTab = 0 // 0 = تلگرام (دست نخورده), 1 = شادوساکس (پینگ ترمینالی)
private lateinit var listContainer: LinearLayout
private lateinit var tabTgBtn: Button
private lateinit var tabV2Btn: Button
private lateinit var infoStatusText: TextView
private lateinit var testActionBtn: Button

// --- فقط برای زیبایی (بدون دخالت در منطق) ---
private lateinit var pingProgress: ProgressBar
private lateinit var accentGlowLine: View
private lateinit var headerBadge: TextView

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initLuxuryUI()
    fetchData()
}

private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

private fun initLuxuryUI() {
    window.statusBarColor = Color.parseColor(C_NAV)
    window.navigationBarColor = Color.parseColor(C_BG_DEEP)

    val root = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = gradient(arrayOf(C_BG_TOP, C_BG_DEEP, C_BG_DEEP), 0, GradientDrawable.Orientation.TOP_BOTTOM)
    }

    // ==================== TOP NAV ====================
    val topNav = RelativeLayout(this).apply {
        background = gradient(arrayOf("#0C1120", C_NAV), 0, GradientDrawable.Orientation.TOP_BOTTOM)
        setPadding(dp(18), dp(18), dp(18), dp(16))
    }

    val brandBox = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        id = View.generateViewId()
    }

    val iconTile = TextView(this).apply {
        text = "H"
        gravity = Gravity.CENTER
        setTextColor(Color.parseColor("#04050A"))
        textSize = 17f
        typeface = uiFont(true)
        background = gradient(arrayOf(C_CYAN, C_BLUE, C_VIOLET), dp(12), GradientDrawable.Orientation.TL_BR)
        layoutParams = LinearLayout.LayoutParams(dp(38), dp(38)).apply { setMargins(0, 0, dp(11), 0) }
        glow(this, C_CYAN, 10)
    }

    val titleCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

    val appLogo = TextView(this).apply {
        text = "HYPER CORE"
        setTextColor(Color.parseColor(C_TEXT))
        textSize = 17f
        typeface = uiFont(true)
        letterSpacing = 0.08f
        post {
            paint.shader = LinearGradient(
                0f, 0f, width.toFloat(), 0f,
                intArrayOf(Color.parseColor(C_CYAN), Color.parseColor("#818CF8"), Color.parseColor(C_PINK)),
                null, Shader.TileMode.CLAMP
            )
            invalidate()
        }
    }

    val appSub = TextView(this).apply {
        text = "PROXY  ·  SHADOWSOCKS  ·  PING LAB"
        setTextColor(Color.parseColor(C_DIM))
        textSize = 8.5f
        typeface = uiFont(false)
        letterSpacing = 0.18f
        setPadding(0, dp(3), 0, 0)
    }

    titleCol.addView(appLogo)
    titleCol.addView(appSub)
    brandBox.addView(iconTile)
    brandBox.addView(titleCol)

    val statusPill = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = createCard("#0B2230", C_CYAN, dp(20), 1)
        setPadding(dp(10), dp(6), dp(11), dp(6))
        val p = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        p.addRule(RelativeLayout.ALIGN_PARENT_END)
        p.addRule(RelativeLayout.CENTER_VERTICAL)
        layoutParams = p
    }
    val liveDot = View(this).apply {
        background = solid(C_CYAN, dp(4))
        layoutParams = LinearLayout.LayoutParams(dp(7), dp(7)).apply { setMargins(0, 0, dp(6), 0) }
        pulse(this)
    }
    headerBadge = TextView(this).apply {
        text = "ONLINE"
        setTextColor(Color.parseColor(C_CYAN))
        textSize = 9.5f
        typeface = uiFont(true)
        letterSpacing = 0.12f
    }
    statusPill.addView(liveDot)
    statusPill.addView(headerBadge)

    topNav.addView(brandBox)
    topNav.addView(statusPill)
    root.addView(topNav)

    accentGlowLine = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(2))
        background = gradient(arrayOf(C_CYAN, C_VIOLET, C_PINK), 0, GradientDrawable.Orientation.LEFT_RIGHT)
    }
    root.addView(accentGlowLine)

    // ==================== TABS ====================
    val tabBox = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        background = createCard("#0C1019", C_STROKE, dp(16), 1)
        setPadding(dp(5), dp(5), dp(5), dp(5))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(16), dp(16), dp(16), dp(10))
        }
        weightSum = 2f
    }

    tabTgBtn = Button(this).apply {
        text = "✈️  پروکسی تلگرام"
        textSize = 12f
        typeface = uiFont(true)
        layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
        setOnClickListener { setTab(0) }
        polish()
    }

    tabV2Btn = Button(this).apply {
        text = "🛡️  شادوساکس"
        textSize = 12f
        typeface = uiFont(true)
        layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
        setOnClickListener { setTab(1) }
        polish()
    }

    tabBox.addView(tabTgBtn)
    tabBox.addView(tabV2Btn)
    root.addView(tabBox)

    // ==================== ACTION CARD ====================
    val actionCard = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = createCard(C_SURFACE, C_STROKE, dp(20), 1)
        setPadding(dp(16), dp(15), dp(16), dp(16))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(dp(16), dp(4), dp(16), dp(14))
        }
        elevation = dp(6).toFloat()
    }

    val caption = TextView(this).apply {
        text = "وضعیت مخزن"
        setTextColor(Color.parseColor(C_DIM))
        textSize = 9.5f
        typeface = uiFont(true)
        letterSpacing = 0.1f
        setPadding(0, 0, 0, dp(5))
    }

    infoStatusText = TextView(this).apply {
        text = "در حال بارگذاری مخزن..."
        setTextColor(Color.parseColor(C_MUTED))
        textSize = 12.5f
        typeface = uiFont(false)
        setLineSpacing(dp(3).toFloat(), 1f)
    }

    pingProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
        isIndeterminate = true
        indeterminateTintList = ColorStateList.valueOf(Color.parseColor(C_CYAN))
        visibility = View.GONE
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(3)).apply {
            setMargins(0, dp(10), 0, 0)
        }
    }

    testActionBtn = Button(this).apply {
        text = "⚡ تست پینگ"
        setTextColor(Color.parseColor("#04050A"))
        textSize = 13f
        typeface = uiFont(true)
        background = ripple(gradient(arrayOf(C_CYAN, C_BLUE), dp(14), GradientDrawable.Orientation.LEFT_RIGHT), "#FFFFFF")
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)).apply {
            setMargins(0, dp(12), 0, 0)
        }
        setOnClickListener { runPingEngine() }
        polish()
        pressScale(this)
        glow(this, C_CYAN, 14)
    }

    actionCard.addView(caption)
    actionCard.addView(infoStatusText)
    actionCard.addView(pingProgress)
    actionCard.addView(testActionBtn)
    root.addView(actionCard)

    // ==================== LIST ====================
    val scroller = ScrollView(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        isVerticalScrollBarEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        clipToPadding = false
    }
    listContainer = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(2), dp(16), dp(28))
    }
    scroller.addView(listContainer)
    root.addView(scroller)

    setContentView(root)
    refreshTabUI()
}

private fun setTab(tab: Int) {
    activeTab = tab
    refreshTabUI()
    renderListView()
}

private fun refreshTabUI() {
    if (activeTab == 0) {
        tabTgBtn.setTextColor(Color.WHITE)
        tabTgBtn.background = createCard("#10243A", C_CYAN, dp(12), 1)
        glow(tabTgBtn, C_CYAN, 8)
        tabV2Btn.setTextColor(Color.parseColor(C_DIM))
        tabV2Btn.background = ripple(solid("#00000000", dp(12)), C_CYAN)
        tabV2Btn.elevation = 0f

        testActionBtn.text = "⚡ تست پینگ پروکسی‌های تلگرام"
        testActionBtn.setTextColor(Color.parseColor("#04050A"))
        testActionBtn.background = ripple(gradient(arrayOf(C_CYAN, C_BLUE), dp(14), GradientDrawable.Orientation.LEFT_RIGHT), "#FFFFFF")
        glow(testActionBtn, C_CYAN, 14)

        accentGlowLine.background = gradient(arrayOf(C_CYAN, C_BLUE, "#0EA5E9"), 0, GradientDrawable.Orientation.LEFT_RIGHT)
        pingProgress.indeterminateTintList = ColorStateList.valueOf(Color.parseColor(C_CYAN))
        headerBadge.setTextColor(Color.parseColor(C_CYAN))
    } else {
        tabV2Btn.setTextColor(Color.WHITE)
        tabV2Btn.background = createCard("#241040", C_VIOLET, dp(12), 1)
        glow(tabV2Btn, C_VIOLET, 8)
        tabTgBtn.setTextColor(Color.parseColor(C_DIM))
        tabTgBtn.background = ripple(solid("#00000000", dp(12)), C_VIOLET)
        tabTgBtn.elevation = 0f

        testActionBtn.text = "⚡ تست پینگ سرورها (مشابه ترمینال لینوکس)"
        testActionBtn.setTextColor(Color.WHITE)
        testActionBtn.background = ripple(gradient(arrayOf(C_VIOLET, C_PINK), dp(14), GradientDrawable.Orientation.LEFT_RIGHT), "#FFFFFF")
        glow(testActionBtn, C_VIOLET, 14)

        accentGlowLine.background = gradient(arrayOf(C_VIOLET, C_PINK, "#F472B6"), 0, GradientDrawable.Orientation.LEFT_RIGHT)
        pingProgress.indeterminateTintList = ColorStateList.valueOf(Color.parseColor(C_VIOLET))
        headerBadge.setTextColor(Color.parseColor(C_VIOLET))
    }
}

private fun fetchData() {
    thread {
        try {
            val pStr = httpGet(PROXY_URL)
            val pArr = JSONArray(pStr)
            proxyItems.clear()
            for (i in 0 until pArr.length()) {
                val o = pArr.getJSONObject(i)
                proxyItems.add(ProxyModel(o.optString("server"), o.optInt("port"), o.optString("secret"), o.optString("tg_link")))
            }

            val vStr = httpGet(V2RAY_URL)
            val vArr = JSONArray(vStr)
            v2rayItems.clear()
            for (i in 0 until vArr.length()) {
                val o = vArr.getJSONObject(i)
                v2rayItems.add(V2rayModel(o.optString("config"), o.optString("protocol", "SS"), o.optString("server"), o.optInt("port", 443), o.optString("remark", "Canada")))
            }

            runOnUiThread {
                infoStatusText.text = "مخزن آماده: ${proxyItems.size} پروکسی | ${v2rayItems.size} سرور"
                renderListView()
            }
        } catch (e: Exception) {
            runOnUiThread {
                infoStatusText.text = "خطا در دریافت اطلاعات: " + (e.message ?: "تایم‌اوت")
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

private fun renderListView() {
    listContainer.removeAllViews()

    if (activeTab == 0) {
        for (p in proxyItems) {
            val v = createProxyView(p)
            listContainer.addView(v)
            enterAnim(v, listContainer.childCount - 1)
        }
    } else {
        val count = Math.min(v2rayItems.size, 100)
        for (i in 0 until count) {
            val v = createV2rayView(v2rayItems[i])
            listContainer.addView(v)
            enterAnim(v, i)
        }
    }
}

private fun createProxyView(item: ProxyModel): View {
    val idx = listContainer.childCount + 1

    val card = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = createCard(if (item.ping > 0) "#0C1522" else C_SURFACE, if (item.ping > 0) C_CYAN else C_STROKE, dp(20), 1)
        setPadding(dp(15), dp(14), dp(15), dp(14))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, dp(12))
        }
        elevation = dp(3).toFloat()
    }

    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    val indexChip = TextView(this).apply {
        text = "$idx"
        gravity = Gravity.CENTER
        setTextColor(Color.parseColor(if (item.ping > 0) C_CYAN else C_DIM))
        textSize = 10.5f
        typeface = uiFont(true)
        background = createCard(C_SURF_2, if (item.ping > 0) C_CYAN else C_STROKE_2, dp(10), 1)
        layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { setMargins(0, 0, dp(10), 0) }
    }

    val infoCol = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    val ipText = TextView(this).apply {
        text = "${item.server}:${item.port}"
        setTextColor(Color.parseColor(C_TEXT))
        textSize = 13f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        letterSpacing = 0.02f
        maxLines = 1
    }

    val typeLine = TextView(this).apply {
        text = "MTPROTO  ·  TELEGRAM"
        setTextColor(Color.parseColor(C_DIM))
        textSize = 8.5f
        typeface = uiFont(false)
        letterSpacing = 0.12f
        setPadding(0, dp(3), 0, 0)
    }

    infoCol.addView(ipText)
    infoCol.addView(typeLine)

    val badgeCol = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.END
    }

    val badge = TextView(this).apply {
        text = if (item.ping > 0) "${item.ping}ms | ${item.stateText}" else item.stateText
        setTextColor(Color.parseColor(pingColor(item.ping)))
        textSize = 11f
        typeface = uiFont(true)
        background = createCard(pingBg(item.ping), pingColor(item.ping), dp(20), 1)
        setPadding(dp(10), dp(5), dp(10), dp(5))
    }

    badgeCol.addView(badge)
    if (item.ping > 0) {
        badgeCol.addView(signalBars(item.ping).apply {
            (layoutParams as? LinearLayout.LayoutParams)?.setMargins(0, dp(6), dp(2), 0)
        })
    }

    row.addView(indexChip)
    row.addView(infoCol)
    row.addView(badgeCol)
    card.addView(row)
    card.addView(divider())

    val btnRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dp(10), 0, 0)
        weightSum = 2f
    }

    val connBtn = Button(this).apply {
        text = "اتصال به تلگرام"
        setTextColor(Color.parseColor("#04050A"))
        textSize = 12f
        typeface = uiFont(true)
        background = ripple(gradient(arrayOf(C_CYAN, C_BLUE), dp(12), GradientDrawable.Orientation.LEFT_RIGHT), "#FFFFFF")
        layoutParams = LinearLayout.LayoutParams(0, dp(42), 1.2f).apply { setMargins(0, 0, dp(8), 0) }
        polish(); pressScale(this)
        setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.tgLink)))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "تلگرام یافت نشد!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val copyBtn = Button(this).apply {
        text = "⧉  کپی"
        setTextColor(Color.parseColor("#CBD5E1"))
        textSize = 12f
        typeface = uiFont(true)
        background = ripple(createCard(C_SURF_2, C_STROKE_2, dp(12), 1), C_CYAN)
        layoutParams = LinearLayout.LayoutParams(0, dp(42), 0.8f)
        polish(); pressScale(this)
        setOnClickListener { copyClip(item.tgLink, "لینک پروکسی کپی شد") }
    }

    btnRow.addView(connBtn)
    btnRow.addView(copyBtn)
    card.addView(btnRow)

    return card
}

private fun createV2rayView(item: V2rayModel): View {
    val idx = listContainer.childCount + 1

    val card = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = createCard(if (item.ping > 0) "#150C24" else C_SURFACE, if (item.ping > 0) C_VIOLET else C_STROKE, dp(20), 1)
        setPadding(dp(15), dp(14), dp(15), dp(14))
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, dp(12))
        }
        elevation = dp(3).toFloat()
    }

    val row = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    val indexChip = TextView(this).apply {
        text = "$idx"
        gravity = Gravity.CENTER
        setTextColor(Color.parseColor(if (item.ping > 0) "#C084FC" else C_DIM))
        textSize = 10.5f
        typeface = uiFont(true)
        background = createCard("#1A1230", if (item.ping > 0) C_VIOLET else C_STROKE_2, dp(10), 1)
        layoutParams = LinearLayout.LayoutParams(dp(30), dp(30)).apply { setMargins(0, 0, dp(10), 0) }
    }

    val infoCol = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    }

    val remark = TextView(this).apply {
        text = item.remark
        setTextColor(Color.parseColor(C_TEXT))
        textSize = 13.5f
        typeface = uiFont(true)
        maxLines = 1
    }

    val hostLine = TextView(this).apply {
        text = "${item.server}:${item.port}"
        setTextColor(Color.parseColor(C_MUTED))
        textSize = 10f
        typeface = Typeface.MONOSPACE
        maxLines = 1
        setPadding(0, dp(3), 0, 0)
    }

    infoCol.addView(remark)
    infoCol.addView(hostLine)

    val badgeCol = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.END
    }

    val status = TextView(this).apply {
        text = if (item.ping > 0) "${item.ping}ms | ${item.stateText}" else item.stateText
        setTextColor(Color.parseColor(pingColor(item.ping)))
        textSize = 11f
        typeface = uiFont(true)
        background = createCard(pingBg(item.ping), pingColor(item.ping), dp(20), 1)
        setPadding(dp(10), dp(5), dp(10), dp(5))
    }

    badgeCol.addView(status)
    if (item.ping > 0) {
        badgeCol.addView(signalBars(item.ping).apply {
            (layoutParams as? LinearLayout.LayoutParams)?.setMargins(0, dp(6), dp(2), 0)
        })
    }

    row.addView(indexChip)
    row.addView(infoCol)
    row.addView(badgeCol)
    card.addView(row)

    val chipRow = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, dp(11), 0, 0)
    }
    chipRow.addView(miniChip(item.protocol.uppercase(), C_VIOLET, "#1B1030"))
    chipRow.addView(miniChip("ICMP TEST", "#67E8F9", "#0C1B26"))
    card.addView(chipRow)
    card.addView(divider())

    val copyBtn = Button(this).apply {
        text = "⧉  کپی کانفیگ شادوساکس"
        setTextColor(Color.WHITE)
        textSize = 12f
        typeface = uiFont(true)
        background = ripple(gradient(arrayOf(C_VIOLET, C_PINK), dp(12), GradientDrawable.Orientation.LEFT_RIGHT), "#FFFFFF")
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(44)).apply {
            setMargins(0, dp(10), 0, 0)
        }
        polish(); pressScale(this)
        setOnClickListener { copyClip(item.config, "کانفیگ کپی شد") }
    }
    card.addView(copyBtn)

    return card
}

// ================= موتور تست (منطق دست‌نخورده) =================
private fun runPingEngine() {
    testActionBtn.isEnabled = false
    testActionBtn.alpha = 0.65f                       // فقط ظاهری
    pingProgress.visibility = View.VISIBLE            // فقط ظاهری
    infoStatusText.text = "در حال پینگ دقیق سرورها..."

    val executor = Executors.newFixedThreadPool(12)

    thread {
        if (activeTab == 0) {
            // بخش تلگرام: دست‌نخورده و بدون تغییر
            for (item in proxyItems) {
                executor.execute {
                    val p = probeTelegramSocket(item.server, item.port)
                    item.ping = p
                    item.stateText = if (p > 0) calculateSpeedLabel(p) else "قطع ❌"
                }
            }
        } else {
            // بخش سرورهای شادوساکس: اجرای مستقیم دستور Ping ترمینال لینوکس
            val count = Math.min(v2rayItems.size, 100)
            for (i in 0 until count) {
                val item = v2rayItems[i]
                executor.execute {
                    val p = linuxIcmpPing(item.server)
                    item.ping = p
                    item.stateText = if (p > 0) calculateSpeedLabel(p) else "قطع ❌"
                }
            }
        }

        executor.shutdown()
        while (!executor.isTerminated) {
            Thread.sleep(60)
        }

        // مرتب‌سازی: سرورهای سالم بالا، پکت‌لاس‌ها پایین
        if (activeTab == 0) {
            proxyItems.sortBy { if (it.ping <= 0) 999999 else it.ping }
        } else {
            v2rayItems.sortBy { if (it.ping <= 0) 999999 else it.ping }
        }

        runOnUiThread {
            testActionBtn.isEnabled = true
            testActionBtn.alpha = 1f                  // فقط ظاهری
            pingProgress.visibility = View.GONE       // فقط ظاهری
            infoStatusText.text = "پینگ‌گیری تمام شد (سرورهای قطع به انتهای لیست رفتند)"
            renderListView()
        }
    }
}

// تست تلگرام: سوکت سالم قبلی
private fun probeTelegramSocket(host: String, port: Int): Int {
    if (host.isEmpty() || port <= 0) return -2
    var total = 0
    var ok = 0
    for (i in 1..2) {
        try {
            val sock = Socket()
            sock.tcpNoDelay = true
            val start = System.currentTimeMillis()
            sock.connect(InetSocketAddress(host, port), 1200)
            val diff = (System.currentTimeMillis() - start).toInt()
            sock.close()
            total += diff
            ok++
        } catch (e: Exception) {}
    }
    return if (ok > 0) total / ok else -2
}

// تست V2Ray/شادوساکس: اجرای دقیق دستور ping لینوکس
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
                if (parts.size >= 2) {
                    avgMs = parts[1].trim().toFloat().toInt()
                }
            }
        }
        val exitCode = process.waitFor()
        if (exitCode == 0 && avgMs > 0) avgMs else -2
    } catch (e: Exception) {
        -2
    }
}

private fun calculateSpeedLabel(pingMs: Int): String {
    return when {
        pingMs in 1..140 -> "بسیار سریع ⚡"
        pingMs in 141..280 -> "خوب"
        else -> "متوسط"
    }
}

private fun copyClip(payload: String, msg: String) {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Data", payload))
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

// ==================== DESIGN TOOLKIT ====================
private fun createCard(bgColor: String, borderColor: String, radius: Int, strokeWidth: Int): GradientDrawable {
    val base = Color.parseColor(bgColor)
    return GradientDrawable().apply {
        gradientType = GradientDrawable.LINEAR_GRADIENT
        orientation = GradientDrawable.Orientation.TOP_BOTTOM
        colors = intArrayOf(shade(base, 0.10f), base, shade(base, -0.06f))
        if (strokeWidth > 0) setStroke(maxOf(strokeWidth, dp(1)), Color.parseColor(borderColor))
        cornerRadius = radius.toFloat()
    }
}

private fun gradient(hexList: Array<String>, radius: Int, o: GradientDrawable.Orientation): GradientDrawable {
    return GradientDrawable().apply {
        orientation = o
        colors = hexList.map { Color.parseColor(it) }.toIntArray()
        cornerRadius = radius.toFloat()
    }
}

private fun solid(hex: String, radius: Int): GradientDrawable = GradientDrawable().apply {
    setColor(Color.parseColor(hex))
    cornerRadius = radius.toFloat()
}

private fun ripple(content: Drawable, rippleHex: String): Drawable =
    RippleDrawable(ColorStateList.valueOf(withAlpha(rippleHex, 0.22f)), content, null)

private fun withAlpha(hex: String, a: Float): Int {
    val c = Color.parseColor(hex)
    return Color.argb((255 * a).toInt(), Color.red(c), Color.green(c), Color.blue(c))
}

private fun shade(color: Int, f: Float): Int {
    fun ch(v: Int) = (v + (if (f > 0) (255 - v) * f else v * f)).toInt().coerceIn(0, 255)
    return Color.rgb(ch(Color.red(color)), ch(Color.green(color)), ch(Color.blue(color)))
}

private fun glow(v: View, hex: String, e: Int) {
    v.elevation = dp(e).toFloat()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        v.outlineSpotShadowColor = Color.parseColor(hex)
        v.outlineAmbientShadowColor = Color.parseColor(hex)
    }
}

private fun Button.polish() {
    isAllCaps = false
    stateListAnimator = null
    minWidth = 0
    minHeight = 0
    setPadding(dp(8), 0, dp(8), 0)
}

private fun pressScale(v: View) {
    v.setOnTouchListener { view, ev ->
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> view.animate().scaleX(0.96f).scaleY(0.94f).setDuration(90).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
        }
        false
    }
}

private fun pulse(v: View) {
    val a = AlphaAnimation(1f, 0.25f).apply {
        duration = 850
        repeatMode = Animation.REVERSE
        repeatCount = Animation.INFINITE
    }
    v.startAnimation(a)
}

private fun enterAnim(v: View, index: Int) {
    v.alpha = 0f
    v.translationY = dp(20).toFloat()
    v.animate()
        .alpha(1f).translationY(0f)
        .setStartDelay((index * 32L).coerceAtMost(520L))
        .setDuration(330)
        .setInterpolator(DecelerateInterpolator())
        .start()
}

private fun divider(): View = View(this).apply {
    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply {
        setMargins(0, dp(12), 0, 0)
    }
    background = gradient(arrayOf("#00000000", C_STROKE_2, "#00000000"), 0, GradientDrawable.Orientation.LEFT_RIGHT)
}

private fun miniChip(txt: String, fg: String, bg: String): TextView = TextView(this).apply {
    text = txt
    setTextColor(Color.parseColor(fg))
    textSize = 8.5f
    typeface = uiFont(true)
    letterSpacing = 0.1f
    background = createCard(bg, withAlphaHex(fg), dp(20), 1)
    setPadding(dp(9), dp(4), dp(9), dp(4))
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { setMargins(0, 0, dp(6), 0) }
}

private fun withAlphaHex(hex: String): String = hex // استروک هم‌رنگ متن چیپ

private fun signalBars(ping: Int): View {
    val strength = when {
        ping <= 0 -> 0
        ping <= 140 -> 4
        ping <= 280 -> 3
        ping <= 500 -> 2
        else -> 1
    }
    val active = pingColor(ping)
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.BOTTOM
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        for (i in 1..4) {
            addView(View(this@MainActivity).apply {
                background = solid(if (i <= strength) active else C_STROKE_2, dp(2))
                layoutParams = LinearLayout.LayoutParams(dp(3), dp(3 + i * 3)).apply {
                    setMargins(dp(1), 0, dp(1), 0)
                }
            })
        }
    }
}

private fun pingColor(ping: Int): String = when {
    ping == -2 -> C_RED
    ping <= 0 -> C_DIM
    ping <= 140 -> C_GREEN
    ping <= 280 -> C_AMBER
    else -> "#FB923C"
}

private fun pingBg(ping: Int): String = when {
    ping == -2 -> "#2A0E17"
    ping <= 0 -> C_SURF_2
    ping <= 140 -> "#0C2119"
    ping <= 280 -> "#251C08"
    else -> "#2A1608"
}

private fun uiFont(bold: Boolean): Typeface {
    return try {
        Typeface.createFromAsset(assets, if (bold) "fonts/Vazirmatn-Bold.ttf" else "fonts/Vazirmatn-Regular.ttf")
    } catch (e: Exception) {
        if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
}
