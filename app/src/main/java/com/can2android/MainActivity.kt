package com.can2android

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this).apply {
            text = "Can2Android\nBasic Android App"
            textSize = 24f
            setPadding(40, 40, 40, 40)
        }
        
        setContentView(textView)
    }
}
