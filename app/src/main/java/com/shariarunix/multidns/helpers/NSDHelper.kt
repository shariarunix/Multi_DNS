package com.shariarunix.multidns.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

class NSDHelper(private val context: Context) {

    companion object {
        const val SERVICE_NAME = "shariar"
        const val SERVICE_TYPE = "_http._tcp."
        const val SERVICE_PORT = 5353
        const val TAG = "NSDHelper"
    }

    private var nsdManager: NsdManager? = null
    private var wifiManager: WifiManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    init {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    suspend fun registerService() {
        Log.e(TAG, "Port assigned: $SERVICE_PORT")

        registrationListener?.let {
            nsdManager?.unregisterService(it)
        }

        val ipBytes = withContext(Dispatchers.IO) { getHostAddress() }
        if (ipBytes == null) {
            Log.e(TAG, "Could not resolve IP address")
            return
        }

        val inetAddress = withContext(Dispatchers.IO) {
            InetAddress.getByAddress(ipBytes)
        }

        Log.e(TAG, "Resolved IP: ${inetAddress.hostAddress}")

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = SERVICE_PORT
            host = inetAddress // This might be ignored in some Android versions
        }

        Log.e(TAG, "Registering service: ${serviceInfo.serviceName} on port ${serviceInfo.port}, host: ${serviceInfo.host}")

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(TAG, "Service registered: ${info.serviceName} at port ${info.port}, host: ${info.host} NSDServiceInfo : $info")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service registration failed: $errorCode")
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo) {
                Log.w(TAG, "Service unregistered: $arg0")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.d(TAG, "Service unregistration failed: $errorCode")
            }
        }

        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener!!)
    }

    fun unregisterService() {
        registrationListener?.let {
            nsdManager?.unregisterService(it)
        }
    }

    @SuppressLint("DefaultLocale")
    private fun getHostAddress(): ByteArray? {
        val wifiInfo: WifiInfo? = wifiManager?.connectionInfo
        if (wifiInfo == null || wifiInfo.ipAddress == 0) {
            Log.e(TAG, "WiFi not connected or no valid IP found.")
            return null
        }

        val ipInt: Int = wifiInfo.ipAddress
        val ipBytes = byteArrayOf(
            (ipInt and 0xFF).toByte(),
            ((ipInt shr 8) and 0xFF).toByte(),
            ((ipInt shr 16) and 0xFF).toByte(),
            ((ipInt shr 24) and 0xFF).toByte()
        )

        val ipStr = ipBytes.joinToString(".") { it.toUByte().toString() }
        Log.e(TAG, "Formatted IP Address: $ipStr")

        return ipBytes
    }

}
