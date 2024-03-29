package com.mrzabbah.mytracker.feature_book_tracker.presentation.books

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrzabbah.mytracker.feature_book_tracker.domain.model.Book
import com.mrzabbah.mytracker.feature_book_tracker.domain.model.UserPreferences
import com.mrzabbah.mytracker.feature_book_tracker.domain.use_case.BookTrackerUseCases
import com.mrzabbah.mytracker.feature_book_tracker.domain.util.BookOrder
import com.mrzabbah.mytracker.feature_book_tracker.domain.util.OrderType
import com.mrzabbah.mytracker.feature_book_tracker.domain.util.SearchMode
import com.mrzabbah.mytracker.feature_book_tracker.presentation.common.SearchTextFieldState
import com.mrzabbah.mytracker.ui.theme.DarkGray
import com.mrzabbah.mytracker.ui.theme.LightGray
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Collections.addAll
import javax.inject.Inject

@HiltViewModel
class BooksViewModel @Inject constructor(
    private val bookTrackerUseCases: BookTrackerUseCases,
) : ViewModel() {

    private val _state =
        mutableStateOf(BooksState()) // State that contains the values the UI will observe
    val state: State<BooksState> = _state

    private val _searchFieldState = mutableStateOf(SearchTextFieldState())
    val searchFieldState: State<SearchTextFieldState> = _searchFieldState

    private var _recentlyDeletedBook: Book? = null

    private var getUserBooksJob: Job? = null
    private var setUserPreferencesJob: Job? = null

    private var _recentlyAuthorSelected: String? = null

    private var _recentlyLabelsSelected: MutableList<Int> = mutableListOf(DarkGray.toArgb())

    init {
        viewModelScope.launch {
            val userPrefs = bookTrackerUseCases.getUserPreferencesUseCase()
            _state.value = state.value.copy(
                bookOrder = userPrefs.bookOrder,
                authorSelected = userPrefs.authorSelected,
                labelsSelected = userPrefs.labelsSelected,
                isAuthorFilterActive = userPrefs.isAuthorFilterActive,
                isLabelFilterActive = userPrefs.isLabelFilterActive
            )
            _recentlyLabelsSelected.addAll(userPrefs.labelsOnFilter)
            getUserBooks(
                state.value.bookOrder,
                state.value.authorSelected,
                state.value.labelsSelected
            )
        }
    }

    fun onEvent(event: BooksEvent) {
        when (event) {
            is BooksEvent.Order -> {
                if (state.value.bookOrder::class == event.bookOrder::class &&
                    state.value.bookOrder.orderType == event.bookOrder.orderType
                )
                    return
                getUserBooks(
                    event.bookOrder,
                    state.value.authorSelected,
                    state.value.labelsSelected
                )
            }
            is BooksEvent.FilterAuthor -> {
                if (_recentlyAuthorSelected != null &&
                    event.author != null &&
                    _recentlyAuthorSelected.equals(event.author)
                )
                    return

                if (_recentlyAuthorSelected == null && event.author == null)
                    return

                _recentlyAuthorSelected = event.author

                if (state.value.isAuthorFilterActive) {
                    _state.value = state.value.copy(
                        authorSelected = _recentlyAuthorSelected
                    )
                    getUserBooks(
                        state.value.bookOrder,
                        _recentlyAuthorSelected,
                        state.value.labelsSelected
                    )
                }
            }
            is BooksEvent.FilterLabel -> {
                if (_recentlyLabelsSelected.contains(event.label))
                    _recentlyLabelsSelected.remove(event.label)
                else
                    _recentlyLabelsSelected.add(event.label)

                _state.value = state.value.copy(
                    labelsSelected = listOf<Int>().apply { addAll(_recentlyLabelsSelected) }
                )
                getUserBooks(
                    state.value.bookOrder,
                    state.value.authorSelected,
                    _recentlyLabelsSelected
                )
            }
            is BooksEvent.DeleteUserBook -> {
                viewModelScope.launch {
                    bookTrackerUseCases.deleteUserBookUseCase(event.book)
                    _recentlyDeletedBook = event.book
                }
            }
            is BooksEvent.RestoreUserBook -> {
                viewModelScope.launch {
                    bookTrackerUseCases.addUserBookUseCase(_recentlyDeletedBook ?: return@launch)
                    _recentlyDeletedBook = null
                }
            }
            is BooksEvent.ToggleUserOptionSection -> {
                _state.value = state.value.copy(
                    isUserOptionSectionVisible = !state.value.isUserOptionSectionVisible,
                    isFilterSectionVisible = false
                )
            }
            is BooksEvent.ToggleFilterSection -> {
                _state.value = state.value.copy(
                    isFilterSectionVisible = !state.value.isFilterSectionVisible,
                    isUserOptionSectionVisible = false
                )
            }
            is BooksEvent.ToggleAuthorFilter -> {
                val authorToDisplay =
                    if (!state.value.isAuthorFilterActive)
                        _recentlyAuthorSelected
                    else
                        null

                _state.value = state.value.copy(
                    isAuthorFilterActive = !state.value.isAuthorFilterActive,
                    authorSelected = authorToDisplay
                )
                getUserBooks(
                    state.value.bookOrder,
                    authorToDisplay,
                    state.value.labelsSelected
                )
            }
            is BooksEvent.ToggleLabelFilter -> {
                val listToDisplay =
                    if (!state.value.isLabelFilterActive)
                        _recentlyLabelsSelected
                    else
                        Book.bookLabels

                _state.value = state.value.copy(
                    isLabelFilterActive = !state.value.isLabelFilterActive,
                    labelsSelected = listToDisplay
                )

                getUserBooks(
                    state.value.bookOrder,
                    state.value.authorSelected,
                    listToDisplay
                )

            }
            is BooksEvent.ChangeSearchFocus -> {
                _searchFieldState.value = searchFieldState.value.copy(
                    isFocused = event.focusState.isFocused
                )
            }
            is BooksEvent.EnteredSearch -> {
                _searchFieldState.value = searchFieldState.value.copy(
                    text = event.value
                )
            }
            BooksEvent.ChangeSearchMode -> {
                val newSearchMode = when(state.value.searchMode) {
                    SearchMode.Advanced -> SearchMode.ByTitle
                    SearchMode.ByAuthor -> SearchMode.Advanced
                    else -> SearchMode.ByAuthor
                }
                _state.value = state.value.copy(
                    searchMode = newSearchMode
                )
            }
            BooksEvent.ToggleReadingBooksSection -> {
                _state.value = state.value.copy(
                    isReadingBooksDisplay = !state.value.isReadingBooksDisplay
                )
            }
            BooksEvent.ToggleYourBooksSection -> {
                _state.value = state.value.copy(
                    isYourBooksDisplay = !state.value.isYourBooksDisplay
                )
            }
        }
    }

    private fun getUserBooks(bookOrder: BookOrder, author: String?, labels: List<Int>) {
        getUserBooksJob?.cancel()
        getUserBooksJob = bookTrackerUseCases.getUserBooksUseCase(bookOrder, author, labels)
            .onEach { books ->
                _state.value = state.value.copy(
                    readingBooks = books.filter { it.active },
                    yourBooks = books.filter { !it.active },
                    bookOrder = bookOrder,
                    authorSelected = author,
                    labelsSelected = labels
                )
                setUserPreferences(state.value.getUserPreferences(_recentlyLabelsSelected))
            }
            .launchIn(viewModelScope)
    }

    private fun setUserPreferences(userPreferences: UserPreferences) {
        setUserPreferencesJob?.cancel()
        setUserPreferencesJob = viewModelScope.launch {
            bookTrackerUseCases.setUserPreferencesUseCase(userPreferences)
        }
    }
}
