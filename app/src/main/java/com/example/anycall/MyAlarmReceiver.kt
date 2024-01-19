package com.example.anycall

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import java.io.IOException

class MyAlarmReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder: NotificationCompat.Builder


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 26 버전 이상
            val channelId = "one-channel"
            val channelName = "My Channel One"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                // 채널에 다양한 정보 설정
                description = "My Channel One Description"
                setShowBadge(true)
                val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(uri, audioAttributes)
                enableVibration(true)
            }
            // 채널을 NotificationManager에 등록
            manager.createNotificationChannel(channel)

            // 채널을 이용하여 builder 생성
            builder = NotificationCompat.Builder(context, channelId)
        } else {
            // 26 버전 이하
            builder = NotificationCompat.Builder(context)
        }
        val myItem = intent.getParcelableExtra<MyItem>(Key.MY_ITEM)
        // 알림의 기본 정보
        builder.run {
            setSmallIcon(R.drawable.call)
            setWhen(System.currentTimeMillis())
            setContentTitle("지금 ${myItem?.name}님에게 전화를 걸 시간이에요")
            myItem?.icon?.let{
                setLargeIcon(getBitmapFromUri(context, it))
            }
        }

        manager.notify(11, builder.build())
    }

    private fun getBitmapFromUri(context: Context, uri: Uri): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            e.printStackTrace()
            BitmapFactory.decodeResource(context.resources, R.drawable.user)
        }
    }

}