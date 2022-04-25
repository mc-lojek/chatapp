package pl.bsk.chatapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import pl.bsk.chatapp.R
import pl.bsk.chatapp.viewmodel.ClientServerViewModel
import timber.log.Timber

class InitialFragment : Fragment() {

    private val viewModel by activityViewModels<ClientServerViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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
    }

    private fun setupOnClicks() {
        requireActivity().findViewById<Button>(R.id.connect_btn).setOnClickListener {
            findNavController().navigate(R.id.action_initialFragment_to_chatFragment)
        }
    }
}