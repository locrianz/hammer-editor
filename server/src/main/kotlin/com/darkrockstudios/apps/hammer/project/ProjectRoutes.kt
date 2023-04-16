package com.darkrockstudios.apps.hammer.project

import com.darkrockstudios.apps.hammer.base.http.*
import com.darkrockstudios.apps.hammer.base.http.synchronizer.EntityConflictException
import com.darkrockstudios.apps.hammer.plugins.ServerUserIdPrincipal
import com.darkrockstudios.apps.hammer.plugins.USER_AUTH_NAME
import com.darkrockstudios.apps.hammer.project.synchronizers.serverEntityHash
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant
import org.koin.ktor.ext.get

fun Application.projectRoutes() {
	routing {
		authenticate(USER_AUTH_NAME) {
			route("/project/{userId}/{projectName}") {
				beginProjectSync()
				endProjectSync()
				uploadEntity()
				downloadEntity()
				deleteEntity()
			}
		}
	}
}

private fun Route.beginProjectSync() {
	val projectRepository: ProjectRepository = get()

	get("/begin_sync") {
		val principal = call.principal<ServerUserIdPrincipal>()!!
		val projectName = call.parameters["projectName"]

		if (projectName == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Parameter", message = "projectName was missing")
			)
		} else {
			val projectDef = ProjectDefinition(projectName)
			val result = projectRepository.beginProjectSync(principal.id, projectDef)
			if (result.isSuccess) {
				val syncBegan = result.getOrThrow()
				call.respond(syncBegan)
			} else {
				call.respond(
					status = HttpStatusCode.BadRequest,
					HttpResponseError(
						error = "Failed to begin sync",
						message = result.exceptionOrNull()?.message ?: "Unknown"
					)
				)
			}
		}
	}
}

private fun Route.endProjectSync() {
	val projectRepository: ProjectRepository = get()

	get("/end_sync") {
		val principal = call.principal<ServerUserIdPrincipal>()!!
		val projectName = call.parameters["projectName"]
		val syncId = call.request.headers[HEADER_SYNC_ID]

		val formParameters = call.receiveParameters()
		val lastSync = try {
			Instant.parse(formParameters["lastSync"].toString())
		} catch (e: IllegalArgumentException) {
			null
		}
		val lastId = formParameters["lastId"].toString().toIntOrNull()

		if (projectName == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Parameter", message = "projectName was missing")
			)
		} else if (syncId == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Parameter", message = "syncId was missing")
			)
		} else {
			val projectDef = ProjectDefinition(projectName)
			val result = projectRepository.endProjectSync(principal.id, projectDef, syncId, lastSync, lastId)
			if (result.isSuccess) {
				val success = result.getOrThrow()
				call.respond(success)
			} else {
				call.respond(
					status = HttpStatusCode.BadRequest,
					HttpResponseError(
						error = "Failed to begin sync",
						message = result.exceptionOrNull()?.message ?: "Unknown"
					)
				)
			}
		}
	}
}

private fun Route.uploadEntity() {
	val projectRepository: ProjectRepository = get()

	post("/upload_entity/{entityId}") {
		val principal = call.principal<ServerUserIdPrincipal>()!!
		val projectName = call.parameters["projectName"]
		val entityId = call.parameters["entityId"]?.toIntOrNull()
		val syncId = call.request.headers[HEADER_SYNC_ID]
		val originalHash = call.request.headers[HEADER_ORIGINAL_HASH]
		val force = call.request.queryParameters["force"]?.toBooleanStrictOrNull()

		val entityTypeHeader = call.request.headers[HEADER_ENTITY_TYPE]
		val type = ApiProjectEntity.Type.fromString(entityTypeHeader ?: "")
		if (type == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Header", message = "x-entity-type was missing")
			)
		} else {
			val entity = when (type) {
				ApiProjectEntity.Type.SCENE -> call.receive<ApiProjectEntity.SceneEntity>()
				ApiProjectEntity.Type.NOTE -> call.receive<ApiProjectEntity.NoteEntity>()
				ApiProjectEntity.Type.TIMELINE_EVENT -> call.receive<ApiProjectEntity.TimelineEventEntity>()
				ApiProjectEntity.Type.ENCYCLOPEDIA_ENTRY -> call.receive<ApiProjectEntity.EncyclopediaEntryEntity>()
				ApiProjectEntity.Type.SCENE_DRAFT -> call.receive<ApiProjectEntity.SceneDraftEntity>()
			}

			if (projectName == null) {
				call.respond(
					status = HttpStatusCode.BadRequest,
					HttpResponseError(error = "Missing Parameter", message = "projectName was missing")
				)
			} else if (entityId == null) {
				call.respond(
					status = HttpStatusCode.BadRequest,
					HttpResponseError(error = "Missing Parameter", message = "entityId was missing")
				)
			} else if (syncId.isNullOrBlank()) {
				call.respond(
					status = HttpStatusCode.BadRequest,
					HttpResponseError(error = "Missing Parameter", message = "syncId was missing")
				)
			} else {
				val projectDef = ProjectDefinition(projectName)
				val result =
					projectRepository.saveEntity(principal.id, projectDef, entity, originalHash, syncId, force ?: false)
				if (result.isSuccess) {
					call.respond(SaveEntityResponse(result.getOrThrow()))
				} else {
					val e = result.exceptionOrNull()
					if (e is EntityConflictException) {
						val entity = e.entity
						when (entity) {
							is ApiProjectEntity.SceneEntity -> call.respond(status = HttpStatusCode.Conflict, entity)
							is ApiProjectEntity.NoteEntity -> call.respond(status = HttpStatusCode.Conflict, entity)
							is ApiProjectEntity.TimelineEventEntity -> call.respond(
								status = HttpStatusCode.Conflict,
								entity
							)

							is ApiProjectEntity.EncyclopediaEntryEntity -> call.respond(
								status = HttpStatusCode.Conflict,
								entity
							)

							is ApiProjectEntity.SceneDraftEntity -> call.respond(
								status = HttpStatusCode.Conflict,
								entity
							)
						}
					} else {
						call.respond(
							status = HttpStatusCode.ExpectationFailed,
							HttpResponseError(
								error = "Save Error",
								message = e?.message ?: "Unknown failure",
							)
						)
					}
				}
			}
		}
	}
}

