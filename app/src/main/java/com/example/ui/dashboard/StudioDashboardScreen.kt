package com.example.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.backup.WBuilderMimeFilters
import com.example.data.backup.rememberWBuilderExportLauncher
import com.example.data.backup.rememberWBuilderImportLauncher
import com.example.domain.model.CustomComponent
import com.example.domain.model.WidgetDimension
import com.example.domain.model.WidgetProject
import com.example.ui.backup.BackupRestoreViewModel
import com.example.ui.backup.BackupUiState
import com.example.ui.components.WidgetMiniPreview
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioDashboardScreen(
    dashboardViewModel: DashboardViewModel,
    backupViewModel: BackupRestoreViewModel,
    onOpenWidgetEditor: (String) -> Unit,
    onOpenComponentBuilder: (String) -> Unit
) {
    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val backupState by backupViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateWidgetDialog by remember { mutableStateOf(false) }
    var showCreateComponentDialog by remember { mutableStateOf(false) }
    var pendingExportProject by remember { mutableStateOf<WidgetProject?>(null) }

    val importLauncher = rememberWBuilderImportLauncher { uri ->
        backupViewModel.importArchive(uri)
    }

    val exportProjectLauncher = rememberWBuilderExportLauncher { uri ->
        val project = pendingExportProject
        if (project != null) {
            backupViewModel.exportProject(project, uri)
            pendingExportProject = null
        } else {
            backupViewModel.exportFullBackup(uri)
        }
    }

    LaunchedEffect(backupState) {
        when (val state = backupState) {
            is BackupUiState.ExportSuccess -> {
                snackbarHostState.showSnackbar(
                    "Exported ${state.summary.projectCount} widget(s) & ${state.summary.bundledAssetCount} asset(s)."
                )
                backupViewModel.clearStatus()
            }
            is BackupUiState.ImportSuccess -> {
                snackbarHostState.showSnackbar(
                    "Restored ${state.summary.importedProjects.size} widget(s) & ${state.summary.importedComponents.size} module(s)."
                )
                backupViewModel.clearStatus()
            }
            is BackupUiState.Error -> {
                snackbarHostState.showSnackbar("Backup error: ${state.message}")
                backupViewModel.clearStatus()
            }
            else -> Unit
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 680.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    title = {
                        Column {
                            Text(
                                text = "W builder",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "Visual Widget Studio",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        OutlinedButton(
                            onClick = { importLauncher.launch(WBuilderMimeFilters.archiveTypes) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("import_restore_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Import .wbuilder backup",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Import / Restore", maxLines = 1)
                        }

                        if (uiState.projects.isNotEmpty() || uiState.customComponents.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    pendingExportProject = null
                                    exportProjectLauncher.launch("WBuilder_Studio_Backup.wbuilder")
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("export_all_backup_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Archive,
                                    contentDescription = "Export full studio backup"
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        StudioTab.entries.forEach { tab ->
                            val selected = uiState.activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { dashboardViewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (tab == StudioTab.WIDGETS) {
                                            Icons.Rounded.Widgets
                                        } else {
                                            Icons.Rounded.DashboardCustomize
                                        },
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (uiState.activeTab == StudioTab.WIDGETS) {
                            showCreateWidgetDialog = true
                        } else {
                            showCreateComponentDialog = true
                        }
                    },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = {
                        Text(
                            if (uiState.activeTab == StudioTab.WIDGETS) {
                                "+ Create New Widget"
                            } else {
                                "+ New Component"
                            }
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("create_primary_fab")
                )
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.height(12.dp))
                        StudioTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = uiState.activeTab == tab,
                                onClick = { dashboardViewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (tab == StudioTab.WIDGETS) {
                                            Icons.Rounded.Widgets
                                        } else {
                                            Icons.Rounded.DashboardCustomize
                                        },
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Shared Studio Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = dashboardViewModel::updateSearchQuery,
                        placeholder = {
                            Text(
                                if (uiState.activeTab == StudioTab.WIDGETS) {
                                    "Search your widgets..."
                                } else {
                                    "Search custom components..."
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { dashboardViewModel.updateSearchQuery("") },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("studio_search_input")
                    )

                    when (uiState.activeTab) {
                        StudioTab.WIDGETS -> {
                            WidgetGalleryTab(
                                projects = uiState.filteredProjects,
                                selectedDimension = uiState.selectedDimensionFilter,
                                onSelectDimension = dashboardViewModel::selectDimensionFilter,
                                onEditProject = { onOpenWidgetEditor(it.id) },
                                onExportProject = { project ->
                                    pendingExportProject = project
                                    val cleanName = project.name
                                        .replace(Regex("[^A-Za-z0-9._-]"), "_")
                                        .ifBlank { "widget" }
                                    exportProjectLauncher.launch("$cleanName.wbuilder")
                                },
                                onDuplicateProject = dashboardViewModel::duplicateProject,
                                onDeleteProject = { dashboardViewModel.deleteProject(it.id) },
                                onCreateWidgetClick = { showCreateWidgetDialog = true },
                                onImportClick = { importLauncher.launch(WBuilderMimeFilters.archiveTypes) }
                            )
                        }
                        StudioTab.COMPONENTS -> {
                            CustomComponentLibraryTab(
                                components = uiState.filteredComponents,
                                categories = uiState.availableCategories,
                                selectedCategory = uiState.selectedComponentCategory,
                                onSelectCategory = dashboardViewModel::selectComponentCategory,
                                onOpenComponentBuilder = { onOpenComponentBuilder(it.id) },
                                onUseInNewWidget = { component ->
                                    dashboardViewModel.createProjectFromComponent(component) { newId ->
                                        onOpenWidgetEditor(newId)
                                    }
                                },
                                onDuplicateComponent = dashboardViewModel::duplicateComponent,
                                onDeleteComponent = dashboardViewModel::deleteComponent,
                                onCreateNewComponent = { showCreateComponentDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateWidgetDialog) {
        CreateWidgetDialog(
            onDismiss = { showCreateWidgetDialog = false },
            onCreate = { name, dimension, customW, customH, starter, transparent ->
                showCreateWidgetDialog = false
                dashboardViewModel.createNewProject(
                    name = name,
                    dimension = dimension,
                    customWidthDp = customW,
                    customHeightDp = customH,
                    includeStarterClock = starter,
                    transparentBackground = transparent
                ) { projectId ->
                    onOpenWidgetEditor(projectId)
                }
            }
        )
    }

    if (showCreateComponentDialog) {
        CreateCustomComponentDialog(
            onDismiss = { showCreateComponentDialog = false },
            onCreate = { name, category, w, h ->
                showCreateComponentDialog = false
                dashboardViewModel.createNewCustomComponent(
                    name = name,
                    category = category,
                    widthDp = w,
                    heightDp = h
                ) { componentId ->
                    onOpenComponentBuilder(componentId)
                }
            }
        )
    }
}

@Composable
private fun WidgetGalleryTab(
    projects: List<WidgetProject>,
    selectedDimension: WidgetDimension?,
    onSelectDimension: (WidgetDimension?) -> Unit,
    onEditProject: (WidgetProject) -> Unit,
    onExportProject: (WidgetProject) -> Unit,
    onDuplicateProject: (WidgetProject) -> Unit,
    onDeleteProject: (WidgetProject) -> Unit,
    onCreateWidgetClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedDimension == null,
                    onClick = { onSelectDimension(null) },
                    label = { Text("All Sizes") },
                    modifier = Modifier.testTag("filter_dimension_all")
                )
            }
            items(WidgetDimension.entries) { dim ->
                FilterChip(
                    selected = selectedDimension == dim,
                    onClick = { onSelectDimension(if (selectedDimension == dim) null else dim) },
                    label = { Text(dim.label) },
                    modifier = Modifier.testTag("filter_dimension_${dim.name.lowercase()}")
                )
            }
        }

        if (projects.isEmpty()) {
            EmptyStudioHero(
                onCreateWidgetClick = onCreateWidgetClick,
                onImportClick = onImportClick
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 270.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 96.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(projects, key = { it.id }) { project ->
                    WidgetProjectGalleryCard(
                        project = project,
                        onEdit = { onEditProject(project) },
                        onExport = { onExportProject(project) },
                        onDuplicate = { onDuplicateProject(project) },
                        onDelete = { onDeleteProject(project) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStudioHero(
    onCreateWidgetClick: () -> Unit,
    onImportClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_studio_hero_1794769000647),
                    contentDescription = "W builder studio workspace banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(20.dp))
                )

                Text(
                    text = "Design Your Own Home Screen",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Build pixel-accurate widgets from scratch with clocks, live weather, telemetry, AI artwork, and custom XML — or import a .wbuilder backup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCreateWidgetClick,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("empty_create_widget_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Create New Widget", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = onImportClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("empty_import_button")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Import")
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetProjectGalleryCard(
    project: WidgetProject,
    onEdit: () -> Unit,
    onExport: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val dateLabel = remember(project.updatedAt) {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(project.updatedAt))
    }

    ElevatedCard(
        onClick = onEdit,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("widget_project_card_${project.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WidgetMiniPreview(
                        project = project,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${project.layers.size} layers · Updated $dateLabel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = project.dimension.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("edit_widget_button_${project.id}")
                ) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit Studio")
                }

                IconButton(
                    onClick = onExport,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("export_widget_button_${project.id}")
                ) {
                    Icon(Icons.Outlined.FileUpload, contentDescription = "Export .wbuilder")
                }

                IconButton(
                    onClick = onDuplicate,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("duplicate_widget_button_${project.id}")
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Duplicate widget")
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_widget_button_${project.id}")
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete widget",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
