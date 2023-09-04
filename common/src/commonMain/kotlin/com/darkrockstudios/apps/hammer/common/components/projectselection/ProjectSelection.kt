package com.darkrockstudios.apps.hammer.common.components.projectselection

import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.darkrockstudios.apps.hammer.MR
import com.darkrockstudios.apps.hammer.common.components.projectselection.aboutapp.AboutApp
import com.darkrockstudios.apps.hammer.common.components.projectselection.accountsettings.AccountSettings
import com.darkrockstudios.apps.hammer.common.components.projectselection.projectslist.ProjectsList
import com.darkrockstudios.apps.hammer.common.dependencyinjection.HammerComponent
import dev.icerock.moko.resources.StringResource

interface ProjectSelection : HammerComponent {
	val slot: Value<ChildSlot<Config, Destination>>

	val showProjectDirectory: Boolean

	fun showLocation(location: Locations)

	enum class Locations(val text: StringResource) {
		Projects(MR.strings.project_select_nav_projects_list),
		Settings(MR.strings.project_select_nav_account_settings),
		AboutApp(MR.strings.project_select_nav_about_app),
	}

	sealed class Config(val location: Locations) : Parcelable {
		@Parcelize
		data object ProjectsList : Config(Locations.Projects)

		@Parcelize
		data object AccountSettings : Config(Locations.Settings)

		@Parcelize
		data object AboutApp : Config(Locations.AboutApp)
	}

	sealed class Destination {
		data class ProjectsListDestination(val component: ProjectsList) : Destination()
		data class AccountSettingsDestination(val component: AccountSettings) : Destination()
		data class AboutAppDestination(val component: AboutApp) : Destination()
	}
}