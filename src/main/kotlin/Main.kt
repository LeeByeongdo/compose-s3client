// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.updateTransition
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.Bucket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
@Preview
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        var submitting by remember { mutableStateOf(false) }
        val transition = updateTransition(targetState = submitting)
        val color by transition.animateColor {
            if (it) {
                Color.Gray
            } else {
                Color.Blue
            }
        }

        val submitButtonTitle = if (!submitting) {
            "Connect"
        } else {
            "Loading..."
        }

        var accessKey by remember { mutableStateOf("") }
        var secretKey by remember { mutableStateOf("") }
        var buckets by remember { mutableStateOf(listOf<Bucket>()) }
        Row {
            Column {
                OutlinedTextField(value = accessKey, onValueChange = { accessKey = it }, label = { Text("Access Key") })
                OutlinedTextField(value = secretKey, onValueChange = { secretKey = it }, label = { Text("Secret Key") })
                Button(
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = color,
                        contentColor = Color.White,
                        disabledBackgroundColor = Color.Gray
                    ),
                    enabled = !submitting,
                    onClick = {
                        scope.launch {
                            submitting = true
                            buckets = withContext(Dispatchers.Default) {
                                fetchBuckets(accessKey, secretKey)
                            }
                            submitting = false
                        }
                    }) {
                    Text(text = submitButtonTitle)
                }
            }
            Column {
                buckets.forEach {
                    @OptIn(ExperimentalFoundationApi::class)
                    Box(modifier = Modifier.combinedClickable(onDoubleClick = {
                        println(it.name)
                    }, onClick = {})) {
                        Text(it.name)
                    }
                }
            }
        }


    }
}

suspend fun fetchBuckets(accessKey: String, secretKey: String): List<Bucket> {
    val credentials = BasicAWSCredentials(accessKey, secretKey)
    val client = AmazonS3ClientBuilder.standard().withCredentials(AWSStaticCredentialsProvider(credentials))
        .withRegion(Regions.AP_NORTHEAST_2)
        .build()

    val buckets = coroutineScope {
        client.listBuckets()
    }

    return buckets
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}

