package com.shariarunix.multidns.helpers
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import fi.iki.elonen.NanoHTTPD
import java.net.InetAddress

class SimpleHttpServer(private val context: Context, private val inetAddress: InetAddress, port: Int) :
    NanoHTTPD(inetAddress.hostAddress, port) {

    override fun serve(session: IHTTPSession?): Response {
        if (session?.uri == "/log" && session.method == Method.GET) {
            val params = session.parameters
            val message = params["message"]?.firstOrNull() ?: "No message received"

            Log.d("SimpleHttpServer", "Received Text: $message")
            context.showNotification(message)

            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Log received: $message")
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>shariarUnix</title>
                <style>
                    /* General Styles */
                    body {
                        font-family: Arial, sans-serif;
                        background-color: #f4f4f4;
                        color: #333;
                        text-align: center;
                        margin: 0;
                        padding: 0;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        height: 100vh;
                    }
            
                    /* Container */
                    .container {
                        background: white;
                        padding: 20px;
                        border-radius: 10px;
                        box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
                        max-width: 400px;
                    }
            
                    h1 {
                        color: #007BFF;
                        font-size: 2em;
                    }
            
                    p {
                        font-size: 1.2em;
                    }
            
                    /* Input Field */
                    input {
                        width: 80%;
                        padding: 10px;
                        margin-top: 10px;
                        border: 1px solid #ccc;
                        border-radius: 5px;
                        font-size: 1em;
                    }
            
                    /* Button */
                    .log-button {
                        background-color: #007BFF;
                        color: white;
                        border: none;
                        padding: 10px 20px;
                        font-size: 1em;
                        cursor: pointer;
                        border-radius: 5px;
                        margin-top: 15px;
                    }
            
                    .log-button:hover {
                        background-color: #0056b3;
                    }
            
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>Hello Guys!</h1>
                    <p>This page is served from an Android app.</p>
                    <input type="text" id="inputText" placeholder="Enter text here..." />
                    <button class="log-button" onclick="sendLog()">Send Text</button>
                </div>
            
                <script>
                    function sendLog() {
                        let textValue = document.getElementById("inputText").value;
                        fetch('/log?message=' + encodeURIComponent(textValue))
                            .then(response => response.text())
                            .then(data => console.log('Log Sent:', data))
                            .catch(error => console.error('Error:', error));
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        return newFixedLengthResponse(Response.Status.OK, "text/html", htmlContent)
    }
}



fun Context.showNotification(message: String) {
    val channelId = "shariarunix_channel"
    val notificationId = 1

    val channel = NotificationChannel(
        channelId,
        "ShariarUnix Notifications",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "General notifications"
    }

    val notificationManager = getSystemService(NotificationManager::class.java)
    notificationManager.createNotificationChannel(channel)

    // Build the Notification
    val builder = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("New Notification")
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)

    // Show the Notification
    with(NotificationManagerCompat.from(this)) {
        if (ActivityCompat.checkSelfPermission(this@showNotification, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        notify(notificationId, builder.build())
    }
}
