package pl.bsk.chatapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import pl.bsk.chatapp.R
import pl.bsk.chatapp.doStuff
import pl.bsk.chatapp.viewmodel.ClientServerViewModel
import timber.log.Timber

class InitialFragment : Fragment() {

    private val viewModel by activityViewModels<ClientServerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_initial, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupOnClicks()

        //doStuff()

    }

    private fun setupOnClicks() {
        requireActivity().findViewById<Button>(R.id.connect_btn).setOnClickListener {
            val ip = requireActivity().findViewById<EditText>(R.id.ip_addr_et).text.toString()
            viewModel.serverAddress = ip
            viewModel.connectToServer(ip) {
                requireActivity().runOnUiThread {
                    if (it == "1") {
                        findNavController().navigate(R.id.action_initialFragment_to_chatFragment)

                    } else {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }

        }
    }
}