package com.smouldering_durtles.wk.dashboard

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
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
    val prioritizeLessons: Boolean = false,
    val prioritizeReviews: Boolean = false,
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
                      onResume: () -> Unit, onBrowse: () -> Unit,
                      onPriorityChange: (Boolean, Boolean) -> Unit = { _, _ -> }) {
    var showPriority by remember { mutableStateOf(false) }
    if (showPriority) {
        AlertDialog(onDismissRequest = { showPriority = false },
            title = { Text(stringResource(R.string.level_up_priority_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.level_up_path_explanation), style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(state.prioritizeLessons, role = Role.Switch, onValueChange = { onPriorityChange(it, state.prioritizeReviews) }), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.level_up_priority_lessons), Modifier.weight(1f))
                        Switch(checked = state.prioritizeLessons, onCheckedChange = null)
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(state.prioritizeReviews, role = Role.Switch, onValueChange = { onPriorityChange(state.prioritizeLessons, it) }), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.level_up_priority_reviews), Modifier.weight(1f))
                        Switch(checked = state.prioritizeReviews, onCheckedChange = null)
                    }
                }
            }, confirmButton = { TextButton(onClick = { showPriority = false }) { Text(stringResource(android.R.string.ok)) } })
    }
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            if (state.level > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.dashboard_level, state.level),
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    state.daysAtLevel?.let { days ->
                        Text(stringResource(R.string.dashboard_days_at_level, days),
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (!state.ready) {
                Text(stringResource(R.string.dashboard_loading))
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Count(state.reviews, stringResource(R.string.dashboard_reviews), Modifier.weight(1f), state.levelUpReviews)
                    Count(state.lessons, stringResource(R.string.dashboard_lessons), Modifier.weight(1f), state.levelUpLessons)
                }
                if (!state.vacation) {
                    TextButton(onClick = { showPriority = true }, enabled = enabled,
                        modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                        Text(stringResource(R.string.level_up_priority_title))
                    }
                }
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
                        if (state.reviews > 0) {
                            Button(onClick = onReviews, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                shape = RoundedCornerShape(12.dp)) {
                                Text(stringResource(R.string.dashboard_start_reviews))
                            }
                        }
                        if (state.lessons > 0) {
                            Button(onClick = onLessons, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                colors = if (state.reviews > 0) ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) else ButtonDefaults.buttonColors(),
                                shape = RoundedCornerShape(12.dp)) {
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
            TextButton(onClick = onBrowse, enabled = enabled,
                modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.dashboard_browse))
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
