package com.example.codeeditor.termux.ssh

import android.content.Context
import com.example.codeeditor.termux.ConnectionFactory
import com.example.codeeditor.termux.TermuxSession

/** Server address and user, remembered between app runs. The password is never stored. */
class SshProfile(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var host: String
        get() = prefs.getString("host", null) ?: SshConfig.DEFAULT_HOST
        set(value) = prefs.edit().putString("host", value).apply()
    var port: Int
        get() = prefs.getInt("port", SshConfig.DEFAULT_PORT)
        set(value) = prefs.edit().putInt("port", value).apply()
    var username: String
        get() = prefs.getString("username", null) ?: SshConfig.DEFAULT_USERNAME
        set(value) = prefs.edit().putString("username", value).apply()

    companion object {
        private const val PREFS_NAME = "termux_ssh_profile"

        /**
            Lets [TermuxSession.shared] connect on its own with the saved profile and the app's key.
            Without a key it can't, since the password isn't saved.
         */
        @JvmStatic
        fun installAsDefault(context: Context) {
            val app = context.applicationContext
            TermuxSession.shared.factory = ConnectionFactory {
                val key = SshKeyStore(app).load() ?: return@ConnectionFactory null
                val profile = SshProfile(app)
                SshTermuxConnection(SshConfig(profile.host, profile.port, profile.username, key = key), HostKeyStore(app))
            }
        }
    }
}
