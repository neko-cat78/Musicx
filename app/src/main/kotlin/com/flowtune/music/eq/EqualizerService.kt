package com.flowtune.music.eq
import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.flowtune.music.eq.data.SavedEQProfile
import com.flowtune.music.eq.audio.CustomEqualizerAudioProcessor
import com.flowtune.music.eq.data.ParametricEQ
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class EqualizerService @Inject constructor() {
    @SuppressLint("UnsafeOptInUsageError")
    private var audioProcessor: CustomEqualizerAudioProcessor? = null
    private var pendingProfile: SavedEQProfile? = null
    private var shouldDisable: Boolean = false
    companion object {
        private const val TAG = "EqualizerService"
    }
    @OptIn(UnstableApi::class)
    fun setAudioProcessor(processor: CustomEqualizerAudioProcessor) {
        this.audioProcessor = processor
        Timber.tag(TAG).d("Audio processor set")
        if (shouldDisable) {
            disable()
            shouldDisable = false
            Timber.tag(TAG).d("Applied pending disable request")
        } else if (pendingProfile != null) {
            val profile = pendingProfile!!
            applyProfile(profile)
            pendingProfile = null
            Timber.tag(TAG).d("Applied pending profile: ${profile.name}")
        }
    }
    @OptIn(UnstableApi::class)
    fun applyProfile(profile: SavedEQProfile): Result<Unit> {
        val processor = audioProcessor
        if (processor == null) {
            Timber.tag(TAG)
                .w("Audio processor not set yet. Storing profile as pending: ${profile.name}")
            pendingProfile = profile
            shouldDisable = false
            return Result.success(Unit)
        }
        try {
            pendingProfile = null
            shouldDisable = false
            val parametricEQ = ParametricEQ(
                preamp = profile.preamp,
                bands = profile.bands
            )
            processor.applyProfile(parametricEQ)
            Timber.tag(TAG)
                .d("Applied EQ profile: ${profile.name} with ${profile.bands.size} bands and ${profile.preamp} dB preamp")
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag(TAG).e("Failed to apply profile: ${e.message}")
            return Result.failure(e)
        }
    }
    @OptIn(UnstableApi::class)
    fun disable() {
        val processor = audioProcessor
        if (processor == null) {
            Timber.tag(TAG).w("Audio processor not set yet. Storing disable as pending")
            shouldDisable = true
            pendingProfile = null
            return
        }
        try {
            pendingProfile = null
            shouldDisable = false
            processor.disable()
            Timber.tag(TAG).d("Equalizer disabled")
        } catch (e: Exception) {
            Timber.tag(TAG).e("Failed to disable equalizer: ${e.message}")
        }
    }
    fun isInitialized(): Boolean {
        return audioProcessor != null
    }
    @OptIn(UnstableApi::class)
    fun isEnabled(): Boolean {
        return audioProcessor?.isEnabled() ?: false
    }
    fun getEqualizerInfo(): EqualizerInfo {
        return EqualizerInfo(
            supportsUnlimitedBands = true,
            maxBands = Int.MAX_VALUE,
            description = "Custom ExoPlayer AudioProcessor with biquad filters"
        )
    }
    fun release() {
        audioProcessor = null
        Timber.tag(TAG).d("Audio processor reference cleared (pending state preserved)")
    }
}
data class EqualizerInfo(
    val supportsUnlimitedBands: Boolean,
    val maxBands: Int,
    val description: String
)