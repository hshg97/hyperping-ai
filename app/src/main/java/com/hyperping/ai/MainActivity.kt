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

    // مدل‌های داده
    data class ProxyItem(val server: String, val port: Int, val secret: String, val tgLink: String, var ping: Int = -1)
    data class V2rayItem(val config: String, val protocol: String, val server: String, val port: Int, val remark: String, var ping: Int = -1)

    private val proxyList = ArrayList<ProxyItem>()
    private val v2rayList = ArrayList<V2rayItem>()

    private var activeTab = 0 // 0 = تلگرام, 1 = وی‌توری
    private lateinit var contentContainer: LinearLayout
    private lateinit var btnTabTelegram: Button
    private lateinit var btnTabV2ray: Button
    private lateinit var statusHeader: TextView
    private lateinit var pingAllBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildModernUI()
        loadDataFromHost()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ================= ساخت رابط کاربری مدرن گیمینگ =================
    private fun buildModernUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#06080D"))
        }

        // ۱. هدر اصلی
        val topBar = RelativeLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0A0E17"))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        val title = TextView(this).apply {
            text = "⚡ HYPER PROXY // V2RAY HUB"
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
        }
        topBar.addView(title)
        root.addView(topBar)

        // ۲. ردیف سوئیچ دو تب
        val tabLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
            weightSum = 2f
        }

        btnTabTelegram = Button(this).apply {
            text = "پروکسی‌های تلگرام"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dp(6), 0)
            }
            setOnClickListener { switchTab(0) }
        }

        btnTabV2ray = Button(this).apply {
            text = "کانفیگ‌های V2Ray"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(6), 0, 0, 0)
            }
            setOnClickListener { switchTab(1) }
        }

        tabLayout.addView(btnTabTelegram)
        tabLayout.addView(btnTabV2ray)
        root.addView(tabLayout)

        // ۳. کارت عملیات سریع (تست پینگ و رفرش)
        val actionCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createCard("#0E131F", "#1A2235", dp(14), 1)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), dp(4), dp(16), dp(12))
            }
            gravity = Gravity.CENTER_VERTICAL
        }

        statusHeader = TextView(this).apply {
            text = "در حال بارگذاری مخزن..."
            setTextColor(Color.parseColor("#8E9DAE"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        pingAllBtn = Button(this).apply {
            text = "⚡ تست پینگ و مرتب‌سازی"
            setTextColor(Color.parseColor("#05070A"))
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            background = createCard("#00FF88", "#00FF88", dp(10), 0)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setOnClickListener { startRealPingTest() }
        }

        actionCard.addView(statusHeader)
        actionCard.addView(pingAllBtn)
        root.addView(actionCard)

        // ۴. محوطه اسکرول برای لیست آیتم‌ها
        val scroller = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(20))
        }
        scroller.addView(contentContainer)
        root.addView(scroller)

        setContentView(root)
        updateTabStyles()
    }

    private fun switchTab(tab: Int) {
        activeTab = tab
        updateTabStyles()
        renderList()
    }

    private fun updateTabStyles() {
        if (activeTab == 0) {
            btnTabTelegram.setTextColor(Color.BLACK)
            btnTabTelegram.background = createCard("#00E5FF", "#00E5FF", dp(10), 0)
            btnTabV2ray.setTextColor(Color.parseColor("#8E9DAE"))
            btnTabV2ray.background = createCard("#121724", "#1E273A", dp(10), 1)
        } else {
            btnTabV2ray.setTextColor(Color.BLACK)
            btnTabV2ray.background = createCard("#00FF88", "#00FF88", dp(10), 0)
            btnTabTelegram.setTextColor(Color.parseColor("#8E9DAE"))
            btnTabTelegram.background = createCard("#121724", "#1E273A", dp(10), 1)
        }
    }

    // ================= دریافت دیتا از هاست p.sosiss.ir =================
    private fun loadDataFromHost() {
        thread {
            try {
                // دریافت پروکسی‌ها
                val pJson = fetchUrl(PROXY_URL)
                val pArr = JSONArray(pJson)
                proxyList.clear()
                for (i in 0 until pArr.length()) {
                    val o = pArr.getJSONObject(i)
                    proxyList.add(
                        ProxyItem(
                            o.optString("server"),
                            o.optInt("port"),
                            o.optString("secret"),
                            o.optString("tg_link")
                        )
                    )
                }

                // دریافت کانفیگ‌های وی‌توری
                val vJson = fetchUrl(V2RAY_URL)
                val vArr = JSONArray(vJson)
                v2rayList.clear()
                for (i in 0 until vArr.length()) {
                    val o = vArr.getJSONObject(i)
                    v2rayList.add(
                        V2rayItem(
                            o.optString("config"),
                            o.optString("protocol", "V2RAY"),
                            o.optString("server"),
                            o.optInt("port", 443),
                            o.optString("remark", "Server")
                        )
                    )
                }

                runOnUiThread {
                    statusHeader.text = "مخزن آماده است (${proxyList.size} پروکسی | ${v2rayList.size} کانفیگ)"
                    renderList()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusHeader.text = "خطا در دریافت مخزن: " + (e.message ?: "تایم‌اوت")
                }
            }
        }
    }

    private fun fetchUrl(u: String): String {
        val conn = URL(u).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        val reader = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8"))
        val sb = StringBuilder()
        var l: String?
        while (reader.readLine().also { l = it } != null) sb.append(l)
        reader.close()
        return sb.toString()
    }

    // ================= رندر کارت‌ها =================
    private fun renderList() {
        contentContainer.removeAllViews()

        if (activeTab == 0) {
            // رندر پروکسی‌های تلگرام
            for (item in proxyList) {
                val card = createProxyCard(item)
                contentContainer.addView(card)
            }
        } else {
            // رندر کانفیگ‌های وی‌توری (نمایش ۵۰ مورد اول برای سرعت بالا)
            val limit = Math.min(v2rayList.size, 80)
            for (i in 0 until limit) {
                val card = createV2rayCard(v2rayList[i])
                contentContainer.addView(card)
            }
        }
    }

    private fun createProxyCard(item: ProxyItem): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCard("#0C101A", "#182033", dp(14), 1)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val info = TextView(this).apply {
            text = "IP: ${item.server} : ${item.port}"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val pingBadge = TextView(this).apply {
            text = if (item.ping > 0) "${item.ping} ms" else if (item.ping == -2) "Timeout" else "---"
            setTextColor(getPingColor(item.ping))
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            background = createCard("#121927", getPingColorHex(item.ping), dp(8), 1)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        topRow.addView(info)
        topRow.addView(pingBadge)
        card.addView(topRow)

        // ردیف دکمه‌ها
        val btnsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
            weightSum = 2f
        }

        val btnConnect = Button(this).apply {
            text = "⚡ اتصال مستقیم به تلگرام"
            setTextColor(Color.BLACK)
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            background = createCard("#00E5FF", "#00E5FF", dp(8), 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f).apply {
                setMargins(0, 0, dp(6), 0)
            }
            setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.tgLink))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "برنامه تلگرام یافت نشد!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnCopy = Button(this).apply {
            text = "کپی لینک"
            setTextColor(Color.WHITE)
            textSize = 11.5f
            background = createCard("#161E2E", "#2A364F", dp(8), 1)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f)
            setOnClickListener {
                copyToClipboard(item.tgLink, "لینک پروکسی تلگرام کپی شد!")
            }
        }

        btnsRow.addView(btnConnect)
        btnsRow.addView(btnCopy)
        card.addView(btnsRow)

        return card
    }

    private fun createV2rayCard(item: V2rayItem): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createCard("#0C101A", "#182033", dp(14), 1)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(10))
            }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val protoBadge = TextView(this).apply {
            text = item.protocol
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = createCard("#0E2218", "#00FF88", dp(6), 1)
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }

        val remarkText = TextView(this).apply {
            text = " " + item.remark
            setTextColor(Color.WHITE)
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(6), 0, dp(6), 0)
            }
        }

        val pingBadge = TextView(this).apply {
            text = if (item.ping > 0) "${item.ping} ms" else if (item.ping == -2) "Timeout" else "---"
            setTextColor(getPingColor(item.ping))
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            background = createCard("#121927", getPingColorHex(item.ping), dp(8), 1)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        topRow.addView(protoBadge)
        topRow.addView(remarkText)
        topRow.addView(pingBadge)
        card.addView(topRow)

        // دکمه کپی کانفیگ
        val btnCopy = Button(this).apply {
            text = "📋 کپی کانفیگ V2Ray (${item.protocol})"
            setTextColor(Color.BLACK)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = createCard("#00FF88", "#00FF88", dp(8), 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, dp(10), 0, 0)
            }
            setOnClickListener {
                copyToClipboard(item.config, "کانفیگ ${item.protocol} کپی شد!")
            }
        }
        card.addView(btnCopy)

        return card
    }

    // ================= تست پینگ واقعی با اینترنت گوشی =================
    private fun startRealPingTest() {
        pingAllBtn.isEnabled = false
        statusHeader.text = "در حال اندازه‌گیری پینگ..."

        val executor = Executors.newFixedThreadPool(12)

        thread {
            if (activeTab == 0) {
                // تست پروکسی تلگرام
                for (item in proxyList) {
                    executor.execute {
                        item.ping = tcpPing(item.server, item.port)
                    }
                }
            } else {
                // تست کانفیگ‌های وی‌توری
                val limit = Math.min(v2rayList.size, 80)
                for (i in 0 until limit) {
                    val item = v2rayList[i]
                    executor.execute {
                        item.ping = tcpPing(item.server, item.port)
                    }
                }
            }

            executor.shutdown()
            while (!executor.isTerminated) {
                Thread.sleep(100)
            }

            // مرتب‌سازی هوشمند از کمترین به بیشترین پینگ
            if (activeTab == 0) {
                proxyList.sortBy { if (it.ping <= 0) 9999 else it.ping }
            } else {
                v2rayList.sortBy { if (it.ping <= 0) 9999 else it.ping }
            }

            runOnUiThread {
                pingAllBtn.isEnabled = true
                statusHeader.text = "مرتب‌سازی بر اساس بهترین پینگ انجام شد ✅"
                renderList()
            }
        }
    }

    private fun tcpPing(host: String, port: Int): Int {
        if (host.isEmpty() || port <= 0) return -2
        return try {
            val sock = Socket()
            val start = System.currentTimeMillis()
            sock.connect(InetSocketAddress(host, port), 1200)
            val ping = (System.currentTimeMillis() - start).toInt()
            sock.close()
            ping
        } catch (e: Exception) {
            -2 // تایم اوت
        }
    }

    private fun getPingColor(p: Int): Int {
        return when {
            p in 1..120 -> Color.parseColor("#00FF88") // عالی - سبز
            p in 121..280 -> Color.parseColor("#00E5FF") // خوب - سایان
            p > 280 -> Color.parseColor("#FFCC00") // متوسط - زرد
            else -> Color.parseColor("#FF3366") // قطع - قرمز
        }
    }

    private fun getPingColorHex(p: Int): String {
        return when {
            p in 1..120 -> "#00FF88"
            p in 121..280 -> "#00E5FF"
            p > 280 -> "#FFCC00"
            else -> "#FF3366"
        }
    }

    private fun copyToClipboard(text: String, msg: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Payload", text))
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
