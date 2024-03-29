package com.mrzabbah.mytracker.feature_book_tracker.presentation.books

import com.mrzabbah.mytracker.feature_book_tracker.domain.model.Book
import com.mrzabbah.mytracker.feature_book_tracker.domain.model.UserPreferences
import com.mrzabbah.mytracker.feature_book_tracker.domain.util.BookOrder
import com.mrzabbah.mytracker.feature_book_tracker.domain.util.OrderType
import com.mrzabbah.mytracker.feature_book_tracker.domain.util.SearchMode

data class BooksState(
    val readingBooks: List<Book> = emptyList(),
    val yourBooks: List<Book> = emptyList(),
    val bookOrder: BookOrder = BookOrder.Date(OrderType.Descending),
    val searchMode: SearchMode = SearchMode.ByTitle,
    val authorSelected: String? = null,
    val labelsSelected: List<Int> = Book.bookLabels,
    val isUserOptionSectionVisible: Boolean = false,
    val isReadingBooksDisplay: Boolean = true,
    val isYourBooksDisplay: Boolean = true,
    val isFilterSectionVisible: Boolean = false,
    val isAuthorFilterActive: Boolean = false,
    val isLabelFilterActive: Boolean = false
) {
    fun getUserPreferences(labelsOnFilter: List<Int>): UserPreferences {
        return UserPreferences(
            bookOrder = bookOrder,
            authorSelected = authorSelected,
            labelsSelected = labelsSelected,
            labelsOnFilter = labelsOnFilter,
            isAuthorFilterActive = isAuthorFilterActive,
            isLabelFilterActive = isLabelFilterActive
        )
    }
}
