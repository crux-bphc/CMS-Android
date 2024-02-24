package crux.bphc.cms.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import crux.bphc.cms.R
import crux.bphc.cms.app.Urls
import crux.bphc.cms.databinding.FragmentInfoBinding
import crux.bphc.cms.widgets.HtmlTextView

class InfoFragment : Fragment() {

    private lateinit var binding: FragmentInfoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentInfoBinding.inflate(layoutInflater)
    }

    override fun onStart() {
        super.onStart()
        requireActivity().title = "About Us"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.image.setOnClickListener { view: View? ->
            val viewIntent = Intent("android.intent.action.VIEW", Urls.WEBSITE_URL)
            startActivity(viewIntent)
        }
        binding.crux.setOnClickListener { view: View? ->
            val viewIntent = Intent("android.intent.action.VIEW", Urls.WEBSITE_URL)
            startActivity(viewIntent)
        }
        binding.description.text =
            HtmlTextView.parseHtml(requireContext().getString(R.string.app_info))
    }
}