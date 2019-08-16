package com.example.silence


import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

abstract class ConfigurableActivity : AppCompatActivity() {

    var config: MutableMap<String, String> = mutableMapOf<String, String>()

     fun saveInitialConfig(): Map<String, String> {
        val config = mapOf(
            "mThreshold" to "65",
            "alarm_path" to "submarine"
        )

        saveConfig(config)
        return config
    }

     fun saveConfig(config: Any) {

        //Write the family map object to a file
        ObjectOutputStream(openFileOutput("silence_config", Context.MODE_PRIVATE)).use { it -> it.writeObject(config) }

         Toast.makeText(
             applicationContext, "Saved config Threshold Crossed, do here your stuff.",
             Toast.LENGTH_LONG
         ).show()
    }

     fun getConfiguration(): Map<*, *> {

        try {

            return ObjectInputStream(FileInputStream("silence_config")).use { it ->
                // Read the family back from the file
                val config = it.readObject()

                // Cast it back into a Map
                when (config) {
                    // We can't use <String, String> because of type erasure
                    is Map<*, *> -> config
                    else -> saveInitialConfig()
                }

            }
        } catch (e: FileNotFoundException) {
            return saveInitialConfig()
        }

    }


}