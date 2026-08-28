package com.pkclear.app

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import kotlin.concurrent.thread

class ClearTileService : TileService() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(Lang.wrap(base))
    }

    override fun onStartListening() = refresh()

    override fun onClick() {
        super.onClick()
        unlockAndRun { perform() }
    }

    private fun perform() {
        if (!(ShizukuRunner.running() && ShizukuRunner.granted())) {
            post(getString(R.string.need_setup), getString(R.string.need_setup_msg))
            openApp()
            return
        }
        tile { subtitle = getString(R.string.clearing); state = Tile.STATE_ACTIVE }
        thread {
            val r = ShizukuRunner.clear(this@ClearTileService)
            post(if (r.success) getString(R.string.cleared) else getString(R.string.clear_error), r.message)
            refresh()
        }
    }

    private fun refresh() {
        val ready = ShizukuRunner.running() && ShizukuRunner.granted()
        tile {
            state = if (ready) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            subtitle = if (ShizukuRunner.running()) getString(R.string.ready) else getString(R.string.not_running)
        }
    }

    private inline fun tile(block: Tile.() -> Unit) {
        qsTile?.apply { block(); updateTile() }
    }

    private fun openApp() {
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(i)
        }
    }

    private fun post(title: String, text: String) {
        getSystemService(NotificationManager::class.java).notify(1,
            NotificationCompat.Builder(this, App.CHANNEL)
                .setSmallIcon(R.drawable.ic_tile)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                .build())
    }
}