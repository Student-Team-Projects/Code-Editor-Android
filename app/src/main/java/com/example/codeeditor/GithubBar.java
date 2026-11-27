package com.example.codeeditor;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * GithubBar is an inheritance class of MiddleBar containing Git/GitHub operations.
 *
 * @param <T> generic content parameter
 */
public class GithubBar<T> extends MiddleBar<T> {

    public GithubBar(@NonNull Context context) {
        super(context);
    }

    public GithubBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GithubBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context) {
        inflate(context, R.layout.github_bar, this);

        Button btnGitInit = findViewById(R.id.btnGitInit);
        Button btnGitStatus = findViewById(R.id.btnGitStatus);
        Button btnGitAddAll = findViewById(R.id.btnGitAddAll);
        Button btnGitCommit = findViewById(R.id.btnGitCommit);
        Button btnGitPush = findViewById(R.id.btnGitPush);
        Button btnGitAddRemote = findViewById(R.id.btnGitAddRemote);
        Button btnGitClone = findViewById(R.id.btnGitClone);

        if (context instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) context;

            if (btnGitInit != null) {
                btnGitInit.setOnClickListener(v -> mainActivity.gitInit());
            }

            if (btnGitStatus != null) {
                btnGitStatus.setOnClickListener(v -> mainActivity.gitStatus());
            }

            if (btnGitAddAll != null) {
                btnGitAddAll.setOnClickListener(v -> mainActivity.gitAddAll());
            }

            if (btnGitCommit != null) {
                btnGitCommit.setOnClickListener(v -> mainActivity.gitCommit());
            }

            if (btnGitPush != null) {
                btnGitPush.setOnClickListener(v -> mainActivity.gitPush());
            }

            if (btnGitAddRemote != null) {
                btnGitAddRemote.setOnClickListener(v -> mainActivity.gitAddRemote());
            }

            if (btnGitClone != null) {
                btnGitClone.setOnClickListener(v -> {
                    mainActivity.disableMainLayout();
                    GitCloneController.setEnabled(mainActivity);
                });
            }
        }
    }
}
