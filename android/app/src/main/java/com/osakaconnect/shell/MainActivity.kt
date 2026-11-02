package com.osakaconnect.shell

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import java.util.UUID

class MainActivity : Activity() {
    // Correlates callbacks across rotation without logging user or device data.
    internal val lifecycleId = UUID.randomUUID().toString()
    private var page = Page.P1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordLifecycle("onCreate")
        page = Page.restore(savedInstanceState?.getString("page"))
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.navigate).setOnClickListener {
            page = if (page == Page.P1) Page.P2 else Page.P1
            renderPage()
        }
        renderPage()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("page", page.name)
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        recordLifecycle("onStart")
    }

    override fun onResume() {
        super.onResume()
        recordLifecycle("onResume")
    }

    override fun onPause() {
        super.onPause()
        recordLifecycle("onPause")
    }

    override fun onStop() {
        super.onStop()
        recordLifecycle("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        recordLifecycle("onDestroy")
    }

    private fun renderPage() {
        findViewById<TextView>(R.id.page_identifier).text = page.name
        findViewById<TextView>(R.id.page_description).setText(
            if (page == Page.P1) R.string.inactive else R.string.active
        )
        findViewById<Button>(R.id.navigate).setText(
            if (page == Page.P1) R.string.go_to_p2 else R.string.back_to_p1
        )
    }

    private fun recordLifecycle(callback: String) {
        // shortcut: 尚未接入 LogRedactor，android-diagnostics 交付后改为经 LogRedactor
        Log.i("OsakaLifecycle", "$lifecycleId $callback")
    }
}
