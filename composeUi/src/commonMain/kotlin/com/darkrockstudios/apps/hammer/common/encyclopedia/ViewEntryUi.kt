package com.darkrockstudios.apps.hammer.common.encyclopedia

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.darkrockstudios.apps.hammer.common.compose.*
import com.darkrockstudios.apps.hammer.common.defaultDispatcher
import com.darkrockstudios.apps.hammer.common.mainDispatcher
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ViewEntryUi(
	component: ViewEntry,
	scope: CoroutineScope,
	modifier: Modifier = Modifier,
	snackbarHostState: SnackbarHostState,
	closeEntry: () -> Unit
) {
	val state by component.state.subscribeAsState()
	var showFilePicker by remember { mutableStateOf(false) }

	var editName by remember { mutableStateOf(false) }
	var entryNameText by remember { mutableStateOf(state.content?.name ?: "") }

	var editText by remember { mutableStateOf(false) }
	var entryText by remember { mutableStateOf(state.content?.text ?: "") }

	var showMenu by remember { mutableStateOf(false) }
	var showDeleteImageDialog by remember { mutableStateOf(false) }
	var showDeleteEntryDialog by remember { mutableStateOf(false) }

	val screen = LocalScreenCharacteristic.current
	val content = state.content

	LaunchedEffect(state.content) {
		state.content?.let {
			entryNameText = it.name
			entryText = it.text
		}
	}

	Box(
		modifier = modifier.fillMaxSize().padding(Ui.Padding.XL),
		contentAlignment = Alignment.TopCenter
	) {
		Column(modifier = Modifier.widthIn(128.dp, 700.dp).wrapContentHeight()) {
			Row(
				modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
				horizontalArrangement = Arrangement.End,
				verticalAlignment = Alignment.CenterVertically
			) {
				if (content != null && (editName || editText)) {
					IconButton(onClick = {
						scope.launch {
							component.updateEntry(
								name = entryNameText,
								text = entryText,
								tags = content.tags
							)

							withContext(mainDispatcher) {
								editName = false
								editText = false
							}

							scope.launch {
								snackbarHostState.showSnackbar("Entry Saved")
							}
						}
					}) {
						Icon(
							Icons.Filled.Check,
							"Save",
							tint = MaterialTheme.colorScheme.onSurface
						)
					}

					IconButton(onClick = {
						scope.launch {
							entryNameText = content.name
							entryText = content.text

							withContext(mainDispatcher) {
								editName = false
								editText = false
							}
						}
					}) {
						Icon(
							Icons.Filled.Cancel,
							"Cancel",
							tint = MaterialTheme.colorScheme.error
						)
					}

					Spacer(modifier = Modifier.size(Ui.Padding.XL))

					Divider(
						color = MaterialTheme.colorScheme.outline,
						modifier = Modifier.fillMaxHeight().width(1.dp)
							.padding(top = Ui.Padding.M, bottom = Ui.Padding.M)
					)

					Spacer(modifier = Modifier.size(Ui.Padding.XL))
				}

				IconButton(
					onClick = { showMenu = true },
				) {
					Icon(
						Icons.Filled.MoreVert,
						contentDescription = "Options",
						tint = MaterialTheme.colorScheme.onSurface
					)

					DropdownMenu(
						expanded = showMenu,
						onDismissRequest = { showMenu = false }
					) {
						if (state.entryImagePath == null) {
							DropdownMenuItem(
								text = { Text("Add Image") },
								onClick = {
									showFilePicker = true
									showMenu = false
								},
							)
						} else {
							DropdownMenuItem(
								text = { Text("Remove Image") },
								onClick = {
									showDeleteImageDialog = true
									showMenu = false
								},
							)
						}

						DropdownMenuItem(
							text = { Text("Delete Entry") },
							onClick = {
								showDeleteEntryDialog = true
								showMenu = false
							},
						)
					}
				}

				if (screen.needsExplicitClose) {
					IconButton(
						onClick = { closeEntry() },
					) {
						Icon(
							Icons.Filled.Close,
							contentDescription = "Close Entry",
							tint = MaterialTheme.colorScheme.onSurface
						)
					}
				}
			}

			if (editName) {
				TextField(
					modifier = Modifier.wrapContentHeight().fillMaxWidth(),
					value = entryNameText,
					onValueChange = { entryNameText = it },
					placeholder = { Text("Name") }
				)
			} else {
				Text(
					entryNameText,
					style = MaterialTheme.typography.displayMedium,
					color = MaterialTheme.colorScheme.onBackground,
					textAlign = TextAlign.Center,
					modifier = Modifier.wrapContentHeight().fillMaxWidth().clickable { editName = true }
				)
			}

			Spacer(modifier = Modifier.size(Ui.Padding.L))

			if (screen.isWide) {
				Row {
					Image(
						modifier = Modifier.weight(1f),
						state = state,
						showDeleteImageDialog = { showDeleteImageDialog = true }
					)
					Contents(
						modifier = Modifier.weight(1f),
						state = state,
						editText = editText,
						entryText = entryText,
						setEntryText = { entryText = it },
						beginEdit = { editText = true }
					)
				}
			} else {
				Column {
					Image(
						modifier = Modifier.weight(1f),
						state = state,
						showDeleteImageDialog = { showDeleteImageDialog = true }
					)
					Contents(
						modifier = Modifier.weight(1f),
						state = state,
						editText = editText,
						entryText = entryText,
						setEntryText = { entryText = it },
						beginEdit = { editText = true }
					)
				}
			}
		}
	}

	FilePicker(show = showFilePicker, fileExtension = "jpg") { path ->
		if (path != null) {
			scope.launch { component.setImage(path) }
		}
		showFilePicker = false
	}

	if (showDeleteImageDialog) {
		ConfirmDialog(
			title = "Delete Image?",
			message = "This cannot be undone!",
			confirmButton = "Delete"
		) { shouldDelete ->
			if (shouldDelete) {
				scope.launch { component.removeEntryImage() }
			}
			showDeleteImageDialog = false
		}
	}

	if (showDeleteEntryDialog) {
		ConfirmDialog(
			title = "Delete Entry?",
			message = "This cannot be undone!",
			confirmButton = "Delete"
		) { shouldDelete ->
			if (shouldDelete) {
				scope.launch(defaultDispatcher) {
					if (component.deleteEntry(state.entryDef)) {
						withContext(mainDispatcher) {
							closeEntry()
						}
						snackbarHostState.showSnackbar("Entry Deleted")
					}
				}
			}
			showDeleteEntryDialog = false
		}
	}
}

