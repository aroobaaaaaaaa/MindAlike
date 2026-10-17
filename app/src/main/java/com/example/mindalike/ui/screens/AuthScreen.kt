package com.example.mindalike.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindalike.data.supabase
import com.example.mindalike.ui.components.MindAlikeLogo
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class ProfileEmailCheck(val university_email: String)

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isSignUpTab by remember { mutableStateOf(false) }
    var isOtpSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Input States
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var universityName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MindAlikeLogo(size = 72.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    isOtpSent -> "Verify Your Email"
                    isSignUpTab -> "Create Account"
                    else -> "Welcome Back"
                },
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when {
                    isOtpSent -> "Enter the verification code sent to\n$email"
                    isSignUpTab -> "Connect with like-minded students across campuses"
                    else -> "Log in to your MindAlike account"
                },
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isOtpSent) {
                        // ================= OTP SCREEN =================
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 9) otpCode = it },
                            label = { Text("Verification Code") },
                            placeholder = { Text("Enter OTP code") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (otpCode.trim().length in 6..9) {
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            supabase.auth.verifyEmailOtp(
                                                type = OtpType.Email.SIGNUP,
                                                email = email.trim(),
                                                token = otpCode.trim()
                                            )
                                            Toast.makeText(context, "Account verified!", Toast.LENGTH_SHORT).show()
                                            onAuthSuccess()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            Toast.makeText(context, "Invalid OTP code", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Enter a valid OTP code", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Verify & Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = { isOtpSent = false }) {
                            Text("Back to Sign Up", color = MaterialTheme.colorScheme.primary)
                        }

                    } else if (isSignUpTab) {
                        // ================= SIGN UP SCREEN =================
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("University Email") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = universityName,
                            onValueChange = { universityName = it },
                            label = { Text("University Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("Location (e.g., Karachi)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val trimmedFullName = fullName.trim()
                                val trimmedEmail = email.trim()
                                val trimmedUni = universityName.trim()
                                val trimmedLocation = location.trim()

                                // Specific field validations
                                when {
                                    trimmedFullName.isEmpty() -> {
                                        Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                                    }
                                    trimmedEmail.isEmpty() || !trimmedEmail.contains("@") -> {
                                        Toast.makeText(context, "Please enter a valid university email", Toast.LENGTH_SHORT).show()
                                    }
                                    trimmedUni.isEmpty() -> {
                                        Toast.makeText(context, "Please enter your university name", Toast.LENGTH_SHORT).show()
                                    }
                                    trimmedLocation.isEmpty() -> {
                                        Toast.makeText(context, "Please enter your location", Toast.LENGTH_SHORT).show()
                                    }
                                    password.length < 6 -> {
                                        Toast.makeText(context, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        isLoading = true
                                        scope.launch {
                                            try {
                                                // 1. Check if email is already registered in public.profiles
                                                val existingProfile = supabase.from("profiles")
                                                    .select {
                                                        filter {
                                                            eq("university_email", trimmedEmail)
                                                        }
                                                    }
                                                    .decodeList<ProfileEmailCheck>()

                                                if (existingProfile.isNotEmpty()) {
                                                    Toast.makeText(context, "Email is already registered! Please log in.", Toast.LENGTH_LONG).show()
                                                    isLoading = false
                                                    return@launch
                                                }

                                                // 2. Proceed with Sign Up
                                                val userMetadata = buildJsonObject {
                                                    put("full_name", trimmedFullName)
                                                    put("university_name", trimmedUni)
                                                    put("location", trimmedLocation)
                                                }

                                                supabase.auth.signUpWith(Email) {
                                                    this.email = trimmedEmail
                                                    this.password = password
                                                    this.data = userMetadata
                                                }

                                                isOtpSent = true
                                                Toast.makeText(context, "Verification code sent!", Toast.LENGTH_SHORT).show()

                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                val msg = e.localizedMessage ?: ""
                                                if (msg.contains("already registered", ignoreCase = true) || msg.contains("user_already_exists", ignoreCase = true)) {
                                                    Toast.makeText(context, "Email is already registered! Please log in.", Toast.LENGTH_LONG).show()
                                                } else {
                                                    Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                                                }
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(onClick = { isSignUpTab = false }) {
                            Text("Already have an account? Log In", color = MaterialTheme.colorScheme.primary)
                        }

                    } else {
                        // ================= LOG IN SCREEN (DEFAULT) =================
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("University Email") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                when {
                                    email.trim().isEmpty() -> {
                                        Toast.makeText(context, "Please enter your university email", Toast.LENGTH_SHORT).show()
                                    }
                                    password.isEmpty() -> {
                                        Toast.makeText(context, "Please enter your password", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        isLoading = true
                                        scope.launch {
                                            try {
                                                supabase.auth.signInWith(Email) {
                                                    this.email = email.trim()
                                                    this.password = password
                                                }
                                                Toast.makeText(context, "Logged in successfully!", Toast.LENGTH_SHORT).show()
                                                onAuthSuccess()
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                Toast.makeText(context, "Login failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Log In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(onClick = { isSignUpTab = true }) {
                            Text("New user? Create Account", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}