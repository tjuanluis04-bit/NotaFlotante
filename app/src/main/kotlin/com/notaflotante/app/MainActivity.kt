package com.notaflotante.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var perm: TextView
    private lateinit var start: TextView
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun btn(t: String, bg: Int, fg: Int, click: () -> Unit) = TextView(this).apply {
        text = t; setTextColor(fg); textSize = 16f; typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER; setPadding(dp(16), dp(16), dp(16), dp(16))
        background = GradientDrawable().apply { setColor(bg); cornerRadius = dp(16).toFloat() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
        setOnClickListener { click() }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val bg = 0xFF14152B.toInt()
        window.statusBarColor = bg; window.navigationBarColor = bg
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(bg)
            setPadding(dp(28), dp(72), dp(28), dp(28))
        }
        root.addView(BubbleView(this), LinearLayout.LayoutParams(dp(88), dp(88)))
        root.addView(TextView(this).apply {
            text = "Nota flotante"; textSize = 32f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(TextView(this).apply {
            text = "Una nota que flota sobre cualquier app."; textSize = 15f
            setTextColor(0xB3FFFFFF.toInt()); setPadding(0, dp(6), 0, dp(28))
        })
        listOf("🎨  Color de texto mientras escribes", "🖍  Resalta palabras con colores",
            "⧉  Copia toda la nota o solo la selección", "↔  Mueve y cambia el tamaño a tu gusto").forEach {
            root.addView(TextView(this).apply {
                text = it; textSize = 15f; setTextColor(Color.WHITE); setPadding(0, dp(7), 0, dp(7))
            })
        }
        root.addView(View(this), LinearLayout.LayoutParams(-1, 0, 1f))
        perm = btn("Dar permiso para mostrar sobre otras apps", 0xFF3A3D5C.toInt(), Color.WHITE) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        start = btn("Mostrar nota flotante", 0xFFFFF176.toInt(), 0xFF14152B.toInt()) {
            if (Settings.canDrawOverlays(this)) {
                startForegroundService(Intent(this, FloatingService::class.java)); moveTaskToBack(true)
            } else perm.performClick()
        }
        root.addView(perm); root.addView(start)
        root.addView(btn("Cerrar nota", 0x00000000, 0xB3FFFFFF.toInt()) {
            stopService(Intent(this, FloatingService::class.java))
        })
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        val ok = Settings.canDrawOverlays(this)
        perm.visibility = if (ok) View.GONE else View.VISIBLE
        start.alpha = if (ok) 1f else 0.45f
    }
}
