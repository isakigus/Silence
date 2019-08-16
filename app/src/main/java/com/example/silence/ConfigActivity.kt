package com.example.silence

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast


class ConfigActivity : ConfigurableActivity() {

    /** Called when the activity is first created.  */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // config = getConfig()
        // Defined SoundLevelView in main.xml file
        setContentView(R.layout.activity_config)
        //


        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "Configuration"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        actionbar.setDisplayHomeAsUpEnabled(true)

        val btn_save = findViewById(R.id.btn_save) as Button

        btn_save.setOnClickListener {
            Toast.makeText(this@ConfigActivity, "You clicked SAVE.", Toast.LENGTH_SHORT).show()
            config.put("alarm_path", "hola")
            config.put("mThreshold", "80.00")

            saveConfig(config)

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