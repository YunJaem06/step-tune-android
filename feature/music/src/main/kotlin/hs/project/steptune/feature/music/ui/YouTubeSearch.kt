package hs.project.steptune.feature.music.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

internal fun Context.openYouTubeSearch(searchQuery: String) {
    val uri = Uri.parse("https://www.youtube.com/results")
        .buildUpon()
        .appendQueryParameter("search_query", searchQuery)
        .build()
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}
