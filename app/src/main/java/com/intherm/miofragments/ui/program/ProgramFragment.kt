package com.intherm.miofragments.ui.program

import android.app.Activity
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.intherm.miofragments.*
import kotlinx.android.synthetic.main.dialog_addprogram.*
import kotlinx.android.synthetic.main.fragment_devicesettings.*
import kotlinx.android.synthetic.main.fragment_program.*
import kotlinx.coroutines.delay
import okhttp3.*
import org.json.JSONArray
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlin.time.ExperimentalTime


class ProgramFragment : Fragment(), MainAdapter.EventListener {

    var host = "http://109.228.229.36:8080/termoServlet"
    val weeklyList = ArrayList<Program>()
    var thermostat = Thermostat()
    val switched = ArrayList<Program>()
    var defaultDay = 1
    private var adapter: MainAdapter? = null
    var updatedProgramTime: Long = 0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_program, container, false)
        return root

    }

    @RequiresApi(Build.VERSION_CODES.N)
    @ExperimentalTime
    override fun onStart() {
        super.onStart()
        recyclerView_programs.layoutManager = LinearLayoutManager(activity)
        val sharedPrefDevice =
            activity!!.getSharedPreferences("device", Activity.MODE_PRIVATE)
        thermostat.thermoId = sharedPrefDevice.getInt("selected device id", 0)

        programJson()

        Pazartesi.setOnClickListener {
            Pazartesi.isEnabled = false
            defaultDay = 1
            switchDays(defaultDay)
        }
        Sali.setOnClickListener {
            Sali.isEnabled = false
            defaultDay = 2
            switchDays(defaultDay)
        }
        Carsamba.setOnClickListener {
            Carsamba.isEnabled = false
            defaultDay = 3
            switchDays(defaultDay)
        }
        Persembe.setOnClickListener {
            Persembe.isEnabled = false
            defaultDay = 4
            switchDays(defaultDay)
        }
        Cuma.setOnClickListener {
            Cuma.isEnabled = false
            defaultDay = 5
            switchDays(defaultDay)
        }
        Cumartesi.setOnClickListener {
            Cumartesi.isEnabled = false
            defaultDay = 6
            switchDays(defaultDay)
        }
        Pazar.setOnClickListener {
            Pazar.isEnabled = false
            defaultDay = 7
            switchDays(defaultDay)
        }
        btn_addprog.setOnClickListener {
            addProgram()
        }
        btn_progsave.setOnClickListener {
            setPrograms()
        }

        Timer("SettingUp", false).schedule(200) {
            activity!!.runOnUiThread {
                Pazartesi.performClick()
            }
        }


    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun addProgram() {
        val dialog = Dialog(context!!)
        dialog.setContentView(R.layout.dialog_addprogram)
        val temp = dialog.findViewById(R.id.id_add_temp_pop) as TextView
        val seekbar = dialog.findViewById(R.id.sb_add_temp) as SeekBar
        val time = dialog.findViewById(R.id.id_add_time_pop) as TextView
        val ok = dialog.findViewById(R.id.btn_add_ok) as Button
        val settime = dialog.findViewById(R.id.btn_add_picktime) as ImageView
        val settime2 = dialog.findViewById(R.id.id_add_time_pop) as TextView
        val wholeweek = dialog.findViewById(R.id.cb_wholeweek) as CheckBox
        seekbar.max = 300
        seekbar.progress = 215
        temp.text = (((seekbar.progress.toDouble()) / 10) + 5).toString()
        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                temp.text = (((progress.toDouble()) / 10) + 5).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                temp.text = (((seekBar!!.progress.toDouble()) / 10) + 5).toString()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                temp.text = (((seekBar!!.progress.toDouble()) / 10) + 5).toString()
            }

        })

        settime.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener =
                TimePickerDialog.OnTimeSetListener { timePicker: TimePicker, hour, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    updatedProgramTime =
                        getMilliFromDate(SimpleDateFormat("HH:mm").format(cal.time).toString())
                    time.text = SimpleDateFormat("HH:mm").format(cal.time)
                }
            TimePickerDialog(
                activity, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(
                    Calendar.MINUTE
                ), true
            ).show()
        }

        settime2.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener =
                TimePickerDialog.OnTimeSetListener { timePicker: TimePicker, hour, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    updatedProgramTime =
                        getMilliFromDate(SimpleDateFormat("HH:mm").format(cal.time).toString())
                    time.text = SimpleDateFormat("HH:mm").format(cal.time)
                }
            TimePickerDialog(
                activity, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(
                    Calendar.MINUTE
                ), true
            ).show()
        }

        ok.setOnClickListener {
            if (wholeweek.isChecked) {
                if (updatedProgramTime != 0L) {
                    weeklyList.add(
                        Program(
                            1,
                            temp.text.toString().toDouble(),
                            0,
                            updatedProgramTime
                        )
                    )
                    weeklyList.add(
                        Program(
                            2,
                            temp.text.toString().toDouble(),
                            0,
                            updatedProgramTime
                        )
                    )
                    weeklyList.add(
                        Program(
                            3,
                            temp.text.toString().toDouble(),
                            0,
                            updatedProgramTime
                        )
                    )
                    weeklyList.add(
                        Program(
                            4,
                            temp.text.toString().toDouble(),
                            0,
                            updatedProgramTime
                        )
                    )
                    weeklyList.add(
                        Program(
                            5,
                            temp.text.toString().toDouble(),
                            0,
                            updatedProgramTime
                        )
                    )
                    weeklyList.add(
                        Program(
                            6,
                            temp.text.toString().toDouble(),
                            0,
                            updatedProgramTime
                        )
                    )
                    weeklyList.add(
                        Program(
                            7,
                            temp.text.toString().toDouble(),
                            0,
                            updatedProgramTime
                        )
                    )
                    dialog.hide()
                    switchDays(defaultDay)
                    updatedProgramTime = 0
                } else {
                    Toast.makeText(
                        activity!!,
                        "Lütfen program saatini seçin ve 00:00 olarak ayarlamayın.",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            } else {
                if (updatedProgramTime != 0L) {
                    weeklyList.add(
                        Program(
                            defaultDay,
                            temp.text.toString().toDouble(),
                            0,
                            updatedProgramTime
                        )
                    )
                    dialog.hide()
                    switchDays(defaultDay)
                    updatedProgramTime = 0
                } else {
                    Toast.makeText(
                        activity!!,
                        "Lütfen program saatini seçin ve 00:00 olarak ayarlamayın.",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }

        }
        dialog.show()
    }

    fun getMilliFromDate(dateFormat: String?): Long {
        var date = Date()
        val formatter = SimpleDateFormat("HH:mm")
        try {
            date = formatter.parse(dateFormat)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        println("Today is $date")
        return date.time.plus(7200000)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun switchDays(typeID: Int) {
        if (switched.isNotEmpty()) {
            for (s in switched) {
                for (w in weeklyList) {
                    if (s.id == w.id) {
                        if (s.programTimeValue != w.programTimeValue || s.temp != w.temp) {
                            w.programTimeValue = s.programTimeValue
                            w.temp = s.temp
                        }
                    }
                }
            }
        }
        weeklyList
        switched.clear()
        if (typeID == 1) {
            for (i in weeklyList) {
                if (i.typeId == typeID) {
                    switched.add(
                        Program(
                            i.typeId,
                            i.temp,
                            i.id,
                            i.programTimeValue
                        )
                    )
                }
            }
            adapter = MainAdapter(activity!!, switched, this)
            recyclerView_programs.adapter = MainAdapter(activity!!, switched, this)
        } else {
            Pazartesi.isEnabled = true
        }
        if (typeID == 2) {
            for (i in weeklyList) {
                if (i.typeId == typeID) {
                    switched.add(
                        Program(
                            i.typeId,
                            i.temp,
                            i.id,
                            i.programTimeValue
                        )
                    )
                }
            }
            recyclerView_programs.adapter = MainAdapter(activity!!, switched, this)
        } else {
            Sali.isEnabled = true
        }
        if (typeID == 3) {
            for (i in weeklyList) {
                if (i.typeId == typeID) {
                    switched.add(
                        Program(
                            i.typeId,
                            i.temp,
                            i.id,
                            i.programTimeValue
                        )
                    )
                }
            }
            recyclerView_programs.adapter = MainAdapter(activity!!, switched, this)
        } else {
            Carsamba.isEnabled = true
        }
        if (typeID == 4) {
            for (i in weeklyList) {
                if (i.typeId == typeID) {
                    switched.add(
                        Program(
                            i.typeId,
                            i.temp,
                            i.id,
                            i.programTimeValue
                        )
                    )
                }
            }
            recyclerView_programs.adapter = MainAdapter(activity!!, switched, this)
        } else {
            Persembe.isEnabled = true
        }
        if (typeID == 5) {
            for (i in weeklyList) {
                if (i.typeId == typeID) {
                    switched.add(
                        Program(
                            i.typeId,
                            i.temp,
                            i.id,
                            i.programTimeValue
                        )
                    )
                }
            }
            recyclerView_programs.adapter = MainAdapter(activity!!, switched, this)
        } else {
            Cuma.isEnabled = true
        }
        if (typeID == 6) {
            for (i in weeklyList) {
                if (i.typeId == typeID) {
                    switched.add(
                        Program(
                            i.typeId,
                            i.temp,
                            i.id,
                            i.programTimeValue
                        )
                    )
                    recyclerView_programs.adapter = MainAdapter(activity!!, switched, this)
                }
            }
        } else {
            Cumartesi.isEnabled = true
        }
        if (typeID == 7) {
            for (i in weeklyList) {
                if (i.typeId == typeID) {
                    switched.add(
                        Program(
                            i.typeId,
                            i.temp,
                            i.id,
                            i.programTimeValue
                        )
                    )
                }
            }
            recyclerView_programs.adapter = MainAdapter(activity!!, switched, this)
        } else {
            Pazar.isEnabled = true
        }

    }

    @RequiresApi(Build.VERSION_CODES.N)
    @ExperimentalTime
    private fun programJson() {

        println("programJson() - Fetch json komutu deneniyor.")

        val url =
            host + "/Program/getProgram?thermoId=" + thermostat.thermoId

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
                        btn_addprog.isEnabled = true
                        btn_progsave.isEnabled = true
                    }
                    if (body != "[]") {
                        val gson = GsonBuilder().create()
                        val programListAllDays: List<Program> =
                            gson.fromJson(body, Array<Program>::class.java).toList()


                        val jsonArray = JSONArray(body)
                        weeklyList.clear()
                        for (jsonIndex in 0..(programListAllDays.count() - 1)) {
                            if (jsonArray.getJSONObject(jsonIndex).getInt("programTime") != 0) {
                                weeklyList.add(
                                    Program(
                                        jsonArray.getJSONObject(jsonIndex).getInt("timeTypeId"),
                                        jsonArray.getJSONObject(jsonIndex).getDouble("temp"),
                                        jsonArray.getJSONObject(jsonIndex).getInt("progId"),
                                        jsonArray.getJSONObject(jsonIndex).getLong("programTime")
                                    )
                                )
                            }
                        }
                    } else {
                        println("empty body!!!")
                    }
                }else{
                    unableToConnect()
                }
            }
        })
        Timer("SettingUp", false).schedule(1000) {
            activity!!.runOnUiThread {
                Pazartesi.performClick()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun deleteweekly() {
        val sharedPreferencesProgram =
            activity!!.getSharedPreferences("program", Activity.MODE_PRIVATE)
        val deletedProgramId = sharedPreferencesProgram.getInt("deleted program id", 0)
        weeklyList.removeIf { it.id == deletedProgramId }
        val editor = sharedPreferencesProgram.edit()
        editor.clear().apply()
    }


    @RequiresApi(Build.VERSION_CODES.N)
    override fun onEvent() {
        deleteweekly()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onOkClick() {
        switchDays(defaultDay)
    }

    fun setPrograms() {
        var mon = 0
        var tue = 0
        var wed = 0
        var thu = 0
        var fri = 0
        var sat = 0
        var sun = 0
        var valid = false
        for (i in weeklyList) {
            if (i.typeId == 1) {
                mon++
            }

            if (i.typeId == 2) {
                tue++
            }

            if (i.typeId == 3) {
                wed++
            }

            if (i.typeId == 4) {
                thu++
            }

            if (i.typeId == 5) {
                fri++
            }

            if (i.typeId == 6) {
                sat++
            }

            if (i.typeId == 7) {
                sun++
            }
        }

        if (mon > 0 && tue > 0 && wed > 0 && thu > 0 && fri > 0 && sat > 0 && sun > 0) {
            valid = true
        }


        if (valid) {
            val program = Gson().toJsonTree(weeklyList) as JsonArray

            println("setPrograms() - Fetch json komutu deneniyor.")

            val url =
                host + "/Program/insert?thermoId=" + thermostat.thermoId + "&programs=" + program

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
                        btn_progsave.isEnabled = true
                        btn_addprog.isEnabled = true
                    }else{
                        unableToConnect()
                    }
                }
            })
            Toast.makeText(activity!!, "HAFTALIK PROGRAMINIZ AYARLANDI", Toast.LENGTH_LONG)
                .show()
        } else {
            Toast.makeText(activity!!, "LÜTFEN HER GÜNE BİR PROGRAM AYARLAYINIZ", Toast.LENGTH_LONG)
                .show()
        }

    }

    private fun unableToConnect() {
        activity!!.runOnUiThread {
            btn_addprog.isEnabled = false
            btn_progsave.isEnabled = false
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


class Program(val typeId: Int, var temp: Double, val id: Int, var programTimeValue: Long)
