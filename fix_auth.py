import re

with open('app/src/main/java/com/example/presentation/auth/AuthScreen.kt', 'r') as f:
    content = f.read()

# Pass viewModel in AuthScreen
content = content.replace("fun AuthScreen(onNavigateToOnboarding: () -> Unit) {", "fun AuthScreen(viewModel: com.example.presentation.viewmodel.ShasthoViewModel = androidx.lifecycle.viewmodel.compose.viewModel(), onNavigateToOnboarding: () -> Unit) {")

old_login = """                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    onNavigateToOnboarding()
                                } else {
                                    Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }"""

new_login = """                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    viewModel.syncDataOnLogin {
                                        isLoading = false
                                        onNavigateToOnboarding()
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }"""

old_signup = """                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                isLoading = false
                                if (task.isSuccessful) {
                                    onNavigateToOnboarding()
                                } else {
                                    Toast.makeText(context, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }"""

new_signup = """                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    viewModel.syncDataOnLogin {
                                        isLoading = false
                                        onNavigateToOnboarding()
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Sign up failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }"""

content = content.replace(old_login, new_login)
content = content.replace(old_signup, new_signup)

with open('app/src/main/java/com/example/presentation/auth/AuthScreen.kt', 'w') as f:
    f.write(content)
