package com.Deathscythe0x1

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class YoutubePlugin : Plugin() {
    override fun load(context: Context) {
        // This function is called when the plugin is loaded, you can do any initialization here.
        registerMainAPI(YoutubeProvider())
    }
}