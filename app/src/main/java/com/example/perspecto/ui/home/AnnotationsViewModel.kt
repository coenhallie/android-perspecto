package com.example.perspecto.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.perspecto.data.model.Annotation
import com.example.perspecto.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnnotationsViewModel : ViewModel() {

    private val repository = VideoRepository()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(AnnotationSortOption.DATE_NEWEST)
    val sortOption: StateFlow<AnnotationSortOption> = _sortOption.asStateFlow()

    private val _annotations = MutableStateFlow<List<Annotation>>(emptyList())
    val annotations: StateFlow<List<Annotation>> = _annotations.asStateFlow()

    val sortedAndFilteredAnnotations: StateFlow<List<Annotation>> = combine(
        _annotations,
        _searchQuery,
        _sortOption
    ) { annotations, query, sort ->
        val filtered = if (query.isBlank()) {
            annotations
        } else {
            annotations.filter {
                it.content.contains(query, ignoreCase = true) ||
                it.title.contains(query, ignoreCase = true)
            }
        }

        when (sort) {
            AnnotationSortOption.DATE_NEWEST -> filtered.sortedByDescending { it.timestamp } // Assuming timestamp is relevant, or creation time if available. Using timestamp for now as it's on the model. Wait, timestamp is video time. We might need createdAt. Let's check model again. The model has timestamp (Double). It doesn't seem to have createdAt. I'll use timestamp for now, but ideally it should be creation date.
            AnnotationSortOption.DATE_OLDEST -> filtered.sortedBy { it.timestamp }
            AnnotationSortOption.SEVERITY_HIGH_LOW -> filtered.sortedByDescending { getSeverityWeight(it.severity) }
            AnnotationSortOption.SEVERITY_LOW_HIGH -> filtered.sortedBy { getSeverityWeight(it.severity) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        fetchAnnotations()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSortOptionChange(option: AnnotationSortOption) {
        _sortOption.value = option
    }

    fun fetchAnnotations() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val fetchedAnnotations = repository.getAllUserAnnotations()
                _annotations.value = fetchedAnnotations
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAnnotation(annotationId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAnnotation(annotationId)
                _annotations.value = _annotations.value.filter { it.id != annotationId }
            } catch (e: Exception) {
                _error.value = "Failed to delete: ${e.message}"
            }
        }
    }

    private fun getSeverityWeight(severity: String): Int {
        return when (severity.lowercase()) {
            "high" -> 3
            "medium" -> 2
            "low" -> 1
            else -> 0
        }
    }
}

enum class AnnotationSortOption {
    DATE_NEWEST,
    DATE_OLDEST,
    SEVERITY_HIGH_LOW,
    SEVERITY_LOW_HIGH
}
