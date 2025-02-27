import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import com.shariarunix.multidns.helpers.SimpleHttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo

class JmDNSHelper(private val context: Context) {

    companion object {
        private const val TAG = "JmDNSHelper"
        private const val SERVICE_NAME = "shariarunix"
        private const val SERVICE_TYPE = "_http._tcp.local."
        private const val SERVICE_PORT = 8080
    }

    private var jmDNS: JmDNS? = null
    private var httpServer: SimpleHttpServer? = null

    suspend fun registerService() {
        val ipBytes = withContext(Dispatchers.IO) { getHostAddress() }
        if (ipBytes == null) {
            Log.e(TAG, "Could not resolve IP address")
            return
        }

        val inetAddress = withContext(Dispatchers.IO) {
            InetAddress.getByAddress(ipBytes)
        }
        Log.e(TAG, "Resolved IP: ${inetAddress.hostAddress}")

        withContext(Dispatchers.IO) {
            val wifiLock = (context.getSystemService(Context.WIFI_SERVICE) as WifiManager).createMulticastLock("JmDNSLock")
            wifiLock.setReferenceCounted(true)
            wifiLock.acquire()

            // Start the HTTP server
            httpServer = SimpleHttpServer(context, inetAddress, SERVICE_PORT)
            httpServer?.start()

            // Register mDNS service
            jmDNS = JmDNS.create(inetAddress, SERVICE_NAME)
            val serviceInfo = ServiceInfo.create(SERVICE_TYPE, SERVICE_NAME, SERVICE_PORT, "Android HTTP Server")
            jmDNS?.registerService(serviceInfo)

            Log.d(TAG, "Service registered: $SERVICE_NAME at ${inetAddress.hostAddress}:${SERVICE_PORT}")
            wifiLock.release()
        }
    }

    fun unregisterService() {
        jmDNS?.unregisterAllServices()
        jmDNS?.close()
        httpServer?.stop()
        Log.d(TAG, "JmDNS service unregistered and HTTP server stopped")
    }

    private fun getHostAddress(): ByteArray? {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo ?: return null
        val ipInt = wifiInfo.ipAddress
        if (ipInt == 0) return null

        return byteArrayOf(
            (ipInt and 0xFF).toByte(),
            ((ipInt shr 8) and 0xFF).toByte(),
            ((ipInt shr 16) and 0xFF).toByte(),
            ((ipInt shr 24) and 0xFF).toByte()
        )
    }
}
