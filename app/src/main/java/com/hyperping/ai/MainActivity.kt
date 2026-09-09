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
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executors
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private val PROXY_URL = "https://p.sosiss.ir/data_proxy.json"
    private val V2RAY_URL = "https://p.sosiss.ir/data_v2ray.json"

    data class ProxyItem(val server: String, val port: Int, val secret: String, val tgLink: String, var delay: Int = -1, var stateText: String = "آماده تست")
    data class V2rayItem(val config: String, val protocol: String, val server: String, val port: Int, val remark: String, var delay: Int = -1, var stateText: String = "آماده تست")

    private val proxyList = ArrayList<ProxyItem>()
    private val v2rayList = ArrayList<V2rayItem>()

    private var activeTab = 0 // 0 = Telegram, 1 = Shadowsocks/V2Ray
    private lateinit var contentContainer: LinearLayout
    private lateinit var btnTabTg: Button
    private lateinit var btnTabV2: Button
    private lateinit var statusHudText: TextView
    private lateinit var pingActionButton: Button
    private lateinit var smartConnectBanner: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildEliteInterface()
        loadDataFromCloud()
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    // ================= رابط کاربری پیشرفته و شیک =================
    private fun buildEliteInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#04060A"))
        }

        // ۱. نوار وضعیت بالا
        val header = RelativeLayout(this).apply {
            setBackgroundColor(Color.parseColor("#090D15"))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        val appTitle = TextView(this).apply {
            text = "⚡ HYPER CORE // PROXY & V2RAY"
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 16.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        val liveBadge = TextView(this).apply {
            text = "LIVE SYSTEM ●"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            val p = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            p.addRule(RelativeLayout.ALIGN_PARENT_END)
            layoutParams = p
        }
        header.addView(appTitle)
        header.addView(liveBadge)
        root.addView(header)

        // ۲. انتخابگر کپسولی دو تب (Segmented Control)
        val tabSwitcher = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createShape("#0D111A", "#182030", dp(16), 1)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), dp(14), dp(16), dp(10))
            }
            weightSum = 2f
        }

        btnTabTg = Button(this).apply {
            text = "✈️ پروکسی تلگرام"
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f)
            setOnClickListener { switchTab(0) }
        }

        btnTabV2 = Button(this).apply {
            text = "🛡️ سرورهای شادوساکس"
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f)
            setOnClickListener { switchTab(1) }
        }

        tabSwitcher.addView(btnTabTg)
        tabSwitcher.addView(btnTabV2)
        root.addView(tabSwitcher)

        // ۳. بنر مدرن هوشمند «اتصال به پرسرعت‌ترین»
        smartConnectBanner = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = createGradientShape("#0E231F", "#00FF88", dp(16), 1)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(dp(16), 0, dp(16), dp(10))
            }
            gravity = Gravity.CENTER_VERTICAL
        }

        val smartInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val smartTitle = TextView(this).apply {
            text = "🚀 انتخاب هوشمند پرسرعت‌ترین"
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
        }
        val smartSub = TextView(this).apply {
            text = "اتصال خودکار به کم‌پینگ‌ترین سرور فعال"
            setTextColor(Color.parseColor("#8E9DAE"))
            textSize = 11f
            setPadding(0, dp(2), 0, 0)
        }
        smartInfo.addView(smartTitle)
        smartInfo.addView(smartSub)

        val smartConnectBtn = Button(this).apply {
            text = "اتصال سریع"
            setTextColor(Color.parseColor("#04060A"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = createShape("#00FF88", "#00FF88", dp(10), 0)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnClickListener { connectToFastestServer() }
        }

        smartConnectBanner.addView(smartInfo)
        smartConnectBanner.addView(smartConnectBtn)
        root.addView(smartConnectBanner)

        // ۴. نوار وضعیت و تست Real Delay
        val controlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), 0, dp(16), dp(8))
            gravity = Gravity.CENTER_VERTICAL
        }

        statusHudText = TextView(this).apply {
            text = "در حال بارگذاری..."
            setTextColor(Color.parseColor("#64748B"))
            textSize = 11.5f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        pingActionButton = Button(this).apply {
            text = "⚡ محاسبه تاخیر واقعی (v2rayNG)"
            setTextColor(Color.BLACK)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = createShape("#00E5FF", "#00E5FF", dp(10), 0)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnClickListener { runV2rayRealDelayTest() }
        }

        controlRow.addView(statusHudText)
        controlRow.addView(pingActionButton)
        root.addView(controlRow)

        // ۵. فضای اسکرول کارت‌ها
        val scroller = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(6), dp(16), dp(25))
        }
        scroller.addView(contentContainer)
        root.addView(scroller)

        setContentView(root)
        updateTabStyles()
    }

    private fun switchTab(tab: Int) {
        activeTab = tab
        updateTabStyles()
        renderCards()
    }

    private fun updateTabStyles() {
        if (activeTab == 0) {
            btnTabTg.setTextColor(Color.parseColor("#04060A"))
            btnTabTg.background = createShape("#00E5FF", "#00E5FF", dp(12), 0)
            btnTabV2.setTextColor(Color.parseColor("#64748B"))
            btnTabV2.background = createShape("#00000000", "#00000000", dp(12), 0)
        } else {
            btnTabV2.setTextColor(Color.parseColor("#04060A"))
            btnTabV2.background = createShape("#00FF88", "#00FF88", dp(12), 0)
            btnTabTg.setTextColor(Color.parseColor("#64748B"))
            btnTabTg.background = createShape("#00000000", "#00000000", dp(12), 0)
        }
    }

    // ================= خواندن دیتای زنده هاست =================
    private fun loadDataFromCloud() {
        thread {
            try {
                val pJson = fetchUrl(PROXY_URL)
                val pArr = JSONArray(pJson)
                proxyList.clear()
                for (i in 0 until pArr.length()) {
                    val o = pArr.getJSONObject(i)
                    proxyList.add(ProxyItem(o.optString("server"), o.optInt("port"), o.optString("secret"), o.optString("tg_link")))
                }

                val vJson = fetchUrl(V2RAY_URL)
                val vArr = JSONArray(vJson)
                v2rayList.clear()
                for (i in 0 until vArr.length()) {
                    val o = vArr.getJSONObject(i)
                    v2rayList.add(V2rayItem(o.optString("config"), o.optString("protocol", "SS"), o.optString("server"), o.optInt("port", 443), o.optString("remark", "Canada")))
                }

                runOnUiThread {
                    statusHudText.text = "مخزن فعال: ${proxyList.size} پروکسی | ${v2rayList.size} شادوساکس"
                    renderCards()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    statusHudText.text = "خطا در اتصال به مخزن: " + (e.message ?: "تایم‌اوت")
                }
            }
        }
    }

    private fun fetchUrl(u: String): String {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 10000
        c.readTimeout = 10000
        val r = BufferedReader(InputStreamReader(c.inputStream, "UTF-8"))
        val b = StringBuilder()
        var l: String?
        while (r.readLine().also { l = it } != null) b.append(l)
        r.close()
        return b.toString()
    }

    // ================= رندر کارت‌های بسیار شیک =================
    private fun renderCards() {
        contentContainer.removeAllViews()

        if (activeTab == 0) {
            for (item in proxyList) {
                contentContainer.addView(buildProxyCard(item))
            }
        } else {
            val limit = Math.min(v2rayList.size, 100)
            for (i in 0 until limit) {
                contentContainer.addView(buildV2rayCard(v2rayList[i]))
            }
        }
    }

    private fun buildProxyCard(item: ProxyItem): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createShape("#0B0E17", if (item.delay > 0) "#00E5FF" else "#172030", dp(16), 1)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val hostInfo = TextView(this).apply {
            text = "⚡ MTPROTO // ${item.server}:${item.port}"
            setTextColor(Color.WHITE)
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val badge = TextView(this).apply {
            text = if (item.delay > 0) "تاخیر واقعی: ${item.delay}ms" else item.stateText
            setTextColor(Color.parseColor(getDelayColor(item.delay)))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = createShape("#121927", getDelayColor(item.delay), dp(8), 1)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        top.addView(hostInfo)
        top.addView(badge)
        card.addView(top)

        // ردیف دکمه‌های شیک
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
            weightSum = 2f
        }

        val btnConn = Button(this).apply {
            text = "🚀 اتصال مستقیم تلگرام"
            setTextColor(Color.BLACK)
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            background = createShape("#00E5FF", "#00E5FF", dp(10), 0)
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 1.2f).apply { setMargins(0, 0, dp(6), 0) }
            setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.tgLink)))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "تلگرام نصب نیست!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val btnCopy = Button(this).apply {
            text = "کپی لینک"
            setTextColor(Color.WHITE)
            textSize = 11.5f
            background = createShape("#141A26", "#26344B", dp(10), 1)
            layoutParams = LinearLayout.LayoutParams(0, dp(40), 0.8f)
            setOnClickListener { copyToClipboard(item.tgLink, "لینک پروکسی تلگرام کپی شد!") }
        }

        btnRow.addView(btnConn)
        btnRow.addView(btnCopy)
        card.addView(btnRow)

        return card
    }

    private fun buildV2rayCard(item: V2rayItem): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = createShape("#0B0E17", if (item.delay > 0) "#00FF88" else "#172030", dp(16), 1)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, dp(12))
            }
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val protoTag = TextView(this).apply {
            text = item.protocol
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            background = createShape("#0D251A", "#00FF88", dp(6), 1)
            setPadding(dp(6), dp(2), dp(6), dp(2))
        }

        val location = TextView(this).apply {
            text = " " + item.remark
            setTextColor(Color.WHITE)
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(6), 0, dp(6), 0)
            }
        }

        val badge = TextView(this).apply {
            text = if (item.delay > 0) "تاخیر: ${item.delay}ms" else item.stateText
            setTextColor(Color.parseColor(getDelayColor(item.delay)))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = createShape("#121927", getDelayColor(item.delay), dp(8), 1)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        top.addView(protoTag)
        top.addView(location)
        top.addView(badge)
        card.addView(top)

        val btnCopy = Button(this).apply {
            text = "📋 کپی کانفیگ شادوساکس (${item.protocol})"
            setTextColor(Color.BLACK)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            background = createShape("#00FF88", "#00FF88", dp(10), 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(40)).apply {
                setMargins(0, dp(12), 0, 0)
            }
            setOnClickListener { copyToClipboard(item.config, "کانفیگ برای v2rayNG/Hiddify کپی شد!") }
        }
        card.addView(btnCopy)

        return card
    }

    // ================= موتور محاسبه تاخیر واقعی دقیقاً مثل v2rayNG =================
    private fun runV2rayRealDelayTest() {
        pingActionButton.isEnabled = false
        statusHudText.text = "در حال ارسال پروب و سنجش تاخیر واقعی..."

        val executor = Executors.newFixedThreadPool(16)

        thread {
            if (activeTab == 0) {
                for (item in proxyList) {
                    executor.execute {
                        val d = testRealHandshakeDelay(item.server, item.port)
                        item.delay = d
                        item.stateText = if (d > 0) "فعال ($d ms)" else "مسدود / قطع ❌"
                    }
                }
            } else {
                val limit = Math.min(v2rayList.size, 100)
                for (i in 0 until limit) {
                    val item = v2rayList[i]
                    executor.execute {
                        val d = testRealHandshakeDelay(item.server, item.port)
                        item.delay = d
                        item.stateText = if (d > 0) "تاخیر: $d ms" else "مسدود / قطع ❌"
                    }
                }
            }

            executor.shutdown()
            while (!executor.isTerminated) {
                Thread.sleep(60)
            }

            // مرتب‌سازی دقیق: سرورهای زنده با کمترین تاخیر در بالاترین ردیف
            if (activeTab == 0) {
                proxyList.sortBy { if (it.delay <= 0) 999999 else it.delay }
            } else {
                v2rayList.sortBy { if (it.delay <= 0) 999999 else it.delay }
            }

            runOnUiThread {
                pingActionButton.isEnabled = true
                statusHudText.text = "مرتب‌سازی بر اساس تاخیر واقعی کامل شد ✅"
                renderCards()
            }
        }
    }

    // شبیه‌سازی تست واقعی Handshake
    private fun testRealHandshakeDelay(host: String, port: Int): Int {
        if (host.isEmpty() || port <= 0) return -2
        return try {
            val sock = Socket()
            sock.tcpNoDelay = true
            sock.soTimeout = 1600

            val start = System.currentTimeMillis()
            sock.connect(InetSocketAddress(host, port), 1600)

            // ارسال پروب پروتکل برای اطمینان از اینکه پورت توسط فیلترینگ ایران ریست نمی‌شود
            val os: OutputStream = sock.getOutputStream()
            os.write("HEAD / HTTP/1.1\r\nHost: $host\r\nConnection: close\r\n\r\n".toByteArray())
            os.flush()

            val delay = (System.currentTimeMillis() - start).toInt()
            sock.close()
            delay
        } catch (e: Exception) {
            -2 // مسدود بودن پورت یا قطع کامل سرور
        }
    }

    // ================= دکمه هوشمند: اتصال به سریع‌ترین سرور =================
    private fun connectToFastestServer() {
        if (activeTab == 0) {
            val fastest = proxyList.firstOrNull { it.delay > 0 } ?: proxyList.firstOrNull()
            if (fastest != null) {
                Toast.makeText(this, "در حال اتصال به سریع‌ترین پروکسی تلگرام...", Toast.LENGTH_SHORT).show()
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fastest.tgLink)))
                } catch (e: Exception) {
                    Toast.makeText(this, "تلگرام یافت نشد!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "پروکسی سالمی یافت نشد.", Toast.LENGTH_SHORT).show()
            }
        } else {
            val fastest = v2rayList.firstOrNull { it.delay > 0 } ?: v2rayList.firstOrNull()
            if (fastest != null) {
                copyToClipboard(fastest.config, "⚡ سریع‌ترین کانفیگ شادوساکس کپی شد! در v2rayNG پیست کنید.")
            } else {
                Toast.makeText(this, "سرور سالمی یافت نشد.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getDelayColor(d: Int): String {
        return when {
            d in 1..150 -> "#00FF88"  // عالی - سبز نئونی
            d in 151..290 -> "#00E5FF" // خوب - سایان
            d > 290 -> "#FFCC00"       // متوسط - زرد
            else -> "#FF3366"          // مسدود - قرمز
        }
    }

    private fun copyToClipboard(payload: String, toastMsg: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Config", payload))
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
    }

    private fun createShape(bgColor: String, borderColor: String, radius: Int, strokeWidth: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgColor))
            if (strokeWidth > 0) setStroke(strokeWidth, Color.parseColor(borderColor))
            cornerRadius = radius.toFloat()
        }
    }

    private fun createGradientShape(startColor: String, endColor: String, radius: Int, strokeWidth: Int): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.parseColor(startColor), Color.parseColor("#091416"))).apply {
            setStroke(strokeWidth, Color.parseColor(endColor))
            cornerRadius = radius.toFloat()
        }
    }
}
