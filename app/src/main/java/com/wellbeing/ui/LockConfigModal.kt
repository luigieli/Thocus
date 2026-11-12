package com.wellbeing.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wellbeing.core.BreakInterval
import com.wellbeing.core.TimeWindow
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class BreakDraft(
    val id: String,
    val startHourText: String,
    val startMinuteText: String,
    val endHourText: String,
    val endMinuteText: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LockConfigModal(
    editingWindow: TimeWindow?,
    installedApps: List<InstalledAppInfo>,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        days: Set<DayOfWeek>,
        startTime: LocalTime,
        endTime: LocalTime,
        packages: Set<String>,
        breaks: List<BreakInterval>,
        isAllDay: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(editingWindow?.name ?: "Focus Time") }
    var selectedDays by remember {
        mutableStateOf(editingWindow?.daysOfWeek ?: defaultWorkDays())
    }
    var isAllDay by remember { mutableStateOf(editingWindow?.isAllDay ?: false) }
    var startHourText by remember { mutableStateOf(editingWindow?.startTime?.hour?.toString() ?: "8") }
    var startMinuteText by remember { mutableStateOf(editingWindow?.startTime?.minute?.toString() ?: "0") }
    var endHourText by remember { mutableStateOf(editingWindow?.endTime?.hour?.toString() ?: "17") }
    var endMinuteText by remember { mutableStateOf(editingWindow?.endTime?.minute?.toString() ?: "0") }

    var breakDrafts by remember {
        mutableStateOf(initialBreakDrafts(editingWindow))
    }

    var selectedPackages by remember {
        mutableStateOf(editingWindow?.blockedPackages ?: emptySet())
    }
    var appSearchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isKeyboardOpen = WindowInsets.isImeVisible

    LaunchedEffect(isKeyboardOpen) {
        if (!isKeyboardOpen && isSearchFocused) {
            focusManager.clearFocus()
            isSearchFocused = false
        }
    }

    val onScrollToSearch: () -> Unit = {
        isSearchFocused = true
        coroutineScope.launch {
            delay(100)
            listState.animateScrollToItem(index = SEARCH_ITEM_INDEX, scrollOffset = 0)
        }
    }

    val filteredApps = remember(installedApps, appSearchQuery) {
        filterApps(installedApps, appSearchQuery)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = if (editingWindow != null) "Edit Focus Window" else "Create New Focus Window",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Window Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Days of Week", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    DayDotsRow(selectedDays) { day ->
                        selectedDays = toggleDay(selectedDays, day)
                    }
                }
                item {
                    AllDaySwitchRow(
                        isAllDay = isAllDay,
                        onToggle = { isAllDay = !isAllDay }
                    )
                }
                item {
                    ScheduleHoursSection(
                        isAllDay = isAllDay,
                        startHourText = startHourText,
                        onStartHourChange = { startHourText = it },
                        startMinuteText = startMinuteText,
                        onStartMinuteChange = { startMinuteText = it },
                        endHourText = endHourText,
                        onEndHourChange = { endHourText = it },
                        endMinuteText = endMinuteText,
                        onEndMinuteChange = { endMinuteText = it }
                    )
                }
                item {
                    MultipleBreaksSection(
                        breakDrafts = breakDrafts,
                        onAddBreak = {
                            breakDrafts = breakDrafts + createDefaultBreakDraft()
                        },
                        onRemoveBreak = { targetId ->
                            breakDrafts = removeBreakDraft(breakDrafts, targetId)
                        },
                        onUpdateBreak = { targetId, transform ->
                            breakDrafts = updateBreakDraft(breakDrafts, targetId, transform)
                        }
                    )
                }
                item {
                    Text("Select Apps to Block (${selectedPackages.size})", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    AppSearchField(
                        query = appSearchQuery,
                        onQueryChange = { appSearchQuery = it },
                        onFocused = onScrollToSearch,
                        onTapped = onScrollToSearch,
                        onSearchDone = { focusManager.clearFocus() }
                    )
                }
                items(
                    items = filteredApps,
                    key = { it.packageName },
                    contentType = { "app_row" }
                ) { app ->
                    val isSelected = selectedPackages.contains(app.packageName)
                    AppPickerRow(app, isSelected) {
                        selectedPackages = togglePackage(selectedPackages, app.packageName)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            StickySaveButton(selectedCount = selectedPackages.size) {
                val windowId = editingWindow?.id ?: 0L
                val startTime = parseTime(startHourText, startMinuteText)
                val endTime = parseTime(endHourText, endMinuteText)
                val breaks = convertDraftsToBreaks(breakDrafts)
                onSave(windowId, name, selectedDays, startTime, endTime, selectedPackages, breaks, isAllDay)
            }
        }
    }
}

