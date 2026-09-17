import sys

with open("app/src/main/java/com/example/player/Media3PlayerManager.kt", "r") as f:
    content = f.read()

state_update = """        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val isEnded = playbackState == Player.STATE_ENDED
            val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
            _uiState.update {
                it.copy(
                    isBuffering = isBuffering,
                    isEnded = isEnded,
                    durationMs = duration,
                    error = null
                )
            }
        }"""

content = content.replace("""        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L
            _uiState.update {
                it.copy(
                    isBuffering = isBuffering,
                    durationMs = duration,
                    error = null
                )
            }
        }""", state_update)

with open("app/src/main/java/com/example/player/Media3PlayerManager.kt", "w") as f:
    f.write(content)
