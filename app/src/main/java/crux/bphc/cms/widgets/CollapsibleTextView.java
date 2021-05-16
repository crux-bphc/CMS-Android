package crux.bphc.cms.widgets;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import crux.bphc.cms.R;
import crux.bphc.cms.utils.Utils;

/**
 * A user interface element that can be collapsed on user input using a
 * {@link ClickableSpan}.
 * <p>
 * Useful when needing to display variable length descriptions in the UI without
 * having it take over the entire screen.
 * </p>
 * <p>
 * See {@link R.styleable#CollapsibleTextView CollapsibleTextView
 * attributes}.
 * </p>
 *
 * @author Abhijeet Viswa
 */
public class CollapsibleTextView extends androidx.appcompat.widget.AppCompatTextView {

    private enum TextState {
        COLLAPSED,
        EXPANDED
    }

    private CharSequence fullText;
    private String collapseText;
    private String expandText;
    private int collapsedLineCount;

    private TextState state = TextState.COLLAPSED;
    private final SpannableStringBuilder spanBuilder = new SpannableStringBuilder();

    private boolean shouldRemeasure = true;

    public CollapsibleTextView(Context context) {
        super(context);
    }

    public CollapsibleTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CollapsibleTextView,
                0, 0
        );

        try {
            fullText = a.getText(R.styleable.CollapsibleTextView_full_text);
            collapseText = a.getString(R.styleable.CollapsibleTextView_collapse_text);
            expandText = a.getString(R.styleable.CollapsibleTextView_expand_text);
            collapsedLineCount = a.getInt(R.styleable.CollapsibleTextView_collapsed_line_count, 3);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // We don't want an infinite measure loop
        if (!shouldRemeasure)
            return;

        // Check if layout line count is greater than maximum. If so, set collapsed
        // text and remeasure
        shouldRemeasure = false;
        Layout layout = getLayout();
        CharSequence textToShow;
        if (state == TextState.COLLAPSED) {
            spanBuilder.clear();
            if (layout.getLineCount() > collapsedLineCount) {
                textToShow = fullText.subSequence(0, layout.getLineEnd(collapsedLineCount - 1));

                spanBuilder.append(Utils.INSTANCE.trimWhiteSpace(textToShow));
                spanBuilder.append("\n");
                spanBuilder.append(expandText);
                spanBuilder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        state = TextState.EXPANDED;
                        requestLayout();
                        shouldRemeasure = true;
                    }
                }, spanBuilder.length() - expandText.length() - 1, spanBuilder.length(), 0);
            } else {
                spanBuilder.append(fullText);
            }
        } else {
            textToShow = fullText; // The full text is shown, only difference is if the view is collapsible
            spanBuilder.clear();
            if (collapseText != null && !collapseText.isEmpty()) {

                spanBuilder.append(textToShow);
                if (spanBuilder.charAt(spanBuilder.length() - 1) != '\n') {
                    spanBuilder.append("\n");
                }
                spanBuilder.append(collapseText);
                spanBuilder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        state = TextState.COLLAPSED;
                        requestLayout();
                        shouldRemeasure = true;
                    }
                }, spanBuilder.length() - collapseText.length() - 1, spanBuilder.length(), 0);
            } else {
                spanBuilder.append(textToShow);
            }
            super.setText(spanBuilder);
        }

        // Have the layout measure itself once more
        super.setText(spanBuilder);
        measure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setFullText(CharSequence fullText) {
        this.fullText = fullText;
        this.state = TextState.COLLAPSED;
        this.shouldRemeasure = true;
        super.setText(fullText);
    }
}
