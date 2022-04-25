package pl.bsk.chatapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import pub.devrel.easypermissions.EasyPermissions
import android.util.Log
import android.widget.Button
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pub.devrel.easypermissions.AfterPermissionGranted
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

const val RC_LOCATION = 2137

class MainActivity : AppCompatActivity(), WifiP2pManager.PeerListListener {
    val manager: WifiP2pManager? by lazy(LazyThreadSafetyMode.NONE) {
        getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager?
    }
    var channel: WifiP2pManager.Channel? = null
    var receiver: BroadcastReceiver? = null
    val intentFilter = IntentFilter().apply {
        addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        askForPermissions()
        channel = manager?.initialize(this, mainLooper, null)
        channel?.also { channel ->
            receiver = WiFiDirectBroadcastReceiver(manager!!, channel, this)
        }
        setupOnClicks()

        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                for (x in (0..100)) {
                    Thread.sleep(100)
                    Timber.d(x.toString())
                }
            }
        }
    }

    fun sendMessage(host:String,port:Int){
        val context = applicationContext
        var len: Int
        val socket = Socket()
        val buf = ByteArray(1024)
        try {
            /**
             * Create a client socket with the host,
             * port, and timeout information.
             */
            socket.bind(null)
            socket.connect((InetSocketAddress(host, port)), 500)

            /**
             * Create a byte stream from a JPEG file and pipe it to the output stream
             * of the socket. This data is retrieved by the server device.
             */
            val message = "elo co tam?"
            val outputStream = socket.getOutputStream()
            val inputStream = message.byteInputStream()
            while (inputStream.read(buf).also { len = it } != -1) {
                outputStream.write(buf, 0, len)
            }
            outputStream.close()
            inputStream.close()
        } catch (e: IOException) {
            //catch logic
            Timber.d("IOEXCEPTION")
        } finally {
            /**
             * Clean up any open sockets when done
             * transferring or if an exception occurred.
             */
            socket.takeIf { it.isConnected }?.apply {
                close()
            }
        }
    }

    suspend fun listenServerSocket() {
        withContext(Dispatchers.IO) {
            val serverSocket = ServerSocket(8888)
            return@withContext serverSocket.use {
                /**
                 * Wait for client connections. This call blocks until a
                 * connection is accepted from a client.
                 */
                val client = serverSocket.accept()

                /**
                 * If this code is reached, a client has connected and transferred data
                 * Save the input stream from the client as a JPEG file
                 */
                //val f = File(Environment.getExternalStorageDirectory().absolutePath +
                //        "/${context.packageName}/wifip2pshared-${System.currentTimeMillis()}.jpg")
                //val dirs = File(f.parent)
                val inputStream = client.getInputStream()
                Timber.d("listenServerSocket")

                //dirs.takeIf { it.doesNotExist() }?.apply {
                //    mkdirs()
                //}
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun setupOnClicks() {
        findViewById<Button>(R.id.button).setOnClickListener {
            Timber.d("onClick")
            manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    manager?.requestPeers(channel) {
                        Timber.d("siemano" + it)
                        if (it.deviceList.isNotEmpty()) {
                            connectToPeer(it.deviceList.random())
                        }
                    }
                }

                override fun onFailure(reasonCode: Int) {
                    Timber.d("failure")
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToPeer(device: WifiP2pDevice) {
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
        channel?.also { channel ->
            manager?.connect(channel, config, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    //success logic
                    Timber.d("polaczono")
                    sendMessage(config.deviceAddress,8888)
                }

                override fun onFailure(reason: Int) {
                    //failure logic
                    Timber.d("Nie polaczono")
                }
            }
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(RC_LOCATION)
    private fun askForPermissions() {
        val perms =
            arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            // Already have permission, do the thing
            Log.i("MainActivity", "Permissions are granted")
        } else {
            // Do not have permissions, request them now
            Log.i("MainActivity", "Permissions are not granted")
            EasyPermissions.requestPermissions(
                this, "Permissions not granted",
                RC_LOCATION, *perms
            )
        }
    }

    /* register the broadcast receiver with the intent values to be matched */
    override fun onResume() {
        super.onResume()
        receiver?.also { receiver ->
            registerReceiver(receiver, intentFilter)
        }
    }

    /* unregister the broadcast receiver */
    override fun onPause() {
        super.onPause()
        receiver?.also { receiver ->
            unregisterReceiver(receiver)
        }
    }

    override fun onPeersAvailable(list: WifiP2pDeviceList?) {
        Timber.d("gowno" + list.toString())
    }
}