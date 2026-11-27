package com.simohypers.limusic.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.simohypers.limusic.ui.navigation.destination.home.SettingsDestination
import com.simohypers.limusic.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import kotlin.reflect.KClass

/** Position of [destination] in [tabs], or -1 when it is not one of them. */
fun limusicTabIndex(
    destination: NavDestination,
    tabs: List<BottomNavScreen>,
): Int = tabs.indexOfFirst { destination.hasRoute(it.destination::class) }

/**
 * Opens a tab the way tapping it does. A tab opened with a plain navigate (an app link) sits on
 * Home's stack, and the next Home tap then saves and restores it straight back.
 */
fun NavController.navigateToTab(destination: Any) =
    navigate(destination) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

/**
 * The bottom tabs, the user's [tabs] in their order. The tab you are in is the last tab on the back
 * stack, so it stays lit on the album or artist pages opened from it. Tapping it there goes back to
 * the tab's root; tapping it on the root calls [onReselect] (scroll to top or refresh). [compact] is
 * the "Compact navigation bar" setting: icons only, centred in a 56dp bar. Off, the bar keeps
 * Material's own 80dp and labels.
 */
@Composable
fun LimusicNavigationBar(
    navController: NavController,
    tabs: List<BottomNavScreen>,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onReselect: (KClass<*>) -> Unit = {},
) {
    val backStack by navController.0currentBackStack.collectAsState()
    val tabEntry = backStack.indexOfLast { limusicTabIndex(it.destination, tabs) >= 0 }
    val stackTab = backStack.getOrNull(tabEntry)?.destination?.let { limusicTabIndex(it, tabs) } ?: -1
    // Settings and the pages opened from it belong to no tab, so none is lit there.
    val inSettings = backStack.indexOfLast { it.destination.hasRoute(SettingsDestination::class) } > tabEntry
    val currentTab = if (inSettings) -1 else stackTab
    val onTabRoot = backStack.lastOrNull()?.destination?.let { limusicTabIndex(it, tabs) } == currentTab
    Column(modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(
            // NavigationBar pads itself by the system inset, so its height has to include it.
            modifier =
                if (compact) {
                    Modifier.height(CompactBarHeight + NavigationBarDefaults.windowInsets.asPaddingValues().calculateBottomPadding())
                } else {
                    Modifier
                },
            containerColor = MaterialTheme.colorScheme.background,
            tonalElevation = 0.dp,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == currentTab
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (selected && onTabRoot) {
                            onReselect(tab.destination::class)
                        } else if (index == stackTab) {
                            // Restoring this tab's saved stack would bring the same page back, so pop to its root.
                            navController.popBackStack(tab.destination, inclusive = false)
                        } else {
                            navController.navigateToTab(tab.destination)
                        }
                    },
                    // Without its label the icon names the tab, for screen readers.
                    icon = { Icon(tab.icon, contentDescription = if (compact) stringResource(tab.title) else null) },
                    label =
                        if (compact) {
                            null
                        } else {
                            { Text(stringResource(tab.title), style = typo().bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                )
            }
        }
    }
}

// Each item holds itself at M3's 80dp unless the bar caps it. With no label, 56dp centres the
// icon and its 32dp indicator with 12dp either side.
private val CompactBarHeight = 56.dp

/**
 * The top bar Home and Library share: the title on the left, the tab's own [actions] on the right. Draws below the status bar and nothing behind itself, so the screen decides the ground.
 */
@Composable
fun LimusicTabTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = typo().titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(text = subtitle, style = typo().bodySmall, maxLines = 1)
            }
        }
        actions()
    }
}
