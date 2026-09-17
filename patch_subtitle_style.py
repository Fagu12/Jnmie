import sys

with open("app/src/main/java/com/example/ui/screens/player/PlayerScreen.kt", "r") as f:
    content = f.read()

import_append = """import androidx.media3.ui.CaptionStyleCompat
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.util.TypedValue
"""
content = content.replace("import androidx.media3.ui.PlayerView\n", "import androidx.media3.ui.PlayerView\n" + import_append)

update_block = """                update = { view ->
                    view.resizeMode = when (playerState.resizeMode) {
                        ResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        ResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        ResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        ResizeMode.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    }
                    state.settings?.let { settings ->
                        view.subtitleView?.let { subtitleView ->
                            val typeface = when (settings.subFontFamily) {
                                "Sans Serif" -> Typeface.SANS_SERIF
                                "Serif" -> Typeface.SERIF
                                "Monospace" -> Typeface.MONOSPACE
                                else -> Typeface.DEFAULT
                            }
                            val textStyle = if (settings.subFontWeight == "Bold") Typeface.BOLD else Typeface.NORMAL
                            val finalTypeface = Typeface.create(typeface, textStyle)
                            
                            val textColor = when (settings.subTextColor) {
                                "Yellow" -> AndroidColor.YELLOW
                                "Cyan" -> AndroidColor.CYAN
                                "Green" -> AndroidColor.GREEN
                                "Magenta" -> AndroidColor.MAGENTA
                                "Red" -> AndroidColor.RED
                                "Blue" -> AndroidColor.BLUE
                                else -> AndroidColor.WHITE
                            }
                            
                            val edgeType = when (settings.subOutlineStyle) {
                                "Shadow" -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
                                "Outline" -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
                                else -> CaptionStyleCompat.EDGE_TYPE_NONE
                            }
                            
                            val bgColor = when (settings.subBackgroundStyle) {
                                "Semi-transparent" -> AndroidColor.argb((255 * settings.subBackgroundOpacity).toInt(), 0, 0, 0)
                                "Solid" -> AndroidColor.BLACK
                                else -> AndroidColor.TRANSPARENT
                            }
                            
                            val captionStyle = CaptionStyleCompat(
                                textColor,
                                bgColor,
                                AndroidColor.TRANSPARENT,
                                edgeType,
                                AndroidColor.BLACK,
                                finalTypeface
                            )
                            subtitleView.setStyle(captionStyle)
                            
                            val sizeMultiplier = when (settings.subFontSize) {
                                "Small" -> 0.8f
                                "Large" -> 1.2f
                                "Extra Large" -> 1.5f
                                else -> 1.0f
                            }
                            subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * sizeMultiplier)
                            
                            val bottomFraction = if (settings.subPosition == "Slightly above bottom") 0.15f else 0.05f
                            subtitleView.setBottomPaddingFraction(bottomFraction)
                        }
                    }
                }"""

content = content.replace("""                update = { view ->
                    view.resizeMode = when (playerState.resizeMode) {
                        ResizeMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        ResizeMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        ResizeMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        ResizeMode.FIXED_16_9 -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    }
                }""", update_block)

with open("app/src/main/java/com/example/ui/screens/player/PlayerScreen.kt", "w") as f:
    f.write(content)
