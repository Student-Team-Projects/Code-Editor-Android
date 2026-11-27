package com.example.codeeditor;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * DirectoryTree is an inheritance class of MiddleBar representing the directory/file tree.
 *
 * @param <T> generic content parameter
 */
public class DirectoryTree<T> extends MiddleBar<T> {

    public DirectoryTree(@NonNull Context context) {
        super(context);
    }

    public DirectoryTree(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DirectoryTree(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context) {
        // Inherits from MiddleBar
    }
}
