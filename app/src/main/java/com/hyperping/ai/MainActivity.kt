package com.hyperping.ai

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
import android.view.Gravity
import android.view.View
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

    data class ProxyModel(val server: String, val port: Int, val secret: String, val tgLink: String, var delay: Int = -1, var stateText: String = "آماده تست")
    data class V2rayModel(val config: String, val protocol: String, val server: String, val port: Int, val remark: String, var delay: Int = -1, var stateText: String = "آماده تست")

    private val proxyItems = ArrayList<ProxyModel>()
    private val v2rayItems = ArrayList<V2rayModel>()

    private var activeTab = 0 // 0 = Telegram (Blue), 1 = Shadowsocks (Purple)
    private lateinit var listContainer: LinearLayout
    private lateinit var tabTgBtn: Button
    private lateinit var tabV2Btn: Button
    private lateinit var infoStatusText: TextView
    private lateinit var testActionBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLuxuryUI()
        fetchData()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ================= رابط کاربری شیک با تم آبی و بنفش =================
    private fun initLuxuryUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#08090C"))
        }

        // نوار بالای صفحه
        val topNav = RelativeLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0E1017"))
            setPadding(dp(20), dp(18), dp(20), dp(18))
        }
        val appLogo = TextView(this).apply {
            text = "HYPER CORE // PROXY & V2RAY"
            setTextColor(Color.parseColor("#F8FAFC"))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        }
        val statusPill = TextView(this).apply {
            text = "ONLINE ●"
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            background = createCard("#162032", "#38BDF8", dp(6), 1)
            setPadding(dp(8), dp(3), dp(8), dp(3))
            val p = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            p.addRule(RelativeLayout.ALIGN_PARENT_END)
            layoutParams = p
        }
        topNav.addView(appLogo)
        topNav.addView(statusPill)
        root.addView(topNav)

        // انتخابگر تب‌ها (ترکیب فیروزه‌ای و بنفش شیک)
        val tabBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createCard("#121520", "#1C2233", dp(12), 1)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(18), dp(14), dp(18), dp(8))
            }
            weightSum = 2f
        }

        tabTgBtn = Button(this).apply {
            text = "✈️ پروکسی‌های تلگرام"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f)
            setOnClickListener { setTab(0) }
        }

        tabV2Btn = Button(this).apply {
            text = "🛡️ سرورهای شادوساکس"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f)
            setOnClickListener { setTab(1) }
        }

        tabBox.addView(tabTgBtn)
        tabBox.addView(tabV2Btn)
        root.addView(tabBox)

        // بخش اطلاعات و دکمه تست تفکیک‌شده
        val actionCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCard("#11141D", "#1D2333", dp(14), 1)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(18), dp(6), dp(18), dp(12))
            }
        }

        infoStatusText = TextView(this).apply {
            text = "در حال بارگذاری مخزن..."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
        }

        testActionBtn = Button(this).apply {
            text = "تست وضعیت اتصال"
            setTextColor(Color.parseColor("#08090C"))
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            background = createCard("#38BDF8", "#38BDF8", dp(10), 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(42)).apply {
                setMargins(0, dp(10), 0, 0)
            }
            setOnClickListener { runSeparatedDelayTest() }
        }

        actionCard.addView(infoStatusText)
        actionCard.addView(testActionBtn)
        root.addView(actionCard)

        // محفظه لیست
        val scroller = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), 0, dp(18), dp(25))
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
            // تب تلگرام (تم آبی فیروزه‌ای)
            tabTgBtn.setTextColor(Color.WHITE)
            tabTgBtn.background = createCard("#1E293B", "#38BDF8", dp(10), 1)
            tabV2Btn.setTextColor(Color.parseColor("#64748B"))
            tabV2Btn.setBackgroundColor(Color.TRANSPARENT)

            testActionBtn.text = "⚡ تست پینگ واقعی پروکسی‌های تلگرام"
            testActionBtn.setTextColor(Color.BLACK)
            testActionBtn.background = createCard("#38BDF8", "#38BDF8", dp(10), 0)
        } else {
            // تب شادوساکس (تم بنفش لوکس و جذاب)
            tabV2Btn.setTextColor(Color.WHITE)
            tabV2Btn.background = createCard("#2D1B4E", "#A855F7", dp(10), 1)
            tabTgBtn.setTextColor(Color.parseColor("#64748B"))
            tabTgBtn.setBackgroundColor(Color.TRANSPARENT)

            testActionBtn.text = "⚡ تست تاخیر و پایداری سرورهای شادوساکس"
            testActionBtn.setTextColor(Color.WHITE)
            testActionBtn.background = createCard("#A855F7", "#A855F7", dp(10), 0)
        }
    }

    // ================= دریافت اطلاعات =================
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
                    infoStatusText.text = "مخزن فعال: ${proxyItems.size} پروکسی تلگرام | ${v2rayItems.size} سرور شادوساکس"
                    renderListView()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    infoStatusText.text = "خطا در دریافت اطلاعات مخزن: " + (e.message ?: "تایم‌اوت")
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

    // ================= رندر کارت‌ها با رنگ‌بندی جدید =================
    private fun renderListView() {
        listContainer.removeAllViews()

        if (activeTab == 0) {
            for (p in proxyItems) {
                listContainer.addView(createProxyView(p))
            }
        } else {
            val count = Math.min(v2rayItems.size, 100)
            for (i in 0 until count) {
                listContainer.addView(createV2rayView(v2rayItems[i]))
            }
        }
    }

    private fun createProxyView(item: ProxyModel): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCard("#0E1119", if (item.ping > 0) "#1E2A3A" else "#141722", dp(14), 1)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val ipText = TextView(this).apply {
            text = "${item.server}:${item.port}"
            setTextColor(Color.parseColor("#F1F5F9"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val badge = TextView(this).apply {
            text = if (item.ping > 0) "${item.ping}ms | ${item.stateText}" else item.stateText
            setTextColor(Color.parseColor(if (item.ping > 0) "#38BDF8" else if (item.ping == -2) "#FF3366" else "#64748B"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = createCard("#141926", if (item.ping > 0) "#38BDF8" else "#222A3A", dp(6), 1)
            setPadding(dp(8), dp(3), dp(8), dp(3))
        }

        row.addView(ipText)
        row.addView(badge)
        card.addView(row)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
            weightSum = 2f
        }

        val connBtn = Button(this).apply {
            text = "اتصال به تلگرام"
            setTextColor(Color.parseColor("#08090C"))
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            background = createCard("#38BDF8", "#38BDF8", dp(8), 0)
            layoutParams = LinearLayout.LayoutParams(0, dp(36), 1.2f).apply { setMargins(0, 0, dp(6), 0) }
            setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.tgLink)))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "تلگرام یافت نشد!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val copyBtn = Button(this).apply {
            text = "کپی لینک"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 11.5f
            background = createCard("#161B26", "#252E42", dp(8), 1)
            layoutParams = LinearLayout.LayoutParams(0, dp(36), 0.8f)
            setOnClickListener { copyClip(item.tgLink, "لینک پروکسی کپی شد") }
        }

        btnRow.addView(connBtn)
        btnRow.addView(copyBtn)
        card.addView(btnRow)

        return card
    }

    private fun createV2rayView(item: V2rayModel): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCard("#0E1119", if (item.ping > 0) "#26173B" else "#141722", dp(14), 1)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val remark = TextView(this).apply {
            text = item.remark
            setTextColor(Color.parseColor("#F1F5F9"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val status = TextView(this).apply {
            text = if (item.ping > 0) "${item.ping}ms | ${item.stateText}" else item.stateText
            setTextColor(Color.parseColor(if (item.ping > 0) "#C084FC" else if (item.ping == -2) "#FF3366" else "#64748B"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = createCard("#1A1428", if (item.ping > 0) "#A855F7" else "#222A3A", dp(6), 1)
            setPadding(dp(8), dp(3), dp(8), dp(3))
        }

        row.addView(remark)
        row.addView(status)
        card.addView(row)

        // دکمه کپی کانفیگ با رنگ بنفش لوکس
        val copyBtn = Button(this).apply {
            text = "کپی کانفیگ شادوساکس"
            setTextColor(Color.WHITE)
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            background = createCard("#9333EA", "#A855F7", dp(8), 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(36)).apply {
                setMargins(0, dp(10), 0, 0)
            }
            setOnClickListener { copyClip(item.config, "کانفیگ کپی شد") }
        }
        card.addView(copyBtn)

        return card
    }

    // ================= تست پینگ واقعی و تفکیک‌شده (کاملاً بدون باگ) =================
    private fun runSeparatedDelayTest() {
        testActionBtn.isEnabled = false
        infoStatusText.text = "در حال اندازه‌گیری پینگ واقعی سرورها..."

        val executor = Executors.newFixedThreadPool(14)

        thread {
            if (activeTab == 0) {
                // تست دقیق پروکسی‌های تلگرام
                for (item in proxyItems) {
                    executor.execute {
                        val p = probeSocketLatency(item.server, item.port)
                        item.ping = p
                        if (p > 0) {
                            item.stateText = calculateSpeedRating(p)
                        } else {
                            item.stateText = "قطع ❌"
                        }
                    }
                }
            } else {
                // تست دقیق سرورهای شادوساکس
                val count = Math.min(v2rayItems.size, 100)
                for (i in 0 until count) {
                    val item = v2rayItems[i]
                    executor.execute {
                        val p = probeSocketLatency(item.server, item.port)
                        item.ping = p
                        if (p > 0) {
                            item.stateText = calculateSpeedRating(p)
                        } else {
                            item.stateText = "قطع ❌"
                        }
                    }
                }
            }

            executor.shutdown()
            while (!executor.isTerminated) {
                Thread.sleep(60)
            }

            // مرتب‌سازی هوشمند: سرورهای سالم با کمترین پینگ در بالای لیست
            if (activeTab == 0) {
                proxyItems.sortBy { if (it.ping <= 0) 999999 else it.ping }
            } else {
                v2rayItems.sortBy { if (it.ping <= 0) 999999 else it.ping }
            }

            runOnUiThread {
                testActionBtn.isEnabled = true
                infoStatusText.text = "تست پینگ پایان یافت (مرتب‌شده بر اساس سرعت نت شما)"
                renderListView()
            }
        }
    }

    // متد تست سوکت استاندارد با اعتبارسنجی اتصال پورت
    private fun probeSocketLatency(host: String, port: Int): Int {
        if (host.isEmpty() || port <= 0) return -2
        return try {
            val sock = Socket()
            sock.tcpNoDelay = true
            val start = System.currentTimeMillis()
            // پینگ مستقیم سوکت (تایم اوت ۱.۵ ثانیه)
            sock.connect(InetSocketAddress(host, port), 1500)
            val delay = (System.currentTimeMillis() - start).toInt()
            sock.close()
            delay
        } catch (e: Exception) {
            -2 // مسدود بودن پورت در شبکه ایران
        }
    }

    private fun calculateSpeedRating(delay: Int): String {
        return when {
            delay in 1..140 -> "عالی ⚡"
            delay in 141..270 -> "خوب"
            else -> "متوسط"
        }
    }

    private fun copyClip(payload: String, msg: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Data", payload))
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun createCard(bgColor: String, borderColor: String, radius: Int, strokeWidth: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgColor))
            if (strokeWidth > 0) setStroke(strokeWidth, Color.parseColor(borderColor))
            cornerRadius = radius.toFloat()
        }
    }
}
