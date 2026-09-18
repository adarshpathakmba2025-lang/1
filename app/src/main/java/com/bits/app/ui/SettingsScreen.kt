package com.bits.app.ui

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bits.app.BuildConfig
import com.bits.app.R
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.launch
import com.bits.app.data.BitsRepository
import com.bits.app.data.Monetization
import com.bits.app.data.WidgetSettings
import com.bits.app.data.ProPlan
import com.bits.app.data.WidgetTheme
import com.bits.app.data.BitsState
import com.bits.app.data.ClockStyle
import com.bits.app.data.ClockStyles
import com.bits.app.data.WidgetThemes
import com.bits.app.data.Category
import com.bits.app.data.addCategory
import com.bits.app.data.deleteCategory
import com.bits.app.data.isSystemCategory
import com.bits.app.data.renameCategory
import com.bits.app.data.reorderCategories
import com.bits.app.data.editBoard
import com.bits.app.data.pruneBoards
import com.bits.app.data.resetBoard
import com.bits.app.data.setShownOnBoard
import com.bits.app.data.withAddToBottom
import com.bits.app.data.withAutoClear
import com.bits.app.data.withClock
import com.bits.app.data.withClockStyle
import com.bits.app.data.withPro
import com.bits.app.data.withWidgetOpacity
import com.bits.app.data.withWidgetTheme
import com.bits.app.ui.theme.BitsColors
import com.bits.app.ui.theme.BitsText
import com.bits.app.widget.BitsWidgetReceiver
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private fun placedWidgetIds(context: Context): List<Int> =
    AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, BitsWidgetReceiver::class.java)).sorted()

private fun hasPlacedWidget(context: Context): Boolean = placedWidgetIds(context).isNotEmpty()

private fun requestPinWidget(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    if (manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(ComponentName(context, BitsWidgetReceiver::class.java), null, null)
    }
}

private fun openStoreListing(context: Context) {
    val packageName = context.packageName
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
    }
}

/**
 * Opens Play's own subscription management page, where Google requires the actual
 * cancel control to live. Once a real subscription product id exists, appending
 * "&sku=<id>&package=<applicationId>" links straight to that one entry instead of the
 * user's full list of Play subscriptions across every app.
 */
private fun openSubscriptionManagement(context: Context) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions")))
}

private fun appVersion(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
} catch (e: Exception) {
    ""
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    state: BitsState,
    repository: BitsRepository,
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
    onReplayTour: () -> Unit,
    onRestorePurchases: () -> Unit,
) {
    var clockPreview by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BitsColors.Ink)
            }
            Text("Settings", style = BitsText.Brand, modifier = Modifier.padding(start = 4.dp))
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 28.dp)
        ) {
            ProBanner(state, onOpenPaywall)

            SectionLabel("Lists")
            SettingsCard {
                ToggleRow(
                    title = "Add new items at the bottom",
                    subtitle = "Off means new items go to the top",
                    checked = state.preferences.addToBottom,
                    onCheckedChange = { on -> repository.edit { it.withAddToBottom(on) } },
                )
                Divider()
                ToggleRow(
                    title = "Clear finished items at midnight",
                    subtitle = "Leave off to keep them",
                    checked = state.preferences.autoClearCompleted,
                    onCheckedChange = { on -> repository.edit { it.withAutoClear(on) } },
                )
            }

            SectionLabel("Your data")
            SettingsCard { BackupRows(repository) }

            SectionLabel("About")
            SettingsCard {
                LinkRow("Replay the tour", "See what Bits can do", onClick = onReplayTour)
                Divider()
                RateRow()
                Divider()
                LinkRow("Restore purchases", "Already bought Pro? Bring it back", onClick = onRestorePurchases)
                if (state.preferences.proPlan == ProPlan.MONTHLY) {
                    Divider()
                    CancelSubscriptionRow()
                }
                Divider()
                VersionRow()
            }

            // Developer-only. Gated on the debug build so a release APK can never expose
            // the Simulate Pro switch, which would hand every paid feature away for free.
            if (BuildConfig.DEBUG) {
                DeveloperCard(state, repository)
            }
        }
    }
}

/** Only ever shown to a monthly subscriber; a lifetime purchase has nothing to cancel. */
@Composable
private fun CancelSubscriptionRow() {
    val context = LocalContext.current
    // Fully named, on purpose: this is the one call site that needs a non-default
    // colour, and naming every argument sidesteps any ambiguity about which
    // positional slot the trailing lambda would otherwise bind to.
    LinkRow(
        title = "Cancel membership",
        subtitle = "Opens Play Store subscription settings",
        color = BitsColors.Danger,
        onClick = { openSubscriptionManagement(context) },
    )
}

