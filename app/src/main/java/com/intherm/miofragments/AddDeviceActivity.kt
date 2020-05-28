package com.intherm.miofragments

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import kotlinx.android.synthetic.main.activity_add_device.*
import kotlinx.android.synthetic.main.app_bar_main.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import kotlinx.android.synthetic.main.app_bar_main.toolbar as toolbar1

class AddDeviceActivity : AppCompatActivity() {

    val host = "http://109.228.229.36:8080/termoServlet"
    val user = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_device)
        setSupportActionBar(toolbar)
        val sharedPrefUser = getSharedPreferences("user info", Context.MODE_PRIVATE)
        val sharedPrefDevice = getSharedPreferences("device", Context.MODE_PRIVATE)
        val editor2 = sharedPrefDevice.edit()
        user.userId = sharedPrefUser.getInt("user id", 0)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = getString(R.string.back)
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        actionbar.setDisplayHomeAsUpEnabled(true)


        btnConfirm.setOnClickListener {
            val thermoName = etThermoName.text.toString()

            val host = "http://109.228.229.36:8080/termoServlet"
            val url =
                host + "/Register/registerThermoFromApp?lat=28&lng=58&userId=" + user.userId + "&thermoName=" + thermoName + "&county=Turkey"
            println("btnThermoName - Fetch json komutu deneniyor.")
            println(url)
            val request = Request.Builder().url(url).build()

            val client = OkHttpClient()

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    println("Basarisiz: " + e.message)
                }

                override fun onResponse(call: Call, response: Response) {
                    println("Kayıt Başarılı")
                    val body = response.body!!.string()
                    println("Cikti: " + body)
                    val code = response.code
                    if (code == 200) {
                        val jsonObject = JSONObject(body)
                        for (jsonIndex in 0 until jsonObject.length()) {
                            editor2.putInt(
                                "selected device id",
                                jsonObject.getString("thermoId").toInt()
                            ).apply()
                            editor2.remove("selected device name").apply()
                        }
                        val intent =
                            Intent(this@AddDeviceActivity, DeviceWifiConnectActivity::class.java)
                        startActivity(intent)
                    } else {
                        unableToConnect()
                    }
                }
            })

        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    fun unableToConnect() {
        runOnUiThread {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Hata!")
            builder.setMessage(getString(R.string.unableToConnect))
            builder.setNeutralButton("Tamam") { _, _ ->

            }
            val dialog: androidx.appcompat.app.AlertDialog = builder.create()
            dialog.show()
        }
    }


}

