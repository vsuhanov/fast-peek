package com.github.vsuhanov.fastpeek

import com.intellij.codeInsight.hint.ImplementationViewElement
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.event.KeyAdapter
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel

class PeekDefinitionViewComponent : JPanel {
    private var myEditorReleased: Boolean = false

    private val TEXT_PAGE_KEY = "Text";
    private lateinit var editorFactory: EditorFactory
    public lateinit var project: Project

    public lateinit var myEditor: EditorEx

    private lateinit var myViewingPanel: JBPanel<JBPanel<*>>

    private lateinit var myTitle: @NlsContexts.TabTitle String

    private lateinit var escKeyHandler: KeyAdapter
    private lateinit var implementationViewElement: ImplementationViewElement


    constructor(elements: Collection<out ImplementationViewElement>, index: Int, escKeyHandler: KeyAdapter) : super(
        BorderLayout()
    ) {
        val firstElement = (if (!elements.isEmpty()) elements.iterator().next() else null) ?: return
        this.escKeyHandler = escKeyHandler
        project = firstElement.project
        implementationViewElement = firstElement
        editorFactory = EditorFactory.getInstance()
        preferredSize = JBUI.size(600, 400)
        setupEditor(firstElement)

        val layout = CardLayout()
        myViewingPanel = JBPanel(layout)
        add(myViewingPanel, BorderLayout.CENTER)

        myViewingPanel.add(myEditor.component, "Text")

        revalidate();
        repaint();
    }

    private fun setupEditor(element: ImplementationViewElement) {
        val document = getDocument(element.containingFile) ?: return
        myEditor = editorFactory.createEditor(
            document,
            project,
            element.containingFile!!,
            false,
            EditorKind.MAIN_EDITOR
        ) as EditorEx
        myEditor.contentComponent.addKeyListener(escKeyHandler)


        myEditor.contentComponent.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (e!!.clickCount == 2 && e.button == MouseEvent.BUTTON1) {
                    triggerAction(PeekAction.MY_ACTION_ID, project, myEditor)
                }
                if (e.clickCount == 1 && e.button == MouseEvent.BUTTON2) {
                    triggerAction("GotoDeclaration", project, myEditor)
                }
                // handle double click
//                component.myEditor.contentComponent.dispatchEvent(e)
            }
        })
        navigateToSymbolWithinEditor(element)
    }

    fun triggerAction(actionId: String, project: Project, editor: Editor) {
        val action = ActionManager.getInstance().getAction(actionId)

        if (action != null) {
            val dataContext =
                IdeFocusManager.getInstance(project).getFocusedDescendantFor(editor.contentComponent)?.let {
                    DataManager.getInstance().getDataContext(it)
                } ?: DataContext.EMPTY_CONTEXT

            val event = AnActionEvent(
                null,
                dataContext,
                "fast-peek-popup",
                action.templatePresentation.clone(),
                ActionManager.getInstance(),
                0
            )
            ActionUtil.performActionDumbAwareWithCallbacks(action, event)

            action.actionPerformed(event)
        }

    }

    private fun getDocument(virtualFile: VirtualFile?): Document? {
        virtualFile ?: return null
        return FileDocumentManager.getInstance().getDocument(virtualFile)
    }

    private fun updateWithFunc(
        viewElements: Collection<out ImplementationViewElement>,
        func: (List<ImplementationViewElement>, List<FileDescriptor>) -> Boolean
    ) {
        val candidates = ArrayList<ImplementationViewElement>(viewElements.size)
        val files = ArrayList<FileDescriptor>(viewElements.size)
        val names = HashSet<String>()

        for (viewElement in viewElements) {
            if (viewElement.name != null) {
                names.add(viewElement.name!!)
            }
            if (names.size > 1) {
                break
            }
        }

        for (element in viewElements) {
            val file = element.containingFile ?: continue
            if (names.size > 1) {
                files.add(FileDescriptor(file, candidates.size))
            } else {
                files.add(
                    FileDescriptor(
                        file,
                        candidates.size,
                    )
                )
            }
            candidates.add(element)
        }

        func(candidates, files)
    }

    fun cleanup() {
        if (!myEditorReleased) {
            myEditorReleased = true // remove notify can be called several times for popup windows
            EditorFactory.getInstance().releaseEditor(myEditor)
        }
    }

    fun update(implementationElements: List<ImplementationViewElement>, elementIndex: Int) {
        updateWithFunc(
            implementationElements
        ) { elements, _ ->
            if (myEditor.isDisposed) return@updateWithFunc false;
            if (elements.isEmpty()) return@updateWithFunc false;

            implementationViewElement = elements[0]
            project = implementationViewElement.project;
            val virtualFile = implementationViewElement.containingFile;

            val document = getDocument(virtualFile)
            if (document != null) {
                replaceEditor(implementationViewElement)
            }

            revalidate();
            repaint();
            navigateToSymbolWithinEditor(implementationViewElement)

            return@updateWithFunc true
        }
    }

    private fun navigateToSymbolWithinEditor(element: ImplementationViewElement) {
        val psiElement = element.elementForShowUsages
        if (psiElement != null) {
            val caretModel = myEditor.caretModel
            caretModel.currentCaret.moveToOffset(psiElement.textRange.startOffset)
            val logicalPosition = myEditor.offsetToLogicalPosition(psiElement.textRange.startOffset)
            ApplicationManager.getApplication().invokeLater {
                IdeFocusManager.getInstance(project).requestFocus(myEditor.contentComponent, true);
//                IdeFocusManager.getGlobalInstance().requestFocus(myEditor.component, false)
                myEditor.caretModel.moveToLogicalPosition(logicalPosition)
                myEditor.scrollingModel.scrollTo(logicalPosition, ScrollType.MAKE_VISIBLE)
            }
        }
    }

    private fun replaceEditor(
        element: ImplementationViewElement,
    ) {

        myViewingPanel.remove(myEditor.component)
        editorFactory.releaseEditor(myEditor)
        setupEditor(element)
        myViewingPanel.add(myEditor.component, TEXT_PAGE_KEY)
    }

    fun hasElementsToShow(): Boolean {
        return true
    }


    fun getPreferredFocusableComponent(): JComponent {
        return myEditor.contentComponent
    }
}


private data class FileDescriptor(
    val file: VirtualFile,
    val index: Int,
)
