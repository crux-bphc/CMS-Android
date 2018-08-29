package helper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.support.v7.widget.AppCompatTextView;


public class CustomTextView extends AppCompatTextView {

    public CustomTextView(Context context) {
        super(context);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            super.setText(Html.fromHtml(text.toString(),Html.FROM_HTML_MODE_LEGACY), type);
        }
        else {
            super.setText(Html.fromHtml(text.toString()), type);
        }
    }
}
