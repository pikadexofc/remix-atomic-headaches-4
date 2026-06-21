package com.example.ui.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

class SoundManager {
    companion object {
        @Volatile
        private var audioState = false
        private val scope = CoroutineScope(Dispatchers.Default)

        fun setAudioEnabled(enabled: Boolean) {
            audioState = enabled
        }

        fun isAudioEnabled(): Boolean = audioState

        fun playSoundAsync(freq: Float = 800f, duration: Float = 0.15f) {
            if (!audioState) return
            scope.launch {
                playSoundInternal(freq, duration)
            }
        }

        private suspend fun playSoundInternal(freq: Float, duration: Float) {
            withContext(Dispatchers.Default) {
                try {
                    val sampleRate = 22050
                    val numSamples = (duration * sampleRate).toInt()
                    if (numSamples <= 0) return@withContext
                    val generatedSnd = ShortArray(numSamples)

                    var phase = 0.0
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        val progress = t / duration
                        
                        // Frequency slides down to 80Hz, matching the React sound sweep
                        val currentFreq = freq - (freq - 80f) * progress
                        phase += 2.0 * Math.PI * currentFreq / sampleRate
                        
                        // Synthesize sine wave
                        val value = sin(phase)
                        
                        // Apply exponential gain decay (envelope) starting at ~0.08 scaling
                        val gain = 0.08 * Math.pow(0.01, progress.toDouble())
                        generatedSnd[i] = (value * gain * Short.MAX_VALUE).toInt().toShort()
                    }

                    val minSize = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    val trackSize = Math.max(numSamples * 2, minSize)

                    val audioTrack = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(trackSize)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()

                    audioTrack.write(generatedSnd, 0, numSamples)
                    audioTrack.play()

                    // Let it play fully then safely release
                    val playDurationMs = (duration * 1000).toLong()
                    kotlinx.coroutines.delay(playDurationMs + 50)
                    try {
                        audioTrack.stop()
                    } catch (e: Exception) {}
                    audioTrack.release()
                } catch (e: Exception) {
                    // Fallback block - ignore compile/runtime audio errors
                }
            }
        }
    }
}
