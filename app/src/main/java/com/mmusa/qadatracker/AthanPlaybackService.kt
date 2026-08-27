package com.mmusa.qadatracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Plays a full athan recording to completion.
 *
 * This exists specifically because playing it directly from inside
 * PrayerAlarmReceiver.onReceive() was cutting recordings off partway through:
 * a plain BroadcastReceiver's hosting process has no guarantee of staying
 * alive once onReceive() returns, and Android can reclaim it within seconds -
 * killing the MediaPlayer along with it before a multi-minute athan finishes.
 * A foreground service keeps the process alive for exactly as long as
 * playback takes, then stops itself and its (silent, low-priority)
 * notification automatically.
 */
class AthanPlaybackService : Service() {

    private var player: MediaPlayer? = null
    private var volumeObserver: android.database.ContentObserver? = null
    private var baselineVolume: Int = -1

    companion object {
        const val EXTRA_RES_ID = "resId"
        const val EXTRA_TITLE = "title"
        const val CHANNEL_ID = "athan_playback_v1"
        const val NOTIF_ID = 7001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resId = intent?.getIntExtra(EXTRA_RES_ID, 0) ?: 0
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Athan"

        ensureChannel()
        startForeground(NOTIF_ID, buildNotification(title))

        if (resId == 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            player = MediaPlayer.create(this, Uri.parse("android.resource://$packageName/$resId"), null, attrs, 0)
            player?.setOnCompletionListener {
                it.release()
                stopForeground(true)
                stopSelf()
            }
            player?.setOnErrorListener { _, _, _ ->
                stopForeground(true)
                stopSelf()
                true
            }
            player?.start()
            watchVolumeButtons()
        } catch (e: Exception) {
            stopForeground(true)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    /**
     * Stops playback entirely the moment the user presses either hardware
     * volume button, rather than just letting it get quieter/louder. There's
     * no public "volume KEY pressed" broadcast in Android, so this watches
     * the system volume setting itself via ContentObserver: pressing either
     * button changes the stored media stream volume, which fires onChange()
     * here regardless of whether the app is in the foreground.
     */
    private fun watchVolumeButtons() {
        val am = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        baselineVolume = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
        val handler = android.os.Handler(mainLooper)
        val observer = object : android.database.ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                val current = am.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                if (baselineVolume != -1 && current != baselineVolume) {
                    stopForeground(true)
                    stopSelf()
                }
            }
        }
        volumeObserver = observer
        try {
            contentResolver.registerContentObserver(
                android.provider.Settings.System.CONTENT_URI, true, observer
            )
        } catch (e: Exception) { /* if this fails, playback just isn't volume-key-interruptible on this device */ }
    }

    override fun onDestroy() {
        volumeObserver?.let {
            try { contentResolver.unregisterContentObserver(it) } catch (e: Exception) { /* already unregistered */ }
        }
        volumeObserver = null
        player?.let {
            try {
                if (it.isPlaying) it.stop()
                it.release()
            } catch (e: Exception) { /* already released or invalid state - safe to ignore */ }
        }
        player = null
        super.onDestroy()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                // Low importance and silent - this channel exists only to satisfy
                // Android's mandatory foreground-service notification requirement,
                // not to alert on its own (the athan audio itself is the alert).
                val channel = NotificationChannel(CHANNEL_ID, "Athan playback", NotificationManager.IMPORTANCE_LOW)
                channel.setSound(null, null)
                mgr.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(title: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
