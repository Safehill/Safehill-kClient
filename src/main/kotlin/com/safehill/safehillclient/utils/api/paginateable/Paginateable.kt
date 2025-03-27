package com.safehill.safehillclient.utils.api.paginateable

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface Paginateable {
    val isLoading: StateFlow<Boolean>

    val isLoadingMore: StateFlow<Boolean>

    val isEndOfContents: StateFlow<Boolean>

    val loadMoreError: StateFlow<Throwable?>

    fun setLoading(isLoading: Boolean)

    fun setIsLoadingMore(isLoadingMore: Boolean)

    fun setEndOfContents(isEndOfContents: Boolean)

    fun setLoadMoreError(throwable: Throwable)
}


class DefaultPaginateable : Paginateable {

    private val _loading: MutableStateFlow<Boolean> = MutableStateFlow(true)
    override val isLoading = _loading.asStateFlow()

    private val _isLoadingMore: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isEndOfMessages = MutableStateFlow(false)
    override val isEndOfContents = _isEndOfMessages.asStateFlow()

    private val _loadMoreError: MutableStateFlow<Throwable?> = MutableStateFlow(null)
    override val loadMoreError = _loadMoreError.asStateFlow()

    override fun setLoading(isLoading: Boolean) {
        _loading.update { isLoading }
    }

    override fun setIsLoadingMore(isLoadingMore: Boolean) {
        _isLoadingMore.update { isLoadingMore }
    }

    override fun setEndOfContents(isEndOfContents: Boolean) {
        _isEndOfMessages.update { isEndOfContents }
    }

    override fun setLoadMoreError(throwable: Throwable) {
        _loadMoreError.update { throwable }
    }

}