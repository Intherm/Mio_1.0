package com.intherm.miofragments

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat
import com.espressif.iot.esptouch.EsptouchTask
import com.espressif.iot.esptouch.IEsptouchListener
import com.espressif.iot.esptouch.IEsptouchResult
import com.espressif.iot.esptouch.IEsptouchTask
import com.espressif.iot.esptouch.util.ByteUtil
import com.espressif.iot.esptouch.util.TouchNetUtil
import kotlinx.android.synthetic.main.activity_device_wifi_connect.*
import java.lang.ref.WeakReference
import java.util.*
import android.text.method.PasswordTransformationMethod as PasswordTransformationMethod


class DeviceWifiConnectActivity : AppCompatActivity(), View.OnClickListener {
    private var mTask: EsptouchAsyncTask4? = null
    private var mReceiverRegistered = false
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(
            context: Context,
            intent: Intent
        ) {
            val action = intent.action ?: return
            val wifiManager = (context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager)
            when (action) {
                WifiManager.NETWORK_STATE_CHANGED_ACTION, LocationManager.PROVIDERS_CHANGED_ACTION -> onWifiChanged(
                    wifiManager.connectionInfo
                )
            }
        }
    }
    private var mDestroyed = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_wifi_connect)

        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = getString(R.string.back)
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)

        etPassword.transformationMethod = PasswordTransformationMethod.getInstance()

        etPassword.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN ->
                    if (etPassword.transformationMethod == PasswordTransformationMethod.getInstance()) {
                        etPassword.transformationMethod =
                            HideReturnsTransformationMethod.getInstance()
                    } else {
                        etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                    }
            }
            v.onTouchEvent(event)
        }

        btnSend.setOnClickListener(this)

        if (isSDKAtLeastP) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                val permissions =
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                requestPermissions(permissions, REQUEST_PERMISSION)
            } else {
                registerBroadcastReceiver()
            }
        } else {
            registerBroadcastReceiver()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!mDestroyed) {
                    registerBroadcastReceiver()
                }
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        mDestroyed = true
        if (mReceiverRegistered) {
            unregisterReceiver(mReceiver)
        }
    }

    private val isSDKAtLeastP: Boolean
        private get() = Build.VERSION.SDK_INT >= 28

    private fun registerBroadcastReceiver() {
        val filter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        if (isSDKAtLeastP) {
            filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        }
        registerReceiver(mReceiver, filter)
        mReceiverRegistered = true
    }

    private fun onWifiChanged(info: WifiInfo?) {
        //If requested permission isn't Granted yet
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Request permission from user
            val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE)
            ActivityCompat.requestPermissions(this, permissions, 0)
        } else {
            val disconnected = info == null || info.networkId == -1 || "<unknown ssid>" == info.ssid
            if (disconnected) {
                tvCurrentSSID.text = ""
                tvCurrentSSID.tag = null
                tvCurrentBSSID.text = ""
                tvWifiError.setText(R.string.no_wifi_connection)
                btnSend.isEnabled = false
                if (isSDKAtLeastP) {
                    checkLocation()
                }
                if (mTask != null) {
                    mTask?.cancelEsptouch()
                    mTask = null
                    AlertDialog.Builder(this)
                        .setMessage(R.string.configure_wifi_change_message)
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
            } else {
                var ssid = info?.ssid
                if (ssid!!.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid?.substring(1, ssid.length - 1)
                }
                tvCurrentSSID.text = ssid + " "
                tvCurrentSSID.tag = ByteUtil.getBytesByString(ssid)
                val ssidOriginalData = TouchNetUtil.getOriginalSsidBytes(info)
                tvCurrentSSID.tag = ssidOriginalData
                val bssid = info?.bssid
                tvCurrentBSSID.text = bssid
                btnSend.isEnabled = true
                tvWifiError.text = ""
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val frequency = info?.frequency
                }
            }
        }
    }

    private fun checkLocation() {
        val enable: Boolean
        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        enable = locationManager != null && LocationManagerCompat.isLocationEnabled(locationManager)
        if (!enable) {
            tvWifiError.setText(R.string.location_disable_message)
        }
    }

    override fun onClick(v: View) {
        if (v === btnSend) {
            val ssid =
                if (tvCurrentSSID.tag == null) ByteUtil.getBytesByString(tvCurrentSSID.text.toString()) else (tvCurrentSSID.tag as ByteArray)
            val password =
                ByteUtil.getBytesByString(etPassword.text.toString())
            val bssid =
                TouchNetUtil.parseBssid2bytes(tvCurrentBSSID.text.toString())
            if (mTask != null) {
                mTask?.cancelEsptouch()
            }
            mTask = EsptouchAsyncTask4(this)
            mTask?.execute(ssid, bssid, password)
        }
    }

    inner private class EsptouchAsyncTask4 internal constructor(activity: DeviceWifiConnectActivity) :
        AsyncTask<ByteArray, IEsptouchResult?, List<IEsptouchResult>?>() {
        private val mActivity: WeakReference<DeviceWifiConnectActivity>
        private val mLock = Any()
        private var mProgressDialog: ProgressDialog? = null
        private var mResultDialog: AlertDialog? = null
        private var mEsptouchTask: IEsptouchTask? = null
        fun cancelEsptouch() {
            cancel(true)
            if (mProgressDialog != null) {
                mProgressDialog?.dismiss()
            }
            if (mResultDialog != null) {
                mResultDialog?.dismiss()
            }
            if (mEsptouchTask != null) {
                mEsptouchTask?.interrupt()
            }
        }

        override fun onPreExecute() {
            val activity: Activity? = mActivity.get()
            mProgressDialog = ProgressDialog(activity)
            mProgressDialog?.setMessage(activity?.getString(R.string.configuring_message))
            mProgressDialog?.setCanceledOnTouchOutside(false)
            mProgressDialog?.setOnCancelListener { dialog: DialogInterface? ->
                synchronized(mLock) {
                    if (mEsptouchTask != null) {
                        mEsptouchTask?.interrupt()
                    }
                }
            }
            mProgressDialog?.setButton(
                DialogInterface.BUTTON_NEGATIVE, activity?.getText(android.R.string.cancel)
            ) { dialog: DialogInterface?, which: Int ->
                synchronized(mLock) {
                    if (mEsptouchTask != null) {
                        mEsptouchTask?.interrupt()
                    }
                }
            }
            mProgressDialog?.show()
        }

        protected override fun onProgressUpdate(vararg values: IEsptouchResult?) {
            val context: Context? = mActivity.get()
            if (context != null) {
                val result = values[0]
                Log.i(TAG, ": $result")
                val text = result?.bssid + " kablosuz ağa bağlandı."
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
        }

        protected override fun doInBackground(vararg params: ByteArray): List<IEsptouchResult>? {
            val activity = mActivity.get()
            var taskResultCount: Int
            synchronized(mLock) {
                val apSsid = params[0]
                val apBssid = params[1]
                val apPassword = params[2]
                val deviceCountData = "2"
                taskResultCount = deviceCountData.toInt()
                val context = activity?.applicationContext
                mEsptouchTask = EsptouchTask(apSsid, apBssid, apPassword, context)
                mEsptouchTask?.setEsptouchListener(IEsptouchListener { values: IEsptouchResult? ->
                    publishProgress(
                        values
                    )
                })
            }
            return mEsptouchTask?.executeForResults(taskResultCount)
        }

        override fun onPostExecute(result: List<IEsptouchResult>?) {
            val activity = mActivity.get()
            activity?.mTask = null
            mProgressDialog?.dismiss()
            if (result == null) {
                mResultDialog = AlertDialog.Builder(activity!!)
                    .setMessage(R.string.configure_result_failed_port)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                mResultDialog?.setCanceledOnTouchOutside(false)
                return
            }
            // check whether the task is cancelled and no results received
            val firstResult = result[0]
            if (firstResult.isCancelled) {
                return
            }
            // the task received some results including cancelled while
// executing before receiving enough results
            if (!firstResult.isSuc) {
                mResultDialog = AlertDialog.Builder(activity!!)
                    .setMessage(R.string.configure_result_failed)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                mResultDialog?.setCanceledOnTouchOutside(false)
                return
            }
            val resultMsgList =
                ArrayList<CharSequence>(result.size)
            for (touchResult in result) {
                val message = activity?.getString(
                    R.string.configure_result_success_item,
                    touchResult.bssid, touchResult.inetAddress.hostAddress
                )
                resultMsgList.add(message!!)
            }
            val items =
                arrayOfNulls<CharSequence>(resultMsgList.size)
            mResultDialog = AlertDialog.Builder(activity!!)
                .setTitle(R.string.configure_result_success)
                .setItems(resultMsgList.toArray(items), null)
                .setPositiveButton(R.string.okay) { dialog, which ->
                    val intent = Intent(this@DeviceWifiConnectActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                .show()
            mResultDialog?.setCanceledOnTouchOutside(false)

        }

        init {
            mActivity = WeakReference(activity)
        }
    }

    companion object {
        private val TAG = DeviceWifiConnectActivity::class.java.simpleName
        private const val REQUEST_PERMISSION = 0x01
        private const val MENU_ITEM_ABOUT = 0
    }

}
