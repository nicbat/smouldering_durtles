package com.smouldering_durtles.wk.dashboard

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
    val reviews: Int = 0,
    val activeSession: Boolean = false,
    val vacation: Boolean = false,
    val level: Int = 0,
    val upcoming: String = "",
    val daysAtLevel: Double? = null,
    val syncing: Boolean = false,
    val syncError: Boolean = false
)

@Composable
fun DashboardTheme(dynamicColor: Boolean, dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = colorResource(R.color.smoky_primary_dark), secondary = colorResource(R.color.smoky_secondary_dark), tertiary = colorResource(R.color.smoky_tertiary_dark))
        else -> lightColorScheme(primary = colorResource(R.color.smoky_primary_light), secondary = colorResource(R.color.smoky_secondary_light), tertiary = colorResource(R.color.smoky_tertiary_light))
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
fun DashboardOverview(state: DashboardState, enabled: Boolean,
                      onReviews: () -> Unit, onLessons: () -> Unit,
                      onResume: () -> Unit, onBrowse: () -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold)
            if (state.level > 0) {
                Text(stringResource(R.string.dashboard_level, state.level),
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                state.daysAtLevel?.let { days ->
                    Text(stringResource(R.string.dashboard_days_at_level, days),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!state.ready) {
                Text(stringResource(R.string.dashboard_loading))
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Count(state.reviews, stringResource(R.string.dashboard_reviews), Modifier.weight(1f))
                    Count(state.lessons, stringResource(R.string.dashboard_lessons), Modifier.weight(1f))
                }
                when {
                    state.activeSession -> {
                        Text(stringResource(R.string.dashboard_session_waiting))
                        Button(onClick = onResume, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.dashboard_resume))
                        }
                    }
                    state.vacation -> Text(stringResource(R.string.dashboard_vacation))
                    else -> {
                        if (state.reviews > 0) {
                            Button(onClick = onReviews, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.dashboard_start_reviews))
                            }
                        }
                        if (state.lessons > 0) {
                            FilledTonalButton(onClick = onLessons, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.dashboard_start_lessons))
                            }
                        }
                        if (state.reviews == 0 && state.lessons == 0) Text(stringResource(R.string.dashboard_caught_up))
                    }
                }
                if (state.upcoming.isNotEmpty()) Text(state.upcoming, style = MaterialTheme.typography.bodyMedium)
            }
            if (state.syncError) {
                Text(stringResource(R.string.dashboard_sync_error), color = MaterialTheme.colorScheme.error)
            } else if (state.syncing) {
                Text(stringResource(R.string.dashboard_syncing), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onBrowse, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dashboard_browse))
            }
        }
    }
}

@Composable
private fun Count(value: Int, label: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(NumberFormat.getIntegerInstance().format(value), style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
