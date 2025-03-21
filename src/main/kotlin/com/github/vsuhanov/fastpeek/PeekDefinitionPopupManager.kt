package com.github.vsuhanov.fastpeek

//import com.intellij.usages.UsageView
import com.intellij.codeInsight.hint.ImplementationViewElement
import com.intellij.codeInsight.hint.ImplementationViewSession
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.reference.SoftReference
import com.intellij.ui.WindowMoveListener
import com.intellij.ui.popup.AbstractPopup
import com.intellij.ui.popup.PopupPositionManager
import com.intellij.ui.popup.PopupUpdateProcessor
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.lang.ref.Reference
import java.lang.ref.WeakReference

class PeekDefinitionPopupManager {
    private var currentPopup: Reference<JBPopup>? = null

    fun showImplementationsPopup(
        session: ImplementationViewSession,
        implementationElements: List<ImplementationViewElement>,
        elementIndex: Int,
    ) {

        var popup = SoftReference.dereference(currentPopup)
//        // popup already exists.
        if (popup is AbstractPopup && popup.isVisible()) {
            (popup.component as? PeekDefinitionViewComponent)?.update(implementationElements, elementIndex)
            return
        }
        val escKeyHandler = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_ESCAPE) {
                    val p = SoftReference.dereference(currentPopup)
                    if (p is AbstractPopup && p.isVisible()) {
                        p.cancel()
                        e.consume()
                        return
                    }
                }
                super.keyPressed(e)
            }
        }
        val component = PeekDefinitionViewComponent(implementationElements, elementIndex, escKeyHandler)

        if (component.hasElementsToShow()) {
            val popup = createPopup(session, component)
            val getEditor = { component.myEditor }
            val getProject = { component.project }
            DataManager.registerDataProvider(component) { dataId ->
                when {
                    CommonDataKeys.EDITOR.`is`(dataId) -> getEditor()
                    CommonDataKeys.PROJECT.`is`(dataId) -> getProject()
                    else -> null
                }
            }

            PopupPositionManager.positionPopupInBestPosition(
                popup,
                session.editor,
                DataManager.getInstance().getDataContext(session.editor?.contentComponent)
            )
            currentPopup = WeakReference(popup)
        }
    }

    fun showImplementationsPopupHelper() {
    }

    fun createPopup(session: ImplementationViewSession, component: PeekDefinitionViewComponent): JBPopup {

        val updateProcessor: PopupUpdateProcessor = object : PopupUpdateProcessor(session.project) {
            override fun updatePopup(lookupItemObject: Any?) {
            }

            override fun onClosed(event: LightweightWindowEvent) {
                component.cleanup()
            }

        }

        val popupBuilder = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(component, component.getPreferredFocusableComponent())
            .setCancelOnWindowDeactivation(false)
            .setCancelOnClickOutside(true)
            .setCancelKeyEnabled(false)
            .setProject(session.project)
            .addListener(updateProcessor)
            .addUserData(updateProcessor)
            .setDimensionServiceKey(session.project, "peek.definition.popup", false)
            .setModalContext(false)
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setFocusable(true)
            .setCancelCallback {
                true
            }


        val listener = WindowMoveListener()
        listener.installTo(component)

        val popup = popupBuilder.createPopup()

        Disposer.register(popup, session)
        Disposer.register(popup) { listener.uninstallFrom(component) }

        return popup
    }

    companion object {
        fun getInstance(): PeekDefinitionPopupManager = service()
    }

}
