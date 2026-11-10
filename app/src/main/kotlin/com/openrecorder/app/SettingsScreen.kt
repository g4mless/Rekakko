// SPDX-License-Identifier: GPL-3.0-only

package com.openrecorder.app

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val VERSION_TAPS_REQUIRED = 10
private const val SETTINGS_PAGE_MAIN = 0
private const val SETTINGS_PAGE_ABOUT_PROJECT = 1
private const val SETTINGS_PAGE_OPEN_SOURCE_LICENSES = 2
private const val SETTINGS_NAVIGATION_DURATION_MILLIS = 300
private const val SETTINGS_NAVIGATION_SETTLE_DURATION_MILLIS = 180
private const val SETTINGS_DESTINATION_PARALLAX_FRACTION = 0.2f
private const val SETTINGS_DESTINATION_DIM_AMOUNT = 0.5f
private val SettingsTransitionCornerRadius = 28.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    selectedThemeIndex: Int,
    selectedAudioIndex: Int,
    selectedSampleRateIndex: Int,
    selectedVideoResolutionIndex: Int,
    selectedVideoFrameRateIndex: Int,
    force16By9Letterboxing: Boolean,
    stopWhenLockScreen: Boolean,
    selectedVideoBitrateIndex: Int,
    selectedVideoCodecIndex: Int,
    selectedCountdownIndex: Int,
    selectedNamingPatternIndex: Int,
    selectedOrientationIndex: Int,
    optionsEnabled: Boolean,
    onThemeSelected: (Int) -> Unit,
    onAudioSelected: (Int) -> Unit,
    onSampleRateSelected: (Int) -> Unit,
    onVideoResolutionSelected: (Int) -> Unit,
    onVideoFrameRateSelected: (Int) -> Unit,
    onForce16By9LetterboxingChanged: (Boolean) -> Unit,
    onStopWhenLockScreenChanged: (Boolean) -> Unit,
    onVideoBitrateSelected: (Int) -> Unit,
    onVideoCodecSelected: (Int) -> Unit,
    onCountdownSelected: (Int) -> Unit,
    onNamingPatternSelected: (Int) -> Unit,
    onOrientationSelected: (Int) -> Unit,
    onViewOnGitHub: () -> Unit,
    onViewLicense: () -> Unit,
    onOpenExternalUrl: (String) -> Unit,
) {
    var settingsPage by rememberSaveable { mutableIntStateOf(SETTINGS_PAGE_MAIN) }
    var versionTapCount by rememberSaveable { mutableIntStateOf(0) }
    val pageTransitionProgress = remember {
        Animatable(if (settingsPage == SETTINGS_PAGE_MAIN) 0f else 1f)
    }
    val coroutineScope = rememberCoroutineScope()
    val transitionInteractionSource = remember { MutableInteractionSource() }
    val transitionShape = remember { RoundedCornerShape(SettingsTransitionCornerRadius) }
    val showingSubpage = settingsPage != SETTINGS_PAGE_MAIN

    fun openSubpage(page: Int) {
        if (settingsPage != SETTINGS_PAGE_MAIN) return

        settingsPage = page
        coroutineScope.launch {
            pageTransitionProgress.snapTo(0f)
            pageTransitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = SETTINGS_NAVIGATION_DURATION_MILLIS,
                    easing = EaseInOut,
                ),
            )
        }
    }
    fun closeSubpage() {
        if (settingsPage == SETTINGS_PAGE_MAIN) return

        coroutineScope.launch {
            pageTransitionProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = SETTINGS_NAVIGATION_DURATION_MILLIS,
                    easing = EaseInOut,
                ),
            )
            settingsPage = SETTINGS_PAGE_MAIN
        }
    }

    if (showingSubpage) {
        PredictiveBackHandler { progress ->
            try {
                progress.collect { backEvent ->
                    pageTransitionProgress.snapTo(
                        1f - backEvent.progress.coerceIn(0f, 1f),
                    )
                }
                pageTransitionProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = SETTINGS_NAVIGATION_SETTLE_DURATION_MILLIS,
                        easing = EaseInOut,
                    ),
                )
                settingsPage = SETTINGS_PAGE_MAIN
            } catch (cancellation: CancellationException) {
                coroutineScope.launch {
                    pageTransitionProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = SETTINGS_NAVIGATION_SETTLE_DURATION_MILLIS,
                            easing = EaseInOut,
                        ),
                    )
                }
                throw cancellation
            }
        }
    }

    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val themeOptions = remember(context) {
        listOf(
            context.getString(R.string.theme_system),
            context.getString(R.string.theme_light),
            context.getString(R.string.theme_dark),
        )
    }
    val audioOptions = remember(context) {
        listOf(
            context.getString(R.string.audio_none),
            context.getString(R.string.audio_microphone),
            context.getString(R.string.audio_internal),
            context.getString(R.string.audio_both),
        )
    }
    val sampleRateOptions = remember(context) {
        listOf(
            context.getString(R.string.sample_rate_44),
            context.getString(R.string.sample_rate_48),
        )
    }
    val videoResolutionOptions = remember(context) {
        listOf(
            context.getString(R.string.video_resolution_native),
            context.getString(R.string.video_resolution_2160p),
            context.getString(R.string.video_resolution_1440p),
            context.getString(R.string.video_resolution_1080p),
            context.getString(R.string.video_resolution_720p),
            context.getString(R.string.video_resolution_480p),
        )
    }
    val videoFrameRateOptions = remember(context) {
        listOf(
            context.getString(R.string.video_frame_rate_auto),
            context.getString(R.string.video_frame_rate_120),
            context.getString(R.string.video_frame_rate_90),
            context.getString(R.string.video_frame_rate_60),
            context.getString(R.string.video_frame_rate_30),
        )
    }
    val videoBitrateOptions = remember(context) {
        listOf(
            context.getString(R.string.video_bitrate_auto),
            context.getString(R.string.video_bitrate_4),
            context.getString(R.string.video_bitrate_8),
            context.getString(R.string.video_bitrate_16),
            context.getString(R.string.video_bitrate_24),
        )
    }
    val videoCodecOptions = remember(context) {
        listOf(
            context.getString(R.string.video_codec_h264),
            context.getString(R.string.video_codec_h265),
        )
    }
    val countdownOptions = remember(context) {
        listOf(
            context.getString(R.string.countdown_0_seconds),
            context.getString(R.string.countdown_3_seconds),
            context.getString(R.string.countdown_5_seconds),
            context.getString(R.string.countdown_10_seconds),
        )
    }
    val namingPatternOptions = remember(context) {
        listOf(
            context.getString(R.string.naming_pattern_day_month_year),
            context.getString(R.string.naming_pattern_month_day_year),
            context.getString(R.string.naming_pattern_year_month_day),
            context.getString(R.string.naming_pattern_year_day_month),
        )
    }
    val orientationOptions = remember(context) {
        listOf(
            context.getString(R.string.orientation_automatic),
            context.getString(R.string.orientation_portrait),
            context.getString(R.string.orientation_landscape),
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .graphicsLayer {
                    translationX = -size.width *
                        SETTINGS_DESTINATION_PARALLAX_FRACTION *
                        pageTransitionProgress.value
                }
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = { Text(stringResource(R.string.nav_settings)) },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 28.dp,
                ),
            ) {
                item {
                    PreferenceTitle(text = stringResource(R.string.section_appearance))
                    PreferenceCard {
                        DropdownPreference(
                            title = stringResource(R.string.theme_label),
                            items = themeOptions,
                            selectedIndex = selectedThemeIndex,
                            enabled = true,
                            onSelected = onThemeSelected,
                        )
                    }
                }

                item {
                    PreferenceTitle(text = stringResource(R.string.section_options))
                    PreferenceCard {
                        DropdownPreference(
                            title = stringResource(R.string.video_resolution_label),
                            items = videoResolutionOptions,
                            selectedIndex = selectedVideoResolutionIndex,
                            enabled = optionsEnabled,
                            onSelected = onVideoResolutionSelected,
                        )
                        DropdownPreference(
                            title = stringResource(R.string.video_frame_rate_label),
                            items = videoFrameRateOptions,
                            selectedIndex = selectedVideoFrameRateIndex,
                            enabled = optionsEnabled,
                            onSelected = onVideoFrameRateSelected,
                        )
                        DropdownPreference(
                            title = stringResource(R.string.audio_label),
                            items = audioOptions,
                            selectedIndex = selectedAudioIndex,
                            enabled = optionsEnabled,
                            onSelected = onAudioSelected,
                        )
                        DropdownPreference(
                            title = stringResource(R.string.sample_rate_label),
                            items = sampleRateOptions,
                            selectedIndex = selectedSampleRateIndex,
                            enabled = optionsEnabled,
                            onSelected = onSampleRateSelected,
                        )
                        SwitchPreference(
                            title = stringResource(R.string.force_16_by_9_letterboxing),
                            summary = stringResource(
                                R.string.force_16_by_9_letterboxing_summary,
                            ),
                            checked = force16By9Letterboxing,
                            enabled = optionsEnabled,
                            onCheckedChange = onForce16By9LetterboxingChanged,
                        )
                        SwitchPreference(
                            title = stringResource(R.string.stop_when_lock_screen),
                            summary = stringResource(
                                R.string.stop_when_lock_screen_summary,
                            ),
                            checked = stopWhenLockScreen,
                            enabled = optionsEnabled,
                            onCheckedChange = onStopWhenLockScreenChanged,
                        )
                        DropdownPreference(
                            title = stringResource(R.string.video_bitrate_label),
                            items = videoBitrateOptions,
                            selectedIndex = selectedVideoBitrateIndex,
                            enabled = optionsEnabled,
                            onSelected = onVideoBitrateSelected,
                        )
                        DropdownPreference(
                            title = stringResource(R.string.video_codec_label),
                            items = videoCodecOptions,
                            selectedIndex = selectedVideoCodecIndex,
                            enabled = optionsEnabled,
                            onSelected = onVideoCodecSelected,
                        )
                        DropdownPreference(
                            title = stringResource(R.string.recording_countdown_label),
                            items = countdownOptions,
                            selectedIndex = selectedCountdownIndex,
                            enabled = optionsEnabled,
                            onSelected = onCountdownSelected,
                        )
                        DropdownPreference(
                            title = stringResource(R.string.naming_pattern_label),
                            items = namingPatternOptions,
                            selectedIndex = selectedNamingPatternIndex,
                            enabled = optionsEnabled,
                            onSelected = onNamingPatternSelected,
                        )
                        DropdownPreference(
                            title = stringResource(R.string.recording_orientation_label),
                            items = orientationOptions,
                            selectedIndex = selectedOrientationIndex,
                            enabled = optionsEnabled,
                            onSelected = onOrientationSelected,
                        )
                    }
                }

                item {
                    PreferenceTitle(text = stringResource(R.string.section_about))
                    PreferenceCard {
                        ArrowPreference(
                            title = stringResource(R.string.about_this_project),
                            onClick = { openSubpage(SETTINGS_PAGE_ABOUT_PROJECT) },
                        )
                        ArrowPreference(
                            title = stringResource(R.string.open_source_licenses),
                            onClick = { openSubpage(SETTINGS_PAGE_OPEN_SOURCE_LICENSES) },
                        )
                    }
                }
            }
        }

        if (showingSubpage) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = SETTINGS_DESTINATION_DIM_AMOUNT * pageTransitionProgress.value
                    }
                    .background(Color.Black)
                    .clickable(
                        interactionSource = transitionInteractionSource,
                        indication = null,
                        onClick = {},
                    ),
            )

            val subpageModifier = Modifier.graphicsLayer {
                translationX = size.width *
                    (1f - pageTransitionProgress.value)
                shape = transitionShape
                clip = pageTransitionProgress.value < 1f
            }

            when (settingsPage) {
                SETTINGS_PAGE_ABOUT_PROJECT -> AboutProjectScreen(
                    modifier = subpageModifier,
                    onBack = ::closeSubpage,
                    onViewOnGitHub = onViewOnGitHub,
                    onViewLicense = onViewLicense,
                    onVersionClick = {
                        val updatedTapCount = versionTapCount + 1
                        if (updatedTapCount >= VERSION_TAPS_REQUIRED) {
                            Toast.makeText(
                                context,
                                R.string.version_tap_thanks,
                                Toast.LENGTH_SHORT,
                            ).show()
                            versionTapCount = 0
                        } else {
                            versionTapCount = updatedTapCount
                        }
                    },
                )

                SETTINGS_PAGE_OPEN_SOURCE_LICENSES -> OpenSourceLicensesScreen(
                    modifier = subpageModifier,
                    onBack = ::closeSubpage,
                    onOpenExternalUrl = onOpenExternalUrl,
                )

                else -> Unit
            }
        }
    }
}
