package com.example.perspecto.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.perspecto.data.model.Annotation
import com.example.perspecto.data.repository.VideoRepository
import com.example.perspecto.ui.video.AnnotationCard
import com.example.perspecto.ui.components.AnnotationCardSkeleton

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.collectAsState

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AnnotationsScreen(
    viewModel: AnnotationsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onAnnotationClick: (String, String) -> Unit,
    onProfileClick: () -> Unit
) {
    val annotations by viewModel.sortedAndFilteredAnnotations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    var showSortSheet by remember { mutableStateOf(false) }
    var revealedAnnotationId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchAnnotations()
    }

    if (showSortSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showSortSheet = false }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sort by",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                AnnotationSortOption.values().forEach { option ->
                    androidx.compose.material3.ListItem(
                        headlineContent = {
                            Text(
                                text = when (option) {
                                    AnnotationSortOption.DATE_NEWEST -> "Date (Newest)"
                                    AnnotationSortOption.DATE_OLDEST -> "Date (Oldest)"
                                    AnnotationSortOption.SEVERITY_HIGH_LOW -> "Severity (High to Low)"
                                    AnnotationSortOption.SEVERITY_LOW_HIGH -> "Severity (Low to High)"
                                }
                            )
                        },
                        leadingContent = {
                            if (option == sortOption) {
                                androidx.compose.material3.Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                    contentDescription = "Selected"
                                )
                            }
                        },
                        modifier = androidx.compose.ui.Modifier.clickable {
                            viewModel.onSortOptionChange(option)
                            showSortSheet = false
                        }
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                com.example.perspecto.ui.components.CommonTopAppBar(
                    title = "All Annotations",
                    onProfileClick = onProfileClick
                )
                com.example.perspecto.ui.components.SearchAndFilterBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSortClick = { showSortSheet = true },
                    placeholder = "Search annotations..."
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                ) {
                    items(5) {
                        AnnotationCardSkeleton()
                    }
                }
            } else if (error != null) {
                Text(
                    text = "Error: $error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (annotations.isEmpty()) {
                Text(
                    text = "No annotations found.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                ) {
                    items(annotations) { annotation ->
                        AnnotationCard(
                            annotation = annotation,
                            isActive = false,
                            isRevealed = revealedAnnotationId == annotation.id,
                            onReveal = { revealedAnnotationId = annotation.id },
                            onCollapse = {
                                if (revealedAnnotationId == annotation.id) {
                                    revealedAnnotationId = null
                                }
                            },
                            onClick = {
                                revealedAnnotationId = null // Close any revealed item on click
                                annotation.videoId?.let { videoId ->
                                    onAnnotationClick(videoId, annotation.id)
                                }
                            },
                            onDelete = {
                                viewModel.deleteAnnotation(annotation.id)
                                if (revealedAnnotationId == annotation.id) {
                                    revealedAnnotationId = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
