package com.Deathscythe0x1

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URLEncoder

class YoutubeProvider : MainAPI() {
    override var mainUrl = "https://www.youtube.com"
    override var name = "YouTube"
    override val supportedTypes = setOf(TvType.Others)
    override var lang = "en"
    override val hasMainPage = true

    // Your country. Change this one line (IN, US, GB). Must match a key below for local rows.
    private val region = "IN"

    // Each pair is: what to search (or "trending") to row title
    private val rowsByRegion: Map<String, List<Pair<String, String>>> = mapOf(
        "IN" to listOf(
            "trending" to "Trending in India",
            "latest hindi songs" to "Hindi songs",
            "punjabi songs" to "Punjabi songs",
            "bollywood trailers" to "Bollywood trailers",
            "movie trailers" to "Movie trailers",
            "south indian movie trailers" to "South Indian trailers",
            "cricket highlights" to "Cricket",
            "ipl highlights" to "IPL",
            "hindi news live" to "India news",
            "hindi comedy" to "Comedy",
            "indian street food" to "Street food",
            "tech reviews" to "Tech",
            "cooking recipes" to "Cooking"
            "hindi tech review" to "Tech in Hindi",
            "hindi gaming" to "Gaming in Hindi",
            "devotional songs" to "Devotional"
        ),
        "US" to listOf(
            "trending" to "Trending in the US",
            "music videos" to "Music",
            "gaming" to "Gaming",
            "latest news" to "News",
            "movie trailers" to "Movie trailers",
            "stand up comedy" to "Comedy",
            "nfl highlights" to "NFL",
            "tech reviews" to "Tech",
            "cooking recipes" to "Cooking"
        ),
        "GB" to listOf(
            "trending" to "Trending in the UK",
            "uk music" to "Music",
            "uk news" to "News",
            "premier league highlights" to "Premier League",
            "british comedy" to "Comedy",
            "movie trailers" to "Movie trailers",
            "tech reviews" to "Tech",
            "baking recipes" to "Baking"
        )
    )

    override val mainPage = mainPageOf(
        *(rowsByRegion[region] ?: rowsByRegion.getValue("US")).toTypedArray()
    )

    // Tried in order. The first one that answers is used.
    private val apis = listOf(
        "https://pipedapi.leptons.xyz",
        "https://pipedapi.nosebs.ru",
        "https://pipedapi.adminforge.de",
        "https://api.piped.private.coffee",
        "https://pipedapi.kavin.rocks"
    )

    data class PipedSearch(val items: List<PipedItem>? = null)
    data class OEmbed(
        val title: String? = null,
        val author_name: String? = null,
        val thumbnail_url: String? = null
    )
    data class PipedItem(
        val url: String? = null,
        val title: String? = null,
        val thumbnail: String? = null
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

    private fun toResults(items: List<PipedItem>): List<SearchResponse> {
        return items.mapNotNull { item ->
            val id = item.url?.substringAfter("v=", "") ?: return@mapNotNull null
            if (id.isBlank()) return@mapNotNull null
            newMovieSearchResponse(
                item.title ?: "Untitled",
                "$mainUrl/watch?v=$id",
                TvType.Others
            ) { posterUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg" }
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse? {
        val result: List<PipedItem>? = if (request.data == "trending") {
            firstWorking { api ->
                app.get("$api/trending?region=$region").parsed<Array<PipedItem>>().toList()
            }
        } else {
            val q = URLEncoder.encode(request.data, "UTF-8")
            firstWorking { api ->
                app.get("$api/search?q=$q&filter=videos").parsed<PipedSearch>().items
            }
        }
        val items = result ?: return null
        return newHomePageResponse(request.name, toResults(items), false)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val q = URLEncoder.encode(query, "UTF-8")
        val res = firstWorking { api ->
            app.get("$api/search?q=$q&filter=videos").parsed<PipedSearch>()
        } ?: return emptyList()
        return toResults(res.items.orEmpty())
    }

    override suspend fun load(url: String): LoadResponse {
        val id = url.substringAfter("v=").substringBefore("&")
        val watch = URLEncoder.encode("https://www.youtube.com/watch?v=$id", "UTF-8")
        val info = try {
            app.get("https://www.youtube.com/oembed?url=$watch&format=json").parsed<OEmbed>()
        } catch (e: Exception) {
            null
        }
        return newMovieLoadResponse(info?.title ?: "YouTube video", url, TvType.Others, url) {
            posterUrl = info?.thumbnail_url ?: "https://i.ytimg.com/vi/$id/hqdefault.jpg"
            plot = info?.author_name?.let { "By $it" }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = loadExtractor(data, subtitleCallback, callback)
}
