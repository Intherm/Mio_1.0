package com.intherm.miofragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TimePicker
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.intherm.miofragments.ui.devices.DeviceListItems
import com.intherm.miofragments.ui.notifications.Notifications
import com.intherm.miofragments.ui.program.Program
import com.intherm.miofragments.ui.users.UserListItems
import kotlinx.android.synthetic.main.devices_row.view.*
import kotlinx.android.synthetic.main.dialog_programs.*
import kotlinx.android.synthetic.main.notifications_row.view.*
import kotlinx.android.synthetic.main.program_row.view.*
import kotlinx.android.synthetic.main.user_row.view.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime


class MainAdapter(ctx: Context, val homeFeed: ArrayList<Program>, val listener: EventListener) :
    RecyclerView.Adapter<CustomViewHolder>() {

    private val inflater: LayoutInflater
    private val Context = ctx
    var updatedProgramTime: Long = 0
    var programid: Int = 0

    interface EventListener {
        fun onEvent()
        fun onOkClick()
    }

    init {
        inflater = LayoutInflater.from(ctx)
    }

    //number of items
    override fun getItemCount(): Int {
        return homeFeed.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cellForRow = layoutInflater.inflate(R.layout.program_row, parent, false)
        return CustomViewHolder(cellForRow)
    }

    @ExperimentalTime
    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val homeFeedItem = homeFeed.get(position)
        holder.view.id_tempreq.text = homeFeedItem.temp.toString()
        holder.view.id_time2.text = String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(homeFeedItem.programTimeValue),
            TimeUnit.MILLISECONDS.toMinutes(homeFeedItem.programTimeValue) -
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(homeFeedItem.programTimeValue))
        )
        holder.view.programID.text = homeFeedItem.id.toString()

        val myDialog = Dialog(this.Context)
        myDialog.setContentView(R.layout.dialog_programs)
        myDialog.id_temp_pop.text = homeFeedItem.temp.toString()
        myDialog.id_time_pop.text = String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toHours(homeFeedItem.programTimeValue),
            TimeUnit.MILLISECONDS.toMinutes(homeFeedItem.programTimeValue) -
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(homeFeedItem.programTimeValue))
        )

        myDialog.sb_temp.max = 300
        myDialog.sb_temp.progress = ((homeFeedItem.temp) * 10).toInt()
        myDialog.id_temp_pop.text = homeFeedItem.temp.toString()
        myDialog.sb_temp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                myDialog.id_temp_pop.text = (((progress.toDouble()) / 10) + 5).toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                myDialog.id_temp_pop.text = (((seekBar!!.progress.toDouble()) / 10) + 5).toString()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                myDialog.id_temp_pop.text = (((seekBar!!.progress.toDouble()) / 10) + 5).toString()
            }

        })

        myDialog.id_time_pop.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener =
                TimePickerDialog.OnTimeSetListener { timePicker: TimePicker, hour, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    updatedProgramTime =
                        getMilliFromDate(SimpleDateFormat("HH:mm").format(cal.time).toString())
                    myDialog.id_time_pop.text = SimpleDateFormat("HH:mm").format(cal.time)
                }
            TimePickerDialog(
                this.Context, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(
                    Calendar.MINUTE
                ), true
            ).show()
        }

        myDialog.btn_picktime.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener =
                TimePickerDialog.OnTimeSetListener { timePicker: TimePicker, hour, minute ->
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                    updatedProgramTime =
                        getMilliFromDate(SimpleDateFormat("HH:mm").format(cal.time).toString())
                    myDialog.id_time_pop.text = SimpleDateFormat("HH:mm").format(cal.time)
                    if (updatedProgramTime == 0L) {
                        homeFeed[position].programTimeValue = 3600
                    }else{
                        homeFeed[position].programTimeValue = updatedProgramTime
                    }
                }
            TimePickerDialog(
                this.Context, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(
                    Calendar.MINUTE
                ), true
            ).show()
        }


        holder.itemView.setOnClickListener {
            myDialog.show()
        }
        myDialog.btn_ok.setOnClickListener {
            holder.view.id_tempreq.text = myDialog.id_temp_pop.text
            holder.view.id_time2.text = myDialog.id_time_pop.text
            homeFeed[position].temp = myDialog.id_temp_pop.text.toString().toDouble()

            listener.onOkClick()
            myDialog.hide()
        }
        holder.view.btn_deleteprog.setOnClickListener {
            programid = holder.view.programID.text.toString().toInt()
            delete(position)
        }
    }


    fun delete(position: Int) {
        homeFeed.removeAt(position)
        val sharedPrefProgram =
            this.Context.getSharedPreferences("program", android.content.Context.MODE_PRIVATE)
        val editor = sharedPrefProgram.edit()
        editor.putInt("deleted program id", programid).apply()
        notifyDataSetChanged()
        listener.onEvent()
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
}


class CustomViewHolder(val view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
    override fun onClick(v: View?) {
        Log.d("RecyclerView", "CLICK!")
    }
}

