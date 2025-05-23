package com.swooby.alfredai.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ContentAlpha
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.swooby.alfredai.R
import com.swooby.alfredai.SharedViewModel
import com.swooby.alfredai.WearViewModel
import com.swooby.alfredai.appViewModels
import com.swooby.alfredai.ui.theme.AlfredAITheme

// TODO: If phone app is not running:
//  https://developer.android.com/reference/androidx/wear/remote/interactions/RemoteActivityHelper
// TODO: Use https://google.github.io/horologist/ ?

class WearActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private val wearViewModel: WearViewModel by appViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate()")
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent {
            WearApp("Wear", wearViewModel)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
        super.onDestroy()
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview")
}

@Composable
fun WearApp(
    targetNameDefault: String,
    wearViewModel: WearViewModel? = null,
) {
    @Suppress("LocalVariableName")
    val TAG = "WearApp"

    val phoneAppNodeId by wearViewModel
        ?.remoteAppNodeId
        ?.collectAsState()
        ?: remember { mutableStateOf(null) }
    Log.d(TAG, "phoneAppNodeId is: $phoneAppNodeId")
    LaunchedEffect(phoneAppNodeId) {
        Log.d(TAG, "phoneAppNodeId changed to: $phoneAppNodeId")
    }

    var isConnectingOrConnected by remember { mutableStateOf(false) }
    val isConnected = phoneAppNodeId != null

    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)

    AlfredAITheme(
        dynamicColor = false,
        darkTheme = true,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            if (isConnected) {
                KeepScreenOnComposable()
            }
            Box {
                when {
                    isConnected -> {
                        CircularProgressIndicator(
                            progress = 1f,
                            indicatorColor = Color.Green,
                            strokeWidth = 6.dp,
                            modifier = Modifier.size(150.dp)
                        )
                    }

                    isConnectingOrConnected -> {
                        CircularProgressIndicator(
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp,
                            modifier = Modifier.size(150.dp)
                        )
                    }

                    else -> {
                        CircularProgressIndicator(
                            progress = 0f,
                            indicatorColor = disabledColor,
                            strokeWidth = 6.dp,
                            modifier = Modifier.size(150.dp)
                        )
                    }
                }
            }
            PushToTalkButton(
                wearViewModel = wearViewModel,
                targetNameDefault = "Wear",
                enabled = isConnected,
                onPushToTalkStart = {
                    wearViewModel?.pushToTalk(true)
                },
                onPushToTalkStop = {
                    wearViewModel?.pushToTalk(false)
                },
            )
        }
    }
}

@Composable
fun PushToTalkButton(
    wearViewModel: WearViewModel? = null,
    targetNameDefault: String,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    iconIdle: Int = R.drawable.baseline_mic_24,
    iconPressed: Int = R.drawable.baseline_mic_24,
    iconDisabled: Int = R.drawable.baseline_mic_off_24,
    onPushToTalkStart: () -> Unit = {},
    onPushToTalkStop: () -> Unit = {},
) {
    val pttState by wearViewModel
        ?.pushToTalkState
        ?.collectAsState()
        ?: remember { mutableStateOf(SharedViewModel.PttState.Idle) }

    val phoneAppNodeId by wearViewModel
        ?.remoteAppNodeId
        ?.collectAsState()
        ?: remember { mutableStateOf(null) }
    val targetName = if (phoneAppNodeId != null) "Phone" else targetNameDefault

    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
    val boxAlpha = if (enabled) 1.0f else 0.38f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(120.dp)
            .border(4.dp, if (enabled) MaterialTheme.colorScheme.primary else disabledColor, shape = CircleShape)
            .background(
                color = if (pttState == SharedViewModel.PttState.Pressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .let { baseModifier ->
                if (enabled) {
                    baseModifier.pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            onPushToTalkStart()
                            do {
                                val event = awaitPointerEvent()
                            } while (event.changes.any { !it.changedToUp() })
                            onPushToTalkStop()
                        }
                    }
                } else {
                    baseModifier.pointerInput(Unit) {
                        awaitEachGesture {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            }
            .then(Modifier.background(Color.Transparent))
            .let {
                it
                    .background(Color.Transparent)
                    .graphicsLayer {
                        this.alpha = boxAlpha
                    }
            }
    ) {
        val iconRes = if (enabled) {
            if (pttState == SharedViewModel.PttState.Pressed) {
                iconPressed
            } else {
                iconIdle
            }
        } else {
            iconDisabled
        }
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = "Microphone",
            modifier = Modifier
                .size(90.dp)
        )
    }
}

@Composable
fun KeepScreenOnComposable() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }
}
