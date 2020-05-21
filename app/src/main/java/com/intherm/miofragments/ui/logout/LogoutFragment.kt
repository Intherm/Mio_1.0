package com.intherm.miofragments.ui.logout


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.intherm.miofragments.LoginActivity
import com.intherm.miofragments.MainActivity

import com.intherm.miofragments.R
import com.intherm.miofragments.ui.home.HomeFragment
import kotlinx.android.synthetic.main.fragment_logout.*


class LogoutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_logout, container, false)
    }

    override fun onStart() {
        super.onStart()
        id_logout.setOnClickListener {
            logOut()
            val intent = Intent(activity, LoginActivity::class.java)
            activity!!.startActivity(intent)
            activity!!.finish()
        }
        id_return.setOnClickListener {
            val intent = Intent(activity, MainActivity::class.java)
            activity!!.startActivity(intent)
            activity!!.finish()
        }
    }
    fun logOut() {
        val sharedPrefUser = activity!!.getSharedPreferences("user info", Context.MODE_PRIVATE)
        sharedPrefUser.edit().clear().apply()
        val sharedPreferencesProgram = activity!!.getSharedPreferences("program", Context.MODE_PRIVATE)
        sharedPreferencesProgram.edit().clear().apply()
        val sharedPrefProg = activity!!.getSharedPreferences("prog", Context.MODE_PRIVATE)
        sharedPrefProg.edit().clear().apply()
    }
}
