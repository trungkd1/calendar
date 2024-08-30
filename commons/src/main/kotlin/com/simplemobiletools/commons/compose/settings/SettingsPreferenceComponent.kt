package com.simplemobiletools.commons.compose.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.compose.extensions.MyDevices
import com.simplemobiletools.commons.compose.theme.AppThemeSurface
import com.simplemobiletools.commons.compose.theme.preferenceSummaryColor
import com.simplemobiletools.commons.compose.theme.preferenceTitleColor

@Composable
fun SettingsPreferenceComponent(
    modifier: Modifier = Modifier,
    preferenceTitle: String,
    preferenceSummary: String? = null,
    isPreferenceEnabled: Boolean = true,
    doOnPreferenceLongClick: (() -> Unit)? = null,
    doOnPreferenceClick: (() -> Unit)? = null,
    preferenceSummaryColor: Color = preferenceSummaryColor(isEnabled = isPreferenceEnabled)
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = isPreferenceEnabled,
                onClick = { doOnPreferenceClick?.invoke() },
                onLongClick = { doOnPreferenceLongClick?.invoke() },
            )
            .padding(20.dp)
            .then(modifier),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = preferenceTitle,
            modifier = Modifier.fillMaxWidth(),
            color = preferenceTitleColor(isEnabled = isPreferenceEnabled),
            fontSize = with(LocalDensity.current) {
                dimensionResource(id = R.dimen.normal_text_size).toSp()
            }
        )
        AnimatedVisibility(visible = !preferenceSummary.isNullOrBlank()) {
            Text(
                text = preferenceSummary.toString(),
                modifier = Modifier
                    .fillMaxWidth(),
                color = preferenceSummaryColor.copy(alpha = 0.6f),
                fontSize = with(LocalDensity.current) {
                    dimensionResource(id = R.dimen.normal_text_size).toSp()
                }
            )
        }
    }
}

@MyDevices
@Composable
private fun SettingsPreferencePreview() {
    AppThemeSurface {
        SettingsPreferenceComponent(
            preferenceTitle = stringResource(id = R.string.language),
            preferenceSummary = stringResource(id = R.string.translation_english),
            isPreferenceEnabled = true,
        )
    }
}
