package com.example.silence

// import android.util.Log

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast


class ConfigActivity : ConfigurableActivity() {

    private var alarmFileText: TextView? = null

    /** Called when the activity is first created.  */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateConfiguration()

        setContentView(R.layout.activity_config)

        val actionbar = supportActionBar
        actionbar!!.title = "Configuration"

        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)

        var soundThreshold = findViewById<TextView>(R.id.new_threshold)
        alarmFileText = findViewById<TextView>(R.id.alarm_file)

        alarmFileText!!.text = config["alarm_path"]
        soundThreshold!!.text = config["mThreshold"]

        val btnSave = findViewById<Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            Toast.makeText(
                this@ConfigActivity,
                "You clicked SAVE.", Toast.LENGTH_SHORT
            ).show()

            config["alarm_path"] = alarmFileText!!.text.toString()
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

        val buttonOpen = findViewById<Button>(R.id.buttonOpen)

        buttonOpen.setOnClickListener {
            Toast.makeText(
                this@ConfigActivity,
                "You clicked OPEN.", Toast.LENGTH_SHORT
            ).show()

            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(intent, 10)

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == 10) {

            val uriSound: Uri? = data!!.data

            alarmFileText!!.text = uriSound.toString()

            Toast.makeText(
                this@ConfigActivity,
                "You clicked HEREEE. $uriSound", Toast.LENGTH_SHORT
            ).show()
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