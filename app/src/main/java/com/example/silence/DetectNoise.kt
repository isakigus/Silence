package com.example.silence

import java.io.IOException
import android.media.MediaRecorder

class DetectNoise {

    private var mediaRecorder: MediaRecorder? = null
    private var EMA = 0.0 /*Exponential Moving Average*/

    //return  (mRecorder.getMaxAmplitude()/2700.0);
    fun getAmplitude(): Double {

        if (mediaRecorder != null)
            return 20.0 * Math.log10(mediaRecorder!!.getMaxAmplitude() / 1.0)
        else
            return 0.0
    }

    fun getAmplitudeEMA(): Double {
        val amplitude = getAmplitude()
        EMA = EMA_FILTER * amplitude + (1.0 - EMA_FILTER) * EMA
        return EMA

    }

    fun start() {

        if (mediaRecorder == null) {

            mediaRecorder = MediaRecorder()
            mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mediaRecorder!!.setOutputFile("/dev/null")

            try {
                mediaRecorder!!.prepare()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            mediaRecorder!!.start()
            EMA = 0.0
        }
    }

    fun stop() {
        if (mediaRecorder != null) {
            mediaRecorder!!.stop()
            mediaRecorder!!.release()
            mediaRecorder = null
        }
    }

    companion object {
        // This file is used to record voice
        private val EMA_FILTER = 0.6
    }
}
