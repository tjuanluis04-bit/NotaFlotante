package com.notaflotante.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.EditText

/** EditText que puede dibujar renglones y margen rojo, como una libreta. */
class NotebookEditText(c: Context) : EditText(c) {
    var ruled = false
    var lineColor = 0
    var marginColor = 0
    private val p = Paint()
    override fun onDraw(cv: Canvas) {
        if (ruled) {
            val d = resources.displayMetrics.density
            val lay = layout
            val pitch = if (lay != null && lay.lineCount > 1) (lay.getLineBaseline(1) - lay.getLineBaseline(0)).toFloat() else lineHeight.toFloat()
            var y = (if (lay != null) lay.getLineBaseline(0).toFloat() else paddingTop + pitch * 0.8f) + 4 * d
            val bottom = (scrollY + height).toFloat()
            p.strokeWidth = d; p.color = lineColor
            while (y < bottom) { cv.drawLine(0f, y, (width + scrollX).toFloat(), y, p); y += pitch }
            p.color = marginColor; cv.drawLine(36 * d, scrollY.toFloat(), 36 * d, bottom, p)
        }
        super.onDraw(cv)
    }
}
