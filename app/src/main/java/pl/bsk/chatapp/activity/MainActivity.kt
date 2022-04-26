package pl.bsk.chatapp.activity

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import pub.devrel.easypermissions.EasyPermissions
import android.util.Log
import androidx.activity.viewModels
import androidx.navigation.fragment.findNavController
import pl.bsk.chatapp.R
import pl.bsk.chatapp.viewmodel.ClientServerViewModel
import pub.devrel.easypermissions.AfterPermissionGranted

const val RC_LOCATION = 2137

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<ClientServerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        askForPermissions()

        viewModel.listenServerConnection {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            runOnUiThread {
                navHostFragment?.findNavController()?.navigate(R.id.chatFragment)
            }
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
}