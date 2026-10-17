package com.example.mindalike.ui.screens.fyp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindalike.data.Community
import com.example.mindalike.data.Post
import com.example.mindalike.data.PostComment
import com.example.mindalike.data.PostLike
import com.example.mindalike.data.Profile
import com.example.mindalike.data.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PostWithAuthor(
    val post: Post,
    val author: Profile?,
    val likeCount: Int = 0,
    val commentCount: Int = 0
)

sealed interface FypUiState<out T> {
    object Loading : FypUiState<Nothing>
    data class Success<T>(val data: List<T>) : FypUiState<T>
    data class Error(val message: String) : FypUiState<Nothing>
}

class FypViewModel : ViewModel() {

    private val _currentUserProfile = MutableStateFlow<Profile?>(null)

    private val _postsState = MutableStateFlow<FypUiState<PostWithAuthor>>(FypUiState.Loading)
    val postsState: StateFlow<FypUiState<PostWithAuthor>> = _postsState.asStateFlow()

    private val _communitiesState = MutableStateFlow<FypUiState<Community>>(FypUiState.Loading)
    val communitiesState: StateFlow<FypUiState<Community>> = _communitiesState.asStateFlow()

    private val _profilesState = MutableStateFlow<FypUiState<Profile>>(FypUiState.Loading)
    val profilesState: StateFlow<FypUiState<Profile>> = _profilesState.asStateFlow()

    init {
        loadFypData()
    }

    fun loadFypData(searchQuery: String = "") {
        viewModelScope.launch {
            try {
                val currentUserId = supabase.auth.currentUserOrNull()?.id
                if (currentUserId != null && _currentUserProfile.value == null) {
                    _currentUserProfile.value = supabase.from("profiles")
                        .select { filter { eq("id", currentUserId) } }
                        .decodeSingleOrNull<Profile>()
                }

                val user = _currentUserProfile.value
                fetchPosts(user, searchQuery)
                fetchCommunities(user, searchQuery)
                fetchProfiles(user, searchQuery)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchPosts(user: Profile?, query: String) {
        _postsState.value = FypUiState.Loading
        try {
            val targetedLimit = 14
            val discoveryLimit = 6

            val rawTargeted = supabase.from("posts")
                .select {
                    filter {
                        if (query.isNotBlank()) ilike("content", "%$query%")
                    }
                    limit(targetedLimit.toLong())
                }.decodeList<Post>()

            val existingIds = rawTargeted.mapNotNull { it.id }.toSet()

            val rawDiscovery = supabase.from("posts")
                .select {
                    filter {
                        if (query.isNotBlank()) ilike("content", "%$query%")
                    }
                    limit((targetedLimit + discoveryLimit).toLong())
                }.decodeList<Post>()
                .filter { it.id !in existingIds }
                .take(discoveryLimit)

            val combinedPosts = (rawTargeted + rawDiscovery).shuffled()

            val postIds = combinedPosts.mapNotNull { it.id }
            val authorIds = combinedPosts.map { it.userId }.distinct()

            val authorsMap = if (authorIds.isNotEmpty()) {
                supabase.from("profiles")
                    .select { filter { isIn("id", authorIds) } }
                    .decodeList<Profile>()
                    .associateBy { it.id }
            } else emptyMap()

            val likesMap = if (postIds.isNotEmpty()) {
                supabase.from("post_likes")
                    .select { filter { isIn("post_id", postIds) } }
                    .decodeList<PostLike>()
                    .groupBy { it.postId }
            } else emptyMap()

            val commentsMap = if (postIds.isNotEmpty()) {
                supabase.from("post_comments")
                    .select { filter { isIn("post_id", postIds) } }
                    .decodeList<PostComment>()
                    .groupBy { it.postId }
            } else emptyMap()

            val postsWithAuthors = combinedPosts.map { post ->
                val id = post.id ?: ""
                PostWithAuthor(
                    post = post,
                    author = authorsMap[post.userId],
                    likeCount = likesMap[id]?.size ?: 0,
                    commentCount = commentsMap[id]?.size ?: 0
                )
            }

            _postsState.value = FypUiState.Success(postsWithAuthors)
        } catch (e: Exception) {
            _postsState.value = FypUiState.Error(e.localizedMessage ?: "Failed to load posts")
        }
    }

    private suspend fun fetchCommunities(user: Profile?, query: String) {
        _communitiesState.value = FypUiState.Loading
        try {
            val targetedLimit = 7
            val discoveryLimit = 3
            val userBadges = user?.badges ?: emptyList()

            val targetedCommunities = if (userBadges.isNotEmpty()) {
                supabase.from("communities")
                    .select {
                        filter {
                            isIn("category", userBadges)
                            if (query.isNotBlank()) ilike("name", "%$query%")
                        }
                        limit(targetedLimit.toLong())
                    }.decodeList<Community>()
            } else emptyList()

            val existingIds = targetedCommunities.mapNotNull { it.id }.toSet()

            val discoveryCommunities = supabase.from("communities")
                .select {
                    filter {
                        if (query.isNotBlank()) ilike("name", "%$query%")
                    }
                    limit((targetedLimit + discoveryLimit).toLong())
                }.decodeList<Community>()
                .filter { it.id !in existingIds }
                .take(discoveryLimit)

            _communitiesState.value = FypUiState.Success(targetedCommunities + discoveryCommunities)
        } catch (e: Exception) {
            _communitiesState.value = FypUiState.Error(e.localizedMessage ?: "Failed to load communities")
        }
    }

    private suspend fun fetchProfiles(user: Profile?, query: String) {
        _profilesState.value = FypUiState.Loading
        try {
            val currentUserId = user?.id ?: ""
            val targetedLimit = 14
            val discoveryLimit = 6

            val targetedProfiles = supabase.from("profiles")
                .select {
                    filter {
                        if (currentUserId.isNotBlank()) neq("id", currentUserId)
                        if (!user?.universityName.isNullOrBlank()) {
                            eq("university_name", user!!.universityName!!)
                        }
                        if (query.isNotBlank()) ilike("full_name", "%$query%")
                    }
                    limit(targetedLimit.toLong())
                }.decodeList<Profile>()

            val existingIds = targetedProfiles.map { it.id }.toSet() + setOf(currentUserId)

            val discoveryProfiles = supabase.from("profiles")
                .select {
                    filter {
                        if (query.isNotBlank()) ilike("full_name", "%$query%")
                    }
                    limit((targetedLimit + discoveryLimit).toLong())
                }.decodeList<Profile>()
                .filter { it.id !in existingIds }
                .take(discoveryLimit)

            _profilesState.value = FypUiState.Success(targetedProfiles + discoveryProfiles)
        } catch (e: Exception) {
            _profilesState.value = FypUiState.Error(e.localizedMessage ?: "Failed to load profiles")
        }
    }
}