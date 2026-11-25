package com.notaflotante.app

import android.app.*
import android.content.*
import android.content.ClipData
import android.content.ClipboardManager
import android.content.pm.ServiceInfo
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.text.*
import android.text.style.*
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*

class Theme(val card: Int, val head: Int, val ink: Int, val hint: Int, val label: Int, val chipDark: Int,
            val stroke: Int, val sel: Int, val text: IntArray, val hl: IntArray, val ruled: Boolean)

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: EditText
    private lateinit var cp: WindowManager.LayoutParams
    private lateinit var bp: WindowManager.LayoutParams
    private var card: LinearLayout? = null
    private var bubble: View? = null
    private var showingBubble = false
    private var th = NIGHT
    private var textColor = 0
    private var insStart = 0
    private var insCount = 0
    private val h = Handler(Looper.getMainLooper())
    private val saver = Runnable { save() }
    private val swatches = mutableListOf<Pair<View, Int>>()

    companion object {
        val OLD_INK = 0xFF22223B.toInt()
        val NIGHT = Theme(0xFF1B1C26.toInt(), 0xFF0F1019.toInt(), 0xFFE8E8F0.toInt(), 0x66FFFFFF, 0xAAFFFFFF.toInt(),
            0xFFE8E8F0.toInt(), 0x33FFFFFF, 0xFFFFFFFF.toInt(),
            intArrayOf(0xFFE8E8F0.toInt(), 0xFFFF6B6B.toInt(), 0xFF6CA8FF.toInt(), 0xFF4ADE80.toInt(), 0xFFC084FC.toInt(), 0xFFFFA94D.toInt()),
            intArrayOf(0xFF8A7A00.toInt(), 0xFF1F7A5A.toInt(), 0xFF1F5E8A.toInt(), 0xFF8A2F5E.toInt(), 0xFF8A4A1F.toInt()), false)
        val NOTE = Theme(0xFFFBF6E6.toInt(), 0xFF2B2D42.toInt(), 0xFF1E2A5A.toInt(), 0x66000000, 0x99000000.toInt(),
            0xFF2B2D42.toInt(), 0x33000000, 0xFF2B2D42.toInt(),
            intArrayOf(0xFF1E2A5A.toInt(), 0xFFD62828.toInt(), 0xFF1D4ED8.toInt(), 0xFF15803D.toInt(), 0xFF7E22CE.toInt(), 0xFFEA580C.toInt()),
            intArrayOf(0xFFFFF176.toInt(), 0xFFA7F3D0.toInt(), 0xFFBAE6FD.toInt(), 0xFFFBCFE8.toInt(), 0xFFFED7AA.toInt()), true)
        val THEMES = arrayOf(NIGHT, NOTE)
        const val FL = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WindowManager::class.java)
        prefs = getSharedPreferences("nota", MODE_PRIVATE)
        th = THEMES[prefs.getInt("theme", 0)]; textColor = th.text[0]
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        if (i?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        goForeground()
        if (card == null) build()
        return START_NOT_STICKY
    }

    private fun goForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("float", "Nota flotante", NotificationManager.IMPORTANCE_LOW))
        val stop = PendingIntent.getService(this, 0, Intent(this, FloatingService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "float").setSmallIcon(R.drawable.ic_fg)
            .setContentTitle("Nota flotante activa").setContentText("Toca “Cerrar” para quitarla")
            .addAction(0, "Cerrar", stop).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun shape(c: Int, r: Float, sc: Int = 0, sw: Int = 0) = GradientDrawable().apply {
        setColor(c); cornerRadius = r; if (sw > 0) setStroke(sw, sc)
    }

    private fun chip(t: String, dark: Boolean, click: () -> Unit) = TextView(this).apply {
        text = t; textSize = 14f; typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (dark) th.chipDark else Color.WHITE); setPadding(dp(10), dp(8), dp(10), dp(8))
        setOnClickListener { click() }
    }

    private fun swatch(c: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) }
        background = oval(c, false)
    }

    private fun oval(c: Int, sel: Boolean) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL; setColor(c)
        setStroke(if (sel) dp(3) else dp(1), if (sel) th.sel else th.stroke)
    }

    private fun refreshSwatches() = swatches.forEach { (v, c) -> v.background = oval(c, c == textColor) }

    private fun row(label: String) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10), 0, dp(6), 0)
        addView(TextView(context).apply {
            text = label; textSize = 12f; setTextColor(th.label)
            layoutParams = LinearLayout.LayoutParams(dp(58), -2)
        })
    }

    private fun build() {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = shape(th.card, dp(18).toFloat(), th.stroke, dp(1))
        }
        val r = dp(18).toFloat()
        val head = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4), dp(2), dp(4), dp(2))
            background = GradientDrawable().apply { setColor(th.head); cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f) }
        }
        val title = TextView(this).apply {
            text = "Nota"; setTextColor(Color.WHITE); textSize = 15f; typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(12), dp(10), dp(4), dp(10)); layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        head.addView(title)
        head.addView(chip(if (th === NIGHT) "▤" else "☾", false) { toggleTheme() })
        head.addView(chip("Copiar", false) { copy() })
        head.addView(chip("—", false) { collapse() })
        head.addView(chip("✕", false) { stopSelf() })
        c.addView(head)

        if (th.text.indexOf(textColor) < 0) textColor = th.text[0]
        val r1 = row("Texto")
        th.text.forEach { col ->
            val v = swatch(col); swatches.add(v to col)
            v.setOnClickListener { textColor = col; refreshSwatches(); if (hasSel()) paint(true, col) }
            r1.addView(v)
        }
        val r2 = row("Resaltar")
        th.hl.forEach { col -> val v = swatch(col); v.setOnClickListener { paint(false, col) }; r2.addView(v) }
        r2.addView(chip("⌫", true) { paint(false, null) })
        c.addView(r1); c.addView(r2); refreshSwatches()

        editor = NotebookEditText(this).apply {
            ruled = th.ruled; lineColor = 0x553F7FBF; marginColor = 0x88D94F4F.toInt()
            background = null; hint = "Escribe tu nota…"; setHintTextColor(th.hint); setTextColor(th.ink)
            textSize = 17f; gravity = Gravity.TOP or Gravity.START; setPadding(if (th.ruled) dp(46) else dp(14), dp(8), dp(14), dp(8))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        prefs.getString("note", null)?.let {
            val sp = SpannableStringBuilder(Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT))
            while (sp.isNotEmpty() && sp.last() == '\n') sp.delete(sp.length - 1, sp.length)
            editor.setText(sp)
            val pal = prefs.getInt("pal", 1)
            if (pal != THEMES.indexOf(th)) remap(editor.text, THEMES[pal], th)
        }
        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, n: Int) { insStart = st; insCount = n }
            override fun afterTextChanged(e: Editable) {
                if (insCount > 0) {
                    val prev = e.getSpans(0, e.length, ForegroundColorSpan::class.java)
                        .firstOrNull { e.getSpanEnd(it) == insStart && it.foregroundColor == textColor }
                    if (prev != null) e.setSpan(prev, e.getSpanStart(prev), insStart + insCount, FL)
                    else e.setSpan(ForegroundColorSpan(textColor), insStart, insStart + insCount, FL)
                }
                h.removeCallbacks(saver); h.postDelayed(saver, 600)
            }
        })
        editor.setOnTouchListener { _, ev ->
            if (ev.action == MotionEvent.ACTION_DOWN && (cp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) != 0) {
                setFocusable(true)
                editor.post { editor.requestFocus(); getSystemService(InputMethodManager::class.java).showSoftInput(editor, 0) }
            }
            false
        }
        c.addView(editor)

        val foot = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(6), 0, dp(6), dp(2)) }
        foot.addView(chip("Pegar", true) { paste() })
        foot.addView(chip("Limpiar", true) { editor.setText("") })
        foot.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        val grip = chip("◢", true) {}
        foot.addView(grip); c.addView(foot)

        if (!::cp.isInitialized) {
        val w = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        cp = WindowManager.LayoutParams(dp(320), dp(400), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, w, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = dp(24); y = dp(120)
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }
        bp = WindowManager.LayoutParams(dp(60), dp(60), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        } else cp.flags = cp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        drag(title, { card }, { cp }, null)
        c.setOnTouchListener { _, ev -> if (ev.action == MotionEvent.ACTION_OUTSIDE) setFocusable(false); false }
        var gx = 0f; var gy = 0f; var gw = 0; var gh = 0
        grip.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> { gx = ev.rawX; gy = ev.rawY; gw = cp.width; gh = cp.height }
                MotionEvent.ACTION_MOVE -> {
                    cp.width = (gw + ev.rawX - gx).toInt().coerceIn(dp(230), resources.displayMetrics.widthPixels)
                    cp.height = (gh + ev.rawY - gy).toInt().coerceIn(dp(260), resources.displayMetrics.heightPixels)
                    wm.updateViewLayout(c, cp)
                }
            }
            true
        }
        card = c
        wm.addView(c, cp)
    }

    private fun drag(handle: View, view: () -> View?, p: () -> WindowManager.LayoutParams, click: (() -> Unit)?) {
        var sx = 0f; var sy = 0f; var px = 0; var py = 0; var moved = false
        handle.setOnTouchListener { _, e ->
            val lp = p()
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; px = lp.x; py = lp.y; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - sx; val dy = e.rawY - sy
                    if (Math.abs(dx) + Math.abs(dy) > dp(6)) moved = true
                    if (moved) { lp.x = px + dx.toInt(); lp.y = py + dy.toInt(); view()?.let { wm.updateViewLayout(it, lp) } }
                }
                MotionEvent.ACTION_UP -> if (!moved) click?.invoke()
            }
            true
        }
    }

    private fun setFocusable(on: Boolean) {
        val c = card ?: return
        if (showingBubble) return
        val f = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        cp.flags = if (on) cp.flags and f.inv() else cp.flags or f
        wm.updateViewLayout(c, cp)
        if (!on) getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(c.windowToken, 0)
    }

    private fun collapse() {
        if (showingBubble) return
        save(); setFocusable(false)
        bp.x = cp.x; bp.y = cp.y
        wm.removeView(card)
        if (bubble == null) {
            bubble = BubbleView(this)
            drag(bubble!!, { bubble }, { bp }) { expand() }
        }
        wm.addView(bubble, bp); showingBubble = true
    }

    private fun expand() {
        wm.removeView(bubble); cp.x = bp.x; cp.y = bp.y
        showingBubble = false; wm.addView(card, cp)
    }

    private fun toggleTheme() {
        val old = th; val nw = if (old === NIGHT) NOTE else NIGHT
        remap(editor.text, old, nw)
        val i = old.text.indexOf(textColor); textColor = if (i >= 0) nw.text[i] else nw.text[0]
        th = nw; prefs.edit().putInt("theme", THEMES.indexOf(nw)).apply()
        save(); wm.removeView(card); swatches.clear(); build()
    }

    private fun remap(t: Editable, a: Theme, b: Theme) {
        for (sp in t.getSpans(0, t.length, Any::class.java)) {
            val st = t.getSpanStart(sp); val en = t.getSpanEnd(sp)
            if (sp is ForegroundColorSpan) {
                var i = a.text.indexOf(sp.foregroundColor); if (sp.foregroundColor == OLD_INK) i = 0
                if (i >= 0) { t.removeSpan(sp); t.setSpan(ForegroundColorSpan(b.text[i]), st, en, FL) }
            } else if (sp is BackgroundColorSpan) {
                val i = a.hl.indexOf(sp.backgroundColor)
                if (i >= 0) { t.removeSpan(sp); t.setSpan(BackgroundColorSpan(b.hl[i]), st, en, FL) }
            }
        }
    }

    private fun hasSel() = editor.selectionStart >= 0 && editor.selectionStart != editor.selectionEnd

    private fun paint(fg: Boolean, color: Int?) {
        if (!hasSel()) { if (!fg) toast("Selecciona palabras para resaltarlas"); return }
        val s = minOf(editor.selectionStart, editor.selectionEnd); val e = maxOf(editor.selectionStart, editor.selectionEnd)
        val t = editor.text
        fun mk(col: Int): Any = if (fg) ForegroundColorSpan(col) else BackgroundColorSpan(col)
        for (sp in t.getSpans(s, e, Any::class.java)) {
            val col = when { fg && sp is ForegroundColorSpan -> sp.foregroundColor
                !fg && sp is BackgroundColorSpan -> sp.backgroundColor
                else -> continue }
            val a = t.getSpanStart(sp); val b = t.getSpanEnd(sp)
            t.removeSpan(sp)
            if (a < s) t.setSpan(mk(col), a, s, FL)
            if (b > e) t.setSpan(mk(col), e, b, FL)
        }
        if (color != null) t.setSpan(mk(color), s, e, FL)
        h.removeCallbacks(saver); h.postDelayed(saver, 600)
    }

    private fun copy() {
        val s = minOf(editor.selectionStart, editor.selectionEnd); val e = maxOf(editor.selectionStart, editor.selectionEnd)
        val sel = s >= 0 && s != e
        val part: CharSequence = if (sel) editor.text.subSequence(s, e) else editor.text
        if (part.isEmpty()) { toast("La nota está vacía"); return }
        val html = Html.toHtml(SpannableString(part), Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newHtmlText("nota", part.toString(), html))
        toast(if (sel) "Selección copiada" else "Nota copiada")
    }

    private fun paste() {
        setFocusable(true); editor.requestFocus()
        editor.post {
            val t = getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.coerceToText(this)
            if (t == null) { toast("Nada que pegar"); return@post }
            val s = maxOf(0, minOf(editor.selectionStart, editor.selectionEnd)); val e = maxOf(0, editor.selectionStart, editor.selectionEnd)
            editor.text.replace(s, e, t)
        }
    }

    private fun save() {
        if (!::editor.isInitialized) return
        prefs.edit().putString("note", Html.toHtml(editor.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)).putInt("pal", THEMES.indexOf(th)).apply()
    }

    override fun onDestroy() {
        h.removeCallbacks(saver); save()
        try { wm.removeView(if (showingBubble) bubble else card) } catch (_: Exception) {}
        super.onDestroy()
    }
}
