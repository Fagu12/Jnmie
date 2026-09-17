import sys

with open("app/src/main/java/com/example/player/PlayerController.kt", "r") as f:
    content = f.read()

content = content.replace("    fun togglePlayPause()", "    fun togglePlayPause()\n    fun cycleResizeMode()\n    fun toggleLock()")

with open("app/src/main/java/com/example/player/PlayerController.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/player/Media3PlayerManager.kt", "r") as f:
    content2 = f.read()
    
impl = """    override fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }
    
    override fun cycleResizeMode() {
        val nextMode = when (_uiState.value.resizeMode) {
            ResizeMode.FIT -> ResizeMode.FILL
            ResizeMode.FILL -> ResizeMode.ZOOM
            ResizeMode.ZOOM -> ResizeMode.FIXED_16_9
            ResizeMode.FIXED_16_9 -> ResizeMode.FIT
        }
        _uiState.update { it.copy(resizeMode = nextMode) }
    }
    
    override fun toggleLock() {
        _uiState.update { it.copy(isLocked = !it.isLocked, isControlsVisible = !it.isLocked) }
    }"""

content2 = content2.replace("""    override fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }""", impl)

with open("app/src/main/java/com/example/player/Media3PlayerManager.kt", "w") as f:
    f.write(content2)
