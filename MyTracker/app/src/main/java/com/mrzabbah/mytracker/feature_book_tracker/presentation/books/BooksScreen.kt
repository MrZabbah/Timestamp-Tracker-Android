package com.mrzabbah.mytracker.feature_book_tracker.presentation.books

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.mrzabbah.mytracker.feature_book_tracker.domain.model.Book
import com.mrzabbah.mytracker.feature_book_tracker.presentation.books.components.FilterSection
import com.mrzabbah.mytracker.feature_book_tracker.presentation.books.components.OrderSection
import com.mrzabbah.mytracker.feature_book_tracker.presentation.common.BookItem
import com.mrzabbah.mytracker.feature_book_tracker.presentation.common.DefaultSearchBar
import com.mrzabbah.mytracker.feature_book_tracker.presentation.common.components.BookListComposable
import com.mrzabbah.mytracker.feature_book_tracker.presentation.util.Screen
import com.mrzabbah.mytracker.ui.theme.CasualBlue
import com.mrzabbah.mytracker.ui.theme.LightGray
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@Composable
fun BooksScreen(
    navController: NavController,
    viewModel: BooksViewModel = hiltViewModel(),
    focusManager: FocusManager
) {
    val state = viewModel.state.value
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    Scaffold(
        scaffoldState = scaffoldState
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            DefaultSearchBar(
                onDone = { query ->
                    if (query.isNotBlank())
                        navController.navigate(
                            Screen.SearchScreen.route +
                                    "/$query/${state.searchMode}"
                        )
                },
                clear = true,
                focusManager = focusManager,
                searchMode = state.searchMode,
                onChangeClick = {
                    viewModel.onEvent(BooksEvent.ChangeSearchMode)
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.onEvent(BooksEvent.ToggleReadingBooksSection)
                    },
                ) {
                    Icon(
                        imageVector = if (state.isReadingBooksDisplay)
                            Icons.Filled.ArrowDropUp
                        else
                            Icons.Filled.ArrowDropDown,
                        contentDescription = "Show/Hide section"
                    )
                }
                Text(
                    text = "Reading",
                    style = MaterialTheme.typography.h4
                )
            }
            Spacer(Modifier.height(8.dp))
            AnimatedVisibility(
                visible = state.isReadingBooksDisplay,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(state.readingBooks) { book ->
                        BookItem(
                            book = book,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    focusManager.clearFocus()
                                    navController.navigate(
                                        Screen.SpecificBookScreen.route +
                                                "/${
                                                    URLEncoder.encode(
                                                        Json.encodeToString(book),
                                                        StandardCharsets.UTF_8.toString()
                                                    )
                                                }"
                                    )
                                },
                            onButtonOptionClick = {
                                focusManager.clearFocus()
                                viewModel.onEvent(BooksEvent.DeleteUserBook(book))
                                scope.launch {
                                    val result = scaffoldState.snackbarHostState.showSnackbar(
                                        message = "\"${book.title}\" has been deleted",
                                        actionLabel = "Undo"
                                    )
                                    if (result == SnackbarResult.ActionPerformed)
                                        viewModel.onEvent(BooksEvent.RestoreUserBook)
                                }
                            },
                            buttonIcon = Icons.Default.Delete,
                            buttonDescription = "Delete user book"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.onEvent(BooksEvent.ToggleYourBooksSection)
                    },
                ) {
                    Icon(
                        imageVector = if (state.isYourBooksDisplay)
                            Icons.Filled.ArrowDropUp
                        else
                            Icons.Filled.ArrowDropDown,
                        contentDescription = "Show/Hide section"
                    )
                }
                Text(
                    text = "Your books",
                    style = MaterialTheme.typography.h4
                )
                Spacer(modifier = Modifier.width(48.dp))
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.onEvent(BooksEvent.ToggleUserOptionSection)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = if (state.isUserOptionSectionVisible) CasualBlue else LightGray
                    )
                }
                IconButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.onEvent(BooksEvent.ToggleFilterSection)
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAlt,
                        contentDescription = "Filter",
                        tint = if (state.isFilterSectionVisible) CasualBlue else LightGray
                    )
                }
            }
            AnimatedVisibility(
                visible = state.isUserOptionSectionVisible || state.isFilterSectionVisible,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                if (state.isUserOptionSectionVisible) {
                    OrderSection(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        bookOrder = state.bookOrder,
                        onOrderChange = {
                            viewModel.onEvent(BooksEvent.Order(it))
                        },
                        focusManager = focusManager
                    )
                }
                if (state.isFilterSectionVisible) {
                    FilterSection(
                        modifier = Modifier
                            .fillMaxWidth(),
                        authorFilter = state.isAuthorFilterActive,
                        labelFilter = state.isLabelFilterActive,
                        labelsSelected = state.labelsSelected,
                        onCheckAuthorFilter = {
                            viewModel.onEvent(BooksEvent.ToggleAuthorFilter)
                        },
                        onCheckLabelFilter = {
                            viewModel.onEvent(BooksEvent.ToggleLabelFilter)
                        },
                        onCheckSpecificLabel = {
                            viewModel.onEvent(BooksEvent.FilterLabel(it))
                        },
                        focusManager = focusManager
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            AnimatedVisibility(
                visible = state.isYourBooksDisplay,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(state.yourBooks) { book ->
                        BookItem(
                            book = book,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    focusManager.clearFocus()
                                    navController.navigate(
                                        Screen.SpecificBookScreen.route +
                                                "/${
                                                    URLEncoder.encode(
                                                        Json.encodeToString(book),
                                                        StandardCharsets.UTF_8.toString()
                                                    )
                                                }"
                                    )
                                },
                            onButtonOptionClick = {
                                focusManager.clearFocus()
                                viewModel.onEvent(BooksEvent.DeleteUserBook(book))
                                scope.launch {
                                    val result = scaffoldState.snackbarHostState.showSnackbar(
                                        message = "\"${book.title}\" has been deleted",
                                        actionLabel = "Undo"
                                    )
                                    if (result == SnackbarResult.ActionPerformed)
                                        viewModel.onEvent(BooksEvent.RestoreUserBook)
                                }
                            },
                            buttonIcon = Icons.Default.Delete,
                            buttonDescription = "Delete user book"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
