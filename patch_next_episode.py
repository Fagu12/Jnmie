import sys

with open("app/src/main/java/com/example/ui/screens/player/PlayerScreen.kt", "r") as f:
    content = f.read()

next_ep_panel = """        // End of Episode Panel
        AnimatedVisibility(
            visible = playerState.isEnded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val nextEp = state.episodes.find { it.number == (state.episode?.number ?: 0) + 1 }
                    if (nextEp != null) {
                        var countdown by remember { mutableIntStateOf(5) }
                        val autoPlay = state.settings?.autoPlayNext ?: true
                        
                        LaunchedEffect(autoPlay, playerState.isEnded) {
                            if (autoPlay && playerState.isEnded) {
                                countdown = 5
                                while(countdown > 0) {
                                    delay(1000)
                                    countdown -= 1
                                }
                                viewModel.loadAnimeAndEpisode(nextEp.number)
                            }
                        }

                        Text("Next Episode", color = Color.LightGray, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Episode ${nextEp.number}: ${nextEp.title ?: ""}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        if (autoPlay) {
                            Text("Starting in $countdown seconds...", color = AnimePrimary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(onClick = { viewModel.loadAnimeAndEpisode(nextEp.number) }, colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary)) {
                                Text("Play Now", color = Color.Black)
                            }
                            OutlinedButton(onClick = onBack) {
                                Text("Cancel", color = Color.White)
                            }
                        }
                    } else {
                        Text("End of available episodes.", color = Color.White, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary)) {
                            Text("Go Back", color = Color.Black)
                        }
                    }
                }
            }
        }
        
        // Overlay Controls"""

content = content.replace("        // Overlay Controls", next_ep_panel)

with open("app/src/main/java/com/example/ui/screens/player/PlayerScreen.kt", "w") as f:
    f.write(content)
