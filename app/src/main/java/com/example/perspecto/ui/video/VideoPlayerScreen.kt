package com.example.perspecto.ui.video

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class) @ExperimentalMaterial3Api
@Composable
fun VideoPlayerScreen(
    videoId: String,
    annotationId: String? = null,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var annotations by remember { mutableStateOf<List<com.example.perspecto.data.model.Annotation>>(emptyList()) }
    var activeAnnotationId by remember { mutableStateOf(annotationId) }
    var videoUrl by remember { mutableStateOf<String?>(null) }
    var videoName by remember { mutableStateOf("Video Player") }
    var isAnnotationsLoading by remember { mutableStateOf(true) }
    val repository = remember { com.example.perspecto.data.repository.VideoRepository() }
    
    // State for adding annotation
    var showBottomSheet by remember { mutableStateOf(false) }
    var newAnnotationContent by remember { mutableStateOf("") }
    var newAnnotationPriority by remember { mutableStateOf("Low") }
    var currentTimestamp by remember { mutableStateOf(0.0) }
    var currentFrame by remember { mutableStateOf<Int?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var revealedAnnotationId by remember { mutableStateOf<String?>(null) }

    // State for sharing
    var showShareSheet by remember { mutableStateOf(false) }
    var isPublic by remember { mutableStateOf(false) }
    var allowAnnotations by remember { mutableStateOf(false) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // Fetch video URL and annotations
    LaunchedEffect(videoId) {
        try {
            val videos = repository.getUserVideos()
            val video = videos.find { it.id == videoId }
            videoUrl = video?.url
            videoName = video?.title?.ifEmpty { video.name } ?: "Video Player"
            isPublic = video?.isPublic ?: false
            allowAnnotations = video?.allowAnnotations ?: false
            
            annotations = repository.getAnnotations(videoId)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isAnnotationsLoading = false
        }
    }

    // Seek to annotation when player is ready and annotations are loaded
    LaunchedEffect(exoPlayer, annotations, annotationId) {
        if (exoPlayer != null && annotations.isNotEmpty() && annotationId != null) {
            val annotation = annotations.find { it.id == annotationId }
            if (annotation != null) {
                activeAnnotationId = annotationId
                val fps = exoPlayer?.videoFormat?.frameRate ?: 0f
                if (annotation.startFrame != null && fps > 0) {
                    val timeMs = (annotation.startFrame.toFloat() / fps * 1000).toLong()
                    exoPlayer?.seekTo(timeMs)
                } else {
                    exoPlayer?.seekTo((annotation.timestamp * 1000).toLong())
                }
            }
        }
    }

    DisposableEffect(context, videoUrl) {
        if (videoUrl != null) {
            val player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUrl!!))
                prepare()
                playWhenReady = false
            }
            exoPlayer = player
        }

        onDispose {
            exoPlayer?.release()
            exoPlayer = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = videoName,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Share,
                            contentDescription = "Share"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = {
                    if (exoPlayer != null) {
                        currentTimestamp = exoPlayer!!.currentPosition / 1000.0
                        val fps = exoPlayer?.videoFormat?.frameRate ?: 0f
                        if (fps > 0) {
                            currentFrame = (currentTimestamp * fps).toInt()
                        } else {
                            currentFrame = null
                        }
                    }
                    newAnnotationContent = ""
                    newAnnotationPriority = "Low"
                    showBottomSheet = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Annotation") },
                text = { Text("Add Annotation") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.33f)
            ) {
                if (exoPlayer != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                controllerAutoShow = false
                            }
                        },
                        update = { playerView ->
                            playerView.player = exoPlayer
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Loading or error state for player
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                }
            }
            
            // Annotations List
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Annotations",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (isAnnotationsLoading) {
                    items(3) {
                        com.example.perspecto.ui.components.AnnotationCardSkeleton()
                    }
                } else {
                    items(annotations) { annotation ->
                        AnnotationCard(
                            annotation = annotation,
                            isActive = annotation.id == activeAnnotationId,
                            isRevealed = revealedAnnotationId == annotation.id,
                            onReveal = { revealedAnnotationId = annotation.id },
                            onCollapse = {
                                if (revealedAnnotationId == annotation.id) {
                                    revealedAnnotationId = null
                                }
                            },
                            onClick = {
                                revealedAnnotationId = null // Close any revealed item on click
                                activeAnnotationId = annotation.id
                                if (exoPlayer != null) {
                                    val fps = exoPlayer?.videoFormat?.frameRate ?: 0f
                                    if (annotation.startFrame != null && fps > 0) {
                                        val timeMs = (annotation.startFrame.toFloat() / fps * 1000).toLong()
                                        exoPlayer?.seekTo(timeMs)
                                    } else {
                                        exoPlayer?.seekTo((annotation.timestamp * 1000).toLong())
                                    }
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    try {
                                        repository.deleteAnnotation(annotation.id)
                                        annotations = annotations.filter { it.id != annotation.id }
                                        if (activeAnnotationId == annotation.id) {
                                            activeAnnotationId = null
                                        }
                                        if (revealedAnnotationId == annotation.id) {
                                            revealedAnnotationId = null
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Annotation",
                    style = MaterialTheme.typography.titleLarge
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "%.2fs".format(currentTimestamp))
                    
                    if (currentFrame != null) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Frame $currentFrame")
                    }
                }

                OutlinedTextField(
                    value = newAnnotationContent,
                    onValueChange = { newAnnotationContent = it },
                    label = { Text("Annotation") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                // Priority Dropdown
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = newAnnotationPriority,
                        onValueChange = { },
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("Low", "Medium", "High").forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(priority) },
                                onClick = {
                                    newAnnotationPriority = priority
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            val newAnnotation = com.example.perspecto.data.model.Annotation(
                                id = UUID.randomUUID().toString(),
                                videoId = videoId,
                                userId = "", // Will be set by repository
                                content = newAnnotationContent,
                                title = "$newAnnotationPriority Priority",
                                severity = newAnnotationPriority.lowercase(),
                                timestamp = currentTimestamp,
                                startFrame = currentFrame,
                                endFrame = currentFrame,
                                frame = currentFrame,
                                projectId = videoId
                            )
                            repository.addAnnotation(newAnnotation)
                            // Refresh annotations
                            annotations = repository.getAnnotations(videoId)
                            showBottomSheet = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Annotation")
                }
                Spacer(modifier = Modifier.fillMaxHeight(0.1f))
            }
        }
    }

    if (showShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Share Video",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Share via Link", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Anyone with the link can view", style = MaterialTheme.typography.bodySmall)
                    }
                    androidx.compose.material3.Switch(
                        checked = isPublic,
                        onCheckedChange = { checked ->
                            isPublic = checked
                            scope.launch {
                                repository.updateVideoSharing(videoId, isPublic, allowAnnotations)
                            }
                        }
                    )
                }

                if (isPublic) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Allow others to annotate", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Viewers can add annotations", style = MaterialTheme.typography.bodySmall)
                        }
                        androidx.compose.material3.Switch(
                            checked = allowAnnotations,
                            onCheckedChange = { checked ->
                                allowAnnotations = checked
                                scope.launch {
                                    repository.updateVideoSharing(videoId, isPublic, allowAnnotations)
                                }
                            }
                        )
                    }

                    OutlinedTextField(
                        value = "https://perspecto.ai?share=$videoId",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Share Link") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString("https://perspecto.ai?share=$videoId"))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link")
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.fillMaxHeight(0.1f))
            }
        }
    }
}


