package com.intherm.miofragments

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_thermo_name.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ThermoNameActivity : AppCompatActivity() {

    var host = "http://109.228.229.36:8080/termoServlet"
    var inviteCode = ""
    val user = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thermo_name)
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

        //If requested permission isn't Granted yet
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Request permission from user
            val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE)
            ActivityCompat.requestPermissions(this, permissions, 0)}

        btnHomepage.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btn_existing_thermo.setOnClickListener {
            showDialog()
        }

        btn_new_thermo.setOnClickListener {
            val intent = Intent(this, AddDeviceActivity::class.java)
            startActivity(intent)
        }

    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun showDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_invitecode)
        val okbtn = dialog.findViewById(R.id.btn_invite_ok) as Button
        val tvinvite = dialog.findViewById(R.id.tv_invitecode) as TextView
        val etinvite = dialog.findViewById(R.id.et_invitecode) as EditText
        okbtn.setOnClickListener {
            inviteCode = etinvite.text.toString()
            dialog.dismiss()
            if (etinvite.text.isNotEmpty()) {
                approveInviteCode()
            } else {
                Toast.makeText(this, "Lütfen davetiye kodunu giriniz.", Toast.LENGTH_LONG).show()
            }
        }
        dialog.show()

    }

    private fun approveInviteCode() {

        val sharedPrefDevice = getSharedPreferences("device", Activity.MODE_PRIVATE)
        val editor = sharedPrefDevice.edit()
        val sharedPrefUser = getSharedPreferences("user info", Activity.MODE_PRIVATE)

        println("approveInviteCode() - Fetch json komutu deneniyor.")

        val url =
            host + "/Register/approveInviteCode?userID=" + user.userId + "&inviteCode=" + inviteCode

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
                    if (body == "{}") {
                        runOnUiThread {
                            tv_wrongcode.visibility = View.VISIBLE
                        }
                    } else {
                        runOnUiThread {
                            tv_wrongcode.visibility = View.INVISIBLE
                        }
                        for (jsonIndex in 0 until jsonObject.length()) {
                            jsonObject.getInt("isOwner")
                            jsonObject.getDouble("accuracy")
                            jsonObject.getString("thermoName")
                            editor.putInt("selected device id", jsonObject.getInt("thermoId"))
                                .apply()
                            sharedPrefUser.edit().putInt("thermo id", jsonObject.getInt("thermoId"))
                                .apply()

                        }
                        val intent = Intent(this@ThermoNameActivity, MainActivity::class.java)
                        this@ThermoNameActivity.startActivity(intent)
                        this@ThermoNameActivity.finish()
                    }
                }else{
                    unableToConnect()
                }
            }
        })
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






