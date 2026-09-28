import re

with open('app/src/main/java/com/example/presentation/viewmodel/ShasthoViewModel.kt', 'r') as f:
    content = f.read()

funcs = """
    fun syncDataOnLogin(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.syncDataOnLogin()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            database.clearAllTables()
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
"""

if "fun syncDataOnLogin" not in content:
    content = content.replace("class ShasthoViewModel(application: Application) : AndroidViewModel(application) {\n    private val database = AppDatabase.getDatabase(application)", "class ShasthoViewModel(application: Application) : AndroidViewModel(application) {\n    private val database = AppDatabase.getDatabase(application)\n" + funcs)
    with open('app/src/main/java/com/example/presentation/viewmodel/ShasthoViewModel.kt', 'w') as f:
        f.write(content)

