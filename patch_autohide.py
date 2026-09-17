import sys

with open("app/src/main/java/com/example/ui/screens/player/PlayerScreen.kt", "r") as f:
    content = f.read()

effect_code = """    val playerController = viewModel.getPlayerController()
    val context = LocalContext.current
    
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(playerState.isControlsVisible, playerState.isPlaying, lastInteraction) {
        if (playerState.isControlsVisible && playerState.isPlaying && !playerState.isLocked) {
            delay(4000)
            viewModel.hideControls()
        }
    }"""

content = content.replace("""    val playerController = viewModel.getPlayerController()
    val context = LocalContext.current""", effect_code)

pointer_code = """            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        lastInteraction = System.currentTimeMillis()
                        viewModel.toggleControls() 
                    },
                    onDoubleTap = { offset ->
                        lastInteraction = System.currentTimeMillis()
                        val width = size.width
                        if (offset.x < width / 2) {
                            viewModel.getPlayerController()?.seekBackward()
                        } else {
                            viewModel.getPlayerController()?.seekForward()
                        }
                    }
                )
            }"""

content = content.replace("""            .pointerInput(Unit) {
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
            }""", pointer_code)

# Ensure interaction resets timer in the control overlay
# We can wrap the controls in pointerInput to catch touches or just rely on the existing.
# Wait, any button press in the controls should reset the timer? Yes, but buttons consume clicks.
# To make it simple, we can add a provider or just let the user tap empty space. 
# Or we can intercept touch events on the Box.
box_code = """            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { lastInteraction = System.currentTimeMillis() })
                    }
            ) {"""
content = content.replace("""            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x66000000))
            ) {""", box_code)

with open("app/src/main/java/com/example/ui/screens/player/PlayerScreen.kt", "w") as f:
    f.write(content)
