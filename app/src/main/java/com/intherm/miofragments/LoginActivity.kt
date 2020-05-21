package com.intherm.miofragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_login.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException


class LoginActivity : AppCompatActivity() {

    val user = User()
    var thermoList = ArrayList<ThermoListItems>()
    var host = "http://109.228.229.36:8080/termoServlet"


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val sharedPrefDevice = getSharedPreferences("device", Activity.MODE_PRIVATE)
        user.thermoId = sharedPrefDevice.getInt("selected device id", 0)
        //Daha önce giriş yapılmış mı denetleyen metod(çıkış yapılmadıysa)
        isLoggedin()

        //Kayıt ekranı butonu
        btnRegister.setOnClickListener {
            errorTextHide()
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
        //Giriş butonu
        btnSignin.setOnClickListener {
            if (etRegEmail.text.toString().isValidEmail()) {
                progressShow()
                errorTextHide()
                signInRequest()

            } else {
                invalidMailAddressTextShow()
            }
        }

    }

    //Girilen mail adresinin mail formatına uygunluğunu denetleyen metod
    fun String.isValidEmail(): Boolean =
        this.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

    //Login sorgusu
    fun signInRequest() {
        //http sorgusu
        val url = host + "/Register/signin?email=" + etRegEmail.text + "&password=" + etRegPass.text

        println("signInRequest - Fetch json komutu deneniyor.")
        println(url)

        //Girilen kullanıcı adı ve şifreyi cihaz hafızasına kaydeden metod
        val sharedPrefUser = getSharedPreferences("user info", Context.MODE_PRIVATE)
        val editor = sharedPrefUser.edit()
        editor.putString("user email", etRegEmail.toString())
        editor.putString("user pass", etRegPass.toString())
        editor.apply()

        //http sorgusunu oluşturan metod
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {

            //sorgu başarısız olduğunda çalışan metod
            override fun onFailure(call: Call, e: IOException) {
                println("Basarisiz: " + e.message)
                progressHide()
                unableToConnect()
            }

            //Sorgu başarılı olduğunda çalışan metod
            override fun onResponse(call: Call, response: Response) {
                val body = response.body!!.string()
                println("Cikti: " + body)
                val code = response.code
                if (code == 200) {
                val jsonObject = JSONObject(body)
                for (jsonIndex in 0 until jsonObject.length()) {
                    user.result = jsonObject.getString("result")
                    //kullanıcı mevcut değil ise json "result" sonucu "empty" gelir.
                    if (user.result.equals("empty")) {
                        progressHide()
                        invalidLoginTextShow()
                    }
                    //kullanıcı mevcut ise json "result" sonucu "full" gelir.
                    if (user.result.equals("full")) {
                        val editor2 = sharedPrefUser.edit()
                        //gelen sonucu telefon hafızasına kaydeden metod
                        editor2.putString("result", user.result).apply()

                        if (user.thermoId == 0) {

                            val jsonObj = JSONObject(
                                body.substring(
                                    body.indexOf("{"),
                                    body.lastIndexOf("}") + 1
                                )
                            )
                            val thermoListJson = jsonObj.getJSONArray("thermoList")

                            //gelen kullanıcı bilgilerini telefon hafızasına kaydeden metod
                            for (i in 0..thermoListJson.length() - 1) {
                                editor2.putInt(
                                    "is owner", thermoListJson.getJSONObject(i).getInt("isOwner")
                                )
                                editor2.putInt(
                                    "thermo id", thermoListJson.getJSONObject(i).getInt("thermoId")
                                )
                                editor2.putString(
                                    "thermo name", thermoListJson.getJSONObject(i).getString("name")
                                )
                            }
                            editor2.putInt("user id", jsonObject.getInt("userId")).apply()
                            progressHide()

                            //başarılı giriş yapıldıysa ana sayfaya yönlendiren metod
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            val sharedPrefDevice =
                                getSharedPreferences("device", Activity.MODE_PRIVATE)
                            user.thermoId =
                                sharedPrefDevice.getInt("selected device id", 0)
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }else{
                    unableToConnect()
                }
            }

        })
//        editor.putString("is logged in", user.result)
    }

    //Kullanıcıya kayıtlı termostatları denetleyen metod
    @RequiresApi(Build.VERSION_CODES.N)
    fun getThermos() {
        val sharedPrefUser = getSharedPreferences("user info", Context.MODE_PRIVATE)
        user.userId = sharedPrefUser.getInt("user id", 0)
        val sharedPrefDevice = getSharedPreferences("device", Context.MODE_PRIVATE)
        val removedDeviceId = sharedPrefDevice.getInt("removed device", 0)

        println("getThermos - Fetch json komutu deneniyor.")

        val url = host + "/Register/getThermos?userID=" + user.userId

        val request = Request.Builder().url(url).build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                println("Basarisiz: " + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                println("Cikti: ")
                println(body)
                val code = response.code
                if (code == 200) {
                    val jsonArray = JSONArray(body)
                    for (jsonIndex in 0..(jsonArray.length() - 1)) {
                        thermoList.add(
                            ThermoListItems(
                                thermoId = jsonArray.getJSONObject(jsonIndex).getInt("thermoId"),
                                accuracy = jsonArray.getJSONObject(jsonIndex).getInt("accuracy"),
                                isOwner = jsonArray.getJSONObject(jsonIndex).getInt("isOwner"),
                                name = jsonArray.getJSONObject(jsonIndex).getString("name"),
                                step = jsonArray.getJSONObject(jsonIndex).getInt("step"),
                                workmode = jsonArray.getJSONObject(jsonIndex).getInt("workMode")
                            )
                        )
                    }
                    if (removedDeviceId == user.thermoId) {
                        val editor = sharedPrefUser.edit()
                        for (t in thermoList) {
                            editor.putInt("thermo id", t.thermoId).apply()
                        }
                    }
                }else{
                    unableToConnect()
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun isLoggedin() {
        val sharedPrefUser = getSharedPreferences("user info", Context.MODE_PRIVATE)
        user.isLogged = sharedPrefUser.getString("result", "").toString()
        //Kullanıcı giriş yapmış ise kayıtlı termostatlar denetlenir.
        if (user.isLogged == "full") {
            getThermos()
        }
        user.thermoId = sharedPrefUser.getInt("thermo id", 0)
        //Kullanıcı giriş yapmış fakat hiçbir termostat eklemediyse termostat ekleme ekranına yönlendirilir.
        if (user.isLogged == "full" && user.thermoId == 0) {
            var intent = Intent(this, ThermoNameActivity::class.java)
            startActivity(intent)
            finish()
        }
        //kullanıcı giriş yapmış ve ekli termostatı var ise ana ekrana yönlendirilir.
        if (user.isLogged == "full" && user.thermoId != 0) {
            var intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    //Yükleme barını gösteren metod
    fun progressShow() {
        runOnUiThread {
            tvLoginConnecting.visibility = View.VISIBLE
            loginProgressBar.visibility = View.VISIBLE
        }
    }

    //Yükleme barını gizleyen metod
    fun progressHide() {
        runOnUiThread {
            tvLoginConnecting.visibility = View.INVISIBLE
            loginProgressBar.visibility = View.INVISIBLE
        }
    }

    //Hatalı kullanıcı mesajını gösteren metod
    fun invalidLoginTextShow() {
        runOnUiThread {
            tvErrorMessage.setText(getString(R.string.userOrPassWrong))
            tvErrorMessage.visibility = View.VISIBLE
        }
    }

    //Hata mesajlarını kaldıran metod
    fun errorTextHide() {
        runOnUiThread {
            tvErrorMessage.visibility = View.INVISIBLE
        }
    }

    //Geçersiz e-posta formatı mesajını gösteren metod
    fun invalidMailAddressTextShow() {
        runOnUiThread {
            tvErrorMessage.setText(getString(R.string.invalidEmail))
            tvErrorMessage.visibility = View.VISIBLE
        }
    }

    //Sunucu ile iletişime geçilemedi alarmı gösteren metod
    fun unableToConnect() {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Hata!")
            builder.setMessage(getString(R.string.unableToConnect))
            builder.setNeutralButton("Tamam") { _, _ ->
                Toast.makeText(
                    applicationContext,
                    "Lütfen internet bağlantınızı kontrol ediniz.",
                    Toast.LENGTH_SHORT
                ).show()
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }
    }

    //Beklenmedik hata alarmı gösteren metod
    fun unexpectedError() {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Hata!")
            builder.setMessage("Beklenmedik bir hata oluştu.")
            builder.setNeutralButton("Tamam") { _, _ ->
                Toast.makeText(applicationContext, "Lütfen tekrar deneyin.", Toast.LENGTH_SHORT)
                    .show()
                val dialog: AlertDialog = builder.create()
                dialog.show()
            }

        }
    }

}

class ThermoListItems(
    val thermoId: Int,
    val workmode: Int,
    val isOwner: Int,
    val name: String,
    val accuracy: Int,
    val step: Int
)