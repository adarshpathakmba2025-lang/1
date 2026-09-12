package com.bits.app.ui

import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.bits.app.R
import com.bits.app.data.BitsRepository
import com.bits.app.data.BitsState
import com.bits.app.data.WidgetThemes
import com.bits.app.data.withAutoClear
import com.bits.app.data.withClock
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

private fun hasPlacedWidget(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context)
    return manager.getAppWidgetIds(ComponentName(context, BitsWidgetReceiver::class.java)).isNotEmpty()
}

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

private fun appVersion(context: Context): String = try {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
} catch (e: Exception) {
    ""
}

@Composable
fun SettingsScreen(
    state: BitsState,
    repository: BitsRepository,
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
    onReplayTour: () -> Unit,
) {
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onBack),
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
                .padding(start = 12.dp, end = 12.dp, bottom = 24.dp)
        ) {
            var showFounderDialog by remember { mutableStateOf(false) }
            ProSection(state) { showFounderDialog = true }
            if (showFounderDialog) {
                FounderDialog(
                    onContinue = {
                        showFounderDialog = false
                        onOpenPaywall()
                    },
                    onDismiss = { showFounderDialog = false },
                )
            }
            WidgetSection(state, repository)
            ThemeSection(state, repository, onOpenPaywall)
            CleanupSection(state, repository)
            BackupSection(repository)
            SettingsCard("Help") {
                ActionRow(
                    title = "Replay the tour",
                    description = "Walk through everything Bits can do.",
                    onClick = onReplayTour,
                )
            }
            TestingSection(state, repository)
            SettingsCard("About") {
                ActionRow(
                    title = "Rate Bits",
                    description = "Enjoying Bits? A quick rating on the Play Store helps a lot.",
                    onClick = { openStoreListing(context) },
                )
                Text(
                    text = "Version ${appVersion(context)}",
                    style = BitsText.Small,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BitsColors.Panel)
            .padding(16.dp)
    ) {
        Text(title, style = BitsText.Subtitle)
        content()
    }
}

@Composable
private fun SwitchRow(title: String, description: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = BitsText.Body)
            if (description != null) {
                Text(description, style = BitsText.Small, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        BitsSwitch(checked = checked)
    }
}

