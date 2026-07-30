package com.pkclear.app

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.R as MR
import com.pkclear.app.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private var pulse: ObjectAnimator? = null
    private var currentIdx = 0
    private var inited = false

    private val onBinder = Shizuku.OnBinderReceivedListener { updateStatus() }
    private val onDead = Shizuku.OnBinderDeadListener { updateStatus() }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Lang.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.pageHome.setBackgroundColor(Color.BLACK)
        b.pageSettings.setBackgroundColor(Color.BLACK)
        b.pageAbout.setBackgroundColor(Color.BLACK)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        Shizuku.addBinderReceivedListenerSticky(onBinder)
        Shizuku.addBinderDeadListener(onDead)

        b.tabHome.foreground = null
        b.tabSettings.foreground = null
        b.tabAbout.foreground = null
        listOf(
            b.btnManual, b.rowSetup, b.rowTile, b.rowWarn,
            b.rowWhy, b.rowRisk, b.rowPriv, b.linkTg, b.linkGh
        ).forEach { it.foreground = null }

        b.tabHome.setOnClickListener { showPage(0) }
        b.tabSettings.setOnClickListener { showPage(1) }
        b.tabAbout.setOnClickListener { showPage(2) }

        b.btnRu.setOnClickListener { pickLang("ru") }
        b.btnEn.setOnClickListener { pickLang("en") }
        b.linkTg.setOnClickListener { openUrl("https://t.me/xmfas_utb") }
        b.linkGh.setOnClickListener { openUrl("https://github.com/Xmfas00") }

        b.rowSetup.setOnClickListener { toggle(b.txtSetup) }
        b.rowTile.setOnClickListener { toggle(b.txtTile) }
        b.rowWarn.setOnClickListener { toggle(b.txtWarn) }
        b.rowWhy.setOnClickListener { toggle(b.txtWhy) }
        b.rowRisk.setOnClickListener { toggle(b.txtRisk) }
        b.rowPriv.setOnClickListener { toggle(b.txtPriv) }

        b.btnClear.setOnClickListener { doClear() }
        b.btnInstall.setOnClickListener { openShizukuPage() }
        b.btnOpenShizuku.setOnClickListener { launchPackage(ShizukuRunner.SHIZUKU_PKG) }
        b.btnGrant.setOnClickListener {
            if (!ShizukuRunner.running()) { toast(getString(R.string.start_first)); return@setOnClickListener }
            ShizukuRunner.request { ok ->
                runOnUiThread {
                    toast(if (ok) getString(R.string.granted_toast) else getString(R.string.denied_toast))
                    updateStatus()
                }
            }
        }
        b.btnManual.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", ShizukuRunner.TARGET, null)))
        }

        b.btnUninstallJoyose.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.joyose_warn_title)
                .setMessage(R.string.joyose_warn_msg)
                .setPositiveButton(R.string.joyose_uninstall) { _, _ -> doJoyoseUninstall() }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        b.btnRestoreJoyose.setOnClickListener { doJoyoseRestore() }

        showPage(0)
        updateStatus()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(onBinder)
        Shizuku.removeBinderDeadListener(onDead)
        pulse?.cancel()
        super.onDestroy()
    }

    private fun pageOf(i: Int): View = when (i) {
        0 -> b.pageHome
        1 -> b.pageSettings
        else -> b.pageAbout
    }

    private fun dp(v: Int): Float = v * resources.displayMetrics.density

    private fun showPage(idx: Int) {
        updateHeaderAndTabs(idx)
        if (inited && idx == currentIdx) return

        val incoming = pageOf(idx)

        if (!inited) {
            for (i in 0..2) {
                val v = pageOf(i)
                v.animate().cancel()
                v.alpha = 1f
                v.translationX = 0f
                v.visibility = if (i == idx) View.VISIBLE else View.GONE
            }
            inited = true
            currentIdx = idx
            return
        }

        val toRight = idx > currentIdx
        val outgoing = pageOf(currentIdx)

        outgoing.animate().cancel()
        outgoing.visibility = View.GONE
        outgoing.alpha = 1f
        outgoing.translationX = 0f

        incoming.visibility = View.VISIBLE
        incoming.translationX = dp(20) * if (toRight) 1f else -1f
        incoming.alpha = 0f
        incoming.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .start()

        currentIdx = idx
    }

    private fun updateHeaderAndTabs(idx: Int) {
        b.headerDot.visibility = if (idx == 0) View.VISIBLE else View.INVISIBLE
        b.headerTitle.text = when (idx) {
            1 -> getString(R.string.settings_title)
            2 -> getString(R.string.about_title)
            else -> getString(R.string.app_name)
        }
        setTab(b.icoHome, b.txtHome, idx == 0)
        setTab(b.icoSettings, b.txtSettings, idx == 1)
        setTab(b.icoAbout, b.txtAbout, idx == 2)
        if (idx == 1) highlightLang()
    }

    private fun setTab(icon: ImageView, text: TextView, active: Boolean) {
        val c = ContextCompat.getColor(this, if (active) R.color.text_main else R.color.text_cap)
        icon.imageTintList = ColorStateList.valueOf(c)
        text.setTextColor(c)
    }

    private fun toggle(v: View) {
        if (v.visibility == View.VISIBLE) {
            v.animate().alpha(0f).setDuration(180)
                .withEndAction { v.visibility = View.GONE }.start()
        } else {
            v.alpha = 0f
            v.visibility = View.VISIBLE
            v.animate().alpha(1f).setDuration(220).start()
        }
    }

    private fun setChip(iv: ImageView, ok: Boolean) {
        val bg = if (ok) "#34C759" else "#2C2C2E"
        val fg = if (ok) Color.WHITE else ContextCompat.getColor(this, R.color.text_dim)
        iv.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bg))
        iv.imageTintList = ColorStateList.valueOf(fg)
    }

    private fun setPulse(on: Boolean) {
        if (on) {
            if (pulse == null) {
                pulse = ObjectAnimator.ofFloat(b.statusDot, "alpha", 1f, 0.35f).apply {
                    duration = 700
                    repeatMode = ObjectAnimator.REVERSE
                    repeatCount = ObjectAnimator.INFINITE
                }
            }
            pulse?.start()
        } else {
            pulse?.cancel()
            b.statusDot.alpha = 1f
        }
    }

    private fun pickLang(code: String) {
        Lang.set(this, code)
        recreate()
    }

    private fun highlightLang() {
        val ru = Lang.get(this) == "ru"
        styleLangButton(b.btnRu, ru)
        styleLangButton(b.btnEn, !ru)
    }

    private fun styleLangButton(btn: MaterialButton, active: Boolean) {
        val primary = MaterialColors.getColor(btn, android.R.attr.colorPrimary)
        val onPrimary = MaterialColors.getColor(btn, MR.attr.colorOnPrimary)
        val outline = MaterialColors.getColor(btn, MR.attr.colorOutline)
        if (active) {
            btn.backgroundTintList = ColorStateList.valueOf(primary)
            btn.setTextColor(onPrimary)
            btn.strokeColor = ColorStateList.valueOf(primary)
        } else {
            btn.backgroundTintList = ColorStateList.valueOf(0x00000000)
            btn.setTextColor(ContextCompat.getColor(this, R.color.text_main))
            btn.strokeColor = ColorStateList.valueOf(outline)
        }
    }

    private fun openUrl(u: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) }
        catch (_: Exception) { toast(getString(R.string.could_not_open)) }
    }

    private fun updateStatus() {
        val granted = ShizukuRunner.running() && ShizukuRunner.granted()
        val installed = ShizukuRunner.installed(this)
        val running = installed && ShizukuRunner.running()

        setChip(b.chip1, installed); b.btnInstall.visibility = if (installed) View.GONE else View.VISIBLE
        setChip(b.chip2, running);   b.btnOpenShizuku.visibility = if (installed) View.VISIBLE else View.GONE
        setChip(b.chip3, granted);   b.btnGrant.visibility = if (granted || !running) View.GONE else View.VISIBLE

        b.btnClear.isEnabled = granted
        b.tvHint.visibility = if (granted) View.GONE else View.VISIBLE

        val dotColor: Int
        when {
            granted -> {
                dotColor = ContextCompat.getColor(this, R.color.ok)
                b.statusText.setText(R.string.status_ready)
                setPulse(false)
            }
            running -> {
                dotColor = ContextCompat.getColor(this, R.color.amber)
                b.statusText.setText(R.string.status_need_grant)
                setPulse(true)
            }
            installed -> {
                dotColor = ContextCompat.getColor(this, R.color.amber)
                b.statusText.setText(R.string.status_need_start)
                setPulse(true)
            }
            else -> {
                dotColor = ContextCompat.getColor(this, R.color.bad)
                b.statusText.setText(R.string.status_need_install)
                setPulse(true)
            }
        }
        b.statusDot.backgroundTintList = ColorStateList.valueOf(dotColor)
        b.headerDot.backgroundTintList = ColorStateList.valueOf(dotColor)

        if (!ShizukuRunner.targetInstalled(this))
            b.tvResult.text = getString(R.string.not_found, ShizukuRunner.TARGET)

        updateJoyose(granted)
    }

    private fun updateJoyose(granted: Boolean) {
        when (ShizukuRunner.joyoseState(this)) {
            ShizukuRunner.PkgState.ACTIVE -> {
                b.joyoseDot.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.ok))
                b.joyoseStatus.setText(R.string.joyose_active)
                b.btnUninstallJoyose.visibility = View.VISIBLE
                b.btnRestoreJoyose.visibility = View.GONE
            }
            ShizukuRunner.PkgState.REMOVED -> {
                b.joyoseDot.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.amber))
                b.joyoseStatus.setText(R.string.joyose_removed)
                b.btnUninstallJoyose.visibility = View.GONE
                b.btnRestoreJoyose.visibility = View.VISIBLE
            }
            ShizukuRunner.PkgState.ABSENT -> {
                b.joyoseDot.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.bad))
                b.joyoseStatus.setText(R.string.joyose_absent)
                b.btnUninstallJoyose.visibility = View.GONE
                b.btnRestoreJoyose.visibility = View.GONE
            }
        }
        b.btnUninstallJoyose.isEnabled = granted
        b.btnRestoreJoyose.isEnabled = granted
    }

    private fun doJoyoseUninstall() {
        b.btnUninstallJoyose.isEnabled = false
        b.tvJoyose.text = getString(R.string.joyose_working)
        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) { ShizukuRunner.uninstallJoyose(this@MainActivity) }
            b.tvJoyose.text = (if (r.success) "✔ " else "✖ ") + r.message
            updateJoyose(ShizukuRunner.running() && ShizukuRunner.granted())
        }
    }

    private fun doJoyoseRestore() {
        b.btnRestoreJoyose.isEnabled = false
        b.tvJoyose.text = getString(R.string.joyose_working)
        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) { ShizukuRunner.restoreJoyose(this@MainActivity) }
            b.tvJoyose.text = (if (r.success) "✔ " else "✖ ") + r.message
            updateJoyose(ShizukuRunner.running() && ShizukuRunner.granted())
        }
    }

    private fun doClear() {
        b.btnClear.isEnabled = false
        b.tvResult.text = getString(R.string.clearing_data)
        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) { ShizukuRunner.clear(this@MainActivity) }
            b.tvResult.text = (if (r.success) "✔ " else "✖ ") + r.message
            b.btnClear.isEnabled = true
        }
    }

    private fun openShizukuPage() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${ShizukuRunner.SHIZUKU_PKG}")))
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app")))
        }
    }

    private fun launchPackage(pkg: String) {
        val i = packageManager.getLaunchIntentForPackage(pkg)
        if (i != null) startActivity(i) else toast(getString(R.string.could_not_open))
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}