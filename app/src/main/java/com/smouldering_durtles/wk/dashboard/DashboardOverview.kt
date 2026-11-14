package com.smouldering_durtles.wk.dashboard

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smouldering_durtles.wk.R
import java.text.NumberFormat

/** Pure presentation state, also used by offline UI tests. No account fixtures required. */
data class DashboardState(
    val ready: Boolean = false,
    val lessons: Int = 0,
    val levelUpLessons: Int = 0,
    val levelUpReviews: Int = 0,
    val reviews: Int = 0,
    val activeSession: Boolean = false,
    val vacation: Boolean = false,
    val level: Int = 0,
    val daysAtLevel: Double? = null,
    val syncing: Boolean = false,
    val syncError: Boolean = false,
    val pendingUploads: Int = 0,
    val audioDownloads: Int = 0,
    val pitchDownloads: Int = 0,
    val offline: Boolean = false,
    val synced: Boolean = false
)

@Composable
fun DashboardTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = androidx.appcompat.view.ContextThemeWrapper(LocalContext.current, R.style.SmokyDashboardTheme)
    val colors = when {
        dark -> darkColorScheme(primary = colorResource(R.color.smoky_primary_dark), secondary = colorResource(R.color.smoky_secondary_dark), tertiary = colorResource(R.color.smoky_tertiary_dark))
        else -> lightColorScheme(primary = colorResource(R.color.smoky_primary_light), secondary = colorResource(R.color.smoky_secondary_light), tertiary = colorResource(R.color.smoky_tertiary_light))
    }
    fun themeColor(attribute: Int) = androidx.compose.ui.graphics.Color(
        com.google.android.material.color.MaterialColors.getColor(context, attribute, "DashboardTheme"))
    val sharedColors = colors.copy(
        primary = themeColor(androidx.appcompat.R.attr.colorPrimary),
        onPrimary = themeColor(com.google.android.material.R.attr.colorOnPrimary),
        primaryContainer = themeColor(com.google.android.material.R.attr.colorPrimaryContainer),
        onPrimaryContainer = themeColor(com.google.android.material.R.attr.colorOnPrimaryContainer),
        secondary = themeColor(com.google.android.material.R.attr.colorSecondary),
        onSecondary = themeColor(com.google.android.material.R.attr.colorOnSecondary),
        secondaryContainer = themeColor(com.google.android.material.R.attr.colorSecondaryContainer),
        onSecondaryContainer = themeColor(com.google.android.material.R.attr.colorOnSecondaryContainer),
        tertiary = themeColor(com.google.android.material.R.attr.colorTertiary),
        onTertiary = themeColor(com.google.android.material.R.attr.colorOnTertiary),
        tertiaryContainer = themeColor(com.google.android.material.R.attr.colorTertiaryContainer),
        onTertiaryContainer = themeColor(com.google.android.material.R.attr.colorOnTertiaryContainer),
        surface = themeColor(com.google.android.material.R.attr.colorSurface),
        onSurface = themeColor(com.google.android.material.R.attr.colorOnSurface),
        surfaceVariant = themeColor(com.google.android.material.R.attr.colorSurfaceVariant),
        onSurfaceVariant = themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant),
        surfaceContainerLow = themeColor(com.google.android.material.R.attr.colorSurfaceContainerLow),
        surfaceContainer = themeColor(com.google.android.material.R.attr.colorSurfaceContainer),
        surfaceContainerHigh = themeColor(com.google.android.material.R.attr.colorSurfaceContainerHigh),
        surfaceContainerHighest = themeColor(com.google.android.material.R.attr.colorSurfaceContainerHighest),
        outline = themeColor(com.google.android.material.R.attr.colorOutline),
        outlineVariant = themeColor(com.google.android.material.R.attr.colorOutlineVariant),
        error = themeColor(androidx.appcompat.R.attr.colorError),
        onError = themeColor(com.google.android.material.R.attr.colorOnError)
    )
    MaterialTheme(colorScheme = sharedColors, content = content)
}

@Composable
fun DashboardOverview(state: DashboardState, enabled: Boolean,
                      onReviews: () -> Unit, onLessons: () -> Unit,
                      onResume: () -> Unit,
                      showTitle: Boolean = true) {
    Surface(shape = if (showTitle) RoundedCornerShape(16.dp) else androidx.compose.ui.graphics.RectangleShape,
        color = if (showTitle) MaterialTheme.colorScheme.surfaceContainerLow else androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.fillMaxWidth().padding(if (showTitle) 16.dp else 0.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showTitle) Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            if (state.level > 0) {
                Text(buildString {
                    append(stringResource(R.string.dashboard_level, state.level))
                    state.daysAtLevel?.let { append(" · "); append(stringResource(R.string.dashboard_days_at_level, it)) }
                }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!state.ready) {
                Text(stringResource(R.string.dashboard_loading))
            } else {
                when {
                    state.activeSession -> {
                        Text(stringResource(R.string.dashboard_session_waiting))
                        Button(onClick = onResume, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp)) {
                            Text(stringResource(R.string.dashboard_resume))
                        }
                    }
                    state.vacation -> Text(stringResource(R.string.dashboard_vacation))
                    else -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = onReviews, enabled = enabled && state.reviews > 0,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                                Text("${state.reviews} reviews")
                            }
                            FilledTonalButton(onClick = onLessons, enabled = enabled && state.lessons > 0,
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                                Text("${state.lessons} lessons")
                            }
                        }
                        if (state.levelUpReviews > 0 || state.levelUpLessons > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (state.levelUpReviews > 0) Text(stringResource(R.string.level_up_available_count, state.levelUpReviews),
                                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                if (state.levelUpLessons > 0) Text(stringResource(R.string.level_up_available_count, state.levelUpLessons),
                                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (state.reviews == 0 && state.lessons == 0) Text(stringResource(R.string.dashboard_caught_up))
                    }
                }
            }
            val syncMessage = when {
                state.offline && state.pendingUploads > 0 -> stringResource(R.string.sync_saved_offline, state.pendingUploads)
                state.offline && state.syncing -> stringResource(R.string.sync_waiting_network)
                state.syncError -> stringResource(R.string.sync_needs_attention)
                state.pendingUploads > 0 -> stringResource(R.string.sync_uploading, state.pendingUploads)
                state.syncing -> stringResource(R.string.dashboard_syncing)
                state.ready && state.synced && !state.offline -> stringResource(R.string.sync_complete)
                else -> null
            }
            syncMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = if (state.syncError && !state.offline) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.audioDownloads > 0 || state.pitchDownloads > 0) {
                Text(stringResource(R.string.sync_media_queued, state.audioDownloads, state.pitchDownloads),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

        }
    }
}

@Composable
private fun Count(value: Int, label: String, modifier: Modifier, pathCount: Int = 0) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(NumberFormat.getIntegerInstance().format(value), style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        Text(label, style = MaterialTheme.typography.bodyMedium)
        if (pathCount > 0) Text(stringResource(R.string.level_up_available_count, pathCount),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
