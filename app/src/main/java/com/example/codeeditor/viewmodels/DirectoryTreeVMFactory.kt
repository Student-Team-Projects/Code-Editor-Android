import android.content.ContentResolver
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.codeeditor.viewmodels.DirectoryTreeVM

class DirectoryTreeVMFactory(
    private val fileProvider: Function1<Uri, DocumentFile?>
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DirectoryTreeVM::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DirectoryTreeVM(fileProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
