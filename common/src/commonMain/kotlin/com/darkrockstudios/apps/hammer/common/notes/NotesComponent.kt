package com.darkrockstudios.apps.hammer.common.notes

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.reduce
import com.darkrockstudios.apps.hammer.common.ProjectComponentBase
import com.darkrockstudios.apps.hammer.common.data.MenuDescriptor
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.data.notes.NoteError
import com.darkrockstudios.apps.hammer.common.data.notes.NotesRepository
import com.darkrockstudios.apps.hammer.common.mainDispatcher
import com.darkrockstudios.apps.hammer.common.projectInject
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesComponent(
	componentContext: ComponentContext,
	private val projectDef: ProjectDef,
	addMenu: (menu: MenuDescriptor) -> Unit,
	removeMenu: (id: String) -> Unit,
) : ProjectComponentBase(projectDef, componentContext), Notes {

	private val notesRepository: NotesRepository by projectInject()

	private val _state = MutableValue(Notes.State(projectDef = projectDef, notes = emptyList()))
	override val state: Value<Notes.State> = _state

	init {
		scope.launch {
			notesRepository.notesListFlow.collect { noteContainers ->
				withContext(mainDispatcher) {
					val notes = noteContainers.map { it.note }
					_state.reduce {
						it.copy(notes = notes)
					}
				}
			}
		}

		notesRepository.loadNotes()
	}

	override fun createNote(noteText: String): NoteError {
		val result = notesRepository.createNote(noteText)
		if (result.isSuccess) {
			notesRepository.loadNotes()
		}

		return result
	}
}