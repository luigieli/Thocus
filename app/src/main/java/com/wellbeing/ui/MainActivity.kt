package com.wellbeing.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wellbeing.R
import com.wellbeing.core.TimeWindow
import com.wellbeing.data.AppDatabase
import com.wellbeing.data.ScheduleRepository

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getInstance(this)
        val repository = ScheduleRepository(database.timeWindowDao())
        viewModel = DashboardViewModel(repository)

        setContent {
            ThocusTheme {
                MainContainer(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reconcile(this)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainContainer(viewModel: DashboardViewModel) {
        val state by viewModel.state.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_app_logo),
                                contentDescription = "App Logo",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Thocus", fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            floatingActionButton = {
                if (state is DashboardState.Operational) {
                    FloatingActionButton(
                        onClick = { viewModel.openCreateModal() },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Focus Window")
                    }
                }
            }
        ) { paddingValues ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                color = MaterialTheme.colorScheme.background
            ) {
                RenderState(state, viewModel)
                RenderModalIfOpen(state, viewModel)
            }
        }
    }

    @Composable
    private fun RenderState(state: DashboardState, viewModel: DashboardViewModel) {
        if (state is DashboardState.Loading) {
            LoadingView()
            return
        }
        if (state is DashboardState.MissingPermissions) {
            PermissionsView(state)
            return
        }
        if (state is DashboardState.Operational) {
            OperationalDashboard(state, viewModel)
            return
        }
    }

    @Composable
    private fun RenderModalIfOpen(state: DashboardState, viewModel: DashboardViewModel) {
        val operational = state as? DashboardState.Operational ?: return
        if (!operational.isConfigModalOpen) {
            return
        }
        LockConfigModal(
            editingWindow = operational.editingWindow,
            installedApps = operational.installedApps,
            onDismiss = { viewModel.closeModal() },
            onSave = { id, name, days, start, end, pkgs, breaks, isAllDay ->
                viewModel.saveFocusWindow(id, name, days, start, end, pkgs, breaks, isAllDay, this@MainActivity)
            }
        )
    }

    @Composable
    private fun LoadingView() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }

    @Composable
    private fun PermissionsView(state: DashboardState.MissingPermissions) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Setup Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            AccessibilityWarningCard(state.isAccessibilityMissing)
            Spacer(modifier = Modifier.height(12.dp))
            AlarmWarningCard(state.isExactAlarmMissing)
        }
    }

    @Composable
    private fun AccessibilityWarningCard(isMissing: Boolean) {
        if (!isMissing) return
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Accessibility Service Disabled", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Enables instant interception of restricted apps.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { openAccessibilitySettings() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Enable in Settings")
                }
            }
        }
    }

    @Composable
    private fun AlarmWarningCard(isMissing: Boolean) {
        if (!isMissing) return
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Exact Alarms Required", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Required to enforce schedule boundaries to the exact second.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { openExactAlarmSettings() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Grant Alarm Permission")
                }
            }
        }
    }

    @Composable
    private fun OperationalDashboard(
        state: DashboardState.Operational,
        viewModel: DashboardViewModel
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }
            item { FocusCountdownCard(state.isFocusActive, state.remainingFocusMinutes) }
            item {
                Text(
                    text = "Focus Windows (${state.activeWindows.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (state.activeWindows.isEmpty()) {
                item { EmptyBlockedAppsCard { viewModel.openCreateModal() } }
            }
            items(state.activeWindows) { window ->
                FocusWindowCard(
                    window = window,
                    onConfigureClick = { viewModel.openEditModal(window) },
                    onDeleteClick = { viewModel.deleteFocusWindow(window.id, this@MainActivity) }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    @Composable
    private fun FocusCountdownCard(isFocusActive: Boolean, remainingMinutes: Long) {
        val accentColor = if (isFocusActive) MaterialTheme.colorScheme.primary else Color.Gray
        val titleText = if (isFocusActive) "FOCUS WINDOW ACTIVE" else "NO ACTIVE FOCUS"
        val subtitleText = if (isFocusActive) "$remainingMinutes min remaining" else "Apps unlock until next scheduled window"

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(76.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = titleText, style = MaterialTheme.typography.labelMedium, color = accentColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = subtitleText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    @Composable
    private fun FocusWindowCard(
        window: TimeWindow,
        onConfigureClick: () -> Unit,
        onDeleteClick: () -> Unit
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = window.name, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatWindowDetails(window),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    IconButton(onClick = onConfigureClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Configure")
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    private fun formatWindowDetails(window: TimeWindow): String {
        val breakText = formatBreakDescription(window.breakCount)
        val timeText = formatWindowHours(window)
        return "$timeText • $breakText • ${window.blockedPackages.size} apps"
    }

    private fun formatWindowHours(window: TimeWindow): String {
        if (window.isAllDay) {
            return "All Day (24h)"
        }
        return "${window.startTime} - ${window.endTime}"
    }

    private fun formatBreakDescription(breakCount: Int): String {
        if (breakCount == 1) {
            return "1 break"
        }
        if (breakCount > 1) {
            return "$breakCount breaks"
        }
        return "No breaks"
    }

    @Composable
    private fun EmptyBlockedAppsCard(onCreateClick: () -> Unit) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No focus windows configured yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onCreateClick) {
                    Text("+ Create First Focus Window")
                }
            }
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }
        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
    }
}
