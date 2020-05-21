package com.intherm.miofragments.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.google.firebase.iid.FirebaseInstanceId
import com.intherm.miofragments.*
import kotlinx.android.synthetic.main.fragment_home.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule


class HomeFragment : Fragment() {

    //VARIABLES:
    val thermostat = Thermostat() //THERMOSTAT'S VARIABLES
    val user = User() //USER'S VARIABLES
    var host = "http://109.228.229.36:8080/termoServlet" //WEBSERVER ADDRESS
    var step = 0.5 //TEMPREQ STEP
    var timer = 6 //TIMER STARTING VALUE
    var deviceList = ArrayList<Devices>() //ARRAY LIST FOR REGISTERED DEVICES(THERMOSTATS)
    var progMode = false //PROGRAM MODE INIT
    var code: Int = 200


    //RUNNABLES
    //FIRST CHECK
    private val thermoHandler: Handler = Handler()
    val mThermoRunnable: Runnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.N)
        override fun run() {
            if (!thermostat.isWorking) {
                CheckThermo()
                progHandlerStop()
                thermoHandler.postDelayed(this, 2000)
            }
            if (thermostat.isWorking) {
                thermoHandlerStop()
                progHandlerRun()
            }
        }
    }
    //ROUTINE CHECK
    private val progHandler: Handler = Handler()
    val mProgRunnable: Runnable = object : Runnable {
        override fun run() {
            progState()
            progHandler.postDelayed(this, 5000)
        }

    }
    //TIMER
    private val tempHandler: Handler = Handler()
    val mTempRunnable: Runnable = object : Runnable {
        override fun run() {
            timer++
            if (timer == 2) {
                setTemp()
            }
            tempHandler.postDelayed(this, 1000)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        return root
    }

    override fun onStart() {
        super.onStart()
        //LOAD BACK LAST VARIABLES OF THERMOSTAT
        val sharedPrefProg = activity!!.getSharedPreferences("prog", Activity.MODE_PRIVATE)
        progMode = sharedPrefProg.getBoolean("progmode", false)
        val sharedPrefUser = activity!!.getSharedPreferences("user info", Activity.MODE_PRIVATE)
        user.thermoId = sharedPrefUser.getInt("thermo id", 0)
        sendFirebaseToken(sharedPrefUser.getInt("user id", 0))
        val sharedPrefMainPage =
            activity!!.getSharedPreferences("values", Activity.MODE_PRIVATE)
        if (sharedPrefMainPage.getString("tempreq", "") != "") {
            thermostat.targetTemp =
                sharedPrefMainPage.getString("tempreq", "").toString().toDouble()
            thermostat.currentTemp =
                sharedPrefMainPage.getString("currenttemp", "").toString().toDouble()
            thermostat.currentHum =
                sharedPrefMainPage.getString("humidity", "").toString().toDouble()
        }
        val sharedPrefDevSet =
            activity!!.getSharedPreferences("settings", Activity.MODE_PRIVATE)
        if (sharedPrefDevSet.getString("step", "") != "") {
            step = sharedPrefDevSet.getString("step", "").toString().toDouble()
        }
        light.isEnabled = progMode


        mTempRunnable.run()

        //MAIN PAGE BUTTONS
        btnminus.setOnClickListener {
            timer = 0
            if (thermostat.targetTemp > 5) {
                thermostat.targetTemp = thermostat.targetTemp.minus(step)
                targetTemp.text = (thermostat.targetTemp.toString() + "°")
                progHandlerStop()
                thermoHandlerStop()
            }
        }
        btnplus.setOnClickListener {
            timer = 0
            if (thermostat.targetTemp < 35) {
                thermostat.targetTemp = thermostat.targetTemp.plus(step)
                targetTemp.text = (thermostat.targetTemp.toString() + "°")
                progHandlerStop()
                thermoHandlerStop()
            }
        }
        sv_program.setOnClickListener {
            if (!light.isEnabled) {
                progMode = true
                thermostat.workmode = 1
                activateProgram()
            } else {
                progMode = false
                thermostat.workmode = 2
                manualMode()
            }
        }

    }

    override fun onPause() {
        super.onPause()
        //SAVE VARIABLES ON PAUSE
        val sharedPrefMainPage = activity!!.getSharedPreferences("values", Activity.MODE_PRIVATE)
        val editor = sharedPrefMainPage.edit()
        editor.putString("tempreq", thermostat.targetTemp.toString()).apply()
        editor.putString("humidity", thermostat.currentHum.toString()).apply()
        editor.putString("currenttemp", thermostat.currentTemp.toString()).apply()
        thermoHandlerStop()
        progHandlerStop()
    }

    override fun onResume() {
        super.onResume()
        //LOADBACK LAST VARIABLES ON RESUME
        val sharedPrefMainPage =
            activity!!.getSharedPreferences("values", Activity.MODE_PRIVATE)
        val sharedPrefProg = activity!!.getSharedPreferences("prog", Activity.MODE_PRIVATE)
        progMode = sharedPrefProg.getBoolean("progmode", false)
        targetTemp.text = sharedPrefMainPage.getString("tempreq", "")
        tvCurrentTemp.text = sharedPrefMainPage.getString("currenttemp", "")
        tvCurrentHum.text = sharedPrefMainPage.getString("humidity", "")
        light.isEnabled = progMode
        runAndGun()
    }

    //IS LOGGED IN CHECK
    fun runAndGun() {
        getThermos()
        thermoHanlerRun()
    }

    //1ST CHECK OF SELECTED THERMOSTAT
    @RequiresApi(Build.VERSION_CODES.N)
    fun CheckThermo() {
        val sharedPrefDevice = activity!!.getSharedPreferences("device", Activity.MODE_PRIVATE)
        val removedDeviceId = sharedPrefDevice.getInt("removed device", 0)
        val selectedDeviceId = sharedPrefDevice.getInt("selected device id", 0)
        if (removedDeviceId == selectedDeviceId) {
            val sharedPrefUser = activity!!.getSharedPreferences("user info", Activity.MODE_PRIVATE)
            user.thermoId = sharedPrefUser.getInt("thermo id", 0)
        } else {
            if (selectedDeviceId != 0 && removedDeviceId == user.thermoId) {
                user.thermoId = sharedPrefDevice.getInt("selected device id", 0)
            }
        }
        if (user.thermoId == 0) {
            progressHide()
            thermoHandlerStop()
            getThermos()
        } else {

            if (user.thermoId != 0 && user.userId != 0) {

                println("CheckThermo() - Fetch json komutu deneniyor.")

                val url =
                    host + "/State/deviceState?thermoId=" + user.thermoId + "&userId=" + user.userId

                println(url)

                val request = Request.Builder().url(url).build()

                val client = OkHttpClient()

                client.newCall(request).enqueue(object : Callback {

                    override fun onFailure(call: Call, e: IOException) {
                        println("Basarisiz: " + e.message)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val body = response.body?.string()
                        code = response.code
                        println("code: " + code)
                        println("Cikti: ")
                        println(body)
                        if (code == 200) {
                            if (body == "[{}]") {
                                val intent = Intent(activity, ThermoNameActivity::class.java)
                                activity!!.startActivity(intent)
                            } else {
                                if (body == "[{\"isOwner\":true}]" || body == "[{\"isOwner\":false}]") {
                                    setThermos()
                                } else {
                                    val jsonArray = JSONArray(body)


                                    for (jsonIndex in 0..(jsonArray.length() - 1)) {
                                        thermostat.isWorking =
                                            jsonArray.getJSONObject(jsonIndex)
                                                .getBoolean("isWorking")
//                          thermostat.currentTemp = jsonArray.getJSONObject(jsonIndex).getString("currentTemp")
                                        thermostat.isOwner =
                                            jsonArray.getJSONObject(jsonIndex).getBoolean("isOwner")
                                        thermostat.isActivated =
                                            jsonArray.getJSONObject(jsonIndex)
                                                .getBoolean("isActivated")
                                        thermostat.thermoId =
                                            jsonArray.getJSONObject(jsonIndex).getInt("thermoId")
                                        thermostat.currentTemp =
                                            jsonArray.getJSONObject(jsonIndex)
                                                .getString("currentTemp")
                                                .toDouble()

                                    }
                                }
                            }
                        } else {
                            unableToConnect()
                        }
                    }
                })
                if (code == 200) {
                    if (thermostat.isWorking && code == 200) {
                        val sharedPrefUser =
                            activity!!.getSharedPreferences("user info", Activity.MODE_PRIVATE)
                        sharedPrefUser.edit().putBoolean("is working", thermostat.isWorking).apply()
                        progressHide()
                        activity!!.runOnUiThread {
                            id_activity_main.setBackgroundResource(R.drawable.background_online)
                        }
                        progHandlerRun()
                        thermoHandlerStop()

                    }
                    if (!thermostat.isWorking && code == 200) {
                        val sharedPrefUser =
                            activity!!.getSharedPreferences("user info", Activity.MODE_PRIVATE)
                        sharedPrefUser.edit().putBoolean("is working", thermostat.isWorking).apply()
                        progressShow()
                        offlineMode()
                    }

                }
            } else {
                println("user.thermoId ya da user.userId boş geldi.")
            }
        }
    }

    private fun progHandlerStop() {
        progHandler.removeCallbacks(mProgRunnable)
    }

    private fun progHandlerRun() {
        mProgRunnable.run()
    }

    private fun thermoHandlerStop() {
        thermoHandler.removeCallbacks(mThermoRunnable)
    }

    private fun thermoHanlerRun() {
        mThermoRunnable.run()
    }

    //RUNNABLE FOR HOMEPAGE INFORMATIONS
    fun progState() {
        if (user.thermoId != 0 && user.userId != 0) {
            println("progState - Fetch json komutu deneniyor.")

            val url =
                host + "/State/programState?thermoId=" + user.thermoId + "&userId=" + user.userId

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
                    code = response.code
                    if (code == 200) {
                        val jsonObject = JSONObject(body)
                        for (jsonIndex in 0 until jsonObject.length()) {

                            thermostat.relayState = jsonObject.getBoolean("relayState")
                            thermostat.isReceiverWorking =
                                jsonObject.getBoolean("isReceiverWorking")
                            thermostat.workmode = jsonObject.getInt("workMode")
                            thermostat.isSet = jsonObject.getBoolean("isSet")
                            thermostat.currentHum = jsonObject.getString("humidity").toDouble()
                            thermostat.targetTemp = jsonObject.getString("tempReq").toDouble()
                            thermostat.currentTemp = jsonObject.getString("currentTemp").toDouble()
                            thermostat.isWorking = jsonObject.getBoolean("isWorking")


                        }
                    } else {
                        unableToConnect()
                    }
                }
            })
            if (code == 200) {
                val sharedPrefProg = activity!!.getSharedPreferences("prog", Activity.MODE_PRIVATE)
                val sharedPrefMainPage =
                    activity!!.getSharedPreferences("values", Activity.MODE_PRIVATE)
                val editor = sharedPrefMainPage.edit()
                editor.putString("tempreq", thermostat.targetTemp.toString()).apply()
                editor.putString("humidity", thermostat.currentHum.toString()).apply()
                editor.putString("currenttemp", thermostat.currentTemp.toString()).apply()
                Timer("SettingUp", false).schedule(200) {
                    activity!!.runOnUiThread {
                        if (thermostat.workmode == 2 && light.isEnabled) {
                            light.isEnabled = false
                            sharedPrefProg.edit().putBoolean("progmode", false).apply()
                        }
                        if (thermostat.workmode == 1 && !light.isEnabled) {
                            light.isEnabled = true
                            sharedPrefProg.edit().putBoolean("progmode", true).apply()
                        }
                    }

                }

                if (thermostat.isWorking) {
                    thermoHandlerStop()
                }
                if (!thermostat.isWorking) {
                    progHandlerStop()
                    thermoHanlerRun()
                }

                activity!!.runOnUiThread {
                    id_activity_main.setBackgroundResource(R.drawable.background_online)
                }
                activity!!.runOnUiThread {
                    tvCurrentTemp.text =
                        (thermostat.currentTemp.toString() + getString(R.string.degree))
                }
                activity!!.runOnUiThread {
                    targetTemp.text =
                        (thermostat.targetTemp.toString() + getString(R.string.degree))
                }
                activity!!.runOnUiThread {
                    tvCurrentHum.text = thermostat.currentHum.toString()
                }
                progressHide()

                if (thermostat.relayState) {
                    activity!!.runOnUiThread {
                        ivFlame.visibility = View.VISIBLE
                    }
                } else {
                    activity!!.runOnUiThread {
                        ivFlame.visibility = View.INVISIBLE
                    }
                }
                if (targetTemp.visibility != View.VISIBLE) {
                    activity!!.runOnUiThread {
                        targetTemp.visibility = View.VISIBLE
                    }
                }
                if (tvCurrentHum.visibility != View.VISIBLE) {
                    activity!!.runOnUiThread {
                        tvCurrentHum.visibility = View.VISIBLE
                    }
                }
                if (tvCurrentTemp.visibility != View.VISIBLE) {
                    activity!!.runOnUiThread {
                        tvCurrentTemp.visibility = View.VISIBLE
                    }
                }
                if (btnplus.isEnabled != true) {
                    activity!!.runOnUiThread {
                        btnplus.isEnabled = true
                    }
                }
                if (btnminus.isEnabled != true) {
                    activity!!.runOnUiThread {
                        btnminus.isEnabled = true
                    }
                }
                if (sv_program.isEnabled != true) {
                    activity!!.runOnUiThread {
                        sv_program.isEnabled = true
                    }
                }
            }
        }
    }

    //SAVE THERMOSTAT TO DEVICE MEMORY AS CURRENT
    private fun setThermos() {
        val sharedPrefUser =
            activity!!.getSharedPreferences("user info", Activity.MODE_PRIVATE)
        val editor = sharedPrefUser.edit()
        for (d in deviceList) {
            if (d.thermoId != user.thermoId) {
                user.thermoId = d.thermoId
                sharedPrefUser.edit().putInt("thermo id", d.thermoId).apply()
                editor.putString("selected device name", d.thermoName).apply()
                editor.putInt("selected device id", d.thermoId).apply()
            }
        }
    }

    //GET THERMOSTAT INFORMATION
    private fun getThermos() {
        val sharedPrefUser =
            activity!!.getSharedPreferences("user info", Activity.MODE_PRIVATE)
        user.userId = sharedPrefUser.getInt("user id", 0)


        if (user.userId != 0) {
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
                    code = response.code
                    if (code == 200) {
                        val jsonArray = JSONArray(body)
                        for (jsonIndex in 0..(jsonArray.length() - 1)) {
                            deviceList.add(
                                Devices(
                                    thermoId = jsonArray.getJSONObject(jsonIndex).getString("thermoId").toInt(),
                                    thermoName = jsonArray.getJSONObject(jsonIndex).getString("name")
                                )
                            )
                        }



                        if (deviceList.isEmpty()) {
                            val intent = Intent(activity, ThermoNameActivity::class.java)
                            activity!!.startActivity(intent)
                            activity!!.finish()
                        }
                    } else {

                    }
                }
            })
        }
        if (code == 200) {
            val sharedPrefDevice = activity!!.getSharedPreferences("device", Activity.MODE_PRIVATE)
            val editor = sharedPrefDevice.edit()
            val removedDeviceId = sharedPrefDevice.getInt("removed device", 0)
            val selectedDeviceId = sharedPrefDevice.getInt("selected device id", 0)
            val selectedDeviceName =
                sharedPrefDevice.getString("selected device name", "").toString()
            var homePageTitle: String


            if (selectedDeviceId != removedDeviceId) {
                user.thermoId = sharedPrefDevice.getInt("selected device id", 0)

            } else {
                for (d in deviceList) {
                    if (d.thermoId == user.thermoId) {
                        editor.putString("selected device name", d.thermoName).apply()
                        editor.putInt("selected device id", d.thermoId).apply()
                    } else {
                        user.thermoId = d.thermoId
                        sharedPrefUser.edit().putInt("thermo id", d.thermoId).apply()
                    }
                }
            }

            if (selectedDeviceId == removedDeviceId || selectedDeviceId == 0) {

                progressHide()
                noThermoMode()


            } else {
                Timer("SettingUp", false).schedule(200) {
                    if (selectedDeviceName != "") {
                        activity!!.runOnUiThread {
                            if (sharedPrefDevice.getString(
                                    "selected device name",
                                    ""
                                ).toString() != ""
                            ) {
                                (activity as MainActivity).supportActionBar!!.title =
                                    sharedPrefDevice.getString("selected device name", "")
                                        .toString()
                            }
                        }
                    } else {
                        for (d in deviceList) {
                            if (selectedDeviceId == d.thermoId) {
                                editor.putString("selected device name", d.thermoName).apply()
                                homePageTitle = d.thermoName
                                activity!!.runOnUiThread {
                                    (activity as MainActivity).supportActionBar?.title =
                                        homePageTitle
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    //SHOW CONNECTING ICON AND STRING
    fun progressShow() {
        activity!!.runOnUiThread {
            tvConnecting.visibility = View.VISIBLE
            mainProgressBar.visibility = View.VISIBLE
        }
    }

    //HIDE CONNECTING ICON AND STRING
    fun progressHide() {
        activity!!.runOnUiThread {
            tvConnecting.visibility = View.INVISIBLE
            mainProgressBar.visibility = View.INVISIBLE
        }
    }

    //SET TARGET TEMPRATURE
    fun setTemp() {
        if (user.thermoId != 0 && user.userId != 0) {

            println("setTemp() - Fetch json komutu deneniyor.")

            val url =
                host + "/Temp?tempReq=" + thermostat.targetTemp + "&thermoId=" + user.thermoId

            println(url)

            val request = Request.Builder().url(url).build()

            val client = OkHttpClient()

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    println("Basarisiz: " + e.message)
                }

                override fun onResponse(call: Call, response: Response) {
                    println("Başarılı")
                }
            })
            val sharedPrefProg = activity!!.getSharedPreferences("prog", Activity.MODE_PRIVATE)
            sharedPrefProg.edit().putBoolean("progmode", false).apply()
            Toast.makeText(activity, "TERMOSTAT SICAKLIK AYARI GÜNCELLENDİ", Toast.LENGTH_LONG)
                .show()
            progHandlerRun()
        }

    }

    //DISABLE PROGRAM MODE
    fun manualMode() {
        if (user.thermoId != 0 && user.userId != 0) {

            println("setTemp() - Fetch json komutu deneniyor.")

            val url =
                host + "/Temp?tempReq=" + thermostat.targetTemp + "&thermoId=" + user.thermoId

            println(url)

            val request = Request.Builder().url(url).build()

            val client = OkHttpClient()

            client.newCall(request).enqueue(object : Callback {

                override fun onFailure(call: Call, e: IOException) {
                    println("Basarisiz: " + e.message)
                }

                override fun onResponse(call: Call, response: Response) {
                    println("Başarılı")
                }
            })
            val sharedPrefProg = activity!!.getSharedPreferences("prog", Activity.MODE_PRIVATE)
            sharedPrefProg.edit().putBoolean("progmode", false).apply()
            Toast.makeText(activity, "PROGRAM MODUNDAN ÇIKILDI", Toast.LENGTH_SHORT)
                .show()
            progHandlerRun()
        }

    }

    //ACTIVATE PROGRAM MODE
    private fun activateProgram() {

        progressShow()
        Toast.makeText(activity!!, "Program moduna geçiliyor", Toast.LENGTH_LONG).show()

        var result = ""
        val sharedPrefProg = activity!!.getSharedPreferences("prog", Activity.MODE_PRIVATE)

        println("activateProgram() - Fetch json komutu deneniyor.")

        val url =
            host + "/Program/activateProgram?thermoId=" + user.thermoId

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
                val jsonObject = JSONObject(body)
                for (jsonIndex in 0 until jsonObject.length()) {
                    result = jsonObject.getString("result")
                }
            }
        })
        if (result == "full") {
            Toast.makeText(activity!!, "PROGRAM MODU AKTİF EDİLDİ", Toast.LENGTH_LONG).show()
            sharedPrefProg.edit().putBoolean("progmode", true).apply()
            progressHide()
        }
        if (result == "empty") {
            Toast.makeText(activity!!, "LÜTFEN ÖNCE PROGRAM AYARLAYINIZ", Toast.LENGTH_LONG).show()
            light.isEnabled = false
            progressHide()
        }
    }

    //SEND FIREBASE TOKEN TO WEBSERVER TO SAVE DATABASE
    fun sendFirebaseToken(userId: Int) {

        val token = FirebaseInstanceId.getInstance().instanceId.toString()

        println("sendFirebaseToken() - Fetch json komutu deneniyor.")

        val url = host + "/Register/fbtkn?firebaseToken=" + token + "&userId=" + userId

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
            }
        })

    }

    //GET SAVED FIREBASE TOKEN FROM DATABASE
    fun getFirebaseToken() {

        var fbtoken: String

        println("sendFirebaseToken() - Fetch json komutu deneniyor.")

        val url = host + "/Register/fbtknget?userId=" + user.userId

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
                val jsonObject = JSONObject(body)
                for (jsonIndex in 0 until jsonObject.length()) {
                    fbtoken = jsonObject.getString("firebaseToken")
                }

            }
        })

    }

    //CHECK CONNECTION AND ITS TYPE
    fun getConnectionType(context: Context): Int {
        var result = 0 // Returns connection type. 0: none; 1: mobile data; 2: wifi
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm?.run {
                cm.getNetworkCapabilities(cm.activeNetwork)?.run {
                    if (hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        result = 2
                    } else if (hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        result = 1
                    }
                }
            }
        } else {
            cm?.run {
                cm.activeNetworkInfo?.run {
                    if (type == ConnectivityManager.TYPE_WIFI) {
                        result = 2
                    } else if (type == ConnectivityManager.TYPE_MOBILE) {
                        result = 1
                    }
                }
            }
        }
        return result
    }

    //UNABLE TO CONNECT
    fun unableToConnect() {
        progHandlerStop()
        thermoHandlerStop()
        noThermoMode()
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

    //SET HOME SCREEN TO OFFLINE MODE
    fun offlineMode() {
        if (targetTemp.visibility != View.INVISIBLE) {
            activity!!.runOnUiThread {
                targetTemp.visibility = View.INVISIBLE
            }
        }
        if (tvCurrentHum.visibility != View.INVISIBLE) {
            activity!!.runOnUiThread {
                tvCurrentHum.visibility = View.INVISIBLE
            }
        }
        if (tvCurrentTemp.visibility != View.INVISIBLE) {
            activity!!.runOnUiThread {
                tvCurrentTemp.visibility = View.INVISIBLE
            }
        }
        if (ivFlame.visibility != View.INVISIBLE) {
            activity!!.runOnUiThread {
                ivFlame.visibility = View.INVISIBLE
            }
        }
        if (btnplus.isEnabled != false) {
            activity!!.runOnUiThread {
                btnplus.isEnabled = false
            }
        }
        if (btnminus.isEnabled != false) {
            activity!!.runOnUiThread {
                btnminus.isEnabled = false
            }
        }
        if (sv_program.isEnabled != false) {
            activity!!.runOnUiThread {
                sv_program.isEnabled = false
            }
        }
        activity!!.runOnUiThread {
            id_activity_main.setBackgroundResource(R.drawable.background_offline)
        }
    }

    //SET HOME SCREEN TO NO THERMOSTAT SELECTED MODE
    fun noThermoMode() {
        if (targetTemp.visibility != View.INVISIBLE) {
            activity!!.runOnUiThread {
                targetTemp.visibility = View.INVISIBLE
            }
        }
        if (tvCurrentHum.visibility != View.INVISIBLE) {
            activity!!.runOnUiThread {
                tvCurrentHum.visibility = View.INVISIBLE
            }
        }
        if (tvCurrentTemp.visibility != View.INVISIBLE) {
            activity!!.runOnUiThread {
                tvCurrentTemp.visibility = View.INVISIBLE
            }
        }
        if (ivFlame.visibility != View.INVISIBLE) {
            activity!!.runOnUiThread {
                ivFlame.visibility = View.INVISIBLE
            }
        }
        if (btnplus.isEnabled != false) {
            activity!!.runOnUiThread {
                btnplus.isEnabled = false
            }
        }
        if (btnminus.isEnabled != false) {
            activity!!.runOnUiThread {
                btnminus.isEnabled = false
            }
        }
        if (sv_program.isEnabled != false) {
            activity!!.runOnUiThread {
                sv_program.isEnabled = false
            }
        }
        activity!!.runOnUiThread {
            (activity as MainActivity).supportActionBar?.title = "Lütfen bir termostat seçiniz"
            id_activity_main.setBackgroundResource(R.drawable.background_nodevice)
        }

    }
}

class Devices(var thermoId: Int, var thermoName: String)