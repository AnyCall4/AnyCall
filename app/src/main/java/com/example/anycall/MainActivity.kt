package com.example.anycall

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.example.anycall.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val tabTextList = listOf(R.string.main_list_first, R.string.main_list_second)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initCallPermission()
        initViewPager()
        loadUserData()
    }

    private fun initViewPager() {
        binding.viewpager2.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewpager2) { tab, pos ->
            tab.text = getString(tabTextList[pos])
        }.attach()
    }

    private fun initCallPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // 권한이 허용된 경우
            initReadContactsPermission()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                MyAdapter.MY_PERMISSIONS_REQUEST_CALL_PHONE
            )
            initReadContactsPermission()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            MyAdapter.MY_PERMISSIONS_REQUEST_CALL_PHONE -> {
                // CALL_PHONE 권한에 대한 응답 확인
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // CALL_PHONE 권한이 승인된 경우, READ_CONTACTS 권한 확인
                    initReadContactsPermission()
                } else {
                    // CALL_PHONE 권한이 거부된 경우, Toast 메시지 표시 및 프래그먼트 실행
                    Toast.makeText(
                        this,
                        R.string.main_call_request,
                        Toast.LENGTH_SHORT
                    ).show()
                    initViewPager()
                }
            }

            101 -> {
                // READ_CONTACTS 권한에 대한 응답 확인
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // READ_CONTACTS 권한이 승인된 경우, initViewPager 실행
                    initViewPager()
                } else {
                    // READ_CONTACTS 권한이 거부된 경우, Toast 메시지 표시 및 initViewPager 실행
                    Toast.makeText(
                        this,
                        R.string.main_read_request,
                        Toast.LENGTH_SHORT
                    ).show()
                    initViewPager()
                }
            }

            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun initReadContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // READ_CONTACTS 권한이 이미 허용된 경우
            initViewPager()
        } else {
            // READ_CONTACTS 권한이 없는 경우, 권한 요청
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                101  // 다른 값으로 설정
            )
        }
    }

    private fun loadUserData() {
        val pref = getSharedPreferences("pref", 0)
        val message = pref.getString("message", "") ?: ""
        User.updateUserMessage(message)
    }

    private fun saveUserMessage(message: String) {
        val pref = getSharedPreferences("pref", 0)
        val edit = pref.edit()
        edit.putString("message", message).apply()
    }
    override fun onStop() {
        saveUserMessage(User.getUser().myMessage)
        super.onStop()
    }
}
