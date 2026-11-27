package com.example.codeeditor;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.codeeditor.termux.ConnectionState;
import com.example.codeeditor.termux.TermuxSession;
import com.example.codeeditor.termux.ssh.HostKeyStore;
import com.example.codeeditor.termux.ssh.SshConfig;
import com.example.codeeditor.termux.ssh.SshKeyStore;
import com.example.codeeditor.termux.ssh.SshProfile;
import com.example.codeeditor.termux.ssh.SshTermuxConnection;

import java.security.KeyPair;

/**
 * TermuxBar is an inheritance class of MiddleBar containing the Termux Connection controls drawn in XML.
 *
 * @param <T> generic content parameter
 */
public class TermuxBar<T> extends MiddleBar<T> {

    private EditText etHost;
    private EditText etPort;
    private EditText etUser;
    private EditText etPassword;
    private TextView tvStatus;
    private TextView tvKeyFingerprint;

    private HostKeyStore hostKeyStore;
    private SshKeyStore sshKeyStore;
    private SshProfile sshProfile;
    private KeyPair loadedKey;

    public TermuxBar(@NonNull Context context) {
        super(context);
    }

    public TermuxBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TermuxBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context) {
        inflate(context, R.layout.termux_bar, this);

        hostKeyStore = new HostKeyStore(context);
        sshKeyStore = new SshKeyStore(context);
        sshProfile = new SshProfile(context);

        etHost = findViewById(R.id.etHost);
        etPort = findViewById(R.id.etPort);
        etUser = findViewById(R.id.etUser);
        etPassword = findViewById(R.id.etPassword);
        tvStatus = findViewById(R.id.tvStatus);
        tvKeyFingerprint = findViewById(R.id.tvKeyFingerprint);

        if (etHost != null) etHost.setText(sshProfile.getHost());
        if (etPort != null) etPort.setText(String.valueOf(sshProfile.getPort()));
        if (etUser != null) etUser.setText(sshProfile.getUsername());

        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnDisconnect = findViewById(R.id.btnDisconnect);
        Button btnResetHostKey = findViewById(R.id.btnResetHostKey);
        Button btnGenerateKey = findViewById(R.id.btnGenerateKey);
        Button btnCopySetupCmd = findViewById(R.id.btnCopySetupCmd);
        Button btnDeleteKey = findViewById(R.id.btnDeleteKey);

        refreshKeyInfo();

        if (btnConnect != null) {
            btnConnect.setOnClickListener(v -> {
                String host = etHost.getText().toString().trim();
                String portStr = etPort.getText().toString().trim();
                String user = etUser.getText().toString().trim();
                String pass = etPassword.getText().toString();

                if (host.isEmpty() || user.isEmpty() || portStr.isEmpty()) {
                    Toast.makeText(context, "Host, Port, and User are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                int port = Integer.parseInt(portStr);
                sshProfile.setHost(host);
                sshProfile.setPort(port);
                sshProfile.setUsername(user);

                SshConfig config = new SshConfig(host, port, user, pass, loadedKey);
                TermuxSession.Companion.getShared().connect(new SshTermuxConnection(config, hostKeyStore));
                updateStatusText(context);
            });
        }

        if (btnDisconnect != null) {
            btnDisconnect.setOnClickListener(v -> {
                TermuxSession.Companion.getShared().disconnect();
                updateStatusText(context);
            });
        }

        if (btnResetHostKey != null) {
            btnResetHostKey.setOnClickListener(v -> {
                String host = etHost.getText().toString().trim();
                String portStr = etPort.getText().toString().trim();
                if (!host.isEmpty() && !portStr.isEmpty()) {
                    int port = Integer.parseInt(portStr);
                    hostKeyStore.reset(host, port);
                    Toast.makeText(context, "Stored host key reset for " + host + ":" + port, Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnGenerateKey != null) {
            btnGenerateKey.setOnClickListener(v -> {
                new Thread(() -> {
                    loadedKey = sshKeyStore.generate();
                    post(this::refreshKeyInfo);
                }).start();
            });
        }

        if (btnCopySetupCmd != null) {
            btnCopySetupCmd.setOnClickListener(v -> {
                if (loadedKey != null) {
                    String line = SshKeyStore.Companion.publicKeyLine(loadedKey.getPublic());
                    String cmd = SshKeyStore.Companion.authorizeCommand(line);
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("Termux Setup Command", cmd));
                        Toast.makeText(context, "Setup command copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, "No key available. Generate a key first.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnDeleteKey != null) {
            btnDeleteKey.setOnClickListener(v -> {
                new Thread(() -> {
                    sshKeyStore.delete();
                    loadedKey = null;
                    post(this::refreshKeyInfo);
                }).start();
            });
        }

        updateStatusText(context);
    }

    private void refreshKeyInfo() {
        new Thread(() -> {
            loadedKey = sshKeyStore.load();
            post(() -> {
                if (tvKeyFingerprint != null) {
                    if (loadedKey != null) {
                        tvKeyFingerprint.setText("Fingerprint: " + SshKeyStore.Companion.fingerprint(loadedKey.getPublic()));
                    } else {
                        tvKeyFingerprint.setText("No key loaded");
                    }
                }
            });
        }).start();
    }

    private void updateStatusText(Context context) {
        if (tvStatus == null) return;
        ConnectionState state = TermuxSession.Companion.getShared().getState().getValue();
        if (state instanceof ConnectionState.Connected) {
            tvStatus.setText("Status: Connected");
        } else if (state instanceof ConnectionState.Connecting) {
            tvStatus.setText("Status: Connecting...");
        } else if (state instanceof ConnectionState.Failed) {
            ConnectionState.Failed failed = (ConnectionState.Failed) state;
            tvStatus.setText("Status: Failed (" + failed.getError().getLocalizedMessage() + ")");
        } else {
            tvStatus.setText("Status: Disconnected");
        }
    }
}
