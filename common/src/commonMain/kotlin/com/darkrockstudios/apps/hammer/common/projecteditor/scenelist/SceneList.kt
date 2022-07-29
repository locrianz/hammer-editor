package com.darkrockstudios.apps.hammer.common.projecteditor.scenelist

import com.arkivanov.decompose.value.Value
import com.darkrockstudios.apps.hammer.common.data.ProjectDef
import com.darkrockstudios.apps.hammer.common.data.SceneBuffer
import com.darkrockstudios.apps.hammer.common.data.SceneItem
import com.darkrockstudios.apps.hammer.common.data.SceneSummary
import com.darkrockstudios.apps.hammer.common.di.HammerComponent

interface SceneList : HammerComponent {
    val state: Value<State>
    fun onSceneSelected(sceneDef: SceneItem)
    fun updateSceneOrder(scenes: SceneSummary)
    fun moveScene(from: Int, to: Int)
    fun loadScenes()
    fun createScene(sceneName: String)
    fun deleteScene(sceneDef: SceneItem)

    fun onSceneListUpdate(scenes: SceneSummary)
    fun onSceneBufferUpdate(sceneBuffer: SceneBuffer)

    data class State(
        val projectDef: ProjectDef,
        val selectedSceneItem: SceneItem? = null,
        val scenes: SceneSummary = SceneSummary(emptyList(), emptySet())
    )
}