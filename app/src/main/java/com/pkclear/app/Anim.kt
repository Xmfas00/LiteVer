package com.pkclear.app

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.R as MR

class Anim(private val ctx: Context) {

    private var pulse: ObjectAnimator? = null

    fun transitionPages(outgoing: View, incoming: View, toRight: Boolean) {
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
    }

    fun showPageInstant(pages: List<View>, idx: Int) {
        pages.forEachIndexed { i, v ->
            v.animate().cancel()
            v.alpha = 1f
            v.translationX = 0f
            v.visibility = if (i == idx) View.VISIBLE else View.GONE
        }
    }

    fun toggleExpand(v: View) {
        if (v.visibility == View.VISIBLE) {
            v.animate().alpha(0f).setDuration(180)
                .withEndAction { v.visibility = View.GONE }.start()
        } else {
            v.alpha = 0f
            v.visibility = View.VISIBLE
            v.animate().alpha(1f).setDuration(220).start()
        }
    }

    fun setPulse(dot: View, on: Boolean) {
        if (on) {
            if (pulse == null) {
                pulse = ObjectAnimator.ofFloat(dot, "alpha", 1f, 0.35f).apply {
                    duration = 700
                    repeatMode = ObjectAnimator.REVERSE
                    repeatCount = ObjectAnimator.INFINITE
                }
            }
            pulse?.start()
        } else {
            pulse?.cancel()
            dot.alpha = 1f
        }
    }

    fun cancelPulse() { pulse?.cancel() }

    // зелёный когда ок, серый когда нет
    fun setChip(iv: ImageView, ok: Boolean) {
        val bg = if (ok) "#34C759" else "#2C2C2E"
        val fg = if (ok) Color.WHITE else ContextCompat.getColor(ctx, R.color.text_dim)
        iv.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bg))
        iv.imageTintList = ColorStateList.valueOf(fg)
    }

    fun setTab(icon: ImageView, text: TextView, active: Boolean) {
        val c = ContextCompat.getColor(ctx, if (active) R.color.text_main else R.color.text_cap)
        icon.imageTintList = ColorStateList.valueOf(c)
        text.setTextColor(c)
    }

    fun styleLangButton(btn: MaterialButton, active: Boolean) {
        val primary = MaterialColors.getColor(btn, android.R.attr.colorPrimary)
        val onPrimary = MaterialColors.getColor(btn, MR.attr.colorOnPrimary)
        val outline = MaterialColors.getColor(btn, MR.attr.colorOutline)

        if (active) {
            btn.backgroundTintList = ColorStateList.valueOf(primary)
            btn.setTextColor(onPrimary)
            btn.strokeColor = ColorStateList.valueOf(primary)
        } else {
            btn.backgroundTintList = ColorStateList.valueOf(0x00000000)
            btn.setTextColor(ContextCompat.getColor(ctx, R.color.text_main))
            btn.strokeColor = ColorStateList.valueOf(outline)
        }
    }

    private fun dp(v: Int): Float = v * ctx.resources.displayMetrics.density
}