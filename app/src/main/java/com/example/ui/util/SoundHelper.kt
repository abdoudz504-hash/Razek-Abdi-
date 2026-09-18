package com.example.ui.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Utility to play scanner beep tones and notification sounds.
 * Uses Android's low-latency ToneGenerator for fast, crisp barcode feedback.
 */
object SoundHelper {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            Log.w("SoundHelper", "Failed to init ToneGenerator with STREAM_MUSIC", e)
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
            } catch (e2: Exception) {
                Log.w("SoundHelper", "Failed to init ToneGenerator with STREAM_SYSTEM", e2)
            }
        }
    }

    /**
     * Plays a crisp POS-style scanner beep (150ms).
     */
    fun playBarcodeBeep() {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                    .startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            } catch (_: Exception) {
                // Ignore silent failure
            }
        }
    }

    /**
     * Plays a warning tone when an uncataloged item is scanned.
     */
    fun playWarningTone() {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 250)
        } catch (e: Exception) {
            try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                    .startTone(ToneGenerator.TONE_PROP_NACK, 250)
            } catch (_: Exception) {
                // Ignore silent failure
            }
        }
    }

    /**
     * Plays a double chime for checkout/calculation complete.
     */
    fun playSuccessChime() {
        try {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        } catch (_: Exception) {
            // Ignore
        }
    }

    /**
     * Cash register sound alias for purchase/activation completion.
     */
    fun playCashRegister() {
        playSuccessChime()
    }
}
