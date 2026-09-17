import sys

with open("app/src/main/java/com/example/ui/screens/player/PlayerScreen.kt", "r") as f:
    content = f.read()

locked_overlay = """        // Overlay Controls
        PlayerSheets(viewModel, state)
        
        // Locked State Overlay
        AnimatedVisibility(
            visible = playerState.isLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = { viewModel.getPlayerController()?.toggleLock() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                ) {
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = "Unlock", tint = Color.White)
                }
            }
        }
        
        AnimatedVisibility(
            visible = playerState.isControlsVisible && playerState.error == null && !playerState.isLocked,"""

content = content.replace("""        // Overlay Controls
        PlayerSheets(viewModel, state)
        AnimatedVisibility(
            visible = playerState.isControlsVisible && playerState.error == null,""", locked_overlay)

top_bar_controls = """                        Text(
                            text = state.episode?.let { "Episode ${it.number}" } ?: "",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.getPlayerController()?.toggleLock() }) {
                        Icon(imageVector = Icons.Filled.LockOpen, contentDescription = "Lock", tint = Color.White)
                    }
                    IconButton(onClick = { viewModel.setSettingsSheetVisible(true) }) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }"""

content = content.replace("""                        Text(
                            text = state.episode?.let { "Episode ${it.number}" } ?: "",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.setSettingsSheetVisible(true) }) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }""", top_bar_controls)
                
with open("app/src/main/java/com/example/ui/screens/player/PlayerScreen.kt", "w") as f:
    f.write(content)
