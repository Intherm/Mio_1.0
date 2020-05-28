package com.intherm.miofragments.ui.devices

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.GsonBuilder
import com.intherm.miofragments.*
import com.intherm.miofragments.ui.home.HomeFragment
import kotlinx.android.synthetic.main.fragment_devices.*
import okhttp3.*
import org.json.JSONArray
import java.io.IOException

class DevicesFragment : Fragment(), DeviceAdapter.EventListener {

    var host = "http://109.228.229.36:8080/termoServlet"
    val deviceList = ArrayList<DeviceListItems>()
    val user = User()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_devices, container, false)

        return root
    }

    override fun onStart() {
        super.onStart()
        recyclerView_devices.layoutManager = LinearLayoutManager(activity)
        val sharedPrefUser =
            activity!!.getSharedPreferences("user info", Activity.MODE_PRIVATE)
        user.userId = sharedPrefUser.getInt("user id", 0)
        user.thermoId = sharedPrefUser.getInt("thermo id", 0)
        devicesJson()
        btn_add.setOnClickListener {
            addDevice()
        }


    }

    private fun devicesJson() {

        if (user.userId != 0) {
            println("devicesJson() - Fetch json komutu deneniyor.")

            val url = host + "/Register/getThermos?userID=" + user.userId

            println(url)

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
                        btn_add.isEnabled = true
                        if (body != "[]") {
                            val gson = GsonBuilder().create()
                            val deviceListAll: List<DeviceListItems> =
                                gson.fromJson(body, Array<DeviceListItems>::class.java).toList()

                            val jsonArray = JSONArray(body)
                            deviceList.clear()
                            for (jsonIndex in 0 until deviceListAll.count()) {
                                deviceList.add(
                                    DeviceListItems(
                                        jsonArray.getJSONObject(jsonIndex).getString("name").toString(),
                                        jsonArray.getJSONObject(jsonIndex).getString("thermoId").toInt(),
                                        jsonArray.getJSONObject(jsonIndex).getInt("isOwner")
                                    )
                                )
                            }

                            activity!!.runOnUiThread {
                                recyclerView_devices.adapter =
                                    DeviceAdapter(activity!!, deviceList, this@DevicesFragment)
                            }
                        }
                    }else{
                        unableToConnect()
                    }
                }
            })
        }
    }

    private fun unableToConnect() {
        activity!!.runOnUiThread {
            btn_add.isEnabled = false
            val builder = androidx.appcompat.app.AlertDialog.Builder(activity!!)
            builder.setTitle("Hata!")
            builder.setMessage(getString(R.string.unableToConnect))
            builder.setNeutralButton("Tamam") { _, _ ->

            }
            val dialog: androidx.appcompat.app.AlertDialog = builder.create()
            dialog.show()
        }

    }

    fun removeDevice() {

        println("deleteDevice() - Fetch json komutu deneniyor.")

        val sharedPrefDevice = activity!!.getSharedPreferences("device", Activity.MODE_PRIVATE)
        val editor = sharedPrefDevice.edit()
        val toRemove = sharedPrefDevice.getInt("removed device", 0).toString()
        val removedDeviceId = sharedPrefDevice.getInt("removed device", 0)
        val selectedDeviceId = sharedPrefDevice.getInt("selected device id", 0)
        if (removedDeviceId == selectedDeviceId) {
            editor.remove("selected device id").apply()
            editor.remove("selected device name").apply()
        }
        val url = host + "/Register/deleteThermo?thermoId=" + toRemove

        println(url)

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
                devicesJson()
            }

        })

    }

    fun addDevice() {
        val intent = Intent(activity, ThermoNameActivity::class.java)
        activity!!.startActivity(intent)
    }

    override fun onDelete() {
        removeDevice()
    }

    override fun onClick() {
        val intent = Intent(activity, MainActivity::class.java)
        activity!!.startActivity(intent)
    }

}

class DeviceListItems(val name: String, val device_id: Int, val isOwner: Int)
