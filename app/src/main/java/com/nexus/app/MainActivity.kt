package com.nexus.app
import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = TextView(this)
        view.text = "Nexus System Online"
        view.textSize = 24f
        view.setTextColor(0xFF00FF00.toInt()) 
        setContentView(view)
    }
}
