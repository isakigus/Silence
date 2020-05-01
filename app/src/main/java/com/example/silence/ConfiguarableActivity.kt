package com.example.silence


import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.*


abstract class ConfigurableActivity : AppCompatActivity() {

    var config: MutableMap<String, String> = mutableMapOf()

    fun saveInitialConfig(): Map<String, String> {
        Log.d("configurable", "==== saveInitialConfig ===")

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

        val file = File(getDir("data", Context.MODE_PRIVATE), "config")
        val outputStream =
            ObjectOutputStream(FileOutputStream(file))

        outputStream.writeObject(config)
        outputStream.flush()
        outputStream.close()


        Toast.makeText(
            applicationContext, message,
            Toast.LENGTH_LONG
        ).show()
    }

    fun getConfiguration(): Map<*, *> {
        val config = _getConfiguration()
        Log.d("getConfiguration", config.toString())
        return config

    }


    private fun _getConfiguration(): Map<*, *> {

        Log.d("configurable", "==== getConfiguration ===")


        try {

            val file = File(getDir("data", Context.MODE_PRIVATE), "config")
            val inputStream =
                ObjectInputStream(FileInputStream(file))

            return inputStream.use { it ->
                // Read the family back from the file
                // Cast it back into a Map
                when (val config = it.readObject()) {
                    // We can't use <String, String> because of type erasure
                    is Map<*, *> -> config
                    else -> saveInitialConfig()
                }

            }

            Log.d("configurable", "==== getConfiguration ===")
        } catch (e: FileNotFoundException) {
            Log.d("configurable", "==== FileNotFoundException ===")
            return saveInitialConfig()
        }

    }

}