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

    // Tried in order. The first one that answers is used.
    private val apis = listOf(
        "https://pipedapi.leptons.xyz",
        "https://pipedapi.nosebs.ru",
        "https://pipedapi.adminforge.de",
        "https://api.piped.private.coffee",
        "https://pipedapi.kavin.rocks"
    )

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

    private suspend fun <T> firstWorking(block: suspend (String) -> T): T? {
        for (api in apis) {
            try {
                return block(api)
            } catch (e: Exception) {
                // this server failed, try the next one
            }
        }
        return null
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val q = URLEncoder.encode(query, "UTF-8")
        val res = firstWorking { api ->
            app.get("$api/search?q=$q&filter=videos").parsed<PipedSearch>()
        } ?: return emptyList()
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
        val v = firstWorking { api ->
            app.get("$api/streams/$id").parsed<PipedStream>()
        }
        return newMovieLoadResponse(v?.title ?: "Untitled", url, TvType.Others, url) {
            posterUrl = v?.thumbnailUrl
            plot = v?.description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = loadExtractor(data, subtitleCallback, callback)
}
