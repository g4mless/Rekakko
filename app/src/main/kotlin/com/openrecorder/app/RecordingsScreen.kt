// SPDX-License-Identifier: GPL-3.0-only
// Adapted from AsteriskNG 1.4.2.
// Copyright 2026, AsteriskNG contributors

package com.openrecorder.app

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

private const val SORT_BY_DATE = 0
private const val SORT_BY_SIZE = 1
private const val SORT_BY_DURATION = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordingsScreen(
    recordings: List<RecordingItem>,
    loading: Boolean,
    loadFailed: Boolean,
    deleting: Boolean,
    onOpenRecording: (RecordingItem) -> Unit,
    onDeleteRecordings: (List<RecordingItem>) -> Unit,
    onShareRecordings: (List<RecordingItem>) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
    var sortOption by rememberSaveable { mutableIntStateOf(SORT_BY_DATE) }
    val busy = loading || deleting
    val selectionMode = selectedIds.isNotEmpty()
    val selectedRecordings = remember(recordings, selectedIds) {
        recordings.filter { it.id in selectedIds }
    }
    val allRecordingIds = remember(recordings) {
        recordings.map(RecordingItem::id).toSet()
    }
    val allRecordingsSelected = allRecordingIds.isNotEmpty() &&
        selectedIds.containsAll(allRecordingIds)
    val sortedRecordings = remember(recordings, sortOption) {
        when (sortOption) {
            SORT_BY_SIZE -> recordings.sortedWith(
                compareByDescending<RecordingItem> { it.sizeBytes }
                    .thenByDescending { it.dateAddedSeconds }
                    .thenByDescending { it.id },
            )

            SORT_BY_DURATION -> recordings.sortedWith(
                compareByDescending<RecordingItem> { it.durationMillis }
                    .thenByDescending { it.dateAddedSeconds }
                    .thenByDescending { it.id },
            )

            else -> recordings.sortedWith(
                compareByDescending<RecordingItem> { it.dateAddedSeconds }
                    .thenByDescending { it.id },
            )
        }
    }
    val sortByLabel = stringResource(R.string.recordings_sort_by)
    var sortMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(recordings) {
        val availableIds = recordings.mapTo(mutableSetOf(), RecordingItem::id)
        selectedIds = selectedIds.intersect(availableIds)
    }

    if (selectionMode && !deleting) {
        BackHandler {
            selectedIds = emptySet()
        }
    }

    fun toggleSelection(recording: RecordingItem) {
        selectedIds = if (recording.id in selectedIds) {
            selectedIds - recording.id
        } else {
            selectedIds + recording.id
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        if (selectionMode) {
                            stringResource(R.string.recordings_selected_count, selectedIds.size)
                        } else {
                            stringResource(R.string.recordings_title)
                        },
                    )
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (selectionMode) {
                        IconButton(
                            enabled = !busy,
                            onClick = {
                                if (allRecordingsSelected) {
                                    selectedIds = emptySet()
                                } else {
                                    selectedIds = allRecordingIds
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (allRecordingsSelected) {
                                    RecorderIcons.AllSelected
                                } else {
                                    RecorderIcons.SelectAll
                                },
                                contentDescription = if (allRecordingsSelected) {
                                    stringResource(R.string.clear_recording_selection)
                                } else {
                                    stringResource(R.string.select_all_recordings)
                                },
                                tint = if (allRecordingsSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                        IconButton(
                            enabled = !busy && selectedRecordings.isNotEmpty(),
                            onClick = { onShareRecordings(selectedRecordings) },
                        ) {
                            Icon(
                                imageVector = RecorderIcons.Share,
                                contentDescription = stringResource(R.string.action_share),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        IconButton(
                            enabled = !busy && selectedRecordings.isNotEmpty(),
                            onClick = { onDeleteRecordings(selectedRecordings) },
                        ) {
                            if (deleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = RecorderIcons.Delete,
                                    contentDescription = stringResource(
                                        R.string.delete_selected_recordings,
                                    ),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        IconButton(
                            enabled = !deleting,
                            onClick = { selectedIds = emptySet() },
                        ) {
                            Icon(
                                imageVector = RecorderIcons.Close,
                                contentDescription = stringResource(
                                    R.string.clear_recording_selection,
                                ),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else {
                        Box {
                            IconButton(
                                enabled = !busy,
                                onClick = { sortMenuExpanded = true },
                            ) {
                                Icon(
                                    imageVector = RecorderIcons.Sort,
                                    contentDescription = sortByLabel,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                            ) {
                                listOf(
                                    SORT_BY_DATE to R.string.recordings_sort_date,
                                    SORT_BY_SIZE to R.string.recordings_sort_size,
                                    SORT_BY_DURATION to R.string.recordings_sort_duration,
                                ).forEach { (option, labelRes) ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(labelRes)) },
                                        trailingIcon = if (sortOption == option) {
                                            {
                                                Icon(
                                                    imageVector = RecorderIcons.Check,
                                                    contentDescription = null,
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        onClick = {
                                            sortOption = option
                                            sortMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 24.dp,
            ),
        ) {
            when {
                loading && recordings.isEmpty() -> item(contentType = "loading") {
                    RecordingsLoadingState(modifier = Modifier.fillParentMaxSize())
                }

                loadFailed && recordings.isEmpty() -> item(contentType = "error") {
                    RecordingsMessageState(
                        title = stringResource(R.string.recordings_load_error),
                        summary = stringResource(R.string.recordings_load_error_summary),
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }

                recordings.isEmpty() -> item(contentType = "empty") {
                    RecordingsMessageState(
                        title = stringResource(R.string.recordings_empty),
                        summary = stringResource(R.string.recordings_empty_summary),
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }

                else -> {
                    item(contentType = "title") {
                        PreferenceTitle(
                            text = stringResource(R.string.recordings_count, recordings.size),
                        )
                    }
                    items(
                        items = sortedRecordings,
                        key = RecordingItem::id,
                        contentType = { "recording" },
                    ) { recording ->
                        RecordingCard(
                            modifier = Modifier.animateItem(),
                            recording = recording,
                            selected = recording.id in selectedIds,
                            selectionMode = selectionMode,
                            enabled = !busy,
                            onClick = {
                                if (selectionMode) {
                                    toggleSelection(recording)
                                } else {
                                    onOpenRecording(recording)
                                }
                            },
                            onLongClick = { toggleSelection(recording) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingCard(
    modifier: Modifier = Modifier,
    recording: RecordingItem,
    selected: Boolean,
    selectionMode: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val selectLabel = stringResource(R.string.select_recording)
    val openLabel = stringResource(R.string.open_recording)
    val selectedDescription = stringResource(R.string.recording_selected)
    val details = remember(context, recording) {
        val size = Formatter.formatShortFileSize(context, recording.sizeBytes)
        val duration = DateUtils.formatElapsedTime(recording.durationMillis / 1_000L)
        val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(recording.dateAddedSeconds * 1_000L))
        "$size Â· $duration Â· $date"
    }
    val selectionBackgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 180, easing = EaseInOut),
        label = "recordingSelectionBackgroundColor",
    )
    val iconBackgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        },
        animationSpec = tween(durationMillis = 180, easing = EaseInOut),
        label = "recordingSelectionIconBackgroundColor",
    )
    val iconContentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 180, easing = EaseInOut),
        label = "recordingSelectionIconContentColor",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { this.selected = selected }
                .background(selectionBackgroundColor)
                .combinedClickable(
                    enabled = enabled,
                    role = if (selectionMode) Role.Checkbox else Role.Button,
                    onClickLabel = if (selectionMode) selectLabel else openLabel,
                    onLongClickLabel = selectLabel,
                    onLongClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (selected) RecorderIcons.Check else RecorderIcons.Recordings,
                    contentDescription = if (selected) selectedDescription else null,
                    modifier = Modifier.size(24.dp),
                    tint = iconContentColor,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recording.name.ifEmpty {
                        context.getString(R.string.recording_untitled)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun RecordingsLoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.recordings_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecordingsMessageState(
    title: String,
    summary: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = RecorderIcons.Recordings,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
