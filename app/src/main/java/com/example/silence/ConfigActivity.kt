package com.example.silence

import android.os.Bundle
// import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast


class ConfigActivity : ConfigurableActivity() {

    /** Called when the activity is first created.  */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        for ((key, value) in getConfiguration()) {
            config[key.toString()] = value.toString()
        }

        // Defined SoundLevelView in main.xml file
        setContentView(R.layout.activity_config)

        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "Configuration"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)

        var soundThreshold = findViewById(R.id.new_threshold) as TextView
        val alarmFileText = findViewById<Button>(R.id.alarm_file) as TextView

        alarmFileText!!.text = "Alarm file: " + config["alarm_path"]

        soundThreshold!!.text = config["mThreshold"]

        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            Toast.makeText(
                this@ConfigActivity,
                "You clicked SAVE.", Toast.LENGTH_SHORT
            ).show()

            // config["alarm_path"] = alarmFileText.text.toString()
            config["mThreshold"] = soundThreshold.text.toString()

            saveConfig(config)

        }

        val btnReset = findViewById<Button>(R.id.btnReset)

        btnReset.setOnClickListener {
            Toast.makeText(
                this@ConfigActivity,
                "You clicked RESET.", Toast.LENGTH_SHORT
            ).show()

            saveInitialConfig()
            finish()
            startActivity(intent)
        }

    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onStop() {
        super.onStop()

    }


}