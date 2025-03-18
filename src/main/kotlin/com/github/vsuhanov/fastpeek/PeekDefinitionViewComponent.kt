package com.github.vsuhanov.fastpeek

import com.intellij.codeInsight.hint.ImplementationViewElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.refactoring.suggested.startOffset
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel

class PeekDefinitionViewComponent : JPanel {
    private var myEditorReleased: Boolean = false

    private val TEXT_PAGE_KEY = "Text";
    private lateinit var editorFactory: EditorFactory
    public lateinit var project: Project
//    private lateinit var mySwitcher: DefinitionSwitcher<ImplementationViewElement>

    public lateinit var myEditor: EditorEx

    //    @Volatile
//    private var myEditorReleased: Boolean = false
    private lateinit var myViewingPanel: JBPanel<JBPanel<*>>

    //    private lateinit var myBinarySwitch: CardLayout
//    private lateinit var myBinaryPanel: JPanel
//    private lateinit var myFileChooser: ComboBox<FileDescriptor>
//    private lateinit var myNonTextEditor: FileEditor
//    private lateinit var myCurrentNonTextEditorProvider: com.intellij.openapi.fileEditor.FileEditorProvider
//    private lateinit var myHint: JBPopup
    private lateinit var myTitle: @NlsContexts.TabTitle String
//    private lateinit var myToolbar: ActionToolbar
//    private lateinit var mySingleEntryPanel: JPanel

    private lateinit var implementationViewElement: ImplementationViewElement


    constructor(elements: Collection<out ImplementationViewElement>, index: Int) : super(BorderLayout()) {
        val firstElement = (if (!elements.isEmpty()) elements.iterator().next() else null) ?: return
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
        navigateToSymbolWithinEditor(element)
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

    public fun cleanup() {
        // TODO: implement cleanup
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
                replaceEditor(implementationViewElement, virtualFile, project, document)
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
            caretModel.currentCaret.moveToOffset(psiElement.startOffset)
            val logicalPosition = myEditor.offsetToLogicalPosition(psiElement.startOffset)
            ApplicationManager.getApplication().invokeLater {
                IdeFocusManager.getInstance(project).requestFocus(myEditor.contentComponent, true);
//                IdeFocusManager.getGlobalInstance().requestFocus(myEditor.component, false)
                myEditor.caretModel.moveToLogicalPosition(logicalPosition)
                myEditor.scrollingModel.scrollTo(logicalPosition, ScrollType.MAKE_VISIBLE)
            }
        }
    }

    private fun tuneEditor() {
        val scheme = EditorColorsManager.getInstance().globalScheme
        myEditor.colorsScheme = scheme
    }

    private fun replaceEditor(
        element: ImplementationViewElement,
        file: VirtualFile?,
        project: Project,
        document: Document
    ) {

        myViewingPanel.remove(myEditor.component)
        editorFactory.releaseEditor(myEditor)
        setupEditor(element)
        myViewingPanel.add(myEditor.component, TEXT_PAGE_KEY)
    }

    fun hasElementsToShow(): Boolean {
        return true
    }

    fun scrollIntoView() {
        navigateToSymbolWithinEditor(implementationViewElement)
    }

    fun getPreferredFocusableComponent(): JComponent {
        return myEditor.contentComponent
    }
}


private data class FileDescriptor(
    val file: VirtualFile,
    val index: Int,
)
