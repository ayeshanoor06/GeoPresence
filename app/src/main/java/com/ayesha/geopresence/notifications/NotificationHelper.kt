package com.ayesha.geopresence.notifications

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ayesha.geopresence.MainActivity
import com.ayesha.geopresence.R
import com.ayesha.geopresence.data.model.AttendanceStatus
import com.ayesha.geopresence.util.PermissionUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationHelper(context: Context) {

    private val appContext = context.applicationContext
    private val manager = NotificationManagerCompat.from(appContext)

    fun attendanceMarked(
        status: AttendanceStatus,
        entryMillis: Long,
        queued: Boolean,
        afterGrace: Boolean
    ) {
        val label = if (status == AttendanceStatus.PRESENT) "Present" else "Late"
        var text = "You arrived on campus at ${formatTime(entryMillis)}."
        if (status == AttendanceStatus.LATE) {
            text += if (afterGrace) " This is after the grace period."
            else " You arrived after the first class started."
        }
        if (queued) text += " Saved on your phone; it will sync when you’re online."

        show(
            id = ID_ENTRY,
            title = "Attendance marked: $label",
            text = text,
            color = if (status == AttendanceStatus.PRESENT) COLOR_PRIMARY else COLOR_GOLD
        )
    }

    fun exitRecorded(durationText: String) = show(
        id = ID_EXIT,
        title = "You left campus",
        text = "Time on campus today: $durationText.",
        color = COLOR_PRIMARY
    )

    fun fakeGpsRejected() = show(
        id = ID_WARNING,
        title = "Attendance rejected",
        text = "A fake GPS location was detected, so your attendance was not recorded. " +
                "The department admin has been notified.",
        color = COLOR_ERROR
    )

    fun saveFailed() = show(
        id = ID_WARNING,
        title = "Couldn’t save your attendance",
        text = "Open GeoPresence and check your internet connection.",
        color = COLOR_ERROR
    )

    fun sendTest() = show(
        id = ID_TEST,
        title = "Notifications are working",
        text = "You’ll get a message like this when your attendance is marked.",
        color = COLOR_PRIMARY
    )

    // ───────────────────────── internals ─────────────────────────

    @SuppressLint("MissingPermission")
    private fun show(id: Int, title: String, text: String, color: Int) {
        if (!PermissionUtils.hasNotifications(appContext)) return
        if (!manager.areNotificationsEnabled()) return
        ensureChannel()

        val openApp = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_geopresence)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(color)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .build()

        manager.notify(id, notification)
    }

    private fun ensureChannel() {
        val channel = NotificationChannelCompat.Builder(
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName("Attendance updates")
            .setDescription("Confirms when your campus attendance is marked")
            .build()
        manager.createNotificationChannel(channel)   // safe to call repeatedly
    }

    private fun formatTime(millis: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

    private companion object {
        const val CHANNEL_ID = "attendance_updates"
        const val ID_ENTRY = 2001
        const val ID_EXIT = 2002
        const val ID_WARNING = 2003
        const val ID_TEST = 2004

        val COLOR_PRIMARY = 0xFF2B638B.toInt()
        val COLOR_GOLD = 0xFF7D5700.toInt()
        val COLOR_ERROR = 0xFFBA1A1A.toInt()
    }
}