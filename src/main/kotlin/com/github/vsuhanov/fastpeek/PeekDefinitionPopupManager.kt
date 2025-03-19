package com.github.vsuhanov.fastpeek

//import com.intellij.usages.UsageView
import com.intellij.codeInsight.hint.ImplementationViewElement
import com.intellij.codeInsight.hint.ImplementationViewSession
import com.intellij.codeInsight.navigation.BackgroundUpdaterTaskBase
import com.intellij.codeInsight.navigation.ImplementationSearcher
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.ui.GenericListComponentUpdater
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Ref
import com.intellij.reference.SoftReference
import com.intellij.ui.WindowMoveListener
import com.intellij.ui.popup.AbstractPopup
import com.intellij.ui.popup.PopupPositionManager
import com.intellij.ui.popup.PopupUpdateProcessor
import com.intellij.usages.Usage
import com.intellij.usages.UsageView
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.lang.ref.Reference
import java.lang.ref.WeakReference

class PeekDefinitionPopupManager {
    private var currentPopup: Reference<JBPopup>? = null
    private var currentTask: Reference<PeekDefinitionUpdaterTask>? = null

    private val usageView = Ref<UsageView?>()

    fun showImplementationsPopup(
            session: ImplementationViewSession,
            implementationElements: List<ImplementationViewElement>,
            elementIndex: Int,
            title: String,
//                                 couldPinPopup: Boolean,
//                                 invokedFromEditor: Boolean,
            invokedByShortcut: Boolean,
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

        var popup = SoftReference.dereference(currentPopup)
//        // popup already exists.
        if (popup is AbstractPopup && popup.isVisible()) {
            val component = popup.component as? PeekDefinitionViewComponent
            if (component != null) {
                component.update(implementationElements, elementIndex)
                // TODO: probably need to update in background in case of some slowness
//                updateInBackground(session, component, popup)

                // TODO: implement background update
//                if (invokedByShortcut) {
//                    popup.focusPreferredComponent()
//                }
            }

//            component.myEditor.contentComponent.addKeyListener()


            return
        }
//
        val escKeyHandler = object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent?) {
//                if (e?.keyCode == KeyEvent.VK_ESCAPE && session.project.service<FastPeekService>()
//                        .isEditorInInsertMode(component.myEditor)

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
//            updateInBackground(session, component, popup)
            val getEditor = { component.myEditor }
            val getProject = { component.project }
            DataManager.registerDataProvider(component) { dataId ->
                when {
                    CommonDataKeys.EDITOR.`is`(dataId) -> getEditor()
                    CommonDataKeys.PROJECT.`is`(dataId) -> getProject()
                    else -> null
                }
            }
//            component.setHint(popup, title)
            // possible that there is nothing to show
//        if (component.hasElementsToShow()) {
//            updateInBackground(session, component, popup, usageView)
//            component.setHint(popup, title)

            PopupPositionManager.positionPopupInBestPosition(
                    popup,
                    session.editor,
                    DataManager.getInstance().getDataContext()
            )
            currentPopup = WeakReference(popup)
        }
    }

    public fun showImplementationsPopupHelper() {
//        var popup = SoftReference.dereference(currentPopup)
//        // popup already exists.
//        if (popup is AbstractPopup && popup.isVisible()) {
//            val component = popup.component as? PeekDefinitionViewComponent
//            if (component != null) {
//                component.scrollIntoView()
//            }
//
//            return
//        }
    }

    private fun updateInBackground(
            session: ImplementationViewSession,
            component: PeekDefinitionViewComponent,
            popup: JBPopup
    ) {
        SoftReference.dereference(currentTask)?.cancelTask()

        if (!session.needUpdateInBackground()) return  // already found

        val task = PeekDefinitionUpdaterTask(session, component).apply {
            val updater =
                    PeekDefinitionViewComponentUpdater(component, if (session.elementRequiresIncludeSelf()) 1 else 0)
            init(popup, updater, usageView)
        }
        currentTask = WeakReference(task)
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
    }

    private class PeekDefinitionUpdaterTask(
            private val mySession: ImplementationViewSession,
            private val myComponent: PeekDefinitionViewComponent
    ) : BackgroundUpdaterTaskBase<ImplementationViewElement?>(
            mySession.project, ImplementationSearcher.getSearchingForImplementations(), null
    ) {
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
                // TODO: use index
//                myElements?.let { myComponent.update(it, myComponent.index) }
                myElements?.let { myComponent.update(it, 0) }
            }
            super.onSuccess()
        }
    }

    private class PeekDefinitionViewComponentUpdater(
            private val myComponent: PeekDefinitionViewComponent,
            private val myIncludeSelfIdx: Int
    ) :
            GenericListComponentUpdater<ImplementationViewElement?> {
        override fun paintBusy(paintBusy: Boolean) {
            //todo notify busy
        }

        override fun replaceModel(data: List<ImplementationViewElement?>) {
            // TODO: fix this
//            val elements = myComponent.elements
//            val startIdx = elements.size - myIncludeSelfIdx
//            val result: MutableList<ImplementationViewElement?> = ArrayList()
//            Collections.addAll(result, *elements)
//            result.addAll(data.subList(startIdx, data.size))
//            myComponent.update(result, myComponent.index)
        }
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

//                .setRequestFocus(invokedFromEditor && LookupManager.getActiveLookup(session.editor) == null)

                .setRequestFocus(true)
                .setFocusable(true)
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
        fun getInstance(): PeekDefinitionPopupManager = service()
    }

}