@Composable
private fun StickySaveButton(
    selectedCount: Int,
    onClick: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = onClick,
                enabled = selectedCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Save Focus Window ($selectedCount Apps)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

private const val SEARCH_ITEM_INDEX = 5

@Composable
private fun AppSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocused: () -> Unit,
    onTapped: () -> Unit,
    onSearchDone: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) {
                onTapped()
            }
        }
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search installed apps...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            SearchClearIcon(query) { onQueryChange("") }
        },
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocused()
                }
            },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchDone() })
    )
}

@Composable
private fun SearchClearIcon(query: String, onClear: () -> Unit) {
    if (query.isEmpty()) {
        return
    }
    IconButton(onClick = onClear) {
        Icon(Icons.Default.Close, contentDescription = "Clear Search")
    }
}


@Composable
private fun AppPickerRow(
    app: InstalledAppInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconThumbnail(app.packageName, app.applicationLabel)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = app.applicationLabel,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        AppSelectionCheckbox(isSelected)
    }
}

@Composable
private fun AppSelectionCheckbox(isSelected: Boolean) {
    if (isSelected) {
        SelectedCheckmarkBox()
        return
    }
    UnselectedCheckmarkBox()
}

@Composable
private fun SelectedCheckmarkBox() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun UnselectedCheckmarkBox() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp)
            )
    )
}

@Composable
private fun AppIconThumbnail(packageName: String, label: String) {
    val cachedBitmap = AppIconCache.get(packageName)
    if (cachedBitmap != null) {
        Image(
            bitmap = cachedBitmap,
            contentDescription = null,
            modifier = Modifier.size(36.dp)
        )
        return
    }
    AppAvatarFallback(label)
}

@Composable
private fun AppAvatarFallback(label: String) {
    val initial = if (label.isNotEmpty()) label.take(1).uppercase() else "?"
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun filterApps(apps: List<InstalledAppInfo>, query: String): List<InstalledAppInfo> {
    if (query.isBlank()) {
        return apps
    }
    val lowerQuery = query.lowercase().trim()
    return apps.filter {
        it.applicationLabel.lowercase().contains(lowerQuery) ||
                it.packageName.lowercase().contains(lowerQuery)
    }
}

private fun togglePackage(existing: Set<String>, packageName: String): Set<String> {
    if (existing.contains(packageName)) {
        return existing - packageName
    }
    return existing + packageName
}

private fun defaultWorkDays(): Set<DayOfWeek> {
    return setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY
    )
}

@Composable
private fun DayDotsRow(
    selectedDays: Set<DayOfWeek>,
    onDayClick: (DayOfWeek) -> Unit
) {
    val days = listOf(
        DayOfWeek.SUNDAY to "S",
        DayOfWeek.MONDAY to "M",
        DayOfWeek.TUESDAY to "T",
        DayOfWeek.WEDNESDAY to "W",
        DayOfWeek.THURSDAY to "T",
        DayOfWeek.FRIDAY to "F",
        DayOfWeek.SATURDAY to "S"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (dayPair in days) {
            val isSelected = selectedDays.contains(dayPair.first)
            DayDot(
                label = dayPair.second,
                isSelected = isSelected,
                onClick = { onDayClick(dayPair.first) }
            )
        }
    }
}

