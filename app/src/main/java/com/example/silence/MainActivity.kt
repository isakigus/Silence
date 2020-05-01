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

import android.net.Uri
import android.widget.Button


/** Still configuration is not working
 * -> save threshold (configuration file) (done)
 * -> browse sound files (done)
 * -> stop/exit button (done)

 * -> show dB scale in other activity (review algorithm)
 * -> noise level chart
 * -> waw loop
 * -> upload to playstore
 * */


class MainActivity : ConfigurableActivity() {
    /** running state  */
    private var appIsRunning = false
    private var alarmIsActivated = false

    /** config state  */
    private var soundLevelLimit: Int = 0

    private var RECORD_AUDIO = 0
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
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: Int = 1

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

            Log.d("MAIN", "sum_noise:$sumNoise")
            Log.d("MAIN", "amp:$waveAmplitude")
            Log.d("MAIN", "counter:$counter")

            avgNoise = sumNoise / counter

            updateDisplay("Monitoring Voice...", waveAmplitude)

            if (waveAmplitude > soundLevelLimit) {
                callForHelp(waveAmplitude)
            }
            // Runnable(mPollTask) will again execute after POLL_INTERVAL
            androidOsHandler.postDelayed(this, POLL_INTERVAL.toLong())
        }
    }

    private fun setPermissions() {

        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            Log.i("SILENCE", "Permission to record denied")
            finishAndRemoveTask()
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
            )

        }

        startRecording();

    }

    private fun startRecording() {

        statusLabel = findViewById<TextView>(R.id.status)
        actualNoiseLabel = findViewById<TextView>(R.id.actual_noise)
        avgNoiseLabel = findViewById<TextView>(R.id.avg_noise)
        maxNoiseLabel = findViewById<TextView>(R.id.max_noise)
        noiseThresholdLabel = findViewById<TextView>(R.id.threshold)

        progressBar = findViewById<ProgressBar>(R.id.progressBar1)
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
        updateConfiguration()

        val btnStop = findViewById<Button>(R.id.btn_stop)

        btnStop.setOnClickListener {
            Toast.makeText(this@MainActivity, "You clicked STOP.",
                Toast.LENGTH_SHORT).show()
            stop()
        }

        val btnStart = findViewById<Button>(R.id.btn_start)

        btnStart.setOnClickListener {
            Toast.makeText(this@MainActivity, "You clicked START.",
                Toast.LENGTH_SHORT).show()
            start()
        }

        val btnConfig = findViewById<Button>(R.id.btn_config)

        btnConfig.setOnClickListener {
            val intent = Intent(this, ConfigActivity::class.java)
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

        val btnStop = findViewById<Button>(R.id.btn_stop)
        btnStop.text= "STOP"

        btnStop.setOnClickListener {
            Toast.makeText(this@MainActivity, "You clicked STOP.",
                Toast.LENGTH_SHORT).show()
            stop()
        }

        soundSensor!!.start()
        if (!wakeLock!!.isHeld()) {
            wakeLock!!.acquire()
        }
        //Noise monitoring start
        androidOsHandler.postDelayed(pollingTask, POLL_INTERVAL.toLong())
    }

    private fun stop() {

        Log.d("Noise", "==== Stop Noise Monitoring===")
        if (wakeLock!!.isHeld()) {
            wakeLock!!.release()
        }

        try {

            if (player != null && player.isPlaying()) {
                player.stop()
                player.reset()
            }
        } catch (e: UninitializedPropertyAccessException) {
            Log.d("Noise", "==== UninitializedPropertyAccessException ===")

        }

        androidOsHandler.removeCallbacks(mSleepTask)
        androidOsHandler.removeCallbacks(pollingTask)
        soundSensor!!.stop()
        progressBar.progress = 0
        maxNoise = 0.0
        avgNoise = 0.0

        val btnStop = findViewById<Button>(R.id.btn_stop)
        btnStop.text= "EXIT"

        btnStop.setOnClickListener {
            Toast.makeText(this@MainActivity, "You clicked Exit.",
                Toast.LENGTH_SHORT).show()
            finishAndRemoveTask()
        }

        updateDisplay("stopped...", 0.0)
        appIsRunning = false
        alarmIsActivated = false


    }

    private fun initializeApplicationConstants() {

        updateConfiguration()

        soundLevelLimit = config["mThreshold"].toString().toInt()
        noiseThresholdLabel!!.text = "Threshold: $soundLevelLimit .00 dB"

    }

    private fun updateDisplay(status: String, signalEMA: Double) {

        statusLabel!!.text = status
        progressBar.progress = signalEMA.toInt()

        Log.d("SOUND", signalEMA.toString())

        maxNoiseLabel!!.setText("Max:%.2f dB".format(maxNoise))
        avgNoiseLabel!!.setText("Avg:%.2f dB".format(avgNoise))

        actualNoiseLabel!!.setTextColor(Color.BLACK)

        if (signalEMA > 0.0) {
            actualNoiseLabel!!.text = "Actual:%.2f dB".format(signalEMA)

        } else {
            actualNoiseLabel!!.text = "Actual:0.00 dB"
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
        actualNoiseLabel!!.text = "Actual:%.2f dB".format(signalEMA)
        actualNoiseLabel!!.setTextColor(Color.RED)
    }


    private fun playAlarm() {

        if (alarmIsActivated)
            return

        alarmIsActivated = true

        val path = config["alarm_path"].toString()
        val uri = Uri.parse(path)

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

