package com.example.codeeditor;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Generic class MiddleBar representing the blank middle bar laying atop the file tree.
 *
 * @param <T> the generic type associated with this MiddleBar
 */
public class MiddleBar<T> extends FrameLayout {

    private T content;

    public MiddleBar(@NonNull Context context) {
        super(context);
        init(context);
    }

    public MiddleBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MiddleBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    protected void init(Context context) {
        inflate(context, R.layout.middle_bar, this);
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }
}
