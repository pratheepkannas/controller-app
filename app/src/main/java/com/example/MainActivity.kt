package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.hardware.input.InputManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.InputDevice
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*

class ControllerViewModel(application: Application) : AndroidViewModel(application), InputManager.InputDeviceListener {
    private val inputManager = application.getSystemService(Context.INPUT_SERVICE) as InputManager
    
    var connectedControllers = mutableStateOf<List<InputDevice>>(emptyList())
        private set
        
    var activeController = mutableStateOf<InputDevice?>(null)
        private set
        
    init {
        inputManager.registerInputDeviceListener(this, Handler(Looper.getMainLooper()))
        updateControllers()
    }
    
    override fun onCleared() {
        inputManager.unregisterInputDeviceListener(this)
        super.onCleared()
    }
    
    override fun onInputDeviceAdded(deviceId: Int) { updateControllers() }
    override fun onInputDeviceRemoved(deviceId: Int) { updateControllers() }
    override fun onInputDeviceChanged(deviceId: Int) { updateControllers() }
    
    private fun updateControllers() {
        val ids = inputManager.inputDeviceIds.toList()
        val controllers = ids.mapNotNull { inputManager.getInputDevice(it) }
            .filter { it.isController() }
        connectedControllers.value = controllers
        if (!controllers.contains(activeController.value)) {
            activeController.value = controllers.firstOrNull()
        }
    }
    
    private fun InputDevice.isController(): Boolean {
        val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        val isJoystick = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        return isGamepad || isJoystick
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                val viewModel: ControllerViewModel = viewModel()
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: ControllerViewModel) {
    val activeController by viewModel.activeController
    var selectedTab by remember { mutableStateOf("Home") }
    
    BackHandler(enabled = selectedTab != "Home") {
        selectedTab = "Home"
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        topBar = { DashboardTopBar(activeController) },
        bottomBar = { DashboardBottomNav(selectedTab) { selectedTab = it } }
    ) { innerPadding ->
        if (activeController == null) {
            NoControllerScreen(modifier = Modifier.padding(innerPadding))
        } else {
            when (selectedTab) {
                "Home" -> DashboardContent(
                    controller = activeController!!,
                    modifier = Modifier.padding(innerPadding)
                )
                else -> PlaceholderScreen(title = selectedTab, modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title Configuration",
            color = TextSecondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun NoControllerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BluetoothSearching,
            contentDescription = "Searching",
            tint = Primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Controller Detected",
            color = TextMain,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Connect a controller via Bluetooth or wired USB to begin configuring profiles and haptics.",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            },
            colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = OnPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Bluetooth Settings", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DashboardContent(controller: InputDevice, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ActiveProfileCard()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val hasGyro = (controller.sources and InputDevice.SOURCE_SENSOR) == InputDevice.SOURCE_SENSOR
            StatCard(
                modifier = Modifier.weight(1f),
                title = "LOW LATENCY",
                value = "Active",
                subtitle = "Optimized",
                indicatorColor = Primary,
                indicatorBgColor = OnPrimary,
                indicatorAlignment = Alignment.CenterEnd
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "GYRO TILT",
                value = if (hasGyro) "Ready" else "N/A",
                subtitle = "Tilt controls",
                indicatorColor = if (hasGyro) Primary else TextSecondary,
                indicatorBgColor = Outline,
                indicatorAlignment = Alignment.CenterStart
            )
        }
        ControlsCard()
        LightingCard()
    }
}

@Composable
fun DashboardTopBar(controller: InputDevice?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .windowInsetsPadding(WindowInsets.statusBars),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (controller != null) "CONTROLLER CONNECTED" else "DISCONNECTED",
                color = if (controller != null) Primary else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = controller?.name ?: "No Device",
                color = TextMain,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        
        if (controller != null) {
            var batteryText = "--%"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val capacity = controller.batteryState.capacity
                if (capacity in 0f..1f) {
                    batteryText = "${(capacity * 100).toInt()}%"
                } else {
                    batteryText = "OK"
                }
            }
            
            Row(
                modifier = Modifier
                    .background(Surface, CircleShape)
                    .border(1.dp, Outline, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Success, CircleShape)
                )
                Text(
                    text = batteryText,
                    color = TextMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ActiveProfileCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(24.dp))
            .border(1.dp, Outline, RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = "Active Profile",
                    tint = OnPrimary
                )
            }
            Column {
                Text(text = "Active Profile", color = TextSecondary, fontSize = 12.sp)
                Text(
                    text = "Warzone FPS Pro",
                    color = TextMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = Outline, contentColor = TextMain),
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Change", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    indicatorColor: Color,
    indicatorBgColor: Color,
    indicatorAlignment: Alignment
) {
    Column(
        modifier = modifier
            .background(Surface, RoundedCornerShape(24.dp))
            .border(1.dp, Outline, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = title,
                color = Primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(16.dp)
                    .background(indicatorBgColor, CircleShape)
                    .padding(2.dp),
                contentAlignment = indicatorAlignment
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(indicatorColor, CircleShape)
                )
            }
        }
        Column {
            Text(
                text = value,
                color = TextMain,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ControlsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(24.dp))
            .border(1.dp, Outline, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ControlSlider(
            title = "Haptic Feedback Intensity",
            value = "75%",
            progress = 0.75f
        )
        ControlSlider(
            title = "Precision Sensitivity",
            value = "High",
            progress = 0.90f
        )
    }
}

@Composable
fun ControlSlider(title: String, value: String, progress: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = value, color = Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Outline, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(8.dp)
                    .background(Primary, CircleShape)
            )
        }
    }
}

@Composable
fun LightingCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LightPanelBackground, RoundedCornerShape(24.dp))
            .border(1.dp, Outline, RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(PinkAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Lighting",
                    tint = OnPinkAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(text = "Hardware Lighting", color = TextMain, fontSize = 14.sp)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(CyanAccent, CircleShape)
                    .border(2.dp, Primary, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(OrangeAccent, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(PurpleAccent, CircleShape)
            )
        }
    }
}

@Composable
fun DashboardBottomNav(selectedTab: String, onTabSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Surface)
            .border(1.dp, Outline)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(
            icon = Icons.Default.Home,
            label = "Home",
            isSelected = selectedTab == "Home",
            onClick = { onTabSelected("Home") }
        )
        NavItem(
            icon = Icons.Outlined.GridView,
            label = "Mapping",
            isSelected = selectedTab == "Mapping",
            onClick = { onTabSelected("Mapping") }
        )
        NavItem(
            icon = Icons.Outlined.Swipe,
            label = "Gesture",
            isSelected = selectedTab == "Gesture",
            onClick = { onTabSelected("Gesture") }
        )
        NavItem(
            icon = Icons.Outlined.Settings,
            label = "Config",
            isSelected = selectedTab == "Config",
            onClick = { onTabSelected("Config") }
        )
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable { onClick() }.padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(32.dp)
                .background(
                    color = if (isSelected) IconBackground else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) OnIconBackground else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            color = if (isSelected) Primary else TextSecondary,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
