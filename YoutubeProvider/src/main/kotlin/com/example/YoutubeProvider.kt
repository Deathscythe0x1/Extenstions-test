package com.Deathscythe0x1

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URLEncoder

class YoutubeProvider : MainAPI() {
    override var mainUrl = "https://www.youtube.com"
    override var name = "YouTube"
    override val supportedTypes = setOf(TvType.Others)
    override var lang = "en"
    override val hasMainPage = false

    // Public Piped servers come and go. If search stops working, change this address.
    private val api = "https://pipedapi.kavin.rocks"

    data class PipedSearch(val items: List<PipedItem>? = null)
    data class PipedItem(
        val url: String? = null,
        val title: String? = null,
        val thumbnail: String? = null
    )
    data class PipedStream(
        val title: String? = null,
        val description: String? = null,
        val thumbnailUrl: String? = null
    )

    override suspend fun search(query: String): List<SearchResponse> {
        val q = URLEncoder.encode(query, "UTF-8")
        val res = app.get("$api/search?q=$q&filter=videos").parsed<PipedSearch>()
        return res.items.orEmpty().mapNotNull { item ->
            val id = item.url?.substringAfter("v=", "") ?: return@mapNotNull null
            if (id.isBlank()) return@mapNotNull null
            newMovieSearchResponse(
                item.title ?: "Untitled",
                "$mainUrl/watch?v=$id",
                TvType.Others
            ) { posterUrl = item.thumbnail }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfter("v=")
        val v = app.get("$api/streams/$id").parsed<PipedStream>()
        return newMovieLoadResponse(v.title ?: "Untitled", url, TvType.Others, url) {
            posterUrl = v.thumbnailUrl
            plot = v.description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = loadExtractor(data, subtitleCallback, callback)
}