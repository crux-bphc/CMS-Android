package crux.bphc.cms.widgets

import android.content.Context
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class HtmlTextView(context: Context, attr: AttributeSet) : AppCompatTextView(context, attr) {

    override fun setText(text: CharSequence, type: BufferType) {
        super.setText(text, type)
    }

    companion object {
        fun parseHtml(text: String?): Spanned? {
            if (text == null) return null
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(text)
            }
        }
    }
}
