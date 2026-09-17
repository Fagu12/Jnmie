import sys

with open("app/src/main/java/com/example/ui/screens/player/PlayerScreen.kt", "r") as f:
    content = f.read()

import_append = """import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
"""
if "import androidx.compose.ui.input.pointer.pointerInput" not in content:
    content = content.replace("import androidx.compose.ui.unit.sp\n", "import androidx.compose.ui.unit.sp\n" + import_append)


old_clickable = """            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                viewModel.toggleControls()
            }"""

new_clickable = """            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { viewModel.toggleControls() },
                    onDoubleTap = { offset ->
                        val width = size.width
                        if (offset.x < width / 2) {
                            viewModel.getPlayerController()?.seekBackward()
                        } else {
                            viewModel.getPlayerController()?.seekForward()
                        }
                    }
                )
            }"""

content = content.replace(old_clickable, new_clickable)

with open("app/src/main/java/com/example/ui/screens/player/PlayerScreen.kt", "w") as f:
    f.write(content)