@Composable
private fun DayDot(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun AllDaySwitchRow(
    isAllDay: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("All Day Focus (24h)", fontWeight = FontWeight.SemiBold)
            Text(
                text = "Restricts selected apps all 24 hours of scheduled days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        androidx.compose.material3.Switch(
            checked = isAllDay,
            onCheckedChange = { onToggle() }
        )
    }
}

@Composable
private fun ScheduleHoursSection(
    isAllDay: Boolean,
    startHourText: String,
    onStartHourChange: (String) -> Unit,
    startMinuteText: String,
    onStartMinuteChange: (String) -> Unit,
    endHourText: String,
    onEndHourChange: (String) -> Unit,
    endMinuteText: String,
    onEndMinuteChange: (String) -> Unit
) {
    if (isAllDay) {
        AllDayInfoBanner()
        return
    }
    TimeRangeInputs(
        startHour = startHourText,
        onStartHourChange = onStartHourChange,
        startMinute = startMinuteText,
        onStartMinuteChange = onStartMinuteChange,
        endHour = endHourText,
        onEndHourChange = onEndHourChange,
        endMinute = endMinuteText,
        onEndMinuteChange = onEndMinuteChange
    )
}

@Composable
private fun AllDayInfoBanner() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Active all 24 hours. Use break windows below to define periods when apps are allowed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun TimeRangeInputs(
    startHour: String,
    onStartHourChange: (String) -> Unit,
    startMinute: String,
    onStartMinuteChange: (String) -> Unit,
    endHour: String,
    onEndHourChange: (String) -> Unit,
    endMinute: String,
    onEndMinuteChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Start Time (24h)", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            TimeInputRow(startHour, onStartHourChange, startMinute, onStartMinuteChange)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Finish Time (24h)", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            TimeInputRow(endHour, onEndHourChange, endMinute, onEndMinuteChange)
        }
    }
}

@Composable
private fun TimeInputRow(
    hour: String,
    onHourChange: (String) -> Unit,
    minute: String,
    onMinuteChange: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = hour,
            onValueChange = onHourChange,
            modifier = Modifier.weight(1f),
            singleLine = true
        )
        Text(" : ", fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = minute,
            onValueChange = onMinuteChange,
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
}

@Composable
private fun MultipleBreaksSection(
    breakDrafts: List<BreakDraft>,
    onAddBreak: () -> Unit,
    onRemoveBreak: (String) -> Unit,
    onUpdateBreak: (String, (BreakDraft) -> BreakDraft) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            BreaksHeader(breakCount = breakDrafts.size, onAddBreak = onAddBreak)
            BreaksContent(breakDrafts, onRemoveBreak, onUpdateBreak)
        }
    }
}

@Composable
private fun BreaksHeader(breakCount: Int, onAddBreak: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Break Windows", fontWeight = FontWeight.SemiBold)
            Text(
                text = formatBreaksSummary(breakCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(onClick = onAddBreak) {
            Text("+ Add Break")
        }
    }
}

private fun formatBreaksSummary(count: Int): String {
    if (count == 0) {
        return "Apps blocked for entire window"
    }
    if (count == 1) {
        return "1 break configured"
    }
    return "$count breaks configured"
}

@Composable
private fun BreaksContent(
    breakDrafts: List<BreakDraft>,
    onRemoveBreak: (String) -> Unit,
    onUpdateBreak: (String, (BreakDraft) -> BreakDraft) -> Unit
) {
    if (breakDrafts.isEmpty()) {
        return
    }
    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Apps remain accessible during break periods.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    for (indexedDraft in breakDrafts.withIndex()) {
        BreakItemCard(
            index = indexedDraft.index,
            draft = indexedDraft.value,
            onRemove = { onRemoveBreak(indexedDraft.value.id) },
            onUpdate = onUpdateBreak
        )
    }
}

@Composable
private fun BreakItemCard(
    index: Int,
    draft: BreakDraft,
    onRemove: () -> Unit,
    onUpdate: (String, (BreakDraft) -> BreakDraft) -> Unit
) {
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Break #${index + 1}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Break",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            BreakTimeInputs(draft = draft, onUpdate = onUpdate)
        }
    }
}