class UserAdaper(
    con: Context,
    val userFeed: ArrayList<UserListItems>,
    val listener: EventListener
) : RecyclerView.Adapter<UserViewHolder>() {

    private val Context = con

    interface EventListener {
        fun onDelete(deletedUserId: Int)
    }

    override fun getItemCount(): Int {
        return userFeed.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cellForRow = layoutInflater.inflate(R.layout.user_row, parent, false)
        return UserViewHolder(cellForRow)
    }

    @ExperimentalTime
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {

        val sharedPrefUser = this.Context.getSharedPreferences("user info", Activity.MODE_PRIVATE)
        val userId = sharedPrefUser.getInt("user id", 0)

        val userFeedItem = userFeed.get(position)
        holder.view.id_user_name.text = userFeedItem.name
        holder.view.tv_userid.text = userFeedItem.userId.toString()
        if (userId == holder.view.tv_userid.text.toString().toInt()) {
            holder.view.user_delete.visibility = View.INVISIBLE
        } else {
            holder.view.user_delete.visibility = View.VISIBLE
        }

        holder.view.user_delete.setOnClickListener {
            val builder = AlertDialog.Builder(Context)
            builder.setTitle("* " + holder.view.id_user_name.text.toString() + " *" + " isimli kullanıcıyı silmek istediğinize emin misiniz?")
            builder.setPositiveButton("Evet") { dialog: DialogInterface?, which: Int ->
                userFeed.removeAt(position)
                listener.onDelete(holder.view.tv_userid.text.toString().toInt())
            }
            builder.setNegativeButton("Hayır") { _: DialogInterface?, _: Int -> }
            builder.show()
        }

    }
}

class UserViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

}

class DeviceAdaper(
    con: Context,
    private val deviceFeed: ArrayList<DeviceListItems>,
    private val listener: EventListener
) :
    RecyclerView.Adapter<DeviceViewHolder>() {

    private val context = con
    var deviceId: Int = 0
    var deviceName: String = ""
    var selectedPosition = -1
    val sharedPrefDevice =
        this.context.getSharedPreferences("device", android.content.Context.MODE_PRIVATE)
    var pos = -1

    interface EventListener {
        fun onDelete()
    }

    //number of items
    override fun getItemCount(): Int {
        return deviceFeed.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cellForRow = layoutInflater.inflate(R.layout.devices_row, parent, false)
        return DeviceViewHolder(cellForRow)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {

        val deviceFeedItem = deviceFeed.get(position)

        holder.view.id_device_card.isClickable = false

        pos = sharedPrefDevice.getInt("selected device position", -1)

        if (pos > -1){
            holder.view.id_device_card.isChecked = pos == position
        }

        holder.view.id_device_name.text = deviceFeedItem.name
        holder.view.tv_device_id.text = deviceFeedItem.device_id.toString()

        holder.view.device_delete.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("* " + holder.view.id_device_name.text.toString() + " *" + " isimli cihazı silmek istediğinize emin misiniz?")
            builder.setPositiveButton("Evet") { dialog: DialogInterface?, which: Int ->
                deviceFeed.removeAt(position)
                val sharedPrefDevice =
                    this.context.getSharedPreferences("device", Activity.MODE_PRIVATE)
                sharedPrefDevice.edit()
                    .putInt("removed device", holder.view.tv_device_id.text.toString().toInt()).apply()
                listener.onDelete()
            }
            builder.setNegativeButton("Hayır") { _: DialogInterface?, _: Int -> }
            builder.show()
        }

        holder.itemView.setOnClickListener {
            deviceId = holder.view.tv_device_id.text.toString().toInt()
            deviceName = holder.view.id_device_name.text.toString()
            selectedPosition = position
            notifyDataSetChanged()
            select()
        }
    }

    private fun select() {
        val editor = sharedPrefDevice.edit()
        editor.putInt("selected device id", deviceId).apply()
        editor.putString("selected device name", deviceName).apply()
        editor.putInt("selected device position", selectedPosition).apply()
        notifyDataSetChanged()
        Toast.makeText(context,"Aftif cihaz: " + deviceName, Toast.LENGTH_LONG).show()
    }
}

class DeviceViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

}

class NotifyAdaper(
    con: Context,
    val notifyFeed: ArrayList<Notifications>,
    val listener: EventListener
) :
    RecyclerView.Adapter<NotifyViewHolder>() {

    private val context = con

    interface EventListener {
        fun onDeleteNotify(notifyId : Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cellForRow = layoutInflater.inflate(R.layout.notifications_row, parent, false)
        return NotifyViewHolder(cellForRow)
    }

    override fun getItemCount(): Int {
        return notifyFeed.count()
    }

    override fun onBindViewHolder(holder: NotifyViewHolder, position: Int) {

        val notifyFeedItem = notifyFeed.get(position)

        holder.view.tv_notifications.text = notifyFeedItem.text
        holder.view.tv_date.text = notifyFeedItem.date

        holder.view.btn_delnotify.setOnClickListener {
            notifyFeed.removeAt(position)
            notifyDataSetChanged()
            notifyItemRemoved(position)
            listener.onDeleteNotify(notifyFeedItem.id)
        }
    }
}
class NotifyViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

}
