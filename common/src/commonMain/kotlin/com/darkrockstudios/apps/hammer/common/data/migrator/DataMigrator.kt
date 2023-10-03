package com.darkrockstudios.apps.hammer.common.data.migrator

import com.darkrockstudios.apps.hammer.common.components.projecteditor.metadata.ProjectMetadata
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.data.globalsettings.GlobalSettingsRepository
import com.darkrockstudios.apps.hammer.common.data.projectmetadatarepository.ProjectMetadataRepository
import com.darkrockstudios.apps.hammer.common.data.projectsrepository.ProjectsRepository
import com.darkrockstudios.apps.hammer.common.fileio.okio.toHPath
import okio.Path.Companion.toPath
import org.koin.mp.KoinPlatform.getKoin
import kotlin.collections.set

// This is only open for testing purposes
open class DataMigrator(
	private val globalSettingsRepository: GlobalSettingsRepository,
	private val projectsRepository: ProjectsRepository,
	private val projectMetadataRepository: ProjectMetadataRepository,
) {
	protected open val latestProjectDataVersion: Int = PROJECT_DATA_VERSION
	protected open fun getMigrators(): Map<Int, Migration> {
		val migrators = mutableMapOf<Int, Migration>()

		getKoin().get<Migration0_1>().addToMap(migrators)

		return migrators
	}

	private fun getProjects(): List<ProjectData> {
		val projDir = globalSettingsRepository.globalSettings.projectsDirectory.toPath()
		val projects = projectsRepository.getProjects(projDir.toHPath()).map { projDef ->
			val metadata = projectMetadataRepository.loadMetadata(projDef)
			ProjectData(projDef, metadata)
		}
		return projects
	}

	fun handleDataMigration() {
		if (checkIfMigrationNeeded()) {
			doMigration()
		}
	}

	fun checkIfMigrationNeeded(): Boolean {
		val projects = getProjects()
		return projects.any { projectData ->
			projectData.projectMetadata.info.dataVersion < latestProjectDataVersion
		}
	}

	private fun doMigration() {
		val projects = getProjects()
		val migrators = getMigrators()

		projects.forEach { projectData ->
			migrateProject(projectData, migrators)
		}
	}

	private fun migrateProject(projectData: ProjectData, migrators: Map<Int, Migration>) {
		val projectDataVersion = projectData.projectMetadata.info.dataVersion
		if (projectDataVersion < latestProjectDataVersion) {
			for (version in projectDataVersion + 1..latestProjectDataVersion) {
				val migrator =
					migrators[version] ?: error("Migrator not found for data version: $version")
				migrator.migrate(projectData.projectDef)

				// Mark that this version migration was successful
				projectMetadataRepository.updateMetadata(projectData.projectDef) { metadata ->
					metadata.copy(
						info = metadata.info.copy(
							dataVersion = version
						)
					)
				}
			}
		}
	}
}

private fun Migration.addToMap(migrators: MutableMap<Int, Migration>) {
	migrators[toVersion] = this
}

data class ProjectData(val projectDef: ProjectDef, val projectMetadata: ProjectMetadata)