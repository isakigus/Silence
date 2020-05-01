package com.example.silence


import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream


abstract class ConfigurableActivity : AppCompatActivity() {

    var config: MutableMap<String, String> = mutableMapOf()

    private fun saveInitialConfig(): Map<String, String> {
        val config = mapOf(
            "mThreshold" to "65",
            "alarm_path" to "submarine"
        )

        saveConfig(config)
        return config
    }

    fun saveConfig(config: Map<String, String>) {

        val threshold = config["mThreshold"]

        val message = "Saved config threshold $threshold db"

        //Write the family map object to a file
        ObjectOutputStream(
            openFileOutput(
                "silence_config",
                Context.MODE_PRIVATE
            )
        ).use { it -> it.writeObject(config) }

        Toast.makeText(
            applicationContext, message,
            Toast.LENGTH_LONG
        ).show()
    }

    fun getConfiguration(): Map<*, *> {

        try {

            return ObjectInputStream(FileInputStream("silence_config")).use { it ->
                // Read the family back from the file
                // Cast it back into a Map
                when (val config = it.readObject()) {
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