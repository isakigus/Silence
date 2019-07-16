package com.example.silence


import java.io.IOException
import android.media.MediaRecorder

class DetectNoise {

    private var mRecorder: MediaRecorder? = null
    private var mEMA = 0.0

    //return  (mRecorder.getMaxAmplitude()/2700.0);
    fun getAmplitude(): Double {

        if (mRecorder != null)
            return 20.0 * Math.log10(mRecorder!!.getMaxAmplitude() / 1.0)
        else
            return 0.0
    }

    fun getAmplitudeEMA(): Double {
        val amp = getAmplitude()
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA
        return mEMA

    }

    fun start() {

        if (mRecorder == null) {

            mRecorder = MediaRecorder()
            mRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
            mRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mRecorder!!.setOutputFile("/dev/null")

            try {
                mRecorder!!.prepare()
            } catch (e: IllegalStateException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }

            mRecorder!!.start()
            mEMA = 0.0
        }
    }

    fun stop() {
        if (mRecorder != null) {
            mRecorder!!.stop()
            mRecorder!!.release()
            mRecorder = null
        }
    }

    companion object {
        // This file is used to record voice
        private val EMA_FILTER = 0.6
    }
}
