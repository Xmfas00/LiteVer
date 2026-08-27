package com.pkclear.app

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import rikka.shizuku.Shizuku

object ShizukuRunner {

    const val TARGET = "com.miui.powerkeeper"
    const val JOYOSE = "com.xiaomi.joyose"
    const val SHIZUKU_PKG = "moe.shizuku.privileged.api"
    private const val REQ = 777

    data class ClearResult(val success: Boolean, val message: String)
    enum class PkgState { ACTIVE, REMOVED, ABSENT }

    fun installed(ctx: Context) = try {
        ctx.packageManager.getPackageInfo(SHIZUKU_PKG, 0); true
    } catch (_: Exception) { false }

    fun targetInstalled(ctx: Context) = try {
        ctx.packageManager.getPackageInfo(TARGET, 0); true
    } catch (_: Exception) { false }

    fun running() = try { Shizuku.pingBinder() } catch (_: Exception) { false }

    fun granted() = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    fun request(onResult: (Boolean) -> Unit) {
        if (granted()) { onResult(true); return }
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode != REQ) return
                Shizuku.removeRequestPermissionResultListener(this)
                onResult(grantResult == PackageManager.PERMISSION_GRANTED)
            }
        }
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(REQ)
    }

    fun joyoseState(ctx: Context): PkgState {
        val pm = ctx.packageManager
        val active = try { pm.getPackageInfo(JOYOSE, 0); true } catch (_: Exception) { false }
        if (active) return PkgState.ACTIVE
        val present = try {
            if (Build.VERSION.SDK_INT >= 33)
                pm.getPackageInfo(JOYOSE,
                    PackageManager.PackageInfoFlags.of(PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong()))
            else @Suppress("DEPRECATION")
            pm.getPackageInfo(JOYOSE, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            true
        } catch (_: Exception) { false }
        return if (present) PkgState.REMOVED else PkgState.ABSENT
    }

    fun clear(ctx: Context): ClearResult = run(ctx, arrayOf("sh", "-c", "pm clear $TARGET")) { ok, _ ->
        if (ok) ctx.getString(R.string.data_cleared, TARGET) else null
    }

    fun uninstallJoyose(ctx: Context): ClearResult =
        run(ctx, arrayOf("sh", "-c", "pm uninstall --user 0 $JOYOSE")) { ok, _ ->
            if (ok) ctx.getString(R.string.joyose_uninstalled) else null
        }

    fun restoreJoyose(ctx: Context): ClearResult =
        run(ctx, arrayOf("sh", "-c", "pm install-existing $JOYOSE")) { ok, _ ->
            if (ok) ctx.getString(R.string.joyose_restored) else null
        }

    fun peakRefresh(ctx: Context, on: Boolean): ClearResult =
        run(ctx, arrayOf("sh", "-c", "settings put system peak_refresh_rate " + if (on) "1" else "0")) { ok, _ ->
            if (ok) ctx.getString(if (on) R.string.peak_enabled else R.string.peak_disabled) else null
        }

    fun peakRefreshEnabled(ctx: Context): Boolean? = try {
        when (sh(arrayOf("sh", "-c", "settings get system peak_refresh_rate")).out.trim()) {
            "1" -> true
            "0" -> false
            else -> null
        }
    } catch (_: Exception) { null }

    private fun run(
        ctx: Context,
        cmd: Array<String>,
        okMsg: (Boolean, String) -> String?
    ): ClearResult = try {
        val r = sh(cmd)
        val good = r.code == 0 &&
                !r.err.contains("Exception", true) &&
                !r.err.contains("Failure", true)
        val msg = okMsg(good, r.out)
        if (msg != null) ClearResult(true, msg)
        else ClearResult(false, (r.err.ifEmpty { r.out }).ifEmpty { "exit=${r.code}" })
    } catch (e: Exception) {
        ClearResult(false, ctx.getString(R.string.err_prefix, e.message ?: ""))
    }

    private data class ShResult(val code: Int, val out: String, val err: String)

    private fun sh(cmd: Array<String>): ShResult {
        val m = Shizuku::class.java.declaredMethods
            .firstOrNull { it.name == "newProcess" && it.parameterCount == 3 }
            ?: throw NoSuchMethodException("Shizuku.newProcess not found")
        m.isAccessible = true
        val p = m.invoke(null, cmd, null, null) as Process
        val out = p.inputStream.bufferedReader().readText().trim()
        val err = p.errorStream.bufferedReader().readText().trim()
        val code = p.waitFor()
        return ShResult(code, out, err)
    }
}