@Composable
private fun RateRow() {
    val context = LocalContext.current
    LinkRow("Rate Bits", "Tell us what to improve \u2014 every review is read") { openStoreListing(context) }
}
@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = BitsText.Small.copy(color = BitsColors.Muted),
        modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp),
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BitsColors.Panel)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun Divider() {
    Box(
        Modifier
            .padding(vertical = 12.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(BitsColors.Muted.copy(alpha = 0.16f))
    )
}

@Composable
private fun Hint(text: String) {
    Text(text, style = BitsText.Small, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
}

@Composable
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = BitsText.Body)
            if (subtitle != null) {
                Text(subtitle, style = BitsText.Small, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        BitsSwitch(checked = checked)
    }
}

@Composable
// onClick stays LAST so the common trailing-lambda call style binds to it.
// color sits before it with a default, which means any call passing onClick
// positionally would bind to color instead - so those few call sites use a
// trailing lambda or a named argument instead of a bare positional one.
private fun LinkRow(title: String, subtitle: String?, color: Color = BitsColors.Ink, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(title, style = BitsText.Body.copy(color = color))
        if (subtitle != null) {
            Text(subtitle, style = BitsText.Small, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun VersionRow() {
    val context = LocalContext.current
    Column {
        Text("Version", style = BitsText.Body)
        Text(appVersion(context), style = BitsText.Small, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun AddWidgetButton() {
    val context = LocalContext.current
    val canPin = remember { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }
    if (canPin) {
        FilledAction("Add widget", modifier = Modifier.padding(top = 12.dp)) { requestPinWidget(context) }
    }
}

@Composable
fun ProBanner(state: BitsState, onOpenPaywall: () -> Unit) {
    // Three states, not two. While Bits isn't charging, "Upgrade to Pro - from Rs229"
    // would be advertising a price for things the person already has and cannot buy.
    val bought = state.preferences.isPro
    val isPro = state.proUnlocked
    Row(
        Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPro) BitsColors.Panel else BitsColors.Amber.copy(alpha = 0.13f))
            .clickable(onClick = onOpenPaywall)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(BitsColors.Amber.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(R.drawable.ic_heart), contentDescription = null, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = when {
                    bought -> "You're Pro \u2014 thank you"
                    !Monetization.ENABLED -> "Everything's unlocked"
                    else -> "Upgrade to Pro"
                },
                style = BitsText.Subtitle.copy(color = if (isPro) BitsColors.Ink else BitsColors.Amber),
            )
            Text(
                text = when {
                    bought -> "See everything you've unlocked"
                    !Monetization.ENABLED -> "Every game, theme and widget list, free"
                    else -> "Games, themes, widget lists \u00b7 from \u20b9229"
                },
                style = BitsText.Small,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
/**
 * Every widget in one place. Free users get a single card that drives all their widgets;
 * Pro users get one card per placed widget, each independently customisable.
 */
@Composable
fun WidgetListsSection(
    state: BitsState,
    repository: BitsRepository,
    onOpenPaywall: () -> Unit,
    clockPreview: String?,
    onPreviewClock: (String?) -> Unit,
) {
    val context = LocalContext.current
    val ids by produceState(initialValue = placedWidgetIds(context)) {
        while (true) {
            delay(1500)
            value = placedWidgetIds(context)
        }
    }

    // Forget settings for widgets that have been removed from the home screen.
    LaunchedEffect(ids) {
        if (ids.isNotEmpty()) repository.edit { it.pruneBoards(ids.toSet()) }
    }

    SectionLabel("Widget lists")
    Text(
        text = "Add as many widgets as you like \u2014 each one can hold its own categories and its own look.",
        style = BitsText.Small,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
    )

    when {
        ids.isEmpty() -> SettingsCard {
            Text("No widget on your home screen yet", style = BitsText.Body)
            Text(
                "Long-press an empty spot, tap Widgets, find Bits.",
                style = BitsText.Small,
                modifier = Modifier.padding(top = 3.dp),
            )
            AddWidgetButton()

            // Lists can still be named and ordered before a widget exists, otherwise
            // removing the last widget would take the only way to manage them with it.
            Divider()
            CategoryManager(
                state = state,
                repository = repository,
                settings = state.widget,
                appWidgetId = null,
                onApply = { transform -> repository.edit { it.copy(widget = transform(it.widget)) } },
            )
        }

        // Without Pro every widget shares one look, so one card covers them all.
        !state.proUnlocked -> {
            WidgetCard(
                title = if (ids.size == 1) "Your widget" else "All ${ids.size} widgets",
                subtitle = if (ids.size == 1) null else "They share one list and look",
                settings = state.widget,
                state = state,
                repository = repository,
                onOpenPaywall = onOpenPaywall,
                clockPreview = clockPreview,
                onPreviewClock = onPreviewClock,
                appWidgetId = null,
            )
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Want them to differ?", style = BitsText.Body)
                        Text(
                            "With Pro, each widget can carry its own categories, theme and clock.",
                            style = BitsText.Small,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Image(painterResource(R.drawable.ic_lock_pixel), contentDescription = "Pro", modifier = Modifier.size(16.dp))
                }
                FilledAction("See Pro", modifier = Modifier.padding(top = 12.dp), onClick = onOpenPaywall)
            }
        }

        else -> ids.forEachIndexed { index, appWidgetId ->
            WidgetCard(
                title = "Widget ${index + 1}",
                subtitle = if (state.hasOwnBoard(appWidgetId)) "Its own list and look" else "Following your main settings",
                settings = state.settingsFor(appWidgetId),
                state = state,
                repository = repository,
                onOpenPaywall = onOpenPaywall,
                clockPreview = clockPreview,
                onPreviewClock = onPreviewClock,
                appWidgetId = appWidgetId,
            )
        }
    }
}

/**
 * One widget's full setup. [appWidgetId] null means the shared settings every widget
 * follows; otherwise edits are scoped to that one widget's board.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetCard(
    title: String,
    subtitle: String?,
    settings: WidgetSettings,
    state: BitsState,
    repository: BitsRepository,
    onOpenPaywall: () -> Unit,
    clockPreview: String?,
    onPreviewClock: (String?) -> Unit,
    appWidgetId: Int?,
) {
    val scope = rememberCoroutineScope()
    // Measuring an offset by hand proved unreliable inside a nested scroll. Asking the
    // preview itself to come into view is handled by the framework and always lands.
    val previewRequester = remember { BringIntoViewRequester() }

    fun jumpToPreview() {
        scope.launch { previewRequester.bringIntoView() }
    }

    var expanded by remember { mutableStateOf(appWidgetId == null) }
    var opacity by remember(settings.opacity) { mutableFloatStateOf(settings.opacity) }
    var themePreview by remember { mutableStateOf<String?>(null) }
    // Null means "show the real setting"; true/false previews the look without saving it.
    var pixelPreview by remember { mutableStateOf<Boolean?>(null) }

    // Edits land on the shared settings or on this widget's own board.
    fun apply(transform: (WidgetSettings) -> WidgetSettings) {
        repository.edit { current ->
            if (appWidgetId == null) current.copy(widget = transform(current.widget))
            else current.editBoard(appWidgetId) { transform(it) }
        }
    }

    SettingsCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = BitsText.Body)
                if (subtitle != null) {
                    Text(subtitle, style = BitsText.Small, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Text(
                if (expanded) "Hide" else "Customise",
                style = BitsText.Small.copy(color = BitsColors.Amber),
            )
        }

        Spacer(Modifier.height(10.dp))
        WidgetPreview(
            state = state,
            opacity = opacity,
            themeOverrideId = themePreview,
            clockOverrideId = clockPreview,
            settings = if (pixelPreview == null) settings else settings.copy(pixelHeadings = pixelPreview == true),
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .bringIntoViewRequester(previewRequester),
        )

        OpacitySlider(
            label = "Background opacity",
            value = opacity,
            onValueChange = { value ->
                opacity = value
                apply { it.copy(opacity = value) }
            },
        )

        val previewing: Any? = themePreview ?: clockPreview ?: pixelPreview
        if (previewing != null) {
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Previewing",
                    style = BitsText.Small.copy(color = BitsColors.Amber),
                    modifier = Modifier.weight(1f),
                )
                TextAction("Done", BitsColors.Muted) {
                    themePreview = null
                    pixelPreview = null
                    onPreviewClock(null)
                }
                // Copied into locals: `themePreview` is a delegated property, which
                // Kotlin refuses to smart-cast to non-null.
                val previewedTheme = themePreview
                val previewedClock = clockPreview
                val themeLocked = previewedTheme != null && !state.canUseTheme(previewedTheme)
                val clockLocked = previewedClock != null && !state.canUseClockStyle(previewedClock)
                val pixelLocked = pixelPreview != null && !state.canUsePixelHeadings
                if (themeLocked || clockLocked || pixelLocked) {
                    FilledAction("Unlock", onClick = onOpenPaywall)
                }
            }
        }

        if (expanded) {
            Spacer(Modifier.height(16.dp))
            CategoryManager(
                state = state,
                repository = repository,
                settings = settings,
                appWidgetId = appWidgetId,
                onApply = { transform -> apply(transform) },
            )

            Divider()
            ToggleRow(
                title = "Show clock",
                subtitle = "Pinned above your lists",
                checked = settings.showClock,
                onCheckedChange = { on -> apply { it.copy(showClock = on) } },
            )

            if (settings.showClock) {
                Spacer(Modifier.height(12.dp))
                Text("Clock style", style = BitsText.Small)
                Text(
                    "Tap to switch. Press and hold any style to try it on the preview.",
                    style = BitsText.Small.copy(color = BitsColors.Muted),
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                )
                ClockStyles.all.chunked(2).forEach { pair ->
                    Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { style ->
                            val locked = !state.canUseClockStyle(style.id)
                            val selected = state.clockStyleFor(settings).id == style.id && clockPreview == null
                            ClockTile(
                                style = style,
                                locked = locked,
                                selected = selected,
                                modifier = Modifier.weight(1f),
                                onSelect = {
                                    if (locked) onOpenPaywall()
                                    else {
                                        onPreviewClock(null)
                                        apply { it.copy(clockStyleOverride = style.id) }
                                        jumpToPreview()
                                    }
                                },
                                onPreview = {
                                    onPreviewClock(style.id)
                                    jumpToPreview()
                                },
                            )
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            Divider()
            Text("Theme", style = BitsText.Small)
            Text(
                "Tap to switch. Press and hold any theme to try it on the preview.",
                style = BitsText.Small.copy(color = BitsColors.Muted),
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
            )

            WidgetThemes.all.chunked(2).forEach { pair ->
                Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { theme ->
                        val usable = state.canUseTheme(theme.id)
                        // A board with no override follows the app theme, so that theme
                        // simply shows as the selected one rather than a separate option.
                        val activeId = if (appWidgetId == null) state.activeTheme.id else state.themeFor(settings).id
                        val selected = activeId == theme.id && themePreview == null
                        ThemeTile(
                            theme = theme,
                            usable = usable,
                            selected = selected,
                            previewing = themePreview == theme.id,
                            modifier = Modifier.weight(1f),
                            onSelect = {
                                if (!usable) onOpenPaywall()
                                else {
                                    themePreview = null
                                    if (appWidgetId == null) repository.edit { it.withWidgetTheme(theme.id) }
                                    else apply { it.copy(themeIdOverride = theme.id) }
                                    jumpToPreview()
                                }
                            },
                            onPreview = {
                                themePreview = theme.id
                                jumpToPreview()
                            },
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            Divider()
            PixelHeadingsRow(
                enabled = settings.pixelHeadings,
                unlocked = state.canUsePixelHeadings,
                onToggle = {
                    if (!state.canUsePixelHeadings) onOpenPaywall()
                    else {
                        pixelPreview = null
                        apply { it.copy(pixelHeadings = !settings.pixelHeadings) }
                        jumpToPreview()
                    }
                },
                onPreview = {
                    pixelPreview = !settings.pixelHeadings
                    jumpToPreview()
                },
            )

            if (appWidgetId != null && state.hasOwnBoard(appWidgetId)) {
                TextAction("Reset this widget to main settings", BitsColors.Muted) {
                    repository.edit { it.resetBoard(appWidgetId) }
                }
            }
        }
    }
}

/**
 * The pixel-headings switch, drawn in the pixel face itself so the control previews the
 * thing it turns on. Tap toggles it; press and hold previews it without saving, matching
 * how themes and clock styles behave.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PixelHeadingsRow(
    enabled: Boolean,
    unlocked: Boolean,
    onToggle: () -> Unit,
    onPreview: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) BitsColors.Amber.copy(alpha = 0.14f) else BitsColors.PanelBase)
            .border(
                width = if (enabled) 1.5.dp else 1.dp,
                color = if (enabled) BitsColors.Amber else BitsColors.Muted.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
            )
            .combinedClickable(onClick = onToggle, onLongClick = onPreview)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "PIXEL HEADINGS",
                style = BitsText.ChipLabel.copy(
                    color = if (!unlocked) BitsColors.Muted else if (enabled) BitsColors.Amber else BitsColors.Ink,
                ),
            )
            Text(
                "Category names on the widget, in the arcade face.",
                style = BitsText.Small.copy(color = BitsColors.Muted),
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        if (!unlocked) {
            Image(painterResource(R.drawable.ic_lock_pixel), contentDescription = "Pro", modifier = Modifier.size(14.dp))
        } else if (enabled) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = BitsColors.Amber, modifier = Modifier.size(16.dp))
        }
    }
}

/** The swatch-style theme tile, the look carried over from the old theme card. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ThemeTile(
    theme: WidgetTheme,
    usable: Boolean,
    selected: Boolean,
    previewing: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(theme.backgroundTint))
            .border(
                width = if (selected || previewing) 2.dp else 1.dp,
                color = if (selected || previewing) Color(theme.accent) else BitsColors.Muted.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
            )
            .combinedClickable(onClick = onSelect, onLongClick = onPreview)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                theme.displayName,
                style = BitsText.Small.copy(color = Color(theme.ink)),
                modifier = Modifier.weight(1f),
            )
            if (!usable) {
                Image(painterResource(R.drawable.ic_lock_pixel), contentDescription = "Pro", modifier = Modifier.size(13.dp))
            } else if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color(theme.accent), modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(theme.accent, theme.ink, theme.doneColor).forEach { swatch ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(swatch))
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClockTile(
    style: ClockStyle,
    locked: Boolean,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
) {
    // Styled like the theme tiles: a dark card, an accent border when chosen, and a small
    // visual of the layout standing in for the theme swatches.
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121A24))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) BitsColors.Amber else BitsColors.Muted.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp),
            )
            .combinedClickable(onClick = onSelect, onLongClick = onPreview)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                style.displayName,
                style = BitsText.Small.copy(color = if (locked) BitsColors.Muted else BitsColors.Ink),
                modifier = Modifier.weight(1f),
            )
            if (locked) {
                Image(painterResource(R.drawable.ic_lock_pixel), contentDescription = "Pro", modifier = Modifier.size(13.dp))
            } else if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = BitsColors.Amber, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        ClockShape(style.id, selected)
    }
}

/**
 * A tiny abstract picture of how each clock is laid out, filling the same role the colour
 * swatches play on a theme tile.
 */
/** Shared with the easter-egg dialog, so both pickers look like one family. */
@Composable
fun ClockShape(styleId: String, selected: Boolean) {
    val strong = if (selected) BitsColors.Amber else BitsColors.Ink.copy(alpha = 0.75f)
    val faint = BitsColors.Muted.copy(alpha = 0.5f)

    @Composable
    fun bar(width: Float, height: Int, color: Color, modifier: Modifier = Modifier) {
        Box(
            modifier
                .fillMaxWidth(width)
                .height(height.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
    }

    Column(
        Modifier.fillMaxWidth().height(34.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        when (styleId) {
            ClockStyle.COMPACT -> Row(verticalAlignment = Alignment.CenterVertically) {
                bar(0.38f, 11, strong)
                Spacer(Modifier.width(5.dp))
                bar(0.45f, 5, faint)
            }
            ClockStyle.STACKED -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                bar(0.34f, 5, faint)
                Spacer(Modifier.height(4.dp))
                bar(0.5f, 13, strong)
            }
            ClockStyle.MONO -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(4) { bar(0.11f, 12, strong) }
                }
                Spacer(Modifier.height(4.dp))
                bar(0.42f, 4, faint)
            }
            ClockStyle.BOLD -> Column(modifier = Modifier.fillMaxWidth()) {
                bar(0.62f, 15, strong)
                Spacer(Modifier.height(3.dp))
                bar(0.4f, 4, faint)
            }
            ClockStyle.DOTTED -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    bar(0.4f, 12, strong)
                    Spacer(Modifier.width(3.dp))
                    bar(0.14f, 7, faint)
                }
                Spacer(Modifier.height(4.dp))
                bar(0.46f, 4, faint)
            }
            // Minimal, and anything unrecognised: big time over a small date.
            else -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                bar(0.5f, 14, strong)
                Spacer(Modifier.height(4.dp))
                bar(0.62f, 4, faint)
            }
        }
    }
}

@Composable
private fun BackupRows(repository: BitsRepository) {
    var message by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<BitsState?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            message = null
            repository.exportBackup(uri) { ok ->
                message = if (ok) "Backup saved." else "Couldn't save there. Try another folder."
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            message = null
            repository.readBackup(uri) { restored ->
                if (restored == null) message = "That file isn't a Bits backup."
                else pendingRestore = restored
            }
        }
    }

    LinkRow("Back up now", "Save everything to a file") {
        exportLauncher.launch("bits-backup-${LocalDate.now()}.json")
    }
    Divider()
    LinkRow("Restore from a backup", "Replaces what's in Bits now") {
        importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
    }

    val pending = pendingRestore
    if (pending != null) {
        Column(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x1FE8907F))
                .padding(12.dp)
        ) {
            Text(
                "Replace everything with this backup? It has ${pending.categories.size} categories and ${pending.items.size} items.",
                style = BitsText.Small.copy(color = BitsColors.Ink),
            )
            Row {
                TextAction("Replace", BitsColors.Danger) {
                    repository.restore(pending)
                    pendingRestore = null
                    message = "Backup restored."
                }
                TextAction("Cancel") { pendingRestore = null }
            }
        }
    }
    message?.let {
        Text(it, style = BitsText.Small.copy(color = BitsColors.Amber), modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun DeveloperCard(state: BitsState, repository: BitsRepository) {
    var confirmReset by remember { mutableStateOf(false) }
    SectionLabel("Developer \u2014 remove before publishing")
    SettingsCard {
        Text("Simulate Pro", style = BitsText.Body)
        Text(
            "Local flag only, no real purchase. Pick a plan so the cancel-membership flow can be tested.",
            style = BitsText.Small,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(ProPlan.NONE to "Off", ProPlan.LIFETIME to "Lifetime", ProPlan.MONTHLY to "Monthly").forEach { (plan, label) ->
                val active = state.preferences.proPlan == plan
                Text(
                    text = label,
                    style = BitsText.Small.copy(color = if (active) BitsColors.Bg else BitsColors.Ink),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) BitsColors.Amber else BitsColors.PanelBase)
                        .clickable { repository.edit { it.withPro(plan != ProPlan.NONE, plan) } }
                        .padding(vertical = 9.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Divider()
        LinkRow("Simulate midnight", "Move Tomorrow into Today now") { repository.simulateMidnight() }
        Divider()
        if (confirmReset) {
            Text("Erase all lists and restore the starter content?", style = BitsText.Small.copy(color = BitsColors.Ink))
            Row {
                TextAction("Erase", BitsColors.Danger) {
                    confirmReset = false
                    repository.resetToSample()
                }
                TextAction("Cancel") { confirmReset = false }
            }
        } else {
            LinkRow("Reset to starter content", "Replaces your lists") { confirmReset = true }
        }
    }
}

/** A faithful copy of the home screen widget. themeOverrideId previews a theme without saving it. */
@Composable
fun WidgetPreview(
    state: BitsState,
    opacity: Float,
    themeOverrideId: String? = null,
    clockOverrideId: String? = null,
    settings: com.bits.app.data.WidgetSettings? = null,
    modifier: Modifier = Modifier,
) {
    // The preview only takes over scrolling once tapped. Otherwise a drag that starts
    // inside it would fight the settings page instead of scrolling it.
    var interactive by remember { mutableStateOf(false) }

    // It hands scrolling back automatically, so the page never stays hard to navigate.
    LaunchedEffect(interactive) {
        if (interactive) {
            delay(15_000)
            interactive = false
        }
    }
    val config = settings ?: state.widget
    val categories = state.categoriesFor(config)
    val theme = if (themeOverrideId != null) WidgetThemes.find(themeOverrideId) else state.themeFor(config)
    val accent = Color(theme.accent)
    val done = Color(theme.doneColor)
    val ink = Color(theme.ink)

    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(BitsColors.HomeWall)
            .then(
                if (interactive) Modifier
                else Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { interactive = true }
            )
    ) {
        DotGrid(Modifier.fillMaxSize(), spacing = 14.dp, color = Color(0x17EAE6DA))

        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(theme.backgroundTint).copy(alpha = opacity))
                .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 4.dp)
        ) {
            if (config.showClock) {
                PreviewClock(clockOverrideId ?: state.clockStyleFor(config).id, ink)
                Spacer(Modifier.height(8.dp))
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (interactive) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            ) {
                if (categories.isEmpty()) {
                    Text("Nothing to show yet.", style = BitsText.WidgetItem.copy(color = BitsColors.Muted))
                }
                categories.forEachIndexed { index, category ->
                    Text(
                        text = category.name.uppercase(),
                        style = if (config.pixelHeadings) BitsText.PixelHeading.copy(color = accent)
                        else BitsText.WidgetHeading.copy(color = accent),
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else 15.dp, bottom = 5.dp),
                    )
                    state.itemsIn(category.id).forEach { item ->
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            CheckVisual(checked = item.done, size = 18.dp)
                            Spacer(Modifier.width(9.dp))
                            Text(
                                item.text,
                                style = if (item.done) BitsText.WidgetItem.copy(color = done, textDecoration = TextDecoration.LineThrough)
                                else BitsText.WidgetItem.copy(color = ink),
                            )
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_game_controller),
                    contentDescription = null,
                    modifier = Modifier.padding(6.dp).size(34.dp),
                )
                Spacer(Modifier.weight(1f))
                Text("Bits", style = BitsText.WidgetHeading.copy(color = BitsColors.Muted), modifier = Modifier.padding(vertical = 8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_tune),
                    contentDescription = null,
                    tint = BitsColors.Muted,
                    modifier = Modifier.padding(6.dp).size(34.dp),
                )
            }
        }

        // Drawn after the widget body, not before it. Sitting earlier in the Box meant
        // the body was painted straight over the top and the hint was never visible.
        if (!interactive) {
            Text(
                text = "Tap to scroll",
                style = BitsText.Small.copy(color = BitsColors.Ink),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC0C131B))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun PreviewClock(styleId: String, ink: Color) {
    val context = LocalContext.current
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000)
        }
    }
    val h24 = DateFormat.is24HourFormat(context)
    fun fmt(pattern: String) = now.format(DateTimeFormatter.ofPattern(pattern))

    val timePattern = if (h24) "HH:mm" else "h:mm"
    when (styleId) {
        ClockStyle.COMPACT -> Row(
            Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(fmt(timePattern), style = BitsText.WidgetClockCompact.copy(color = ink))
            Text(fmt("EEE, d MMM"), style = BitsText.WidgetDate, modifier = Modifier.padding(start = 10.dp))
        }
        ClockStyle.STACKED -> Column(
            Modifier.fillMaxWidth().height(84.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(fmt("EEEE").uppercase(), style = BitsText.WidgetDate)
            Text(fmt(timePattern), style = BitsText.WidgetClock.copy(color = ink))
        }
        ClockStyle.MONO -> Column(
            Modifier.fillMaxWidth().height(78.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(fmt(if (h24) "HH:mm" else "hh:mm"), style = BitsText.WidgetClockMono.copy(color = ink))
            Text(fmt("yyyy-MM-dd"), style = BitsText.WidgetDateMono)
        }
        ClockStyle.BOLD -> Column(
            Modifier.fillMaxWidth().height(88.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(fmt(timePattern), style = BitsText.WidgetClockBold.copy(color = ink))
            Text(fmt("EEEE, d MMMM"), style = BitsText.WidgetDate)
        }
        ClockStyle.DOTTED -> Column(
            Modifier.fillMaxWidth().height(80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(fmt(if (h24) "HH:mm:ss" else "h:mm:ss"), style = BitsText.WidgetClockSeconds.copy(color = ink))
            Text(fmt("EEEE, d MMMM"), style = BitsText.WidgetDate)
        }
        else -> Column(
            Modifier.fillMaxWidth().height(88.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(fmt(timePattern), style = BitsText.WidgetClock.copy(color = ink))
            Text(fmt("EEEE, d MMMM"), style = BitsText.WidgetDate, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

/**
 * The category list for one widget card. It does two jobs at once: choosing which lists
 * this widget carries, and managing the lists themselves.
 *
 * These used to be separate — a "Your lists" block for renaming and ordering, and this
 * one for picking. Showing the same names twice on a single page made it unclear which
 * copy governed what, so the management controls moved onto these rows instead.
 *
 * The checkbox is scoped to this widget. Renaming, reordering and deleting change the
 * list itself, so they apply everywhere.
 */
@Composable
private fun CategoryManager(
    state: BitsState,
    repository: BitsRepository,
    settings: WidgetSettings,
    appWidgetId: Int?,
    onApply: ((WidgetSettings) -> WidgetSettings) -> Unit,
) {
    var renaming by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }

    // A small flourish: with pixel headings on, the names here wear the same face the
    // widget will use, so the setting shows itself in the list it governs.
    val pixel = settings.pixelHeadings && state.canUsePixelHeadings

    Text("Categories", style = BitsText.Small)
    Text(
        text = "Tap a name to show or hide it on this widget. Tap the pencil to rename.",
        style = BitsText.Small.copy(color = BitsColors.Muted),
        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
    )

    val ordered = state.sortedCategories
    ordered.forEach { category ->
        val index = ordered.indexOf(category)
        val system = isSystemCategory(category.id)
        CategoryManagerRow(
            category = category,
            shown = category.id !in settings.hiddenCategoryIds,
            system = system,
            pixel = pixel,
            // Today and Tomorrow anchor the top of every list, so nothing moves past them.
            canMoveUp = !system && index > 0 && !isSystemCategory(ordered[index - 1].id),
            canMoveDown = !system && index < ordered.lastIndex,
            renaming = renaming == category.id,
            confirming = confirmDelete == category.id,
            onToggleShown = {
                val shown = category.id !in settings.hiddenCategoryIds
                val hidden = if (shown) settings.hiddenCategoryIds + category.id
                else settings.hiddenCategoryIds - category.id
                onApply { it.copy(hiddenCategoryIds = hidden) }
            },
            onStartRename = {
                confirmDelete = null
                renaming = category.id
            },
            onRename = { name ->
                val trimmed = name.trim()
                if (trimmed.isNotEmpty() && trimmed != category.name) {
                    repository.edit { it.renameCategory(category.id, trimmed) }
                }
                renaming = null
            },
            onCancelRename = { renaming = null },
            onMove = { up ->
                val ids = ordered.map { it.id }.toMutableList()
                val from = ids.indexOf(category.id)
                val to = if (up) from - 1 else from + 1
                if (to in ids.indices) {
                    ids.add(to, ids.removeAt(from))
                    repository.edit { it.reorderCategories(ids) }
                }
            },
            onAskDelete = {
                renaming = null
                confirmDelete = category.id
            },
            onCancelDelete = { confirmDelete = null },
            onConfirmDelete = {
                repository.edit { it.deleteCategory(category.id) }
                confirmDelete = null
            },
        )
    }

    var newCategory by remember { mutableStateOf("") }
    InputPill(
        value = newCategory,
        onValueChange = { newCategory = it },
        placeholder = "New category",
        onSubmit = {
            val name = newCategory.trim()
            if (name.isNotEmpty()) {
                repository.edit { current ->
                    val added = current.addCategory(name)
                    val created = added.sortedCategories.lastOrNull()
                    when {
                        created == null -> added
                        appWidgetId == null -> added
                        else -> {
                            // Show it here only; keep every other widget as it was.
                            var next = added.copy(
                                widget = added.widget.copy(
                                    hiddenCategoryIds = added.widget.hiddenCategoryIds + created.id
                                )
                            )
                            next.boards.keys.forEach { other ->
                                if (other != appWidgetId) next = next.setShownOnBoard(other, created.id, false)
                            }
                            next.setShownOnBoard(appWidgetId, created.id, true)
                        }
                    }
                }
                newCategory = ""
            }
        },
    )
}

@Composable
private fun CategoryManagerRow(
    category: Category,
    shown: Boolean,
    system: Boolean,
    pixel: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    renaming: Boolean,
    confirming: Boolean,
    onToggleShown: () -> Unit,
    onStartRename: () -> Unit,
    onRename: (String) -> Unit,
    onCancelRename: () -> Unit,
    onMove: (Boolean) -> Unit,
    onAskDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    // Deleting a list takes its bits with it, so it asks first, inline.
    if (confirming) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
                "Delete \u201C${category.name}\u201D? Everything in it goes too.",
                style = BitsText.Small.copy(color = BitsColors.Ink),
            )
            Row(Modifier.padding(top = 4.dp)) {
                Spacer(Modifier.weight(1f))
                TextAction("Cancel", BitsColors.Muted, onCancelDelete)
                TextAction("Delete", BitsColors.Danger, onConfirmDelete)
            }
        }
        return
    }

    if (renaming) {
        var draft by remember(category.id) { mutableStateOf(category.name) }
        InputPill(
            value = draft,
            onValueChange = { draft = it },
            placeholder = "List name",
            onSubmit = { onRename(draft) },
            modifier = Modifier.padding(vertical = 6.dp),
            submitLabel = "SAVE",
            onCancel = onCancelRename,
        )
        return
    }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The pencil is its own target, so a tap on the name is free to mean something
        // else. Today and Tomorrow can't be renamed, so they get the space but no
        // pencil, which keeps every name on the same left edge.
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .then(if (system) Modifier else Modifier.clickable(onClick = onStartRename)),
            contentAlignment = Alignment.Center,
        ) {
            if (!system) {
                Image(
                    painter = painterResource(R.drawable.ic_pencil_pixel),
                    contentDescription = "Rename ${category.name}",
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        val base = if (pixel) BitsText.PixelHeading else BitsText.Body
        Text(
            text = if (pixel) category.name.uppercase() else category.name,
            style = when {
                !shown -> base.copy(color = BitsColors.Muted.copy(alpha = 0.5f))
                system -> base.copy(color = BitsColors.Muted)
                else -> base.copy(color = BitsColors.Ink)
            },
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                // A single tap carries this list on the widget, or takes it off again.
                .toggleable(
                    value = shown,
                    role = Role.Switch,
                    onValueChange = { onToggleShown() },
                )
                .padding(vertical = 9.dp, horizontal = 4.dp),
        )

        if (system) {
            // Today and Tomorrow are part of how Bits works, so they are fixed.
            Text("Fixed", style = BitsText.Small.copy(color = BitsColors.Muted.copy(alpha = 0.7f)))
        } else {
            MoveArrow("\u25B2", canMoveUp) { onMove(true) }
            MoveArrow("\u25BC", canMoveDown) { onMove(false) }
            TextAction("Delete", BitsColors.Danger, onAskDelete)
        }
    }
}

@Composable
private fun MoveArrow(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = glyph,
        style = BitsText.Small.copy(
            color = if (enabled) BitsColors.Amber else BitsColors.Muted.copy(alpha = 0.3f),
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    )
}
