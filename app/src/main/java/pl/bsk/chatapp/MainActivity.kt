package pl.bsk.chatapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import pub.devrel.easypermissions.EasyPermissions
import android.util.Log
import android.widget.Button
import pub.devrel.easypermissions.AfterPermissionGranted
import timber.log.Timber

const val RC_LOCATION = 2137

class MainActivity : AppCompatActivity(),WifiP2pManager.PeerListListener {
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

    }

    fun setupOnClicks(){
        findViewById<Button>(R.id.button).setOnClickListener{
            Timber.d("onClick")
            manager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {

                override fun onSuccess() {
                    Timber.d("success")
                }

                override fun onFailure(reasonCode: Int) {
                    Timber.d("failure")
                }
            })
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
                this,"Permissions not granted",
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
        Timber.d(list.toString())
    }
}