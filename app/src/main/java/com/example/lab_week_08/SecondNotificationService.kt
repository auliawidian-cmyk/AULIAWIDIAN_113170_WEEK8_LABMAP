package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

//SecondNotificationService is the second foreground service
//that will be executed after the ThirdWorker is finished
class SecondNotificationService : Service() {

    //The notificationBuilder will build and update the notification
    private lateinit var notificationBuilder: NotificationCompat.Builder

    //The serviceHandler will handle the background thread where the countdown runs
    private lateinit var serviceHandler: Handler

    //We don't need to bind this service to any component
    //so we can safely return null
    override fun onBind(intent: Intent?): IBinder? = null

    //The onCreate() callback will run once when the service is first created
    override fun onCreate() {
        super.onCreate()

        //Create and initialize the foreground notification
        notificationBuilder = startForegroundService()

        //Create a new background thread to handle countdown logic
        val handlerThread = HandlerThread("SecondNotificationThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    //This function will create and start the notification in the foreground
    private fun startForegroundService(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()
        val notificationBuilder = getNotificationBuilder(pendingIntent, channelId)

        //Start the notification as a foreground service
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    //This pending intent will redirect the user back to MainActivity
    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flag)
    }

    //Create notification channel for Android 8.0+ devices
    private fun createNotificationChannel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "002"
            val channelName = "002 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, channelPriority)
            val service = requireNotNull(
                ContextCompat.getSystemService(this, NotificationManager::class.java)
            )
            service.createNotificationChannel(channel)
            channelId
        } else {
            ""
        }

    //Build the notification with title, content, and icon
    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Third worker process is done")
            .setContentText("This is the second notification service running!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Second notification started")
            .setOngoing(true)

    //This callback runs each time the service is started
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        //Execute countdown on the background thread
        serviceHandler.post {
            countDownFromTenToZero(notificationBuilder)
            notifyCompletion(Id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return returnValue
    }

    //Simulate countdown from 10 to 0 seconds while updating notification text
    private fun countDownFromTenToZero(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            notificationBuilder.setContentText("$i seconds remaining until completion...")
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    //Notify MainActivity that the service has finished
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }

    companion object {
        //Notification constants
        const val NOTIFICATION_ID = 0xCA8
        const val EXTRA_ID = "Id"

        //LiveData used to track when this service is completed
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}