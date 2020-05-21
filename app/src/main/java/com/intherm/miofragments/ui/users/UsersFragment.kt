package com.intherm.miofragments.ui.users

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.GsonBuilder
import com.intherm.miofragments.*
import kotlinx.android.synthetic.main.fragment_users.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class UsersFragment : Fragment(), UserAdaper.EventListener {

    var host = "http://109.228.229.36:8080/termoServlet"
    val userList = ArrayList<UserListItems>()
    var thermostat = Thermostat()
    var user = User()
    var inviteCode = ""


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_users, container, false)
        return root
    }

    override fun onStart() {
        super.onStart()
        recyclerView_users.layoutManager = LinearLayoutManager(activity)
        val sharedPrefDevice =
            activity!!.getSharedPreferences("device", Activity.MODE_PRIVATE)
        thermostat.thermoId = sharedPrefDevice.getInt("selected device id", 0)
        usersJson()
        val sharedPrefUser = activity!!.getSharedPreferences("user info", Activity.MODE_PRIVATE)
        user.userId = sharedPrefUser.getInt("user id", 0)

        btn_add_user.setOnClickListener {
            user.isOwner = 0
            showInviteDialog()
        }
    }

    fun showInviteDialog() {
        val dialog = Dialog(activity!!)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_adduser)
        val tvInviteCode = dialog.findViewById(R.id.tvInviteCode) as TextView
//        val tvInviteTitle = dialog.findViewById(R.id.tvInviteTitle) as TextView
        val btnadduser = dialog.findViewById(R.id.btn_invite_code) as Button
        val rbtnauth = dialog.findViewById(R.id.rbtn_auth_user) as RadioButton
        val rbtnpassive = dialog.findViewById(R.id.rbtn_passive_user) as RadioButton

        rbtnauth.setOnClickListener {
            user.isOwner = 2
        }
        rbtnpassive.setOnClickListener {
            user.isOwner = 1
        }
        btnadduser.setOnClickListener {
            if (user.isOwner == 1 || user.isOwner == 2) {
                generateInviteCode()
                Timer("SettingUp", false).schedule(200) {
                    activity!!.runOnUiThread {
                        tvInviteCode.text = inviteCode
                    }
                }
            } else {
                Toast.makeText(activity!!, "Lütfen kullanıcı tipini seçiniz", Toast.LENGTH_LONG)
                    .show()
            }
        }

        dialog.show()
    }

    fun generateInviteCode() {

        println("generateInviteCode() - Fetch json komutu deneniyor.")

        val url =
            host + "/Register/getInviteCode?thermoId=" + thermostat.thermoId + "&userID=" + user.userId + "&isOwner=" + user.isOwner

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
                        inviteCode = jsonObject.getString("inviteCode")
                    }
                } else {
                    unableToConnect()
                }
            }
        })
    }

    private fun usersJson() {

        println("programJson() - Fetch json komutu deneniyor.")

        val url =
            host + "/State/getThermoUsers?thermoId=" + thermostat.thermoId

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
                    activity!!.runOnUiThread {
                        btn_add_user.isEnabled = true
                    }
                    if (body != "[]") {
                        val gson = GsonBuilder().create()
                        val userListAll: List<UserListItems> =
                            gson.fromJson(body, Array<UserListItems>::class.java).toList()

                        val jsonArray = JSONArray(body)
                        userList.clear()
                        for (jsonIndex in 0..(userListAll.count() - 1)) {
                            userList.add(
                                UserListItems(
                                    jsonArray.getJSONObject(jsonIndex).getString("name").toString(),
                                    jsonArray.getJSONObject(jsonIndex).getString("userId").toInt()
                                )
                            )
                        }

                        activity!!.runOnUiThread {
                            recyclerView_users.adapter =
                                UserAdaper(activity!!, userList, this@UsersFragment)
                        }
                    }
                }else{
                    activity!!.runOnUiThread {
                        btn_add_user.isEnabled = false
                    }
                    unableToConnect()
                }
            }
        })
    }

    override fun onDelete(deletedUserId: Int) {

        val url =
            host + "/Register/removeUserAuth?thermoId=" + thermostat.thermoId + "&userId=" + deletedUserId

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
                usersJson()
            }

        })
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


}

class UserListItems(val name: String, val userId: Int)
