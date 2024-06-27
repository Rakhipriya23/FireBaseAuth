
package com.example.firebaseauth




import android.content.ContentValues.TAG
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.firebaseauth.ui.theme.FireBaseAuthTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import java.util.UUID

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
//            FireBaseAuthTheme {
//                OTPScreen()
//            }

            FireBaseAuthTheme {
                //fetchUserDisplay()
                ImageUploadScreen()
            }

           // AddUserScreen()

//            val navController = rememberNavController()
//            NavHost(navController = navController, startDestination = "signup") {
//                composable("signup") {
//                    SignUpScreen(
//                        signUp = { email, password -> signUp(email, password, navController) },
//                        navController = navController
//                    )
//                }
//                composable("login") {
//                    LoginScreen(
//                        signIn = { email, password -> signIn(email, password) },
//                        navController = navController
//                    )
//                }
//            }
        }
    }
    private val auth = FirebaseAuth.getInstance()
    val firebaseDB = Firebase.firestore
    var storedVerificationId: String? = null
    lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
//for signUp
    private fun signUp(email: String, password: String, navController: NavController) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("User created: ${auth.currentUser}")
                    navController.navigate("login")
                } else {
                    println("User couldn't be created: ${task.exception?.message}")
                }
            }
    }
    //for Sign In
    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("User Logged In: ${auth.currentUser?.email}")
                } else {
                    println("User couldn't be logged in: ${task.exception?.message}")
                }
            }
    }

    //for add User

    fun addUserToFirebaseDB(name: String, age: Int) {
        val isAdult = age >= 18
        val firebaseUser = FirebaseUser(name, age, isAdult)

        firebaseDB.collection("users")
            .add(firebaseUser)
            .addOnSuccessListener { dRef ->
                // Successfully added user to Firestore
                Log.d(TAG, "Document added with ID: ${dRef.id}")
            }
            .addOnFailureListener { e ->
                // Failed to add user to Firestore
                Log.w(TAG, "Document Could Not be added: $e")
            }
    }
    //for fetch User---> means Read
    fun fetchFirebaseUser(onResult: (List<FirebaseUser>) -> Unit) {
        firebaseDB.collection("users")
            .get()
            .addOnSuccessListener { result ->
                val usersList = result.map { document ->
                    document.toObject(FirebaseUser::class.java)
                }
                onResult(usersList)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error Getting Data", e)
            }
    }

//upload image

    val storage= Firebase.storage
    val storageRef = storage.reference
    fun uploadImage(uri: Uri, context: android.content.Context) {
        val fileName = "images/${UUID.randomUUID()}.jpg"
        val imageRef = storageRef.child(fileName)

        imageRef.putFile(uri)
            .addOnCompleteListener { takeSnapShot ->
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    Toast.makeText(
                        context,"Image uploaded successfully:${uri}",Toast.LENGTH_SHORT).show()



                }

            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Image upload Failed:${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
















//..........................................................................


     val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            // This callback will be invoked in two situations:
            // 1 - Instant verification. In some cases the phone number can be instantly
            //     verified without needing to send or enter a verification code.
            // 2 - Auto-retrieval. On some devices Google Play services can automatically
            //     detect the incoming verification SMS and perform verification without
            //     user action.
            Log.d(TAG, "onVerificationCompleted:$credential")
            signINWithPhoneCred(credential)
        }

        override fun onVerificationFailed(e: FirebaseException) {
            // This callback is invoked in an invalid request for verification is made,
            // for instance if the the phone number format is not valid.
            Log.w(TAG, "onVerificationFailed", e)

            if (e is FirebaseAuthInvalidCredentialsException) {
                // Invalid request
            } else if (e is FirebaseTooManyRequestsException) {
                // The SMS quota for the project has been exceeded
            } else if (e is FirebaseAuthMissingActivityForRecaptchaException) {
                // reCAPTCHA verification attempted with null Activity
            }

            // Show a message and update the UI
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken,
        ) {
            // The SMS verification code has been sent to the provided phone number, we
            // now need to ask the user to enter the code and then construct a credential
            // by combining the code with a verification ID.
            Log.d(TAG, "onCodeSent:$verificationId")

            // Save verification ID and resending token so we can use them later
             storedVerificationId = verificationId
             resendToken = token
        }
    }
     fun startPhoneNumberVerification(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
     fun verifyPhoneWithCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(storedVerificationId!!, code)
        signINWithPhoneCred(credential)

    }
    fun signINWithPhoneCred(cred: PhoneAuthCredential) {
        auth.signInWithCredential(cred)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")

                    val user = task.result?.user
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                    // Update UI
                }
            }
    }

    @Composable
    fun OTPScreen() {
        val phoneNumber = remember { mutableStateOf("") }
        val otpCode = remember { mutableStateOf("") }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            TextField(value = phoneNumber.value, onValueChange = {
                phoneNumber.value = it
            },
                label = { Text(text = "Enter Phone Number")},
                modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                startPhoneNumberVerification(phoneNumber.value)
            }) {
                Text(text = "Send OTP")
            }
            TextField(value = otpCode.value, onValueChange = {
                otpCode.value = it
            },
                label = { Text(text = "Enter OTP")},
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                verifyPhoneWithCode(otpCode.value)
            }) {
                Text(text = "Verify OTP")
            }
        }
    }

    //model
    data class FirebaseUser(
        val name: String = "",
        val age: Int = 0,
        val inAdult: Boolean = false

    )

    @Composable
    fun AddUserScreen() {
        val name = remember { mutableStateOf("") }
        val age = remember { mutableStateOf("") }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .padding(20.dp)
                        .background(
                            color = Color(0xFFDFE8EB),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(25.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ADD User Detail",
                            fontSize = 30.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = name.value,
                            onValueChange = { name.value = it },
                            label = {
                                Text(
                                    text = "Enter Name", color = Color.Black
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Gray, shape = RoundedCornerShape(8.dp)),
//                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextField(
                            value = age.value,
                            onValueChange = { age.value = it },
                            label = {
                                Text(
                                    text = "Enter Age",
                                    color = Color.Black
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Gray, shape = RoundedCornerShape(8.dp)),
                            //keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                addUserToFirebaseDB(
                                    name.value,
                                    age.value.toInt()
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF324FEC))
                        ) {
                            Text(
                                text = "Add User",
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun fetchUserDisplay() {
        val usersList = remember {
            mutableStateOf<List<FirebaseUser>>(emptyList())
        }
        //call the api
        LaunchedEffect(Unit) {
            fetchFirebaseUser { users ->
                usersList.value = users
            }

        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Text(
                text = "Fetch The Existing Users",
                fontSize = 26.sp,
                color = Color.Black,
                modifier = Modifier.padding(top = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {

                itemsIndexed(usersList.value) { index, user ->
                    val backgroundColor =
                        if (index % 2 == 0) Color(0xFFE4DDDD) else Color(0xFFDAB7AC)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Name: ${user.name}, Age: ${user.age}",
                            fontSize = 18.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Divider()
                }
            }
        }
    }



    @Composable
    fun ImageUploadScreen() {
        val context = LocalContext.current
        val imageUri = remember {
            mutableStateOf<Uri?>(null)
        }
        val launcher= rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                uri: Uri?->imageUri.value=uri
            uri.let { uploadImage(it!!,context) }

        }
        Column(modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = {launcher.launch("image/*") }) {
                Text(text = "Select Image")
            }
            Spacer(modifier = Modifier.height(20.dp))
            imageUri.value.let {uri-> Image(painter = rememberAsyncImagePainter(uri), contentDescription = "Upload Image",
                modifier = Modifier.size(250.dp)) }
        }
    }



}