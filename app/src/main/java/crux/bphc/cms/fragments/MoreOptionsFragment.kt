package crux.bphc.cms.fragments

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import crux.bphc.cms.R
import crux.bphc.cms.databinding.FragmentMoreOptionsBinding
import crux.bphc.cms.databinding.RowMoreOptionsBinding
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * This fragment adds support for a BottomSheet to show more options and should
 * be preferred over a normal dialog box.
 *
 * @author Abhijeet Viswa
 */
class MoreOptionsFragment : BottomSheetDialogFragment() {
    private lateinit var viewModel: OptionsViewModel
    private lateinit var header: String
    private lateinit var options: ArrayList<Option>
    private lateinit var binding: FragmentMoreOptionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentMoreOptionsBinding.inflate(layoutInflater)

        val args = arguments
        if (args != null) {
            header = args.getString("header") ?: ""
            options = args.getParcelableArrayList("options") ?: arrayListOf()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // obtain the view model
        viewModel = ViewModelProvider(requireActivity()).get(OptionsViewModel::class.java)
        if (header.compareTo("") != 0) {
            (view.findViewById<View>(R.id.more_options_header) as TextView).text = header
        }

        // create the list
        val listView = view.findViewById<ListView>(R.id.more_options_list)
        val arrayAdapter: ArrayAdapter<String> = object : ArrayAdapter<String>(requireActivity(),
            R.layout.row_more_options
        ){
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val vh: OptionViewHolder
                val outView = if (convertView == null) {
                    val inflater = requireActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    val rowBinding = RowMoreOptionsBinding.inflate(inflater)
                    val v = rowBinding.root
                    vh = OptionViewHolder(
                        rowBinding.moreOptionsText,
                        rowBinding.moreOptionsIcon
                    )
                    v.tag = vh
                    v
                } else {
                    vh = convertView.tag as OptionViewHolder
                    convertView
                }

                // Bind row
                val option = options[position]
                vh.text.text = option.optionText
                if (option.drawableIcon != 0) {
                    vh.icon.setImageResource(option.drawableIcon)
                    vh.icon.visibility = View.VISIBLE
                }
                return outView
            }
        }

        for (option in options) {
            arrayAdapter.add(option.optionText)
        }
        listView.adapter = arrayAdapter
        listView.onItemClickListener = OnItemClickListener { _: AdapterView<*>?, _: View?,
                                                             position: Int, _: Long ->
            viewModel.setSelection(options[position])
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Remove all observers when being dismissed so that they
        // aren't invoked the next time the Fragment is created
        viewModel.selection.removeObservers(requireActivity())
        viewModel.clearSelection()
    }

    /**
     * Wrapper class specifying an option.
     * @property id an integer to identify this option uniquely
     * @property optionText the text to be shown to the user
     * @property drawableIcon drawable resource id to be used as icon for the option
     */
    @Parcelize
    class Option(
        val id: Int,
        val optionText: String?,
        val drawableIcon: Int,
    ) : Parcelable

    /**
     * `ViewModel` to observe selection events.
     */
    class OptionsViewModel : ViewModel() {
        private var _selection  = MutableLiveData<Option?>()
        val selection: LiveData<Option?>
            get() = _selection

        fun setSelection(option: Option?) {
            _selection.value = option
        }

        /**
         * Clear the selection after observing and handling it. Ensure that
         * you deregister yourself before calling this method so that clearing
         * the selection does not cause a callback.
         */
        fun clearSelection() {
            _selection.value = null
        }
    }

    internal data class OptionViewHolder(
        var text: TextView,
        var icon: ImageView
    )

    companion object {
        /**
         * Create a new instance of the @code `MoreOptionsFragment`
         * @param header The header to be set to the dialog. Set to `null`
         * or an empty string if no header is required.
         * @param options The list of options of that should be shown to the user.
         * @return An instance of `MoreOptionsFragment`
         */
        @JvmStatic
        fun newInstance(header: String, options: ArrayList<Option>): MoreOptionsFragment {
            val args = Bundle()
            args.putString("header", header)
            args.putParcelableArrayList("options", options)
            val fragment = MoreOptionsFragment()
            fragment.arguments = args
            return fragment
        }
    }
}