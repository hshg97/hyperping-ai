package com.hyperping.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor

class LocalDnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val channelId = "hyperping_vpn"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "HyperPing", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("HyperPing AI")
                .setContentText("سرویس محافظ DNS فعال است")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("HyperPing AI")
                .setContentText("سرویس محافظ DNS فعال است")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build()
        }

        startForeground(1017, notification)

        val builder = Builder()
        builder.setSession("HyperPing AI")
        builder.addAddress("10.0.0.2", 32)
        builder.addDnsServer("178.22.122.100")
        builder.addDnsServer("185.51.200.2")

        vpnInterface = builder.establish()
        return START_STICKY
    }

    override fun onDestroy() {
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }
}
