package com.zktest

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.zkteco.temperature.device.Device
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val TAG: String = MainActivity::class.java.simpleName
    private var mHandler: Handler? = null
    private var usbService: UsbService? = null
    val device = Device(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeUSBService()
        device.openDevice()
    }
    private fun initializeUSBService() {
        Log.d(TAG, "initializeUSBService")
        //Registering USB Service
        mHandler = MyHandler()
        registerUSBService()
        startUSBService(UsbService::class.java, usbConnection)
    }

    private fun registerUSBService() {
        Log.d(TAG, "registerUSBService")
        val filter = IntentFilter()
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED)
        filter.addAction(UsbService.ACTION_NO_USB)
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED)
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED)
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED)
        registerReceiver(mUsbReceiver, filter)
    }

    private val mUsbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "onReceive::$intent.action")
            //displays toast/error message for the status of USB connection
            when (intent.action) {
                UsbService.ACTION_USB_PERMISSION_GRANTED ->
                    showToast(getString(R.string.usb_ready))
                UsbService.ACTION_USB_PERMISSION_NOT_GRANTED ->
                    showToast(getString(R.string.usb_permission_not_granted))
                UsbService.ACTION_NO_USB ->
                    showToast(getString(R.string.no_usb_connected))
                UsbService.ACTION_USB_DISCONNECTED ->
                    showToast(getString(R.string.usb_disconnected))
                UsbService.ACTION_USB_NOT_SUPPORTED ->
                    showToast(getString(R.string.usb_device_not_supported))
                else -> showToast(intent.action.toString())
            }
        }
    }
    private val usbConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            Log.d(TAG, "onServiceConnected")
            usbService = (arg1 as UsbService.UsbBinder).service
            usbService?.setHandler(mHandler)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.d(TAG, "onServiceDisconnected")
            usbService = null
        }
    }

    private fun startUSBService(
        service: Class<*>,
        serviceConnection: ServiceConnection
    ) {
        Log.d(TAG, "startUSBService")
        if (!UsbService.SERVICE_CONNECTED) {//If USB service is not connected then starting the service
            val startService = Intent(applicationContext, service)
            applicationContext?.startService(startService)
        }
        val bindingIntent = Intent(applicationContext, service)
        applicationContext?.bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun showToast(status: String) {
        Toast.makeText(
            this,
            status,
            Toast.LENGTH_SHORT
        ).show()
    }

    inner class MyHandler : Handler() {

        override fun handleMessage(msg: Message) {
            Log.d(TAG, "handleMessage::$msg")
            when (msg.what) {
                UsbService.MESSAGE_FROM_SERIAL_PORT -> {
                    val data = msg.obj as String
                    //Handling the received data here
                    if(data.contains("GetT\n"))
                    {
                        findViewById<TextView>(R.id.temp).text = device.maxTemperature.toString()
                        val temp = device.maxTemperature
                        sendData(temp)
                    }

                }
            }
        }
    }
    private fun sendData(temp:ByteArray) {
        Log.d(TAG, "startScan")
        if (usbService != null) { // if UsbService was correctly binded, Send data
            usbService?.write(temp)
        }
    }
}
