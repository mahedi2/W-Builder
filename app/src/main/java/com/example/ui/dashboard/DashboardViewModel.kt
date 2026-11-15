package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.CustomComponentRepository
import com.example.data.repository.WidgetRepository
import com.example.domain.model.CustomComponent
import com.example.domain.model.GradientConfig
import com.example.domain.model.LayerPayload
import com.example.domain.model.LayerStyle
import com.example.domain.model.LayerType
import com.example.domain.model.WidgetDimension
import com.example.domain.model.WidgetLayer
import com.example.domain.model.WidgetProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class StudioTab(val label: String) {
    WIDGETS("My Widgets"),
    COMPONENTS("My Custom Components")
}

data class DashboardUiState(
    val activeTab: StudioTab = StudioTab.WIDGETS,
    val searchQuery: String = "",
    val selectedDimensionFilter: WidgetDimension? = null,
    val selectedComponentCategory: String = "All",
    val projects: List<WidgetProject> = emptyList(),
    val filteredProjects: List<WidgetProject> = emptyList(),
    val customComponents: List<CustomComponent> = emptyList(),
    val filteredComponents: List<CustomComponent> = emptyList(),
    val availableCategories: List<String> = listOf("All")
)

class DashboardViewModel(
    private val widgetRepository: WidgetRepository,
    private val componentRepository: CustomComponentRepository
) : ViewModel() {

    private val activeTab = MutableStateFlow(StudioTab.WIDGETS)
    private val searchQuery = MutableStateFlow("")
    private val dimensionFilter = MutableStateFlow<WidgetDimension?>(null)
    private val categoryFilter = MutableStateFlow("All")

    val uiState: StateFlow<DashboardUiState> = combine(
        activeTab,
        searchQuery,
        dimensionFilter,
        categoryFilter,
        widgetRepository.observeAllProjects(),
        componentRepository.observeAllComponents()
    ) { values ->
        val tab = values[0] as StudioTab
        val query = (values[1] as String).trim()
        val dimFilter = values[2] as WidgetDimension?
        val catFilter = values[3] as String
        @Suppress("UNCHECKED_CAST")
        val projects = values[4] as List<WidgetProject>
        @Suppress("UNCHECKED_CAST")
        val components = values[5] as List<CustomComponent>

        val filteredProjects = projects.filter { project ->
            val matchesQuery = query.isEmpty() ||
                project.name.contains(query, ignoreCase = true) ||
                project.description.contains(query, ignoreCase = true)
            val matchesDim = dimFilter == null || project.dimension == dimFilter
            matchesQuery && matchesDim
        }

        val categories = listOf("All") + components
            .map { it.category.ifBlank { "General" } }
            .distinct()
            .sorted()

        val filteredComponents = components.filter { component ->
            val matchesQuery = query.isEmpty() ||
                component.name.contains(query, ignoreCase = true) ||
                component.category.contains(query, ignoreCase = true)
            val matchesCat = catFilter == "All" ||
                component.category.equals(catFilter, ignoreCase = true)
            matchesQuery && matchesCat
        }

        DashboardUiState(
            activeTab = tab,
            searchQuery = query,
            selectedDimensionFilter = dimFilter,
            selectedComponentCategory = catFilter,
            projects = projects,
            filteredProjects = filteredProjects,
            customComponents = components,
            filteredComponents = filteredComponents,
            availableCategories = categories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun selectTab(tab: StudioTab) {
        activeTab.value = tab
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectDimensionFilter(dimension: WidgetDimension?) {
        dimensionFilter.value = dimension
    }

    fun selectComponentCategory(category: String) {
        categoryFilter.value = category
    }

    fun createNewProject(
        name: String,
        dimension: WidgetDimension,
        customWidthDp: Float,
        customHeightDp: Float,
        includeStarterClock: Boolean,
        transparentBackground: Boolean = false,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val projectId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val starterLayers = if (includeStarterClock) {
                listOf(
                    WidgetLayer(
                        id = UUID.randomUUID().toString(),
                        projectId = projectId,
                        name = "Studio Clock",
                        type = LayerType.DIGITAL_CLOCK,
                        style = LayerStyle(
                            xDp = 16f,
                            yDp = 16f,
                            widthDp = 150f,
                            heightDp = 52f,
                            fontFamilyName = "Space Grotesk",
                            fontSizeSp = 34f,
                            foregroundColorHex = "#FFFFFFFF"
                        ),
                        payload = LayerPayload.DigitalClock(is24Hour = false)
                    ),
                    WidgetLayer(
                        id = UUID.randomUUID().toString(),
                        projectId = projectId,
                        name = "Today Date",
                        type = LayerType.DATE_TEXT,
                        style = LayerStyle(
                            xDp = 16f,
                            yDp = 68f,
                            widthDp = 160f,
                            heightDp = 28f,
                            fontFamilyName = "Plus Jakarta Sans",
                            fontSizeSp = 14f,
                            foregroundColorHex = "#FF5CE1E6"
                        ),
                        payload = LayerPayload.DateText()
                    )
                )
            } else {
                emptyList()
            }

            val project = WidgetProject(
                id = projectId,
                name = name.trim().ifBlank { "Untitled Widget" },
                dimension = dimension,
                customWidthDp = customWidthDp.coerceIn(80f, 500f),
                customHeightDp = customHeightDp.coerceIn(60f, 500f),
                backgroundColorHex = if (transparentBackground) "#00000000" else "#FF181C2E",
                backgroundGradient = GradientConfig(
                    enabled = !transparentBackground,
                    startColorHex = "#FF181C2E",
                    endColorHex = "#FF222A45",
                    angleDegrees = 135f
                ),
                backgroundCornerRadiusDp = 24f,
                createdAt = now,
                updatedAt = now,
                layers = starterLayers
            )
            widgetRepository.saveProject(project)
            onCreated(projectId)
        }
    }

    fun duplicateProject(project: WidgetProject) {
        viewModelScope.launch {
            val newId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val copy = project.copy(
                id = newId,
                name = "${project.name} Copy",
                createdAt = now,
                updatedAt = now,
                layers = project.layers.map {
                    it.copy(id = UUID.randomUUID().toString(), projectId = newId)
                }
            )
            widgetRepository.saveProject(copy)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            widgetRepository.deleteProject(projectId)
        }
    }

    fun createNewCustomComponent(
        name: String,
        category: String,
        widthDp: Float,
        heightDp: Float,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val component = CustomComponent(
                id = id,
                name = name.trim().ifBlank { "Custom Module" },
                category = category.trim().ifBlank { "General" },
                widthDp = widthDp.coerceIn(60f, 400f),
                heightDp = heightDp.coerceIn(40f, 400f),
                layers = listOf(
                    WidgetLayer(
                        id = UUID.randomUUID().toString(),
                        projectId = id,
                        name = "Label",
                        type = LayerType.CUSTOM_TEXT,
                        style = LayerStyle(
                            xDp = 12f,
                            yDp = 12f,
                            widthDp = 120f,
                            heightDp = 32f,
                            fontFamilyName = "Space Grotesk",
                            fontSizeSp = 15f
                        ),
                        payload = LayerPayload.CustomText(text = name.ifBlank { "Module" })
                    )
                ),
                createdAt = now,
                updatedAt = now
            )
            componentRepository.saveComponent(component)
            onCreated(id)
        }
    }

    fun createProjectFromComponent(
        component: CustomComponent,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val projectId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val wrappedLayer = WidgetLayer(
                id = UUID.randomUUID().toString(),
                projectId = projectId,
                name = component.name,
                type = LayerType.CUSTOM_COMPONENT_REF,
                style = LayerStyle(
                    xDp = 16f,
                    yDp = 16f,
                    widthDp = component.widthDp,
                    heightDp = component.heightDp,
                    paddingDp = 0f
                ),
                payload = LayerPayload.CustomComponentRef(
                    componentId = component.id,
                    componentName = component.name,
                    embeddedLayers = component.layers
                )
            )
            val project = WidgetProject(
                id = projectId,
                name = "${component.name} Widget",
                dimension = WidgetDimension.SIZE_4X2,
                createdAt = now,
                updatedAt = now,
                layers = listOf(wrappedLayer)
            )
            widgetRepository.saveProject(project)
            onCreated(projectId)
        }
    }

    fun duplicateComponent(component: CustomComponent) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newId = UUID.randomUUID().toString()
            componentRepository.saveComponent(
                component.copy(
                    id = newId,
                    name = "${component.name} Copy",
                    createdAt = now,
                    updatedAt = now,
                    layers = component.layers.map {
                        it.copy(id = UUID.randomUUID().toString(), projectId = newId)
                    }
                )
            )
        }
    }

    fun deleteComponent(componentId: String) {
        viewModelScope.launch {
            componentRepository.deleteComponent(componentId)
        }
    }

    companion object {
        fun provideFactory(
            widgetRepository: WidgetRepository,
            componentRepository: CustomComponentRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(widgetRepository, componentRepository)
            }
        }
    }
}
