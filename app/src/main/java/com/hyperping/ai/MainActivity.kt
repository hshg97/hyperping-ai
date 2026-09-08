package com.hyperping.ai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private val VPN_REQUEST_CODE = 1017

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("HyperPingPrefs", Context.MODE_PRIVATE)
        val isUnlocked = sharedPref.getBoolean("is_authenticated", false)

        setContent {
            var authenticated by remember { mutableStateOf(isUnlocked) }
            var isConnected by remember { mutableStateOf(false) }

            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF08090C))) {
                if (!authenticated) {
                    PasscodeScreen { enteredCode ->
                        if (enteredCode == "1017") {
                            sharedPref.edit().putBoolean("is_authenticated", true).apply()
                            authenticated = true
                        } else {
                            Toast.makeText(this@MainActivity, "دسترسی غیرمجاز! کد امنیتی اشتباه است.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    DashboardScreen(
                        isConnected = isConnected,
                        onConnectClick = {
                            if (!isConnected) {
                                prepareAndConnectVpn()
                                isConnected = true
                            } else {
                                stopService(Intent(this@MainActivity, LocalDnsVpnService::class.java))
                                isConnected = false
                            }
                        }
                    )
                }
            }
        }
    }

    private fun prepareAndConnectVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            onActivityResult(VPN_REQUEST_CODE, Activity.RESULT_OK, null)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val serviceIntent = Intent(this, LocalDnsVpnService::class.java).apply {
                putExtra("PRIMARY_DNS", "178.22.122.100")
                putExtra("SECONDARY_DNS", "185.51.200.2")
            }
            startService(serviceIntent)
            Toast.makeText(this, "محافظ DNS گیمینگ فعال شد!", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun PasscodeScreen(onSuccess: (String) -> Unit) {
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("HYPERPING AI", color = Color(0xFF00FF88), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("کد فعال‌سازی VIP را وارد کنید", color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSuccess(code) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("ورود به سیستم", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardScreen(isConnected: Boolean, onConnectClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isConnected) "محافظ DNS متصل است" else "سیستم آماده اتصال",
            color = if (isConnected) Color(0xFF00FF88) else Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onConnectClick,
            modifier = Modifier.size(160.dp),
            shape = RoundedCornerShape(100.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) Color(0xFFFF3366) else Color(0xFF00FF88)
            )
        ) {
            Text(
                text = if (isConnected) "قطع اتصال" else "اتصال سریع",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