@Composable
private fun ActionRow(title: String, description: String?, color: Color = BitsColors.Ink, onClick: () -> Unit) {
    Column(
        Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(title, style = BitsText.Body.copy(color = color))
        if (description != null) {
            Text(description, style = BitsText.Small, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ProSection(state: BitsState, onOpenPaywall: () -> Unit) {
    Column(
        Modifier
            .padding(top = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (state.preferences.isPro) BitsColors.Panel else BitsColors.Amber.copy(alpha = 0.14f))
            .clickable(onClick = onOpenPaywall)
            .padding(16.dp)
    ) {
        Text(
            text = if (state.preferences.isPro) "You're Pro \u2014 thank you!" else "Upgrade to Pro",
            style = BitsText.Subtitle.copy(color = if (state.preferences.isPro) BitsColors.Ink else BitsColors.Amber),
        )
        Text(
            text = if (state.preferences.isPro) {
                "More games and widget themes as they arrive. Tap to see what's included."
            } else {
                "Extra games, widget themes, and more \u2014 a small one-time thank-you that keeps Bits going."
            },
            style = BitsText.Small,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ThemeSection(state: BitsState, repository: BitsRepository, onOpenPaywall: () -> Unit) {
    // Previewing a locked theme doesn't save it — it only changes what this screen shows,
    // so a free user can see what they'd be getting without it affecting their real widget.
    var previewId by remember(state.preferences.widgetThemeId) { mutableStateOf(state.preferences.widgetThemeId) }
    val previewTheme = WidgetThemes.find(previewId)
    val isPro = state.preferences.isPro

    SettingsCard("Widget theme") {
        Text(
            text = "Tap any theme to try it on the preview above. Classic is free \u2014 the rest come with Pro.",
            style = BitsText.Small,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )
        WidgetPreview(
            state = state,
            opacity = state.widget.opacity,
            themeOverrideId = previewId,
            modifier = Modifier.fillMaxWidth().height(220.dp),
        )
        Spacer(Modifier.height(12.dp))

        val rows = WidgetThemes.all.chunked(2)
        rows.forEach { pair ->
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { theme ->
                    val locked = !theme.free && !isPro
                    val selected = theme.id == state.preferences.widgetThemeId
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BitsColors.PanelBase)
                            .clickable {
                                previewId = theme.id
                                if (!locked) repository.edit { it.withWidgetTheme(theme.id) }
                            }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(theme.accent))
                            )
                            Text(
                                theme.displayName,
                                style = BitsText.Small.copy(color = BitsColors.Ink),
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                            )
                            if (locked) {
                                Text("\uD83D\uDD12", style = BitsText.Small)
                            } else if (selected) {
                                Text("\u2713", style = BitsText.Small.copy(color = BitsColors.Amber))
                            }
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        if (!isPro && previewTheme.free.not()) {
            FilledAction(text = "Unlock with Pro", modifier = Modifier.padding(top = 12.dp), onClick = onOpenPaywall)
        }
    }
}

@Composable
private fun WidgetSection(state: BitsState, repository: BitsRepository) {
    val context = LocalContext.current
    val canPin = remember { AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported }
    // Local value keeps the slider smooth; each change is saved and the widget follows.
    var opacity by remember { mutableFloatStateOf(state.widget.opacity) }

    // Notice when the widget is added or removed while this page is open.
    val placed by produceState(initialValue = hasPlacedWidget(context)) {
        while (true) {
            delay(1500)
            value = hasPlacedWidget(context)
        }
    }

    SettingsCard("Widget") {
        Spacer(Modifier.height(10.dp))
        WidgetPreview(
            state = state,
            opacity = opacity,
            modifier = Modifier.fillMaxWidth().height(340.dp),
        )

        SwitchRow(
            title = "Show clock",
            description = "Time and date stay pinned at the top of the widget.",
            checked = state.widget.showClock,
            onCheckedChange = { on -> repository.edit { it.withClock(on) } },
        )

        OpacitySlider(
            label = "Background opacity",
            value = opacity,
            onValueChange = { value ->
                opacity = value
                repository.edit { it.withWidgetOpacity(value) }
            },
        )

        Text(
            text = "To choose which categories appear, tap Edit on the home page and use the switches.",
            style = BitsText.Small,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (!placed) {
            if (canPin) {
                FilledAction(
                    text = "Add widget to home screen",
                    modifier = Modifier.padding(top = 14.dp),
                    onClick = { requestPinWidget(context) },
                )
            }
            Text(
                text = "Or long-press an empty spot on your home screen, tap Widgets, and find Bits.",
                style = BitsText.Small,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun CleanupSection(state: BitsState, repository: BitsRepository) {
    SettingsCard("Daily cleanup") {
        SwitchRow(
            title = "Clear finished items at midnight",
            description = "Checked-off items are removed from every category when the day changes. Leave this off to keep them.",
            checked = state.preferences.autoClearCompleted,
            onCheckedChange = { on -> repository.edit { it.withAutoClear(on) } },
        )
    }
}

@Composable
private fun BackupSection(repository: BitsRepository) {
    var message by remember { mutableStateOf<String?>(null) }
    var pendingRestore by remember { mutableStateOf<BitsState?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            message = null
            repository.exportBackup(uri) { ok ->
                message = if (ok) "Backup saved." else "Couldn't save the backup. Try a different folder."
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            message = null
            repository.readBackup(uri) { restored ->
                if (restored == null) {
                    message = "That file isn't a Bits backup. Pick a file saved with \u201CBack up now\u201D."
                } else {
                    pendingRestore = restored
                }
            }
        }
    }

    SettingsCard("Backup") {
        ActionRow(
            title = "Back up now",
            description = "Save all your categories, items, and settings to a file, for example in Downloads.",
            onClick = { exportLauncher.launch("bits-backup-${LocalDate.now()}.json") },
        )
        ActionRow(
            title = "Restore from a backup",
            description = "Load a backup file. This replaces everything currently in Bits.",
            onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) },
        )

        val pending = pendingRestore
        if (pending != null) {
            val categoryCount = pending.categories.size
            val itemCount = pending.items.size
            Column(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1FE8907F))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Replace everything with this backup? It has $categoryCount " +
                        (if (categoryCount == 1) "category" else "categories") +
                        " and $itemCount " + (if (itemCount == 1) "item" else "items") +
                        ". What's in Bits now will be erased.",
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
            Text(it, style = BitsText.Small.copy(color = BitsColors.Amber), modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun TestingSection(state: BitsState, repository: BitsRepository) {
    var confirmReset by remember { mutableStateOf(false) }
    SettingsCard("Developer \u2014 remove before publishing") {
        Text(
            text = "Nothing below is real billing. This just flips a local flag so Pro-gated UI can be checked before Play Billing is wired up.",
            style = BitsText.Small,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
        )
        SwitchRow(
            title = "Simulate Pro",
            description = "Pretend the purchase happened, to test games and themes.",
            checked = state.preferences.isPro,
            onCheckedChange = { on -> repository.edit { it.withPro(on) } },
        )
        ActionRow(
            title = "Simulate midnight",
            description = "Run the day change now: unfinished Tomorrow items move into Today.",
            onClick = { repository.simulateMidnight() },
        )
        if (confirmReset) {
            Text(
                text = "Erase all lists and restore the starter content? Widget and cleanup settings are kept.",
                style = BitsText.Small.copy(color = BitsColors.Ink),
                modifier = Modifier.padding(top = 10.dp),
            )
            Row {
                TextAction("Erase lists", BitsColors.Danger) {
                    confirmReset = false
                    repository.resetToSample()
                }
                TextAction("Cancel") { confirmReset = false }
            }
        } else {
            ActionRow(
                title = "Reset to starter content",
                description = "Replace your lists with the starter categories and examples.",
                onClick = { confirmReset = true },
            )
        }
    }
}

/** A faithful copy of the home screen widget. themeOverrideId lets Settings preview a theme without saving it. */
@Composable
fun WidgetPreview(state: BitsState, opacity: Float, themeOverrideId: String? = null, modifier: Modifier = Modifier) {
    val categories = state.widgetCategories
    val theme = WidgetThemes.find(themeOverrideId ?: state.preferences.widgetThemeId)
    val accent = Color(theme.accent)
    val done = Color(theme.doneColor)
    val background = Color(theme.backgroundTint)

    Box(modifier.clip(RoundedCornerShape(24.dp)).background(BitsColors.HomeWall)) {
        DotGrid(Modifier.fillMaxSize(), spacing = 14.dp, color = Color(0x17EAE6DA))

        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(background.copy(alpha = opacity))
                .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 4.dp)
        ) {
            if (state.widget.showClock) {
                PreviewClock()
                Spacer(Modifier.height(8.dp))
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (categories.isEmpty()) {
                    Text(
                        text = "Nothing to show. In the app, tap Edit and switch on a category.",
                        style = BitsText.WidgetItem.copy(color = BitsColors.Muted),
                    )
                }
                categories.forEachIndexed { index, category ->
                    Text(
                        text = category.name.uppercase(),
                        style = BitsText.WidgetHeading.copy(color = accent),
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else 15.dp, bottom = 5.dp),
                    )
                    state.itemsIn(category.id).forEach { item ->
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            CheckVisual(checked = item.done, size = 17.dp)
                            Spacer(Modifier.width(9.dp))
                            Text(
                                item.text,
                                style = if (item.done) BitsText.WidgetItem.copy(color = done, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                else BitsText.WidgetItem,
                            )
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_game_controller),
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp).size(18.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Bits",
                    style = BitsText.WidgetHeading.copy(color = BitsColors.Muted),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_tune),
                    contentDescription = null,
                    tint = BitsColors.Muted,
                    modifier = Modifier.padding(9.dp).size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun PreviewClock() {
    val context = LocalContext.current
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1000)
        }
    }
    val timePattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    Column(
        Modifier.fillMaxWidth().height(88.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(now.format(DateTimeFormatter.ofPattern(timePattern)), style = BitsText.WidgetClock)
        Text(
            text = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            style = BitsText.WidgetDate,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}