@Composable
private fun Image(
	modifier: Modifier = Modifier,
	state: ViewEntry.State,
	showDeleteImageDialog: () -> Unit
) {
	if (state.entryImagePath != null) {
		Box(modifier = modifier.wrapContentHeight()) {
			ImageItem(
				path = state.entryImagePath,
				modifier = Modifier.wrapContentHeight()
					.align(Alignment.TopEnd)
					.clickable(onClick = showDeleteImageDialog),
				contentScale = ContentScale.FillWidth,
			)
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Contents(
	modifier: Modifier = Modifier,
	state: ViewEntry.State,
	editText: Boolean,
	entryText: String,
	setEntryText: (String) -> Unit,
	beginEdit: () -> Unit,
) {
	val content = state.content

	Column(
		modifier = modifier
			.wrapContentHeight()
			.padding(start = Ui.Padding.XL, end = Ui.Padding.XL, bottom = Ui.Padding.XL)
			.verticalScroll(rememberScrollState())
	) {
		AssistChip(
			onClick = {},
			enabled = false,
			label = { Text(state.entryDef.type.text) },
			leadingIcon = { Icon(getEntryTypeIcon(state.entryDef.type), state.entryDef.type.text) },
			modifier = Modifier.padding(end = Ui.Padding.L)
		)

		Spacer(modifier = Modifier.size(Ui.Padding.XL))

		if (content != null) {
			Column {
				if (editText) {
					OutlinedTextField(
						value = entryText,
						onValueChange = setEntryText,
						modifier = Modifier.fillMaxWidth().padding(PaddingValues(bottom = Ui.Padding.XL)),
						placeholder = { Text(text = "Describe your entry") },
						maxLines = 10,
					)
				} else {
					Text(
						entryText,
						modifier = Modifier.fillMaxWidth().clickable { beginEdit() },
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onBackground,
					)
				}

				Spacer(modifier = Modifier.size(Ui.Padding.XL))

				TagRow(
					tags = content.tags,
					modifier = Modifier.fillMaxWidth(),
					alignment = Alignment.End
				)
			}
		} else {
			CircularProgressIndicator()
		}
	}
}