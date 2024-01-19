package com.example.anycall

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.anycall.MyItem.Companion.dataList
import com.example.anycall.databinding.AddUserDialogBinding
import com.example.anycall.databinding.FragmentContactsBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ContactsFragment : Fragment(), ContactDetailFragment.OnFavoriteChangedListener {
    private var isImageSelected = false
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding: FragmentContactsBinding
    private val DEFAULT_GALLERY_REQUEST_CODE = 123
    private lateinit var selectedImageUri: Uri
    private lateinit var userImg: ImageView
    private lateinit var adapter: MyAdapter
    private var isDataLoaded = false
    private lateinit var searchView: SearchView
    private lateinit var originalDataList: MutableList<MyItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentContactsBinding.inflate(layoutInflater)
        setHasOptionsMenu(true)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    /**
     * 사용권한 여부 물어보기
     */
    private fun initPermission() {
        val status = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_CONTACTS
        )
        if (status != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_CONTACTS),
                100
            )
        } else {
            updateContactList()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // READ_CONTACTS 권한이 부여되면 연락처 업데이트를 진행
            updateContactList()
        }
    }

    /**
     * 전화번호부에서 데이터 가져오기
     */
    private fun updateContactList() {
        if (isDataLoaded) return

        isDataLoaded = true
        val cursor = requireActivity().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val nameColumnIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val phoneNumberColumnIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val contactIdColumnIndex =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID)
                if (nameColumnIndex >= 0 && phoneNumberColumnIndex >= 0 && contactIdColumnIndex >= 0) {
                    val name = cursor.getString(nameColumnIndex)
                    val phoneNumber = cursor.getString(phoneNumberColumnIndex)
                    val rawContactId = cursor.getLong(contactIdColumnIndex)

                    // 여기서 필요한 데이터를 가져와서 MyItem 객체를 생성하여 dataList에 추가
                    val newItem = MyItem(
                        icon = getContactPhotoUri(rawContactId) ?: null,
                        name = name ?: "",
                        email = getEmail(rawContactId) ?: "",
                        myMessage = "",
                        phoneNum = phoneNumber ?: ""
                    )

                    dataList.add(newItem)
                }
            }
            cursor.close()
        }

        if (!::adapter.isInitialized) {
            adapter = MyAdapter(dataList)
            binding.recyclerView.adapter = adapter
        }

        adapter.notifyDataSetChanged()
    }

    private fun getEmail(contactId: Long): String? {
        val emailCursor = requireActivity().contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
            arrayOf(contactId.toString()),
            null
        )

        var email: String? = null
        emailCursor?.use {
            if (it.moveToFirst()) {
                val emailColumnIndex =
                    it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                email = it.getString(emailColumnIndex)
            }
        }

        emailCursor?.close()
        return email
    }

    private fun getContactPhotoUri(rawContactId: Long): Uri? {
        val photoUri = ContentUris.withAppendedId(
            ContactsContract.Contacts.CONTENT_URI,
            rawContactId
        )
        val inputStream: InputStream? =
            ContactsContract.Contacts.openContactPhotoInputStream(
                requireActivity().contentResolver,
                photoUri
            )

        val result: Uri? = if (inputStream != null) {
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            saveImageToInternalStorage(bitmap, rawContactId.toString()) // 저장된 파일의 Uri를 반환
        } else {
            // 연락처에 사진이 없을 경우
            Uri.parse("android.resource://com.example.anycall/drawable/user")
        }
        return result
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap, fileName: String): Uri? {
        // 내부 저장소에 이미지를 저장하고 해당 파일의 Uri를 반환합니다.
        val wrapper = ContextWrapper(requireContext())
        var file = wrapper.getDir("images", Context.MODE_PRIVATE)
        file = File(file, "$fileName.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
            return Uri.parse(file.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        // 이미지 저장에 실패한 경우 null을 반환
        return null
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_contacts, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_list_view -> {
                // 메뉴 아이템이 클릭되었을 때의 동작을 여기에 작성합니다.
                binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
                adapter.setGridView(false)
                binding.recyclerView.adapter = adapter
                true
            }

            R.id.action_grid_view -> {
                // 메뉴 아이템이 클릭되었을 때의 동작을 여기에 작성합니다.
                binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
                adapter.setGridView(true)
                binding.recyclerView.adapter = adapter
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactsBinding.inflate(inflater, container, false)
        initPermission()

        originalDataList = ArrayList(dataList)
        searchView = binding.searchViewPhoneBook
        searchView.setOnQueryTextListener(searchViewTextListener)

        return binding.root
    }

    var searchViewTextListener: SearchView.OnQueryTextListener =
        object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // 검색 버튼을 눌렀을 때의 동작
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // 텍스트가 변경될 때마다 호출되는 메서드
                filterData(newText)
                return true
            }
        }

    private fun filterData(query: String) {
        // 검색어에 따라 데이터를 필터링하여 새로운 리스트 생성
        val filteredList = originalDataList.filter { item ->
            item.name.contains(query, ignoreCase = true)
        }.toMutableList()

        // 어댑터에 필터링된 데이터 설정
        adapter.setData(filteredList)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbar)
        adapter = MyAdapter(MyItem.dataList)
        with(binding) {
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            //플로팅버튼 스크롤 안할시 사라지게
            val scrollHandler = Handler()
            val delayMillis = 2000L // 2초
            var isScrolling = false
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    isScrolling = newState != RecyclerView.SCROLL_STATE_IDLE
                    if (!isScrolling) {
                        scrollHandler.postDelayed({
                            if (!isScrolling) {
                                floatingBtn.hide()
                            }
                        }, delayMillis)
                    } else {
                        floatingBtn.show()
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (isScrolling) {
                        scrollHandler.removeCallbacksAndMessages(null)
                    }
                }
            })

            floatingBtn.setOnClickListener {
                val dialogView = AddUserDialogBinding.inflate(layoutInflater)
                val alertDialog = AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                    .setView(dialogView.root)
                    .setCancelable(false)
                    .create()
                dialogView.addUserImg.setOnClickListener {
                    val intent = Intent()
                    intent.type = "image/*"
                    intent.action = Intent.ACTION_GET_CONTENT
                    startActivityForResult(intent, DEFAULT_GALLERY_REQUEST_CODE)
                }
                userImg = dialogView.addUserImg
                with(dialogView) {
                    addDialogCancel.setOnClickListener {
                        alertDialog.dismiss()
                    }
                }
                val listener = DialogInterface.OnClickListener { p0, p1 ->
                    val name = dialogView.addUserName.text.toString().trim()
                    val phone = dialogView.addUserPhone.text.toString().trim()
                    val state = dialogView.addUserStatus.text.toString().trim()
                    val email = dialogView.addUserEmail.text.toString().trim()
                    val newItem: MyItem
                    if (name.isBlank() || phone.isBlank()) {
                        Toast.makeText(requireContext(), R.string.contacts_valid_empty, Toast.LENGTH_SHORT).show()
                        return@OnClickListener
                    }
                    if(phone.length != 11 || phone.contains("-")){
                        Toast.makeText(requireContext(), R.string.contacts_valid_length, Toast.LENGTH_SHORT).show()
                        return@OnClickListener
                    }
                    if (isImageSelected) {
                        newItem = MyItem(
                            selectedImageUri,
                            name,
                            email,
                            state,
                            phone
                        )
                        isImageSelected = !isImageSelected
                    } else {
                        newItem = MyItem(
                            Uri.parse("android.resource://com.example.anycall/drawable/user"),
                            name,
                            email,
                            state,
                            phone
                        )
                    }
                    dataList.add(newItem)
                    originalDataList = ArrayList(dataList)
                    adapter.notifyDataSetChanged()
                    alertDialog.dismiss()
                }
                dialogView.addDialogOkbutton.apply {
                    setOnClickListener {
                        listener.onClick(null, DialogInterface.BUTTON_POSITIVE)
                    }
                }
                alertDialog.show()
            }
        }

        adapter.itemClick = object : MyAdapter.ItemClick {
            override fun onClick(item: MyItem, position: Int) {
                val detailFragment = ContactDetailFragment.newInstance(item)
                detailFragment.listener = this@ContactsFragment
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    replace(R.id.frame, detailFragment)
                    setReorderingAllowed(true)
                    addToBackStack("")
                }.commit()
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            DEFAULT_GALLERY_REQUEST_CODE -> {
                data ?: return
                selectedImageUri = data.data as Uri
                Glide.with(this).load(selectedImageUri).into(userImg)
                isImageSelected = !isImageSelected
            }

            else -> {
                userImg.setImageResource(R.drawable.user)
            }
        }
    }


    override fun onFavoriteChanged(item: MyItem) {
        adapter.notifyDataSetChanged()
    }
}