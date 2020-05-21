package com.intherm.miofragments.ui.notifications

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.GsonBuilder
import com.intherm.miofragments.NotifyAdaper
import com.intherm.miofragments.R
import com.intherm.miofragments.User
import kotlinx.android.synthetic.main.fragment_notifications.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.concurrent.schedule


class NotificationsFragment : Fragment(), NotifyAdaper.EventListener {

    var host = "http://109.228.229.36:8080/termoServlet"
    val notifications = ArrayList<Notifications>()
    val user = User()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_notifications, container, false)
        return root

    }

    override fun onStart() {
        super.onStart()
        recyclerview_notifications.layoutManager = LinearLayoutManager(activity)

        val sharedPrefUser = activity!!.getSharedPreferences("user info", Activity.MODE_PRIVATE)
        user.userId = sharedPrefUser.getInt("user id", 0)

        btn_delAllNotify.setOnClickListener {
            deleteAllNotifications()
        }

        Timer("Wait 0.5", false).schedule(500) {
            getNotifications()
        }


    }

    fun getNotifications() {

        println("getNotifications() - Fetch json komutu deneniyor.")

        val url =
            host + "/State/getNotifications?userId=" + user.userId

        println(url)

        val request = Request.Builder().url(url).build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                println("Basarisiz: " + e.message)
            }

            @RequiresApi(Build.VERSION_CODES.N)
            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                println("Cikti: ")
                println(body)
                val code = response.code
                if (code == 200) {
                    activity!!.runOnUiThread {
                        btn_delAllNotify.isEnabled = true
                    }
                    if (body != "[]") {
                        val gson = GsonBuilder().create()
                        val programListAllDays: List<Notifications> =
                            gson.fromJson(body, Array<Notifications>::class.java).toList()


                        val jsonArray = JSONArray(body)
                        notifications.clear()
                        for (jsonIndex in 0..(programListAllDays.count() - 1)) {
                            notifications.add(
                                Notifications(
                                    jsonArray.getJSONObject(jsonIndex).getString("date"),
                                    jsonArray.getJSONObject(jsonIndex).getBoolean("isSent"),
                                    jsonArray.getJSONObject(jsonIndex).getInt("id"),
                                    jsonArray.getJSONObject(jsonIndex).getString("text"),
                                    jsonArray.getJSONObject(jsonIndex).getString("title"),
                                    jsonArray.getJSONObject(jsonIndex).getInt("userId")
                                )
                            )
                        }
                        activity!!.runOnUiThread {
                            recyclerview_notifications.adapter =
                                NotifyAdaper(activity!!, notifications, this@NotificationsFragment)
                            tv_count.text =
                                (getString(R.string.notification) + notifications.count().toString())
                        }
                    }
                }else{
                    unableToConnect()
                }
            }
        })
    }

    override fun onDeleteNotify(notifyId: Int) {

        progressShow()

        val url = "$host/State/deleteNotification?notifId=$notifyId"

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
                progressHide()
            }

        })
    }

    private fun deleteAllNotifications() {

        val url = host + "/State/deleteAllNotifications?userId=" + user.userId

        println(url)

        val request = Request.Builder().url(url).build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                println("Basarisiz: " + e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body!!.string()
                println("Cikti: ")
                println(body)
                val code = response.code
                if (code == 200) {
                    val jsonObject = JSONObject(body)
                    for (jsonIndex in 0 until jsonObject.length()) {
                        if (jsonObject.getString("result") == "success") {
                            activity!!.runOnUiThread {
                                Toast.makeText(
                                    activity,
                                    "Tüm bildirimler silindi",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                                getNotifications()
                            }
                        } else {
                            activity!!.runOnUiThread {
                                Toast.makeText(
                                    activity,
                                    "İşlem başarısız, lütfen tekrar deneyin",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } else {
                    unableToConnect()
                }
            }

        })
    }

    private fun unableToConnect() {
        activity!!.runOnUiThread {
            btn_delAllNotify.isEnabled = false
            val builder = androidx.appcompat.app.AlertDialog.Builder(activity!!)
            builder.setTitle("Hata!")
            builder.setMessage(getString(R.string.unableToConnect))
            builder.setNeutralButton("Tamam") { _, _ ->

            }
            val dialog: androidx.appcompat.app.AlertDialog = builder.create()
            dialog.show()
        }
    }

    private fun progressShow() {
        activity!!.runOnUiThread {
            pb_loading.visibility = View.VISIBLE
        }
    }

    private fun progressHide() {
        activity!!.runOnUiThread {
            pb_loading.visibility = View.INVISIBLE
        }
    }
}

class Notifications(
    val date: String,
    val isSent: Boolean,
    val id: Int,
    val text: String,
    val title: String,
    val userid: Int
)
