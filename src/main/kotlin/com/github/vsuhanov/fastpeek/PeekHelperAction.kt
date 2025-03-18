package com.github.vsuhanov.fastpeek

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class PeekHelperAction : AnAction() {
    private val MY_ACTION_ID = "com.github.vsuhanov.fastpeek.PeekActionHelper"


    override fun actionPerformed(anActionEvent: AnActionEvent) {
        PeekDefinitionPopupManager.getInstance()
            .showImplementationsPopupHelper()
    }


}
