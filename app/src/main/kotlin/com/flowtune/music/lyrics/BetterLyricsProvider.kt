package com.flowtune.music.lyrics
import android.content.Context
import com.flowtune.betterlyrics.BetterLyrics
import com.flowtune.music.constants.EnableBetterLyricsKey
import com.flowtune.music.utils.dataStore
import com.flowtune.music.utils.get
object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"
    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true
    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration, album)
    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        BetterLyrics.getAllLyrics(title, artist, duration, album, callback)
    }
}