package com.example.jarvis.bubble

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.jarvis.AssistantEngine
import com.example.jarvis.MainActivity
import com.example.jarvis.R
import com.example.jarvis.speech.Status

class BubbleService : Service() {

    private lateinit var wm: WindowManager
    private lateinit var engine: AssistantEngine
    private var bubble: View? = null
    private var pulseAnimator: ObjectAnimator? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WindowManager::class.java)
        createNotificationChannel()

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            fgsType
        )

        engine = AssistantEngine(this) { state ->
            setBubbleState(state)
        }

        addBubble()
    }

    private fun addBubble() {
        val size = (56 * resources.displayMetrics.density).toInt()
        val v = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(COLOR_IDLE)
            }
            elevation = 10f * resources.displayMetrics.density
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val lp = WindowManager.LayoutParams(
            size,
            size,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 400
        }

        v.setOnTouchListener(DragTouchListener(lp, wm, v) {
            engine.toggleListening()
        })

        try {
            wm.addView(v, lp)
            bubble = v
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setBubbleState(status: Status) {
        val v = bubble ?: return
        val bg = v.background as? GradientDrawable ?: return

        when (status) {
            Status.Listening -> {
                updateFgsMicrophone(true)
                bg.setColor(COLOR_LISTENING)
                startPulseAnimation(v)
            }
            Status.Thinking -> {
                stopPulseAnimation(v)
                updateFgsMicrophone(false)
                bg.setColor(COLOR_THINKING)
            }
            Status.Speaking -> {
                stopPulseAnimation(v)
                updateFgsMicrophone(false)
                bg.setColor(COLOR_SPEAKING)
            }
            Status.Idle -> {
                stopPulseAnimation(v)
                updateFgsMicrophone(false)
                bg.setColor(COLOR_IDLE)
            }
        }
    }

    private fun updateFgsMicrophone(listening: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val type = if (listening) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            }
            try {
                ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), type)
            } catch (_: Exception) {}
        }
    }

    private fun startPulseAnimation(v: View) {
        pulseAnimator?.cancel()
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.18f, 1.0f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.18f, 1.0f)
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(v, scaleX, scaleY).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    private fun stopPulseAnimation(v: View) {
        pulseAnimator?.cancel()
        pulseAnimator = null
        v.scaleX = 1.0f
        v.scaleY = 1.0f
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BubbleService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Jarvis Assistant Active")
            .setContentText("Tap bubble to speak")
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Bubble Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service for Jarvis overlay bubble"
            }
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopPulseAnimation(bubble ?: return)
        bubble?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {}
        }
        bubble = null
        engine.release()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "jarvis_bubble_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.example.jarvis.ACTION_STOP"

        const val COLOR_IDLE = 0xFF7F77DD.toInt()       // Purple
        const val COLOR_LISTENING = 0xFFFF4444.toInt()  // Red
        const val COLOR_THINKING = 0xFFFFAA00.toInt()   // Amber
        const val COLOR_SPEAKING = 0xFF00B4D8.toInt()   // Teal
    }
}
