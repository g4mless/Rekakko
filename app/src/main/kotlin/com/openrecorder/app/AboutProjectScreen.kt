// SPDX-License-Identifier: GPL-3.0-only

package com.openrecorder.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutProjectScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onViewOnGitHub: () -> Unit,
    onViewLicense: () -> Unit,
    onVersionClick: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val layoutDirection = LocalLayoutDirection.current

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.about_this_project)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = RecorderIcons.Back,
                            contentDescription = stringResource(R.string.navigate_back),
                            modifier = Modifier.graphicsLayer {
                                if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
                            },
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 12.dp,
                bottom = 28.dp,
            ),
        ) {
            item {
                AboutProjectHeader()
            }

            item {
                PreferenceCard {
                    BasicPreference(
                        title = stringResource(R.string.view_on_github),
                        onClick = onViewOnGitHub,
                        onClickLabel = stringResource(R.string.view_on_github),
                    )
                    BasicPreference(
                        title = stringResource(R.string.about_license),
                        summary = stringResource(R.string.about_license_value),
                        onClick = onViewLicense,
                        onClickLabel = stringResource(R.string.open_license),
                    )
                    BasicPreference(
                        title = stringResource(R.string.about_version),
                        summary = stringResource(R.string.about_version_value),
                        onClick = onVersionClick,
                        onClickLabel = stringResource(R.string.about_version),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutProjectHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = null,
                modifier = Modifier.size(104.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.about_tagline),
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
