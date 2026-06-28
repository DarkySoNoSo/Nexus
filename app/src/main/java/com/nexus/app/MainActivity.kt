package com.nexus.app
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "NEXUS SYSTEM ONLINE"
        tv.textSize = 26f
        setContentView(tv)
    }
}
