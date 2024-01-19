package com.example.anycall

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.example.anycall.databinding.FragmentContactDetailBinding

class ContactDetailFragment : Fragment() {
    interface OnFavoriteChangedListener{
        fun onFavoriteChanged(item:MyItem)
    }
    private var receiveData: MyItem? = null
    var listener: OnFavoriteChangedListener? = null

    private val binding by lazy { FragmentContactDetailBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        receiveData = arguments?.getParcelable<MyItem>(Key.EXTRA_USER)
        binding.userImage.setImageURI(receiveData?.icon)
        binding.userName.text = receiveData?.name
        val formattedPhoneNum = StringBuilder(receiveData?.phoneNum)
            .insert(3, "-")
            .insert(8, "-")
            .toString()
        binding.userPhone.text = formattedPhoneNum
        binding.userMessage.text = receiveData?.myMessage
        binding.userEmail.text = receiveData?.email
        receiveData?.let {
            if (it.favorite) {
                binding.ivDetailLike.setImageResource(R.drawable.ic_star_fill)
            } else {
                binding.ivDetailLike.setImageResource(R.drawable.ic_star_blank)
            }
        }

        binding.btnMessage.setOnClickListener {
            val message = receiveData?.phoneNum
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("smsto:" + message)
            startActivity(intent)
        }

        binding.btnCall.setOnClickListener {
            val dial = receiveData?.phoneNum
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:" + dial)
            startActivity(intent)
        }
        with(binding){
            eventFive.setOnClickListener {
                checkNotificationPermission()
                sendNotification(5000)
            }
            eventTen.setOnClickListener {
                checkNotificationPermission()
                sendNotification(10000)
            }
        }

        binding.ivDetailLike.setOnClickListener {
            receiveData?.let {
                if (MyItem.clickFavorite(it)) {
                    binding.ivDetailLike.setImageResource(R.drawable.ic_star_fill)
                } else {
                    binding.ivDetailLike.setImageResource(R.drawable.ic_star_blank)
                }
                listener?.onFavoriteChanged(it)
            }
        }
    }
    companion object {
        @JvmStatic
        fun newInstance(myItem: MyItem) =
            ContactDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(Key.EXTRA_USER, myItem)
                }
            }
    }
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
                }
                requireActivity().startActivity(intent)
            }
        }
    }
    private fun sendNotification(timeInMillis: Long) {
        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val notificationIntent = Intent(requireContext(), MyAlarmReceiver::class.java).apply {
            putExtra(Key.MY_ITEM,receiveData)
        }
        val pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val futureInMillis = SystemClock.elapsedRealtime() + timeInMillis
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent)
    }
}