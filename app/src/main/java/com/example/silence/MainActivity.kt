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

import android.net.Uri
import android.view.MotionEvent
import android.view.View
import android.widget.Button


/** Still configuration is not working
 * -> save threshold (configuration file)
 * -> browse sound files
 * -> show dB scale in other activity
 * -> noise level chart
 * -> waw loop
 * */


class MainActivity : ConfigurableActivity() {
    /** running state  */
    private var appIsRunning = false
    private var alarmIsActivated = false

    /** config state  */
    private var soundLevelLimit: Int = 0

    internal var RECORD_AUDIO = 0
    private var wakeLock: PowerManager.WakeLock? = null

    private val androidOsHandler = Handler() // to create a thread

    /** References to view elements */
    private var statusLabel: TextView? = null
    private var actualNoiseLabel: TextView? = null
    private var maxNoiseLabel: TextView? = null
    private var avgNoiseLabel: TextView? = null
    private var noiseThresholdLabel: TextView? = null
    private var maxNoise: Double = 0.0
    private var avgNoise: Double = 0.0
    private var sumNoise: Double = 0.0
    private var counter: Double = 0.0
    private lateinit var player: MediaPlayer

    /** sound data source */
    private var soundSensor: DetectNoise? = null
    internal lateinit var progressBar: ProgressBar

    /****************** Define runnable thread again and again detect noise  */

    private val mSleepTask = Runnable { //Log.i("Noise", "runnable mSleepTask");
        start()
    }

    // Create runnable thread to Monitor Voice
    private val pollingTask = object : Runnable {
        override fun run() {
            val waveAmplitude = soundSensor!!.getAmplitude()

            if (waveAmplitude > maxNoise) {
                maxNoise = waveAmplitude
            }

            if (waveAmplitude > 0) {
                sumNoise += waveAmplitude
                counter += 1.0
            }

            Log.d("MAIN", "sum_noise:" + sumNoise)
            Log.d("MAIN", "amp:" + waveAmplitude)
            Log.d("MAIN", "counter:" + counter)

            avgNoise = sumNoise / counter
            //Log.i("Noise", "runnable mPollTask");
            updateDisplay("Monitoring Voice...", waveAmplitude)

            if (waveAmplitude > soundLevelLimit) {
                callForHelp(waveAmplitude)
                //Log.i("Noise", "==== onCreate ===");
            }
            // Runnable(mPollTask) will again execute after POLL_INTERVAL
            androidOsHandler.postDelayed(this, POLL_INTERVAL.toLong())
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

        statusLabel = findViewById(R.id.status) as TextView
        actualNoiseLabel = findViewById(R.id.actual_noise) as TextView
        avgNoiseLabel = findViewById(R.id.avg_noise) as TextView
        maxNoiseLabel = findViewById(R.id.max_noise) as TextView
        noiseThresholdLabel = findViewById(R.id.threshold) as TextView

        progressBar = findViewById(R.id.progressBar1) as ProgressBar
        // Used to record voice
        soundSensor = DetectNoise()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "NoiseAlert:")
    }


    /** Called when the activity is first created.  */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Defined SoundLevelView in main.xml file
        setContentView(R.layout.activity_main)
        setPermissions()

        for ((key, value) in getConfiguration()) {
            config.put(key.toString(), value.toString())
        }

        val btnStop = findViewById(R.id.btn_stop) as Button

        btnStop.setOnClickListener {
            Toast.makeText(this@MainActivity, "You clicked STOP.", Toast.LENGTH_SHORT).show()
            stop()
        }

        val btnStart = findViewById(R.id.btn_start) as Button

        btnStart.setOnClickListener {
            Toast.makeText(this@MainActivity, "You clicked START.", Toast.LENGTH_SHORT).show()
            start()
        }

        val btnConfig = findViewById(R.id.btn_config) as Button

        btnConfig.setOnClickListener {
            val intent = Intent(this, ConfigActivity::class.java)
            // start your next activity
            // intent.putExtra("threshold", mThreshold);
            // intent.putExtra("file", config["alarm_path"].toString());
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()

        initializeApplicationConstants()
        if (!appIsRunning) {
            appIsRunning = true
            start()
        }
    }

    override fun onStop() {
        super.onStop()
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

        soundSensor!!.start()
        if (!wakeLock!!.isHeld()) {
            wakeLock!!.acquire()
        }
        //Noise monitoring start
        androidOsHandler.postDelayed(pollingTask, POLL_INTERVAL.toLong())
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
        if (wakeLock!!.isHeld()) {
            wakeLock!!.release()
        }

        if (player != null && player.isPlaying()) {
            player.stop()
            player.reset()

        }

        androidOsHandler.removeCallbacks(mSleepTask)
        androidOsHandler.removeCallbacks(pollingTask)
        soundSensor!!.stop()
        progressBar.setProgress(0)
        updateDisplay("stopped...", 0.0)
        appIsRunning = false
        alarmIsActivated = false

    }


    private fun initializeApplicationConstants() {
        // Set Noise Threshold

        for ((k, v) in getConfiguration()) {
            config.put(k.toString(), v.toString())
        }

        soundLevelLimit = config.get("mThreshold").toString().toInt()

        noiseThresholdLabel!!.setText("Threshold:" + soundLevelLimit + ".00 dB")

    }

    private fun updateDisplay(status: String, signalEMA: Double) {
        statusLabel!!.setText(status)
        //
        progressBar.setProgress(signalEMA.toInt())
        Log.d("SOUND", signalEMA.toString())

        maxNoiseLabel!!.setText("Max:%.2f dB".format(maxNoise))
        avgNoiseLabel!!.setText("Avg:%.2f dB".format(avgNoise))

        actualNoiseLabel!!.setTextColor(Color.BLACK)

        if (signalEMA > 0.0) {
            actualNoiseLabel!!.setText("Actual:%.2f dB".format(signalEMA))

        } else {
            actualNoiseLabel!!.setText("Actual:0.00 dB")
        }
    }


    private fun callForHelp(signalEMA: Double) {
        playAlarm()

        // Show alert when noise Threshold crossed
        Toast.makeText(
            applicationContext, "Noise Threshold Crossed!",
            Toast.LENGTH_LONG
        ).show()


        Log.d("SOUND", signalEMA.toString())
        actualNoiseLabel!!.setText("Actual:%.2f dB".format(signalEMA))
        actualNoiseLabel!!.setTextColor(Color.RED)
    }


    private fun playAlarm() {

        if (alarmIsActivated)
            return

        alarmIsActivated = true


        val path = config["alarm_path"].toString()
        val uri =
            Uri.parse(
                "android.resource://" + getApplicationContext().getPackageName() +
                        "/raw/" + path
            )

        Log.d("Noise", "" + uri)
        player = MediaPlayer.create(applicationContext, uri)

        player?.setOnCompletionListener {
            player.reset()
            alarmIsActivated = false
        }

        player?.setOnPreparedListener {
            player.start()
        }

    }

    companion object {
        /* constants */
        private const val POLL_INTERVAL = 300
    }

}

