package com.flowtune.music.lyrics
import android.content.Context
import com.flowtune.simpmusic.SimpMusicLyrics
import com.flowtune.music.constants.EnableSimpMusicKey
import com.flowtune.music.utils.dataStore
import com.flowtune.music.utils.get
object SimpMusicLyricsProvider : LyricsProvider {
    override val name = "SimpMusic"
    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableSimpMusicKey] ?: true
    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
    ): Result<String> = SimpMusicLyrics.getLyrics(id, duration)
    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        album: String?,
        callback: (String) -> Unit,
    ) {
        SimpMusicLyrics.getAllLyrics(id, duration, callback)
    }
}