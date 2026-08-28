package com.pkclear.app

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import com.pkclear.app.databinding.ActivityMainBinding

class StatusManager(
    private val ctx: Context,
    private val b: ActivityMainBinding,
    private val anim: Anim
) {

    fun refresh() {
        val granted = ShizukuRunner.running() && ShizukuRunner.granted()
        val installed = ShizukuRunner.installed(ctx)
        val running = installed && ShizukuRunner.running()

        anim.setChip(b.chip1, installed)
        b.btnInstall.visibility = if (installed) View.GONE else View.VISIBLE

        anim.setChip(b.chip2, running)
        b.btnOpenShizuku.visibility = if (installed) View.VISIBLE else View.GONE

        anim.setChip(b.chip3, granted)
        b.btnGrant.visibility = if (granted || !running) View.GONE else View.VISIBLE

        b.btnClear.isEnabled = granted
        b.tvHint.visibility = if (granted) View.GONE else View.VISIBLE

        updateStatusIndicator(granted, running, installed)

        if (!ShizukuRunner.targetInstalled(ctx))
            b.tvResult.text = ctx.getString(R.string.not_found, ShizukuRunner.TARGET)

        updateJoyose(granted)

        b.btnPeak.isEnabled = granted
        updatePeak()
    }

    private fun updateStatusIndicator(granted: Boolean, running: Boolean, installed: Boolean) {
        val dotColor: Int
        when {
            granted -> {
                dotColor = ContextCompat.getColor(ctx, R.color.ok)
                b.statusText.setText(R.string.status_ready)
                anim.setPulse(b.statusDot, false)
            }
            running -> {
                dotColor = ContextCompat.getColor(ctx, R.color.amber)
                b.statusText.setText(R.string.status_need_grant)
                anim.setPulse(b.statusDot, true)
            }
            installed -> {
                dotColor = ContextCompat.getColor(ctx, R.color.amber)
                b.statusText.setText(R.string.status_need_start)
                anim.setPulse(b.statusDot, true)
            }
            else -> {
                dotColor = ContextCompat.getColor(ctx, R.color.bad)
                b.statusText.setText(R.string.status_need_install)
                anim.setPulse(b.statusDot, true)
            }
        }

        b.statusDot.backgroundTintList = ColorStateList.valueOf(dotColor)
        b.headerDot.backgroundTintList = ColorStateList.valueOf(dotColor)
    }

    fun updateJoyose(granted: Boolean) {
        when (ShizukuRunner.joyoseState(ctx)) {
            ShizukuRunner.PkgState.ACTIVE -> {
                b.joyoseDot.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.ok))
                b.joyoseStatus.setText(R.string.joyose_active)
                b.btnUninstallJoyose.visibility = View.VISIBLE
                b.btnRestoreJoyose.visibility = View.GONE
            }
            ShizukuRunner.PkgState.REMOVED -> {
                b.joyoseDot.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.amber))
                b.joyoseStatus.setText(R.string.joyose_removed)
                b.btnUninstallJoyose.visibility = View.GONE
                b.btnRestoreJoyose.visibility = View.VISIBLE
            }
            ShizukuRunner.PkgState.ABSENT -> {
                b.joyoseDot.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.bad))
                b.joyoseStatus.setText(R.string.joyose_absent)
                b.btnUninstallJoyose.visibility = View.GONE
                b.btnRestoreJoyose.visibility = View.GONE
            }
        }

        b.btnUninstallJoyose.isEnabled = granted
        b.btnRestoreJoyose.isEnabled = granted
    }

    fun updatePeak() {
        when (ShizukuRunner.peakRefreshEnabled(ctx)) {
            true -> b.peakStatus.setText(R.string.peak_on)
            false -> b.peakStatus.setText(R.string.peak_off)
            null -> b.peakStatus.setText(R.string.peak_unknown)
        }
    }
}