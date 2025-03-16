package com.github.vsuhanov.ideapeek

//import com.intellij.usages.UsageView
import com.intellij.codeInsight.hint.ImplementationViewComponent
import com.intellij.codeInsight.hint.ImplementationViewElement
import com.intellij.codeInsight.hint.ImplementationViewSession
import com.intellij.codeInsight.navigation.BackgroundUpdaterTaskBase
import com.intellij.codeInsight.navigation.ImplementationSearcher
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.reference.SoftReference
import com.intellij.ui.WindowMoveListener
import com.intellij.ui.popup.AbstractPopup
import com.intellij.ui.popup.PopupPositionManager
import com.intellij.ui.popup.PopupUpdateProcessor
import com.intellij.usages.Usage
import java.lang.ref.Reference
import java.lang.ref.WeakReference

class PeekDefinitionPopupManager {
    private var currentPopup: Reference<JBPopup>? = null
    private var currentTask: Reference<ImplementationsUpdaterTask>? = null

    private class ImplementationsUpdaterTask(private val mySession: ImplementationViewSession,
                                             private val myComponent: ImplementationViewComponent) : BackgroundUpdaterTaskBase<ImplementationViewElement?>(
            mySession.project, ImplementationSearcher.getSearchingForImplementations(), null) {
        private var myElements: List<ImplementationViewElement>? = null
        override fun getCaption(size: Int): String? {
            return null
        }

        override fun createUsage(element: ImplementationViewElement): Usage? {
            return element.usage
        }

        override fun run(indicator: ProgressIndicator) {
            super.run(indicator)
            myElements = mySession.searchImplementationsInBackground(indicator) {
                updateComponent(it)
            }
        }

        override fun getCurrentSize(): Int {
            return myElements?.size ?: super.getCurrentSize()
        }

        override fun onSuccess() {
            if (!cancelTask()) {
                // TODO: is this from newer version of Kotlin?
//                myElements?.let { myComponent.update(it, myComponent.index) }
            }
            super.onSuccess()
        }
    }


    fun showImplementationsPopup(
            session: ImplementationViewSession,
            implementationElements: List<ImplementationViewElement>,
            elementIndex: Int,
            title: String,
//                                 couldPinPopup: Boolean,
//                                 invokedFromEditor: Boolean,
//                                 invokedByShortcut: Boolean,
//                                 updatePopup: (lookupItemObject: Any?) -> Unit
    ) {
//        val usageView = Ref<UsageView?>()
//        val showInFindWindowProcessor = if (couldPinPopup) {
//            Consumer<ImplementationViewComponent> { component ->
//                usageView.set(component.showInUsageView())
//                currentTask = null
//            }
//        }
//        else {
//            null
//        }

        var popup = com.intellij.reference.SoftReference.dereference(currentPopup)
        // popup already exists.
        if (popup is AbstractPopup && popup.isVisible()) {
            val component = popup.component as? ImplementationViewComponent
            if (component != null) {
                //TODO: update contents of the existing popup with all the new stuff
//                component.update(implementationElements, elementIndex)
////                component.setShowInFindWindowProcessor(showInFindWindowProcessor)
//                updateInBackground(session, component, popup, usageView)
//                if (invokedByShortcut) {
//                    popup.focusPreferredComponent()
//                }
//                return
            }
        }

        val component = PeekDefinitionViewComponent(implementationElements, elementIndex)

        popup = createPopup(session, component)
        // possible that there is nothing to show
//        if (component.hasElementsToShow()) {
//            updateInBackground(session, component, popup, usageView)
//            component.setHint(popup, title)

        PopupPositionManager.positionPopupInBestPosition(popup, session.editor, DataManager.getInstance().getDataContext())
        currentPopup = WeakReference(popup)
    }


    fun createPopup(session: ImplementationViewSession, component: PeekDefinitionViewComponent): JBPopup {

        val updateProcessor: PopupUpdateProcessor = object : PopupUpdateProcessor(session.project) {
            override fun updatePopup(lookupItemObject: Any?) {
                // TODO: properly handle this
//                updatePopup(lookupItemObject)
            }

            override fun onClosed(event: LightweightWindowEvent) {
                component.cleanup()
            }
        }

        val popupBuilder = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(component, component)
                .setProject(session.project)
                .addListener(updateProcessor)
                .addUserData(updateProcessor)
                .setDimensionServiceKey(session.project, "peek.definition.popup", false)
                .setResizable(true)
                .setMovable(true)
//                .setRequestFocus(invokedFromEditor && LookupManager.getActiveLookup(session.editor) == null)
                .setRequestFocus(true)
                .setCancelCallback {
                    SoftReference.dereference(currentTask)?.cancelTask()
                    true
                }

        val listener = WindowMoveListener()
        listener.installTo(component)

        val popup = popupBuilder.createPopup()

        Disposer.register(popup, session)
        Disposer.register(popup, Disposable { listener.uninstallFrom(component) })

        return popup
    }

    companion object {
        @JvmStatic
        fun getInstance(): PeekDefinitionPopupManager = service()
    }

}