private fun Route.downloadEntity() {
	val projectRepository: ProjectRepository = get()

	get("/download_entity/{entityId}") {
		val principal = call.principal<ServerUserIdPrincipal>()!!
		val projectName = call.parameters["projectName"]
		val entityId = call.parameters["entityId"]?.toIntOrNull()
		val entityHash = call.request.headers[HEADER_ENTITY_HASH]
		val syncId = call.request.headers[HEADER_SYNC_ID]

		if (projectName == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Parameter", message = "projectName was missing")
			)
		} else if (entityId == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Parameter", message = "entityId was missing")
			)
		} else if (syncId == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Header", message = "syncId was missing")
			)
		} else {
			val projectDef = ProjectDefinition(projectName)

			val result =
				projectRepository.loadEntity(principal.id, projectDef, entityId, syncId)
			if (result.isSuccess) {
				val serverEntity = result.getOrThrow()
				val serverEntityHash = serverEntityHash(serverEntity)

				if (entityHash != null && entityHash == serverEntityHash) {
					call.respond(HttpStatusCode.NotModified)
				} else {
					println("Entity Download for ID $entityId because hash mismatched:\nClient: $entityHash\nServer: $serverEntityHash")
					call.response.headers.append(HEADER_ENTITY_TYPE, serverEntity.type.toString())
					when (serverEntity) {
						is ApiProjectEntity.SceneEntity -> call.respond(serverEntity)
						is ApiProjectEntity.NoteEntity -> call.respond(serverEntity)
						is ApiProjectEntity.TimelineEventEntity -> call.respond(serverEntity)
						is ApiProjectEntity.EncyclopediaEntryEntity -> call.respond(serverEntity)
						is ApiProjectEntity.SceneDraftEntity -> call.respond(serverEntity)
					}
				}
			} else {
				val e = result.exceptionOrNull()
				if (e is EntityConflictException) {
					call.respond(
						status = HttpStatusCode.Conflict,
						HttpResponseError(error = "Save Error", message = e.message ?: "Unknown failure")
					)
				} else {
					println("Entity Download failed for ID $entityId: " + e?.message)
					call.respond(
						status = HttpStatusCode.InternalServerError,
						HttpResponseError(error = "Save Error", message = e?.message ?: "Unknown failure")
					)
				}
			}
		}
	}
}

private fun Route.deleteEntity() {
	val projectRepository: ProjectRepository = get()

	get("/delete_entity/{entityId}") {
		val principal = call.principal<ServerUserIdPrincipal>()!!
		val projectName = call.parameters["projectName"]
		val entityId = call.parameters["entityId"]?.toIntOrNull()
		val syncId = call.request.headers[HEADER_SYNC_ID]

		if (projectName == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Parameter", message = "projectName was missing")
			)
		} else if (entityId == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Parameter", message = "entityId was missing")
			)
		} else if (syncId == null) {
			call.respond(
				status = HttpStatusCode.BadRequest,
				HttpResponseError(error = "Missing Header", message = "syncId was missing")
			)
		} else {
			val projectDef = ProjectDefinition(projectName)
			val result = projectRepository.deleteEntity(principal.id, projectDef, entityId, syncId)

			if (result.isSuccess) {
				call.respond(HttpStatusCode.OK, DeleteIdsResponse(true))
			} else {
				val e = result.exceptionOrNull()
				if (e is NoEntityTypeFound) {
					call.respond(HttpStatusCode.OK, DeleteIdsResponse(false))
				} else {
					val message = result.exceptionOrNull()?.message ?: "Unknown failure"
					call.respond(
						status = HttpStatusCode.InternalServerError,
						HttpResponseError(error = "Failed to delete Entity", message = message)
					)
				}
			}
		}
	}
}