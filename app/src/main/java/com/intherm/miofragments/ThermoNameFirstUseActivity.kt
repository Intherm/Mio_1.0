package com.intherm.miofragments

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_thermo_name.*

import kotlinx.android.synthetic.main.activity_thermo_name_first_use.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ThermoNameFirstUseActivity : AppCompatActivity() {

    var host = "http://109.228.229.36:8080/termoServlet"
    var inviteCode = ""
    val user = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thermo_name_first_use)
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

        btn_FirstSignout.setOnClickListener {
            logOut()
        }

        btn_first_existing_thermo.setOnClickListener {
            showDialog()
        }

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
                            tv_first_wrongcode.visibility = View.VISIBLE
                        }
                    } else {
                        runOnUiThread {
                            tv_first_wrongcode.visibility = View.INVISIBLE
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
                        val intent = Intent(this@ThermoNameFirstUseActivity, MainActivity::class.java)
                        this@ThermoNameFirstUseActivity.startActivity(intent)
                        this@ThermoNameFirstUseActivity.finish()
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

    private fun logOut() {
        val sharedPrefUser = getSharedPreferences("user info", Context.MODE_PRIVATE)
        sharedPrefUser.edit().clear().apply()
        val sharedPreferencesProgram = getSharedPreferences("program", Context.MODE_PRIVATE)
        sharedPreferencesProgram.edit().clear().apply()
        val sharedPrefProg = getSharedPreferences("prog", Context.MODE_PRIVATE)
        sharedPrefProg.edit().clear().apply()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

}
