package com.pkclear.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.pkclear.app.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var anim: Anim
    private lateinit var status: StatusManager

    private var currentIdx = 0
    private var inited = false

    private val onBinder = Shizuku.OnBinderReceivedListener { status.refresh() }
    private val onDead = Shizuku.OnBinderDeadListener { status.refresh() }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Lang.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        anim = Anim(this)
        status = StatusManager(this, b, anim)

        listOf(b.pageHome, b.pageSettings, b.pageAbout).forEach {
            it.setBackgroundColor(Color.BLACK)
        }

        listOf(
            b.tabHome, b.tabSettings, b.tabAbout,
            b.btnManual, b.rowSetup, b.rowTile, b.rowWarn,
            b.rowWhy, b.rowRisk, b.rowPriv, b.linkTg, b.linkGh
        ).forEach { it.foreground = null }

        setupClicks()

        Shizuku.addBinderReceivedListenerSticky(onBinder)
        Shizuku.addBinderDeadListener(onDead)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }

        showPage(0)
        status.refresh()
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(onBinder)
        Shizuku.removeBinderDeadListener(onDead)
        anim.cancelPulse()
        super.onDestroy()
    }

    private fun setupClicks() {
        b.tabHome.setOnClickListener { showPage(0) }
        b.tabSettings.setOnClickListener { showPage(1) }
        b.tabAbout.setOnClickListener { showPage(2) }

        b.btnRu.setOnClickListener { pickLang("ru") }
        b.btnEn.setOnClickListener { pickLang("en") }

        b.linkTg.setOnClickListener { openUrl("https://t.me/xmfas_utb") }
        b.linkGh.setOnClickListener { openUrl("https://github.com/Xmfas00") }

        b.rowSetup.setOnClickListener { anim.toggleExpand(b.txtSetup) }
        b.rowTile.setOnClickListener { anim.toggleExpand(b.txtTile) }
        b.rowWarn.setOnClickListener { anim.toggleExpand(b.txtWarn) }
        b.rowWhy.setOnClickListener { anim.toggleExpand(b.txtWhy) }
        b.rowRisk.setOnClickListener { anim.toggleExpand(b.txtRisk) }
        b.rowPriv.setOnClickListener { anim.toggleExpand(b.txtPriv) }

        b.btnClear.setOnClickListener { doClear() }
        b.btnInstall.setOnClickListener { openShizukuPage() }
        b.btnOpenShizuku.setOnClickListener { launchPackage(ShizukuRunner.SHIZUKU_PKG) }

        b.btnGrant.setOnClickListener {
            if (!ShizukuRunner.running()) {
                toast(getString(R.string.start_first))
                return@setOnClickListener
            }
            ShizukuRunner.request { ok ->
                runOnUiThread {
                    toast(if (ok) getString(R.string.granted_toast) else getString(R.string.denied_toast))
                    status.refresh()
                }
            }
        }

        b.btnManual.setOnClickListener {
            startActivity(Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", ShizukuRunner.TARGET, null)
            ))
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
        b.btnPeak.setOnClickListener { doPeakToggle() }
    }

    private fun showPage(idx: Int) {
        updateHeader(idx)
        if (inited && idx == currentIdx) return

        val pages = listOf(b.pageHome, b.pageSettings, b.pageAbout)

        if (!inited) {
            anim.showPageInstant(pages, idx)
            inited = true
            currentIdx = idx
            return
        }

        anim.transitionPages(pages[currentIdx], pages[idx], idx > currentIdx)
        currentIdx = idx
    }

    private fun updateHeader(idx: Int) {
        b.headerDot.visibility = if (idx == 0) View.VISIBLE else View.INVISIBLE

        b.headerTitle.text = when (idx) {
            1 -> getString(R.string.settings_title)
            2 -> getString(R.string.about_title)
            else -> getString(R.string.app_name)
        }

        anim.setTab(b.icoHome, b.txtHome, idx == 0)
        anim.setTab(b.icoSettings, b.txtSettings, idx == 1)
        anim.setTab(b.icoAbout, b.txtAbout, idx == 2)

        if (idx == 1) highlightLang()
    }

    private fun pickLang(code: String) {
        Lang.set(this, code)
        recreate()
    }

    private fun highlightLang() {
        val ru = Lang.get(this) == "ru"
        anim.styleLangButton(b.btnRu, ru)
        anim.styleLangButton(b.btnEn, !ru)
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

    private fun doJoyoseUninstall() {
        b.btnUninstallJoyose.isEnabled = false
        b.tvJoyose.text = getString(R.string.joyose_working)

        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) { ShizukuRunner.uninstallJoyose(this@MainActivity) }
            b.tvJoyose.text = (if (r.success) "✔ " else "✖ ") + r.message
            status.updateJoyose(ShizukuRunner.running() && ShizukuRunner.granted())
        }
    }

    private fun doJoyoseRestore() {
        b.btnRestoreJoyose.isEnabled = false
        b.tvJoyose.text = getString(R.string.joyose_working)

        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) { ShizukuRunner.restoreJoyose(this@MainActivity) }
            b.tvJoyose.text = (if (r.success) "✔ " else "✖ ") + r.message
            status.updateJoyose(ShizukuRunner.running() && ShizukuRunner.granted())
        }
    }

    private fun doPeakToggle() {
        val target = ShizukuRunner.peakRefreshEnabled(this) != true

        b.btnPeak.isEnabled = false
        b.tvPeak.text = getString(R.string.peak_working)

        lifecycleScope.launch {
            val r = withContext(Dispatchers.IO) { ShizukuRunner.peakRefresh(this@MainActivity, target) }
            b.tvPeak.text = (if (r.success) "✔ " else "✖ ") + r.message
            status.updatePeak()
            b.btnPeak.isEnabled = ShizukuRunner.running() && ShizukuRunner.granted()
        }
    }

    private fun openShizukuPage() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${ShizukuRunner.SHIZUKU_PKG}")))
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app")))
        }
    }

    private fun launchPackage(pkg: String) {
        val i = packageManager.getLaunchIntentForPackage(pkg)
        if (i != null) startActivity(i) else toast(getString(R.string.could_not_open))
    }

    private fun openUrl(u: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) }
        catch (_: Exception) { toast(getString(R.string.could_not_open)) }
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}