package com.intherm.miofragments.ui.devicesettings

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.google.gson.GsonBuilder
import com.intherm.miofragments.R
import com.intherm.miofragments.Thermostat
import com.intherm.miofragments.UserAdaper
import com.intherm.miofragments.ui.users.UserListItems
import kotlinx.android.synthetic.main.fragment_devicesettings.*
import kotlinx.android.synthetic.main.fragment_users.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.roundToInt

class DevicesettingsFragment : Fragment() {

    var thermostat = Thermostat()
    var host = "http://109.228.229.36:8080/termoServlet"
    var accuracy = 0.1
    var step = 0.5
    var ledTime = 5.0
    var calibration = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_devicesettings, container, false)
        return root
    }

    override fun onStart() {
        super.onStart()

        val sharedPrefDevice = activity!!.getSharedPreferences("device", Activity.MODE_PRIVATE)
        thermostat.thermoId = sharedPrefDevice.getInt("selected device id", 0)

        sbStep.max = 9
        sbAccuracy.max = 10
        sbCal.max = 20
        sbLed.max = 9

        btn_save_settings.setOnClickListener {
            sendSettings()
        }

        getThermostatSettings()


        sbStep.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvstepval.text = (((progress) + 1).toDouble() / 10).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                tvstepval.text = (((seekBar!!.progress) + 1).toDouble() / 10).toString()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                tvstepval.text = (((seekBar!!.progress) + 1).toDouble() / 10).toString()
            }
        })

        sbAccuracy.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvaccuval.text = ((progress).toDouble() / 10).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                tvaccuval.text = ((seekBar!!.progress).toDouble() / 10).toString()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                tvaccuval.text = ((seekBar!!.progress).toDouble() / 10).toString()
            }
        })

        sbLed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvledval.text = ((progress) + 1).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                tvledval.text = ((seekBar!!.progress) + 1).toString()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                tvledval.text = ((seekBar!!.progress) + 1).toString()
            }
        })

        sbCal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvacalval.text = (((progress) - 10).toDouble() / 10).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                tvacalval.text = (((seekBar!!.progress) - 10).toDouble() / 10).toString()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                tvacalval.text = (((seekBar!!.progress) - 10).toDouble() / 10).toString()
            }
        })


    }

    fun getThermostatSettings() {
        println("getThermostatSettings() - Fetch json komutu deneniyor.")

        val url = host + "/State/getProperties?thermoId=" + thermostat.thermoId

        println(url)

        val request = Request.Builder().url(url).build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                println("Basarisiz: " + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body!!.string()
                println("Cikti: " + body)
                val code = response.code
                if (code == 200) {
                    val jsonObject = JSONObject(body)
                    for (jsonIndex in 0 until jsonObject.length()) {
                        accuracy = jsonObject.getDouble("accuracy")
                        step = jsonObject.getDouble("step")
                        ledTime = jsonObject.getDouble("ledTime")
                        calibration = jsonObject.getDouble("calibration")


                    }
                } else {
                    unableToConnect()
                }
            }
        })
        Timer("SettingUp", false).schedule(200) {
            activity!!.runOnUiThread {
                setValues()
            }
        }
    }

    private fun unableToConnect() {
        activity!!.runOnUiThread {
            val builder = androidx.appcompat.app.AlertDialog.Builder(activity!!)
            builder.setTitle("Hata!")
            builder.setMessage(getString(R.string.unableToConnect))
            builder.setNeutralButton("Tamam") { _, _ ->

            }
            val dialog: androidx.appcompat.app.AlertDialog = builder.create()
            dialog.show()
        }
    }

    fun setValues() {
        sbStep.progress = (((step) - 0.1) * 10).toInt()
        sbLed.progress = ((ledTime) - 1).toInt()
        sbCal.progress = ((calibration + 1) * 10).toInt()
        sbAccuracy.progress = ((accuracy) * 10).toInt()
        tvstepval.text = step.toString()
        tvledval.text = ledTime.toString()
        tvacalval.text = calibration.toString()
        tvaccuval.text = accuracy.toString()
    }

    fun sendSettings() {

        accuracy = tvaccuval.text.toString().toDouble()
        ledTime = tvledval.text.toString().toDouble()
        step = tvstepval.text.toString().toDouble()
        calibration = tvacalval.text.toString().toDouble()

        val sharedPrefDevSet = activity!!.getSharedPreferences("settings", Activity.MODE_PRIVATE)
        val editor = sharedPrefDevSet.edit()
        editor.putString("step", step.toString()).apply()

        println("programJson() - Fetch json komutu deneniyor.")

        val url =
            host + "/State/setSettings1?thermoId=" + thermostat.thermoId + "&accuracy=" + accuracy + "&step=" + step + "&ledTime=" + ledTime + "&calibration=" + calibration

        println(url)

        val request = Request.Builder().url(url).build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                println("Basarisiz: " + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body!!.string()
                println("Cikti: " + body)
                val code = response.code
                if (code == 200) {
                    val jsonObject = JSONObject(body)
                    for (jsonIndex in 0 until jsonObject.length()) {
                        if (jsonObject.getString("result") == "ok") {
                            activity!!.runOnUiThread {
                                Toast.makeText(
                                    activity,
                                    "Ayarlarınız kaydedildi",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                        } else {
                            activity!!.runOnUiThread {
                                Toast.makeText(
                                    activity,
                                    "Kaydedilemedi, lütfen tekrar deneyin",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                }else{
                    unableToConnect()
                }
            }
        })
    }
}