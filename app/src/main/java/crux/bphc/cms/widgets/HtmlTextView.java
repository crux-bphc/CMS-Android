package crux.bphc.cms.widgets;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;

public class HtmlTextView extends AppCompatTextView {

    public HtmlTextView(Context context) {
        super(context);
    }

    public HtmlTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HtmlTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
    }

    public static Spanned parseHtml(String text) {
        if (text == null) return null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT);
        } else {
            //noinspection deprecation
            return Html.fromHtml(text);
        }
    }
}
