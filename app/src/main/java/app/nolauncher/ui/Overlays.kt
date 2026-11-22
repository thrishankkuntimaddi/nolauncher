package app.nolauncher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nolauncher.data.AppEntry

@Composable
private fun Scrim(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg.copy(alpha = 0.94f))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss)
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun AppMenu(
    app: AppEntry,
    pinned: Boolean,
    homeFull: Boolean,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onHide: () -> Unit,
    onInfo: () -> Unit,
    onUninstall: () -> Unit,
) = Scrim(onDismiss) {
    Column(
        Modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Txt(app.label, size = 34.sp, weight = FontWeight.Bold, align = TextAlign.Center, maxLines = 2)
        if (app.isWork) Txt("Work profile", color = Palette.Muted, size = 14.sp)
        Spacer(Modifier.height(28.dp))
        when {
            pinned -> MenuItem("Remove from home", onTogglePin)
            homeFull -> Txt("Home is full", color = Palette.Faint, size = 20.sp, modifier = Modifier.padding(8.dp))
            else -> MenuItem("Keep on home", onTogglePin)
        }
        MenuItem("Rename", onRename)
        MenuItem("Hide", onHide)
        MenuItem("App info", onInfo)
        MenuItem("Uninstall", onUninstall)
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    Txt(
        text,
        size = 20.sp,
        color = Palette.Muted,
        modifier = Modifier.quietClick(onClick = onClick).padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
fun RenameDialog(
    app: AppEntry,
    originalLabel: String,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) = Scrim(onDismiss) {
    var value by remember { mutableStateOf(TextFieldValue(app.label, TextRange(0, app.label.length))) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            .clickable(remember { MutableInteractionSource() }, null) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Txt("Rename", color = Palette.Muted, size = 14.sp)
        Spacer(Modifier.height(12.dp))
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = TextStyle(
                color = Palette.Ink, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(Palette.Ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave(value.text) }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (app.label != originalLabel) MenuItem("Reset") { onSave(null) }
            MenuItem("Cancel", onDismiss)
            Txt(
                "Save", size = 20.sp, weight = FontWeight.SemiBold,
                modifier = Modifier.quietClick { onSave(value.text) }.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
    }
}
