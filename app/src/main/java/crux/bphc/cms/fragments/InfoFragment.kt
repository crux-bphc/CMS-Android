package crux.bphc.cms.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import crux.bphc.cms.R
import crux.bphc.cms.app.Urls
import crux.bphc.cms.widgets.HtmlTextView

class InfoFragment : Fragment() {

    override fun onStart() {
        super.onStart()
        requireActivity().title = "About Us"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageView>(R.id.image).setOnClickListener { view: View? ->
            val viewIntent = Intent("android.intent.action.VIEW", Urls.WEBSITE_URL)
            startActivity(viewIntent)
        }
        view.findViewById<View>(R.id.crux).setOnClickListener { view: View? ->
            val viewIntent = Intent("android.intent.action.VIEW", Urls.WEBSITE_URL)
            startActivity(viewIntent)
        }
        view.findViewById<HtmlTextView>(R.id.description).text =
            HtmlTextView.parseHtml(requireContext().getString(R.string.app_info))
    }
}