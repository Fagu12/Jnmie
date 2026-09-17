import sys

with open("app/src/main/java/com/example/player/PlayerModels.kt", "r") as f:
    content = f.read()

content = content.replace("    val error: String? = null", "    val error: String? = null,\n    val isEnded: Boolean = false")

with open("app/src/main/java/com/example/player/PlayerModels.kt", "w") as f:
    f.write(content)
