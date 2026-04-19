package com.grantlittman.wearapp.presentation.navigation

/**
 * Navigation route constants.
 */
object Routes {
    const val PATTERN_LIST = "pattern_list"
    const val TIMER = "timer"
    const val EDITOR_NEW = "editor_new"
    const val EDITOR_EDIT = "editor_edit/{patternId}"

    fun editorEdit(patternId: String) = "editor_edit/$patternId"
}
