package com.example.myopencvapp

import android.content.Context
import android.os.*
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myopencvapp.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        loadFragment(FaceMatchFragment())

    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {

        if (currentFocus != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

//    private fun initialize() {
//        bottomNav.setOnItemSelectedListener {
//            when (it.itemId) {
//                (R.id.home_icon) -> {
//                    loadFragment(HomeFragment())
//                    true
//                }
//
//                (R.id.face_icon) -> {
//                    loadFragment(FaceMatchFragment())
//                    true
//                }
//                else -> {
//                    loadFragment(HomeFragment())
//                    true
//                }
//            }
//        }
//    }


    private fun loadFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.commit()
    }


}
