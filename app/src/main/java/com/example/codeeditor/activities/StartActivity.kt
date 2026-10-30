package com.example.codeeditor.activities

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.codeeditor.R
import com.example.codeeditor.ui.theme.ThemeManager

class StartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.initTheme(this)
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        enableEdgeToEdge()
        setContentView(R.layout.activity_start)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mainView.post {
            adjustSizes()
        }

        val navigateToMain = {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<Button>(R.id.continueButton)?.setOnClickListener {
            navigateToMain()
        }

        findViewById<Button>(R.id.newButton)?.setOnClickListener {
            navigateToMain()
        }
    }

    private fun adjustSizes() {
        val windowMetrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds
        } else {
            val dm = resources.displayMetrics
            Rect(0, 0, dm.widthPixels, dm.heightPixels)
        }

        val screenHeight = windowMetrics.height().toFloat()
        if (screenHeight <= 0) return

        // Rule: 50sp = 10% of screen height (0.10 * screenHeight)
        // 1sp in PX = (0.10 * screenHeight) / 50 = 0.002 * screenHeight
        val spToPxFactor = (0.10f * screenHeight) / 50f

        // Adjust TextView text sizes dynamically
        val textView = findViewById<TextView>(R.id.textView)
        textView?.setTextSize(TypedValue.COMPLEX_UNIT_PX, 40f * spToPxFactor)

        val textView2 = findViewById<TextView>(R.id.textView2)
        textView2?.setTextSize(TypedValue.COMPLEX_UNIT_PX, 30f * spToPxFactor)

        val continueButton = findViewById<Button>(R.id.continueButton)
        continueButton?.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * spToPxFactor)

        val newButton = findViewById<Button>(R.id.newButton)
        newButton?.setTextSize(TypedValue.COMPLEX_UNIT_PX, 20f * spToPxFactor)

        // Adjust ImageView (logo) size relative to screen height (~35% screen height)
        val imageView = findViewById<ImageView>(R.id.imageView)
        imageView?.let {
            val params = it.layoutParams
            params.height = (screenHeight * 0.35f).toInt()
            params.width = (screenHeight * 0.42f).toInt()
            it.layoutParams = params

            if (params is ViewGroup.MarginLayoutParams) {
                params.bottomMargin = (screenHeight * 0.02f).toInt()
                it.layoutParams = params
            }
        }

        // Adjust spacing between elements
        val margin2Percent = (screenHeight * 0.02f).toInt()

        (textView?.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            it.bottomMargin = margin2Percent
            textView.layoutParams = it
        }

        (continueButton?.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            it.bottomMargin = margin2Percent
            continueButton.layoutParams = it
        }
    }
}
