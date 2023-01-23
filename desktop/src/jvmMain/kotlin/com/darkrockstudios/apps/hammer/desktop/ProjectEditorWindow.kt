package com.darkrockstudios.apps.hammer.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.lifecycle.LifecycleController
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.darkrockstudios.apps.hammer.common.compose.Ui
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.projectroot.ProjectRoot
import com.darkrockstudios.apps.hammer.common.projectroot.ProjectRootComponent
import com.darkrockstudios.apps.hammer.common.projectroot.ProjectRootUi
import com.darkrockstudios.apps.hammer.common.projectroot.getDestinationIcon

@ExperimentalComposeApi
@ExperimentalDecomposeApi
@ExperimentalMaterialApi
@Composable
internal fun ApplicationScope.ProjectEditorWindow(
	app: ApplicationState,
	projectDef: ProjectDef
) {
	val lifecycle = remember { LifecycleRegistry() }
	val compContext = remember { DefaultComponentContext(lifecycle) }
	val windowState = rememberWindowState(size = DpSize(1200.dp, 800.dp))
	LifecycleController(lifecycle, windowState)

	val closeDialog = app.shouldShowConfirmClose.subscribeAsState()

	val component = remember<ProjectRoot> {
		ProjectRootComponent(
			componentContext = compContext,
			projectDef = projectDef,
			addMenu = { menu ->
				app.addMenu(menu)
			},
			removeMenu = { menuId ->
				app.removeMenu(menuId)
			}
		)
	}

	val menu by app.menu.subscribeAsState()

	Window(
		title = "Hammer - ${projectDef.name}",
		state = windowState,
		onCloseRequest = { onRequestClose(component, app, ApplicationState.CloseType.Application) }
	) {
		Column {
			MenuBar {
				Menu("File") {
					Item("Close Project", onClick = {
						onRequestClose(component, app, ApplicationState.CloseType.Project)
					})
					Item("Exit", onClick = {
						onRequestClose(component, app, ApplicationState.CloseType.Application)
					})
				}

				menu.forEach { menuDescriptor ->
					Menu(menuDescriptor.label) {
						menuDescriptor.items.forEach { itemDescriptor ->
							Item(
								itemDescriptor.label,
								onClick = { itemDescriptor.action(itemDescriptor.id) },
								shortcut = itemDescriptor.shortcut?.toDesktopShortcut()
							)
						}
					}
				}
			}

			val destinations = remember { ProjectRoot.DestinationTypes.values() }
			val router by component.routerState.subscribeAsState()

			Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
				NavigationRail(modifier = Modifier.padding(top = Ui.Padding.M)) {
					destinations.forEach { item ->
						NavigationRailItem(
							icon = { Icon(imageVector = getDestinationIcon(item), contentDescription = item.text) },
							label = { Text(item.text) },
							selected = router.active.instance.getLocationType() == item,
							onClick = { component.showDestination(item) }
						)
					}
				}

				ProjectRootUi(component)
			}
		}

		if (closeDialog.value != ApplicationState.CloseType.None) {
			confirmCloseDialog(closeDialog.value) { result, closeType ->
				if (result == ConfirmCloseResult.SaveAll) {
					component.storeDirtyBuffers()
				}

				app.dismissConfirmProjectClose()

				if (result != ConfirmCloseResult.Cancel) {
					performClose(app, closeType)
				}
			}
		}
	}
}