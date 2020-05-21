package com.intherm.miofragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_register.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


class RegisterActivity : AppCompatActivity() {

    var host = "http://109.228.229.36:8080/termoServlet"
    var langId = "1"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)



        btnregisterCancel.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }


        btnSignup.setOnClickListener {

            val enteredpass = etRegPass.text.toString()
            val enteredconfirm = etConfirm.text.toString()

            if (checkFields(enteredpass, enteredconfirm) == true) {
                val url =
                    host + "/Register/signup?email=" + etEmail.text + "&password=" + etRegPass.text + "&name=" + et_name.text + "&address=" + et_adress.text + "&langId=" + langId

                println("btnSignup - Fetch json komutu deneniyor.")
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
                            val sharedPrefUser = getSharedPreferences(
                                "user info",
                                Context.MODE_PRIVATE
                            )
                            val editor = sharedPrefUser.edit()
                            for (jsonIndex in 0 until jsonObject.length()) {
                                if (jsonObject.getString("result").equals("exist")) {
                                    runOnUiThread {
                                        tvemailExists.text =
                                            getString(R.string.email_exists)
                                        tvemailExists.visibility = View.VISIBLE
                                    }
                                } else {
                                    runOnUiThread {
                                        tvemailExists.visibility = View.INVISIBLE
                                    }
                                    editor.putInt(
                                        "user id",
                                        jsonObject.getInt("userId")
                                    ).apply()
                                    val intent =
                                        Intent(this@RegisterActivity, LoginActivity::class.java)
                                    startActivity(intent)
                                }

                            }
                        }else{
                            unableToConnect()
                        }
                    }
                })
            }
        }
    }

    private fun unableToConnect() {
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

    fun checkFields(pass: String, confirm: String): Boolean {

        var resultmail = false
        var resultpass = false
        var resultname = false
        var resultconfirm = false
        var passmatch: Boolean
        var resultadress: Boolean

        if (etEmail.text.toString().isValidEmail()) {
            if (tvemailExists.visibility == View.VISIBLE) {
                runOnUiThread {
                    tvemailExists.visibility = View.INVISIBLE
                }
            }
            resultmail = true

        } else {
            if (tvemailExists.visibility != View.VISIBLE) {
                runOnUiThread {
                    tvemailExists.text = getString(R.string.invalid_email)
                    tvemailExists.visibility = View.VISIBLE
                }
            }
            resultmail = false
        }

        if (et_name.text.isNotEmpty()) {
            if (tv_name_error.visibility != View.INVISIBLE) {
                runOnUiThread {
                    tv_name_error.visibility = View.INVISIBLE
                }
            }
            resultname = true
        } else {
            if (tv_name_error.visibility != View.VISIBLE) {
                runOnUiThread {
                    tv_name_error.visibility = View.VISIBLE
                }
            }
            resultname = false
        }

        if (etRegPass.text.isNotEmpty()) {
            if (tv_pass_error.visibility != View.INVISIBLE) {
                runOnUiThread {
                    tv_pass_error.visibility = View.INVISIBLE
                }
            }
            resultpass = true
        } else {
            if (tv_pass_error.visibility != View.VISIBLE) {
                runOnUiThread {
                    tv_pass_error.visibility = View.VISIBLE
                }
            }
            resultpass = false
        }
        if (etConfirm.text.isNotEmpty()) {
            if (tv_confirm_error.visibility != View.INVISIBLE) {
                runOnUiThread {
                    tv_confirm_error.visibility = View.INVISIBLE
                }
            }
            resultconfirm = true
        } else {
            if (tv_confirm_error.visibility != View.VISIBLE) {
                runOnUiThread {
                    tv_confirm_error.visibility = View.VISIBLE
                }
            }
            resultconfirm = false
        }
        if (pass == confirm) {
            if (tv_confirm_error.visibility != View.INVISIBLE) {
                runOnUiThread {
                    tv_confirm_error.visibility = View.INVISIBLE
                }
            }
            passmatch = true
        } else {
            if (tv_confirm_error.visibility != View.VISIBLE) {
                runOnUiThread {
                    tv_confirm_error.visibility = View.VISIBLE
                }
            }
            passmatch = false
        }

        if (et_adress.text.isNotEmpty()) {
            if (tv_address_error.visibility != View.INVISIBLE) {
                runOnUiThread {
                    tv_address_error.visibility = View.INVISIBLE
                }
            }
            resultadress = true
        } else {
            if (tv_address_error.visibility != View.VISIBLE) {
                runOnUiThread {
                    tv_address_error.visibility = View.VISIBLE
                }
            }
            resultadress = false
        }
        return resultadress && resultconfirm && resultmail && resultname && resultpass && passmatch
    }

    fun String.isValidEmail(): Boolean = this.isNotEmpty() &&
            Patterns.EMAIL_ADDRESS.matcher(this).matches()
}


