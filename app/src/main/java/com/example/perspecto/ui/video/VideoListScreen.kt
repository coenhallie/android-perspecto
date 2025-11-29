package com.example.perspecto.ui.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.perspecto.ui.components.VideoCardSkeleton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    viewModel: VideoListViewModel = viewModel(),
    onVideoClick: (String) -> Unit, // Pass video URL or ID
    onProfileClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()

    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showSortSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    // Handle back press to navigate up folder hierarchy
    androidx.activity.compose.BackHandler(enabled = uiState.currentFolderName != null) {
        viewModel.onBackClick()
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
                SortOption.values().forEach { option ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = when (option) {
                                    SortOption.FILE_SIZE -> "File Size"
                                    SortOption.UPDATE_DATE -> "Update Date"
                                    SortOption.FILE_NAME -> "File Name"
                                }
                            )
                        },
                        leadingContent = {
                            if (option == sortOption) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                    contentDescription = "Selected"
                                )
                            }
                        },
                        modifier = Modifier.clickable {
                            viewModel.onSortOptionChange(option)
                            showSortSheet = false
                        }
                    )
                }
            }
        }
    }

    // DragValue enum is needed. It might be defined in AnnotationComponents.kt or we can define it here or reuse it.
    // If it's internal or private in AnnotationComponents, we need to redefine it or make it public.
    // Let's assume we can reuse it if it's in the same package, or define a local one if needed.
    // Checking imports... AnnotationComponents is in the same package.
    
    // We need to import DragValue if it's not top-level or if it's not imported.
    // It was defined at the bottom of AnnotationComponents.kt as `enum class DragValue { Start, End }`
    // Since they are in the same package `com.example.perspecto.ui.video`, it should be available.

    Scaffold(
        topBar = {
            Column {
                com.example.perspecto.ui.components.CommonTopAppBar(
                    title = uiState.currentFolderName ?: "My Videos",
                    onProfileClick = onProfileClick,
                    navigationIcon = if (uiState.currentFolderName != null) {
                        {
                            androidx.compose.material3.IconButton(onClick = { viewModel.onBackClick() }) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    } else null
                )
                com.example.perspecto.ui.components.SearchAndFilterBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSortClick = { showSortSheet = true },
                    placeholder = "Search videos..."
                )
            }
        }
    ) { innerPadding ->
        @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.fetchData(isRefresh = true) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading) {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(5) { // Show 5 skeleton items
                            VideoCardSkeleton()
                        }
                    }
                } else if (error != null) {
                    Text(
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Display Folders first
                        items(uiState.folders) { folderWithCount ->
                            FolderItem(
                                name = folderWithCount.folder.name,
                                count = folderWithCount.count,
                                onClick = { viewModel.onFolderClick(folderWithCount.folder.id) }
                            )
                        }
                        
                        // Display Videos
                        items(uiState.videos, key = { it.id }) { video ->
                            VideoItem(
                                name = video.title.ifEmpty { video.name }, // Use title if available, fallback to name
                                videoUrl = video.url,
                                annotationCount = video.annotationCount,
                                onClick = { onVideoClick(video.id) }, // Pass video.id (UUID) instead of videoId
                                onDelete = { viewModel.deleteVideo(video.id, video.url) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderItem(
    name: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Folder,
                contentDescription = "Folder",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            // Video Count Indicator
            if (count > 0) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun VideoItem(
    name: String,
    videoUrl: String,
    annotationCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = ImageLoader.Builder(context)
        .components {
            add(VideoFrameDecoder.Factory())
        }
        .build()

    val density = androidx.compose.ui.platform.LocalDensity.current
    
    val state = androidx.compose.runtime.remember {
        AnchoredDraggableState(
            initialValue = DragValue.Start,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = splineBasedDecay(density)
        )
    }

    val anchors = androidx.compose.runtime.remember(density) {
        DraggableAnchors {
            DragValue.Start at 0f
            DragValue.End at with(density) { -100.dp.toPx() } // Reveal width in px
        }
    }

    androidx.compose.runtime.LaunchedEffect(anchors) {
        state.updateAnchors(anchors)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Background (Delete Button)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.large)
                .clickable { onDelete() },
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Foreground (Card)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = state.offset.takeIf { !it.isNaN() }?.roundToInt() ?: 0, y = 0) }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal
                )
                .clickable { onClick() },
            shape = MaterialTheme.shapes.large, // Expressive shape
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp) // Fixed height to prevent it from being too tall
            ) {
                // Video Thumbnail Area (Left)
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(videoUrl)
                            .videoFrameMillis(1000) // Capture frame at 1 second
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = "Video Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Content Area (Right)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    AssistChip(
                        onClick = { /* No-op */ },
                        label = { Text("$annotationCount Annotations") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                modifier = Modifier.size(AssistChipDefaults.IconSize)
                            )
                        }
                    )
                }
            }
        }
    }
}
