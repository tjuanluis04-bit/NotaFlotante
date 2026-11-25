package com.notaflotante.app

import android.content.Context
import android.graphics.*
import android.view.View

/** Burbuja: círculo degradado con una hoja de nota (esquina doblada) y trazos de colores. */
class BubbleView(c: Context) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val d = resources.displayMetrics.density; val cx = w / 2
        val r = cx - 2 * d
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, w, w, 0xFF4F46E5.toInt(), 0xFFC026D3.toInt(), Shader.TileMode.CLAMP)
        cv.drawCircle(cx, cx, r, p); p.shader = null
        p.style = Paint.Style.STROKE; p.strokeWidth = 2 * d; p.color = 0x66FFFFFF
        cv.drawCircle(cx, cx, r, p)
        cv.save(); cv.rotate(-8f, cx, cx)
        val pw = w * 0.46f; val ph = w * 0.54f; val fold = pw * 0.3f
        val l = cx - pw / 2; val t = cx - ph / 2; val rt = l + pw; val b = t + ph
        p.style = Paint.Style.FILL; p.pathEffect = CornerPathEffect(3 * d); p.color = 0xFFFFFDF5.toInt()
        val paper = Path().apply { moveTo(l, t); lineTo(rt - fold, t); lineTo(rt, t + fold); lineTo(rt, b); lineTo(l, b); close() }
        cv.drawPath(paper, p)
        p.color = 0xFFD9D3C0.toInt()
        cv.drawPath(Path().apply { moveTo(rt - fold, t); lineTo(rt - fold, t + fold); lineTo(rt, t + fold); close() }, p)
        p.pathEffect = null
        val pad = pw * 0.16f
        // resaltado amarillo + línea de texto encima
        p.color = 0xFFFFE066.toInt(); cv.drawRect(l + pad, t + ph * 0.36f, rt - pad, t + ph * 0.36f + 5 * d, p)
        p.color = 0xFF2B2D42.toInt(); cv.drawRect(l + pad, t + ph * 0.36f + 1.5f * d, rt - pad * 1.6f, t + ph * 0.36f + 3.5f * d, p)
        p.color = 0xFFD62828.toInt(); cv.drawRect(l + pad, t + ph * 0.60f, rt - pad * 1.2f, t + ph * 0.60f + 2.2f * d, p)
        p.color = 0xFF1D4ED8.toInt(); cv.drawRect(l + pad, t + ph * 0.78f, l + pw * 0.62f, t + ph * 0.78f + 2.2f * d, p)
        cv.restore()
    }
}
