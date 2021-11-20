package bobko.todomail.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Spinner(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = label,
        onValueChange = {},
        enabled = false,
        modifier = modifier.clickable(enabled = onClick != null) {
            onClick?.invoke()
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = TextFieldDefaults.outlinedTextFieldColors(
            disabledLeadingIconColor = MaterialTheme.colors.onBackground,
            disabledTextColor = MaterialTheme.colors.onBackground,
            disabledTrailingIconColor = MaterialTheme.colors.onBackground
        )
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TextDivider(text: String) {
    ListItem(modifier = Modifier.height(32.dp)) {
        Text(text, color = MaterialTheme.colors.primary, style = MaterialTheme.typography.subtitle2)
    }
}
