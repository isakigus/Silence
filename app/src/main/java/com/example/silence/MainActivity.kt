package com.example.silence

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.media.MediaPlayer
import android.graphics.Color
import android.graphics.PorterDuff
import java.io.*

import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.widget.Button


/** Still configuration is not working
 * -> save threshold (configuration file)
 * -> browse sound files
 * -> show dB scale in other activity*/


class MainActivity : ConfigurableActivity() {
    /** running state  */
    private var mRunning = false
    private var sound_working = false

    /** config state  */
    private var mThreshold: Int = 0

    internal var RECORD_AUDIO = 0
    private var mWakeLock: PowerManager.WakeLock? = null

    private val mHandler = Handler()

    /** References to view elements */
    private var mStatusView: TextView? = null
    private var tv_noice: TextView? = null
    private var max_noise: TextView? = null
    private var avg_noise: TextView? = null
    private var threshold: TextView? = null
    private var _max_noise: Double = 0.0
    private var _avg_noise: Double = 0.0
    private var sum_noise: Double = 0.0
    private var counter: Double = 0.0
    private lateinit var player: MediaPlayer

    /** sound data source */
    private var mSensor: DetectNoise? = null
    internal lateinit var bar: ProgressBar
    /****************** Define runnable thread again and again detect noise  */

    private val mSleepTask = object : Runnable {
        override fun run() {
            //Log.i("Noise", "runnable mSleepTask");
            start()
        }
    }

    // Create runnable thread to Monitor Voice
    private val mPollTask = object : Runnable {
        override fun run() {
            val amp = mSensor!!.getAmplitude()

            if (amp > _max_noise) {
                _max_noise = amp
            }

            if (amp > 0) {
                sum_noise = sum_noise + amp
                counter += 1.0
            }

            Log.d("MAIN", "sum_noise:" + sum_noise)
            Log.d("MAIN", "amp:" + amp)
            Log.d("MAIN", "counter:" + counter)

            _avg_noise = sum_noise / counter
            //Log.i("Noise", "runnable mPollTask");
            updateDisplay("Monitoring Voice...", amp)

            if (amp > mThreshold) {
                callForHelp(amp)
                //Log.i("Noise", "==== onCreate ===");
            }
            // Runnable(mPollTask) will again execute after POLL_INTERVAL
            mHandler.postDelayed(this, POLL_INTERVAL.toLong())
        }
    }

    private fun setPermissions() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("SILENCE", "Permission to record denied")

        } else {
            startRecording();
        }
    }

    private fun startRecording() {

        mStatusView = findViewById(R.id.status) as TextView
        tv_noice = findViewById(R.id.tv_noice) as TextView
        avg_noise = findViewById(R.id.avg_noise) as TextView
        max_noise = findViewById(R.id.max_noise) as TextView
        threshold = findViewById(R.id.threshold) as TextView

        bar = findViewById(R.id.progressBar1) as ProgressBar
        // Used to record voice
        mSensor = DetectNoise()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "NoiseAlert:")
    }


    /** Called when the activity is first created.  */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Defined SoundLevelView in main.xml file
        setContentView(R.layout.activity_main)
        setPermissions()

        for ((k, v) in getConfiguration()) {
            config.put(k.toString(), v.toString())
        }

        val btn_stop = findViewById(R.id.btn_stop) as Button

        btn_stop.setOnClickListener {
            Toast.makeText(this@MainActivity, "You clicked STOP.", Toast.LENGTH_SHORT).show()
            stop()
        }

        val btn_start = findViewById(R.id.btn_start) as Button

        btn_start.setOnClickListener {
            Toast.makeText(this@MainActivity, "You clicked START.", Toast.LENGTH_SHORT).show()
            start()
        }

        val btn_config = findViewById(R.id.btn_config) as Button

        btn_config.setOnClickListener {
            val intent = Intent(this, ConfigActivity::class.java)
            // start your next activity
            // intent.putExtra("threshold", mThreshold);
            // intent.putExtra("file", config["alarm_path"].toString());
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()
        //Log.i("Noise", "==== onResume ===");

        initializeApplicationConstants()
        if (!mRunning) {
            mRunning = true
            start()
        }
    }

    override fun onStop() {
        super.onStop()
        // Log.i("Noise", "==== onStop ===");
        //Stop noise monitoring
        stop()
    }

    private fun start() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO
            )
        }

        //Log.i("Noise", "==== start ===");
        mSensor!!.start()
        if (!mWakeLock!!.isHeld()) {
            mWakeLock!!.acquire()
        }
        //Noise monitoring start
        // Runnable(mPollTask) will execute after POLL_INTERVAL
        mHandler.postDelayed(mPollTask, POLL_INTERVAL.toLong())
    }

    fun buttonEffect(button: View) {
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.background.setColorFilter(-0x1f0b8adf, PorterDuff.Mode.SRC_ATOP)
                    v.invalidate()
                }
                MotionEvent.ACTION_UP -> {
                    v.background.clearColorFilter()
                    v.invalidate()
                }
            }
            false
        }
    }

    private fun stop() {
        Log.d("Noise", "==== Stop Noise Monitoring===")
        if (mWakeLock!!.isHeld()) {
            mWakeLock!!.release()
        }

        if (player != null && player.isPlaying()) {
            player.stop()
            player.reset()

        }

        mHandler.removeCallbacks(mSleepTask)
        mHandler.removeCallbacks(mPollTask)
        mSensor!!.stop()
        bar.setProgress(0)
        updateDisplay("stopped...", 0.0)
        mRunning = false
        sound_working = false

    }


    private fun initializeApplicationConstants() {
        // Set Noise Threshold

        for ((k, v) in getConfiguration()) {
            config.put(k.toString(), v.toString())
        }

        mThreshold = config.get("mThreshold").toString().toInt()

        threshold!!.setText("Threshold:" + mThreshold + ".00 dB")

    }

    private fun updateDisplay(status: String, signalEMA: Double) {
        mStatusView!!.setText(status)
        //
        bar.setProgress(signalEMA.toInt())
        Log.d("SOUND", signalEMA.toString())

        max_noise!!.setText("Max:%.2f dB".format(_max_noise))
        avg_noise!!.setText("Avg:%.2f dB".format(_avg_noise))

        tv_noice!!.setTextColor(Color.BLACK)

        if (signalEMA > 0.0) {
            tv_noice!!.setText("Actual:%.2f dB".format(signalEMA))


        } else {
            tv_noice!!.setText("Actual:0.00 dB")
        }
    }


    private fun callForHelp(signalEMA: Double) {
        playAlarm()

        // Show alert when noise Threshold crossed
        Toast.makeText(
            applicationContext, "Noise Threshold Crossed, do here your stuff.",
            Toast.LENGTH_LONG
        ).show()


        Log.d("SOUND", signalEMA.toString())
        tv_noice!!.setText("Actual:%.2f dB".format(signalEMA))
        tv_noice!!.setTextColor(Color.RED)
    }


    private fun playAlarm() {

        if (sound_working)
            return

        sound_working = true


        val path = config["alarm_path"].toString()
        val uri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/raw/" + path)

        Log.d("Noise", "" + uri)
        player = MediaPlayer.create(applicationContext, uri)

        player?.setOnCompletionListener {
            player.reset()
            sound_working = false
        }

        player?.setOnPreparedListener {
            player.start()
        }

    }

    companion object {
        /* constants */
        private val POLL_INTERVAL = 300
    }

}

