package com.example.perspecto.ui.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perspecto.data.model.Video
import com.example.perspecto.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.async

class VideoListViewModel : ViewModel() {

    private val repository = VideoRepository()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.UPDATE_DATE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _videos = MutableStateFlow<List<Video>>(emptyList())
    private val _folders = MutableStateFlow<List<com.example.perspecto.data.model.Folder>>(emptyList())
    private val _projectFolders = MutableStateFlow<List<com.example.perspecto.data.model.ProjectFolder>>(emptyList())
    
    private val _currentFolderId = MutableStateFlow<String?>(null)
    val currentFolderId: StateFlow<String?> = _currentFolderId.asStateFlow()

    // Combined state for UI: List of items (either Folder or Video)
    val uiState: StateFlow<UiState> = kotlinx.coroutines.flow.combine(
        kotlinx.coroutines.flow.combine(_videos, _folders, _projectFolders) { v, f, pf -> Triple(v, f, pf) },
        _currentFolderId,
        _searchQuery,
        _sortOption
    ) { (videos, folders, projectFolders), currentFolderId, query, sort ->
        
        val filteredFolders: List<com.example.perspecto.data.model.Folder>
        val filteredVideos: List<Video>

        if (query.isBlank()) {
            // Normal navigation mode
            // 1. Filter folders based on current level
            filteredFolders = folders.filter { it.parentId == currentFolderId }
            
            // 2. Filter videos based on current level
            filteredVideos = if (currentFolderId == null) {
                // Root: Show videos that are NOT in any folder (or at least not in projectFolders for this user)
                val videosInFolders = projectFolders.map { it.projectId }.toSet()
                videos.filter { it.id !in videosInFolders }
            } else {
                // In a folder: Show videos linked to this folder
                val videosInThisFolder = projectFolders.filter { it.folderId == currentFolderId }.map { it.projectId }.toSet()
                videos.filter { it.id in videosInThisFolder }
            }
        } else {
            // Search mode - Global search
            // Show all matching folders and videos, ignoring hierarchy
            filteredFolders = folders.filter { it.name.contains(query, ignoreCase = true) }
            filteredVideos = videos.filter { it.title.contains(query, ignoreCase = true) }
        }

        // 4. Apply Sort (Only for videos for now, folders usually stay at top or sorted by name)
        val sortedVideos = when (sort) {
            SortOption.FILE_SIZE -> filteredVideos.sortedByDescending { it.fileSize ?: 0L }
            SortOption.UPDATE_DATE -> filteredVideos.sortedByDescending { it.updatedAt }
            SortOption.FILE_NAME -> filteredVideos.sortedBy { it.title }
        }
        
        val sortedFolders = filteredFolders.sortedBy { it.name }
        
        // Calculate video counts for each folder
        val folderCounts = sortedFolders.map { folder ->
            val count = projectFolders.count { it.folderId == folder.id }
            FolderWithCount(folder, count)
        }

        UiState(
            folders = folderCounts,
            videos = sortedVideos,
            currentFolderName = folders.find { it.id == currentFolderId }?.name
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = UiState()
    )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchData()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSortOptionChange(option: SortOption) {
        _sortOption.value = option
    }
    
    fun onFolderClick(folderId: String) {
        _currentFolderId.value = folderId
    }
    
    fun onBackClick() {
        val currentId = _currentFolderId.value
        if (currentId != null) {
            // Find parent of current folder to go back up one level
            // Or just go to root if we assume 1 level depth for now?
            // The folder structure supports nesting.
            val currentFolder = _folders.value.find { it.id == currentId }
            _currentFolderId.value = currentFolder?.parentId
        }
    }

    fun fetchData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            } else {
                _isLoading.value = true
            }
            _error.value = null
            try {
                // Fetch all data in parallel
                val videosDeferred = async { repository.getUserVideos() }
                val foldersDeferred = async { repository.getUserFolders() }
                val projectFoldersDeferred = async { repository.getProjectFolders() }

                _videos.value = videosDeferred.await()
                _folders.value = foldersDeferred.await()
                _projectFolders.value = projectFoldersDeferred.await()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to fetch data"
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun deleteVideo(videoId: String, videoUrl: String) {
        viewModelScope.launch {
            try {
                // Optimistically remove from list for better UX
                val currentVideos = _videos.value
                _videos.value = currentVideos.filter { it.id != videoId }
                
                repository.deleteVideo(videoId, videoUrl)
                
                // Ideally we should re-fetch to be sure, but optimistic update is faster.
                // If it fails, we might want to revert or show error.
                // For now, let's just re-fetch silently to ensure consistency.
                fetchData(isRefresh = false) 
            } catch (e: Exception) {
                _error.value = "Failed to delete video: ${e.message}"
                // Revert optimistic update by re-fetching
                fetchData(isRefresh = false)
            }
        }
    }
}

data class UiState(
    val folders: List<FolderWithCount> = emptyList(),
    val videos: List<Video> = emptyList(),
    val currentFolderName: String? = null
)

data class FolderWithCount(
    val folder: com.example.perspecto.data.model.Folder,
    val count: Int
)

enum class SortOption {
    FILE_SIZE,
    UPDATE_DATE,
    FILE_NAME
}