@Composable
private fun BreakTimeInputs(
    draft: BreakDraft,
    onUpdate: (String, (BreakDraft) -> BreakDraft) -> Unit
) {
    TimeRangeInputs(
        startHour = draft.startHourText,
        onStartHourChange = { value ->
            onUpdate(draft.id) { it.copy(startHourText = value) }
        },
        startMinute = draft.startMinuteText,
        onStartMinuteChange = { value ->
            onUpdate(draft.id) { it.copy(startMinuteText = value) }
        },
        endHour = draft.endHourText,
        onEndHourChange = { value ->
            onUpdate(draft.id) { it.copy(endHourText = value) }
        },
        endMinute = draft.endMinuteText,
        onEndMinuteChange = { value ->
            onUpdate(draft.id) { it.copy(endMinuteText = value) }
        }
    )
}

private fun initialBreakDrafts(editingWindow: TimeWindow?): List<BreakDraft> {
    if (editingWindow == null) {
        return emptyList()
    }
    val drafts = mutableListOf<BreakDraft>()
    for (interval in editingWindow.breaks) {
        drafts.add(createBreakDraftFromInterval(interval))
    }
    return drafts
}

private fun createBreakDraftFromInterval(interval: BreakInterval): BreakDraft {
    return BreakDraft(
        id = UUID.randomUUID().toString(),
        startHourText = interval.startTime.hour.toString(),
        startMinuteText = interval.startTime.minute.toString(),
        endHourText = interval.endTime.hour.toString(),
        endMinuteText = interval.endTime.minute.toString()
    )
}

private fun createDefaultBreakDraft(): BreakDraft {
    return BreakDraft(
        id = UUID.randomUUID().toString(),
        startHourText = "12",
        startMinuteText = "0",
        endHourText = "13",
        endMinuteText = "0"
    )
}

private fun updateBreakDraft(
    existingList: List<BreakDraft>,
    targetId: String,
    transform: (BreakDraft) -> BreakDraft
): List<BreakDraft> {
    val result = mutableListOf<BreakDraft>()
    for (item in existingList) {
        if (item.id == targetId) {
            result.add(transform(item))
        }
        if (item.id != targetId) {
            result.add(item)
        }
    }
    return result
}

private fun removeBreakDraft(
    existingList: List<BreakDraft>,
    targetId: String
): List<BreakDraft> {
    val result = mutableListOf<BreakDraft>()
    for (item in existingList) {
        if (item.id != targetId) {
            result.add(item)
        }
    }
    return result
}

private fun convertDraftsToBreaks(drafts: List<BreakDraft>): List<BreakInterval> {
    val breaksList = mutableListOf<BreakInterval>()
    for (draft in drafts) {
        val startTime = parseTime(draft.startHourText, draft.startMinuteText)
        val endTime = parseTime(draft.endHourText, draft.endMinuteText)
        breaksList.add(BreakInterval(startTime, endTime))
    }
    return breaksList
}

private fun toggleDay(existing: Set<DayOfWeek>, day: DayOfWeek): Set<DayOfWeek> {
    if (existing.contains(day)) {
        return existing - day
    }
    return existing + day
}

private fun parseTime(hourStr: String, minuteStr: String): LocalTime {
    val hour = hourStr.trim().toIntOrNull() ?: 0
    val minute = minuteStr.trim().toIntOrNull() ?: 0
    return LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}
