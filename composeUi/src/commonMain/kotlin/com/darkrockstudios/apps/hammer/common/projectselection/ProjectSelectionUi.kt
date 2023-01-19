package com.darkrockstudios.apps.hammer.common.projectselection

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AlertDialog
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.darkrockstudios.apps.hammer.common.compose.MpScrollBar
import com.darkrockstudios.apps.hammer.common.compose.Ui
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.libraries.mpfilepicker.DirectoryPicker

@ExperimentalMaterialApi
@ExperimentalComposeApi
@Composable
fun ProjectSelectionUi(component: ProjectSelectionComponent, modifier: Modifier = Modifier) {
	val state by component.state.subscribeAsState()
	var newProjectNameText by remember { mutableStateOf("") }
	var projectsPathText by remember { mutableStateOf(state.projectsDir.path) }
	var projectDefDeleteTarget by remember { mutableStateOf<ProjectDef?>(null) }
	var showDirectoryPicker by remember { mutableStateOf(false) }

	BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
		Column(
			modifier = modifier
				.padding(Ui.PADDING)
				.wrapContentWidth()
				.widthIn(min = 0.dp, max = 512.dp)
				.fillMaxHeight()
				.align(Alignment.Center)
		) {
			TextField(
				value = newProjectNameText,
				onValueChange = { newProjectNameText = it },
				label = { Text("New Project Name") },
			)

			Button(onClick = {
				component.createProject(newProjectNameText)
				newProjectNameText = ""
			}) {
				Text("Create Project")
			}

			if (component.showProjectDirectory) {
				TextField(
					value = projectsPathText,
					onValueChange = { projectsPathText = it },
					label = { Text("Path to Projects Directory") }
				)
				Button(onClick = { showDirectoryPicker = true }) {
					Text("Select Dir")
				}
			}

			Row(modifier = Modifier.fillMaxWidth()) {
				val listState: LazyListState = rememberLazyListState()
				LazyColumn(
					modifier = Modifier.weight(1f),
					state = listState,
					contentPadding = PaddingValues(Ui.PADDING)
				) {
					item {
						Row(
							modifier = Modifier.fillMaxWidth()
								.wrapContentHeight()
								.padding(vertical = 12.dp),
							horizontalArrangement = Arrangement.Center,
							verticalAlignment = Alignment.CenterVertically
						) {
							Text(
								"\uD83D\uDCDD Projects:",
								style = MaterialTheme.typography.headlineSmall,
								color = MaterialTheme.colorScheme.onBackground
							)
						}
					}
					items(state.projectDefs.size) { index ->
						ProjectCard(state.projectDefs[index], component::selectProject) { project ->
							projectDefDeleteTarget = project
						}
					}
				}
				MpScrollBar(state = listState)
			}
		}
	}

	projectDefDeleteTarget?.let { project ->
		projectDeleteDialog(project) { deleteProject ->
			if (deleteProject) {
				component.deleteProject(project)
			}

			projectDefDeleteTarget = null
		}
	}

	DirectoryPicker(showDirectoryPicker) { path ->
		showDirectoryPicker = false

		if (path != null) {
			projectsPathText = path
			component.setProjectsDir(projectsPathText)
		}
	}
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProjectCard(
	projectDef: ProjectDef,
	onProjectClick: (projectDef: ProjectDef) -> Unit,
	onProjectAltClick: (projectDef: ProjectDef) -> Unit
) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.padding(Ui.PADDING)
			.combinedClickable(
				onClick = { onProjectClick(projectDef) },
				onLongClick = { onProjectAltClick(projectDef) }
			),
	) {
		Column(modifier = Modifier.padding(Ui.PADDING)) {
			Text(
				projectDef.name,
				modifier = Modifier.padding(bottom = Ui.PADDING),
				style = MaterialTheme.typography.headlineSmall,
				fontWeight = FontWeight.Bold
			)
			Text(
				"Path: ${projectDef.path}",
				style = MaterialTheme.typography.bodySmall
			)
		}
	}
}

@ExperimentalMaterialApi
@ExperimentalComposeApi
@Composable
fun projectDeleteDialog(projectDef: ProjectDef, dismissDialog: (Boolean) -> Unit) {
	AlertDialog(
		title = { Text("Delete Project") },
		text = { Text("Are you sure you want to delete this project: ${projectDef.name}") },
		onDismissRequest = { /* noop */ },
		buttons = {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Button(onClick = { dismissDialog(true) }) {
					Text("DELETE")
				}
				Button(onClick = { dismissDialog(false) }) {
					Text("Dismiss")
				}
			}
		},
		modifier = Modifier.width(300.dp).padding(Ui.PADDING)
	)
}