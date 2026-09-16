package com.example.ui.screens.extensions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Extension
import com.example.domain.model.ExtensionCapability
import com.example.domain.model.Repository
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.BadgeNsfw
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    viewModel: ExtensionsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val installedIds = remember(uiState.installedExtensions) {
        uiState.installedExtensions.map { it.id }.toSet()
    }

    val filteredInstalled = remember(uiState.installedExtensions, uiState.query, uiState.filterTag) {
        uiState.installedExtensions.filter { ext ->
            val matchesQuery = ext.name.contains(uiState.query, ignoreCase = true) ||
                    ext.language.contains(uiState.query, ignoreCase = true) ||
                    ext.author.contains(uiState.query, ignoreCase = true) ||
                    ext.description?.contains(uiState.query, ignoreCase = true) == true
            val matchesFilter = when (uiState.filterTag) {
                "Updates" -> ext.hasUpdate
                "Fast" -> ext.id.contains("pahe") || ext.id.contains("neko")
                "Dual Audio" -> ext.capabilities.contains(ExtensionCapability.MULTI_AUDIO) || ext.id.contains("kage") || ext.id.contains("koto")
                "Installed" -> true
                "Available" -> false
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    val filteredAvailable = remember(uiState.availableExtensions, installedIds, uiState.query, uiState.filterTag) {
        uiState.availableExtensions.filter { ext ->
            val notInstalled = ext.id !in installedIds
            val matchesQuery = ext.name.contains(uiState.query, ignoreCase = true) ||
                    ext.language.contains(uiState.query, ignoreCase = true) ||
                    ext.author.contains(uiState.query, ignoreCase = true) ||
                    ext.description?.contains(uiState.query, ignoreCase = true) == true
            val matchesFilter = when (uiState.filterTag) {
                "Updates" -> false
                "Fast" -> ext.id.contains("pahe") || ext.id.contains("neko")
                "Dual Audio" -> ext.capabilities.contains(ExtensionCapability.MULTI_AUDIO) || ext.id.contains("kage") || ext.id.contains("koto")
                "Installed" -> false
                "Available" -> true
                else -> true
            }
            notInstalled && matchesQuery && matchesFilter
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar with Settings > Extensions/Repositories Breadcrumb
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("extensions_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Settings",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Settings",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(10.dp).padding(horizontal = 2.dp)
                            )
                            Text(
                                text = "Extensions & Repositories",
                                color = AnimePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = if (uiState.selectedSection == "Repositories") "Repository Management" else "Anime Scraper Add-ons",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.refreshAllRepositories() },
                        modifier = Modifier.testTag("refresh_repositories_button")
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                color = AnimePrimary,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Refresh Repositories",
                                tint = AnimePrimary
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.setAddRepoDialogVisible(true) },
                        modifier = Modifier.testTag("add_repository_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Repository",
                            tint = AnimePrimary
                        )
                    }
                }
            }

            // Segmented Section Tabs: "Repositories" vs "Extensions"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                val isRepo = uiState.selectedSection == "Repositories"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isRepo) AnimePrimary else Color.Transparent)
                        .clickable { viewModel.setSelectedSection("Repositories") }
                        .padding(vertical = 10.dp)
                        .testTag("tab_repositories"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Source,
                            contentDescription = null,
                            tint = if (isRepo) Color.Black else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Repositories (${uiState.repositories.size})",
                            color = if (isRepo) Color.Black else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                val isExt = uiState.selectedSection == "Extensions"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isExt) AnimePrimary else Color.Transparent)
                        .clickable { viewModel.setSelectedSection("Extensions") }
                        .padding(vertical = 10.dp)
                        .testTag("tab_extensions"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Extension,
                            contentDescription = null,
                            tint = if (isExt) Color.Black else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Extensions (${uiState.installedExtensions.size})",
                            color = if (isExt) Color.Black else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.totalUpdatesCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isExt) Color.Black else AnimePrimary)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${uiState.totalUpdatesCount}",
                                    color = if (isExt) AnimePrimary else Color.Black,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Error Banner if present
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.error?.let { err ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF3B1E22))
                            .border(1.dp, Color(0xFFE57373), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFFF8A80),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = err,
                            color = Color(0xFFFFCDD2),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(
                            onClick = { viewModel.retryLoading() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Retry", color = AnimePrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // SECTION 1: REPOSITORIES VIEW
            if (uiState.selectedSection == "Repositories") {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("repositories_list"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp, top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Overview Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Security,
                                        contentDescription = null,
                                        tint = AnimePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Secure Anime Repositories",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (uiState.isRefreshing) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            color = AnimePrimary,
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Syncing...", color = AnimePrimary, fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Configured Git manifests for discovering, installing, and updating anime streaming scrapers. All extensions execute within a code-safe sandbox with zero in-process arbitrary binary loading.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { viewModel.setAddRepoDialogVisible(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("add_repo_cta_button")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Repository", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.refreshAllRepositories() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B1D30)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("refresh_all_repos_button")
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = AnimePrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Sync Repos", color = AnimePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Repository List Items
                    items(uiState.repositories) { repo ->
                        RepositoryCard(
                            repo = repo,
                            isSyncing = uiState.refreshingRepoUrl == repo.url || uiState.isRefreshing,
                            onSync = { viewModel.refreshRepository(repo.url) },
                            onRemove = { viewModel.setRepoToDelete(repo) }
                        )
                    }

                    if (uiState.repositories.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Source, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("No repositories configured", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Add a Git repository URL above to discover anime sources.", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // SECTION 2: EXTENSIONS VIEW
                // Search Input
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.setQuery(it) },
                    placeholder = { Text("Search anime scrapers & providers...", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = AnimePrimary
                        )
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AnimePrimary,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .testTag("extension_search_input")
                )

                // Filter Tag Chips (Strictly anime categories)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tags = listOf(
                        "All",
                        "Installed",
                        "Available",
                        if (uiState.totalUpdatesCount > 0) "Updates (${uiState.totalUpdatesCount})" else "Updates",
                        "Fast",
                        "Dual Audio"
                    )
                    items(tags) { tag ->
                        val cleanTag = if (tag.startsWith("Updates")) "Updates" else tag
                        val isSelected = uiState.filterTag == cleanTag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AnimePrimary else DarkSurfaceVariant)
                                .border(1.dp, if (isSelected) AnimePrimary else DarkCardBorder, RoundedCornerShape(12.dp))
                                .clickable { viewModel.setFilterTag(cleanTag) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .testTag("filter_tag_$cleanTag")
                        ) {
                            Text(
                                text = tag,
                                color = if (isSelected) Color.Black else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("extensions_list"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp, top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Installed Extensions Group
                    if (filteredInstalled.isNotEmpty() && uiState.filterTag != "Available") {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Installed Anime Extensions (${filteredInstalled.size})",
                                    color = AnimePrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                if (uiState.totalUpdatesCount > 0) {
                                    TextButton(
                                        onClick = { viewModel.updateAllExtensions() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("update_all_extensions_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.SystemUpdate,
                                            contentDescription = null,
                                            tint = AnimePrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Update All (${uiState.totalUpdatesCount})",
                                            color = AnimePrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        items(filteredInstalled) { ext ->
                            ExtensionCard(
                                extension = ext,
                                isInstalled = true,
                                isUpdating = uiState.updatingExtensionId == ext.id,
                                onToggle = { isEnabled -> viewModel.toggleExtension(ext.id, isEnabled) },
                                onInfo = { viewModel.selectExtension(ext) },
                                onUpdate = { viewModel.updateExtension(ext.id) },
                                onUninstall = { viewModel.uninstallExtension(ext.id) },
                                onInstall = {}
                            )
                        }
                    }

                    // Available Extensions Group
                    if (filteredAvailable.isNotEmpty() && uiState.filterTag != "Installed" && uiState.filterTag != "Updates") {
                        item {
                            Text(
                                text = "Available Community Extensions (${filteredAvailable.size})",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                            )
                        }

                        items(filteredAvailable) { ext ->
                            ExtensionCard(
                                extension = ext,
                                isInstalled = false,
                                isUpdating = false,
                                onToggle = {},
                                onInfo = { viewModel.selectExtension(ext) },
                                onUpdate = {},
                                onUninstall = {},
                                onInstall = { viewModel.installExtension(ext) }
                            )
                        }
                    }

                    // Empty state
                    if (filteredInstalled.isEmpty() && filteredAvailable.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Extension,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No extensions found",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Try clearing search filters or refreshing repositories",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Toast / Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }

    // Add Repository Dialog with real-time URL validation
    if (uiState.showAddRepoDialog) {
        AddRepositoryDialog(
            isAdding = uiState.isAddingRepo,
            validationError = uiState.urlValidationError,
            onDismiss = { viewModel.setAddRepoDialogVisible(false) },
            onAdd = { url -> viewModel.addRepository(url) },
            onValidate = { url -> viewModel.validateUrl(url) }
        )
    }

    // Remove Repository Confirmation Dialog
    uiState.repoToDelete?.let { repo ->
        AlertDialog(
            onDismissRequest = { viewModel.setRepoToDelete(null) },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Remove Repository?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to remove \"${repo.name}\"?",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Installed extensions from this repository will remain installed on your device until manually uninstalled.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRemoveRepository() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_remove_repo_button")
                ) {
                    Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setRepoToDelete(null) }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Extension Detail Modal Bottom Sheet
    uiState.selectedExtension?.let { ext ->
        val isInstalled = installedIds.contains(ext.id)
        ExtensionDetailBottomSheet(
            extension = ext,
            isInstalled = isInstalled,
            onDismiss = { viewModel.selectExtension(null) },
            onToggle = { isEnabled -> viewModel.toggleExtension(ext.id, isEnabled) },
            onUpdate = {
                viewModel.updateExtension(ext.id)
                viewModel.selectExtension(null)
            },
            onInstall = {
                viewModel.installExtension(ext)
                viewModel.selectExtension(null)
            },
            onUninstall = {
                viewModel.uninstallExtension(ext.id)
                viewModel.selectExtension(null)
            }
        )
    }
}

@Composable
private fun RepositoryCard(
    repo: Repository,
    isSyncing: Boolean,
    onSync: () -> Unit,
    onRemove: () -> Unit
) {
    val dateStr = remember(repo.lastRefreshed) {
        val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        sdf.format(Date(repo.lastRefreshed))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("repo_card_${repo.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF161726)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Source,
                contentDescription = null,
                tint = AnimePrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = repo.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1B3828))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Git: ${repo.branch}",
                        color = Color(0xFF4CAF50),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${repo.extensionCount} extensions • Synced $dateStr",
                color = TextSecondary,
                fontSize = 11.sp
            )

            Text(
                text = repo.url,
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onSync,
                modifier = Modifier.size(36.dp).testTag("sync_repo_${repo.name}")
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        color = AnimePrimary,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Sync Repository",
                        tint = AnimePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(36.dp).testTag("remove_repo_${repo.name}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Remove repository",
                    tint = Color(0xFFFF5252).copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ExtensionCard(
    extension: Extension,
    isInstalled: Boolean,
    isUpdating: Boolean,
    onToggle: (Boolean) -> Unit,
    onInfo: () -> Unit,
    onUpdate: () -> Unit,
    onUninstall: () -> Unit,
    onInstall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant)
            .border(
                1.dp,
                if (extension.hasUpdate) AnimePrimary.copy(alpha = 0.5f) else DarkCardBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onInfo)
            .padding(14.dp)
            .testTag("extension_card_${extension.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF161726)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Extension,
                contentDescription = null,
                tint = if (isInstalled && extension.isEnabled) AnimePrimary else TextMuted,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = extension.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (extension.hasUpdate) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AnimePrimary)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "UPDATE",
                            color = Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (extension.isNsfw) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BadgeNsfw.copy(alpha = 0.2f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "18+",
                            color = BadgeNsfw,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = extension.language,
                        color = TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = if (extension.hasUpdate) "${extension.version} → ${extension.latestVersion}" else extension.version,
                    color = if (extension.hasUpdate) AnimePrimary else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = if (extension.hasUpdate) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "• ${extension.author}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onInfo,
                modifier = Modifier.size(34.dp).testTag("info_ext_${extension.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "View Details",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (isInstalled) {
                if (extension.hasUpdate) {
                    IconButton(
                        onClick = onUpdate,
                        modifier = Modifier.size(34.dp).testTag("update_ext_${extension.id}")
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                color = AnimePrimary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdate,
                                contentDescription = "Update Extension",
                                tint = AnimePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Switch(
                    checked = extension.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AnimePrimary
                    ),
                    modifier = Modifier.testTag("extension_toggle_${extension.id}")
                )
            } else {
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("install_ext_${extension.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudDownload,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Install", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionDetailBottomSheet(
    extension: Extension,
    isInstalled: Boolean,
    onDismiss: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onUpdate: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .testTag("extension_detail_bottom_sheet")
        ) {
            // Header with Icon & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF161726)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Extension,
                        contentDescription = null,
                        tint = AnimePrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = extension.name,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ID: ${extension.id} • ${extension.version}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (extension.isNsfw) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BadgeNsfw.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "NSFW 18+",
                            color = BadgeNsfw,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = DarkCardBorder)
            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = "About Extension",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = extension.description ?: "Decoupled anime streaming and scraper provider.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Extension Capabilities Badges
            Text(
                text = "Capabilities & Decoupled APIs",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                extension.capabilities.forEach { cap ->
                    CapabilityBadge(cap)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details metadata table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow(label = "Base Host", value = extension.baseUrl)
                DetailRow(label = "Author", value = extension.author)
                DetailRow(label = "Language", value = extension.language)
                DetailRow(label = "Supported Streams", value = extension.supportedQualities.joinToString(", "))
                DetailRow(label = "Security Mode", value = "Sandboxed Engine (Zero DEX)")
                DetailRow(label = "Update Status", value = if (extension.hasUpdate) "Update Available (${extension.latestVersion})" else "Up to date")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Actions
            if (isInstalled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (extension.hasUpdate) {
                        Button(
                            onClick = onUpdate,
                            colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("sheet_update_button")
                        ) {
                            Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onUninstall,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331C1C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("sheet_uninstall_button")
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Uninstall", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onInstall,
                    colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("sheet_install_button")
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Install Extension", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CapabilityBadge(capability: ExtensionCapability) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1B2238))
            .border(1.dp, Color(0xFF2C395B), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (capability) {
                    ExtensionCapability.SEARCH -> Icons.Filled.Search
                    ExtensionCapability.DETAILS -> Icons.Filled.Info
                    ExtensionCapability.EPISODES -> Icons.Filled.Folder
                    ExtensionCapability.STREAM_SOURCES -> Icons.Filled.Source
                    ExtensionCapability.AUTO_SKIP_SEGMENTS -> Icons.Filled.FastForward
                    ExtensionCapability.MULTI_SUBTITLES -> Icons.Filled.Language
                    ExtensionCapability.MULTI_AUDIO -> Icons.Filled.CheckCircle
                    ExtensionCapability.DIRECT_DOWNLOAD -> Icons.Filled.CloudDownload
                },
                contentDescription = null,
                tint = AnimePrimary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = capability.displayName,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextMuted, fontSize = 11.sp)
        Text(text = value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun AddRepositoryDialog(
    isAdding: Boolean,
    validationError: String?,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onValidate: (String) -> String?
) {
    var urlInput by remember { mutableStateOf("") }
    var clientError by remember { mutableStateOf<String?>(null) }

    val presetRepos = listOf(
        "Official JustAnime Scraper Repo" to "https://raw.githubusercontent.com/justanime/anime-extensions/main/index.json",
        "Community Anime Providers" to "https://github.com/justanime/anime-extensions",
        "Dual-Audio Mirror Sources" to "https://github.com/justanime/dual-audio-repo"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Code, contentDescription = null, tint = AnimePrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Extension Repository", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column {
                Text(
                    text = "Enter a Git repository URL (e.g. GitHub, GitLab) or direct raw index JSON manifest:",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        clientError = if (it.isNotBlank()) onValidate(it) else null
                    },
                    placeholder = { Text("https://github.com/user/anime-repo", color = TextMuted) },
                    singleLine = true,
                    isError = clientError != null || validationError != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AnimePrimary,
                        unfocusedBorderColor = DarkCardBorder,
                        errorBorderColor = Color(0xFFFF5252),
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_repo_input")
                )

                // Inline validation feedback
                val displayError = clientError ?: validationError
                if (displayError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(displayError, color = Color(0xFFFF5252), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Quick Presets:", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                presetRepos.forEach { (label, presetUrl) ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .clickable {
                                urlInput = presetUrl
                                clientError = onValidate(presetUrl)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = AnimePrimary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val err = onValidate(urlInput)
                    if (err != null) {
                        clientError = err
                    } else if (urlInput.isNotBlank()) {
                        onAdd(urlInput.trim())
                    }
                },
                enabled = !isAdding && urlInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AnimePrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("dialog_confirm_add_repo")
            ) {
                if (isAdding) {
                    CircularProgressIndicator(
                        color = Color.Black,
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Validating...", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else {
                    Text("Add Repository", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isAdding) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
