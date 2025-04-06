package com.github.vsuhanov.fastpeek

import com.intellij.codeInsight.hint.ImplementationViewElement
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR
import com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.*
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor.NAVIGATE_IN_EDITOR
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiManager
import com.intellij.ui.DeferredIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.UIManager

class PeekDefinitionViewComponent : JPanel, DataProvider {
    private var myEditorReleased: Boolean = false

    private lateinit var editorFactory: EditorFactory
    internal lateinit var project: Project

    internal lateinit var myEditor: EditorEx
    private lateinit var myViewingPanel: JBPanel<JBPanel<*>>

    private lateinit var label: JBTextField
    private lateinit var iconLabel: JBLabel

    private lateinit var escKeyHandler: KeyAdapter
    private lateinit var implementationViewElement: ImplementationViewElement
//    private lateinit var connection: MessageBusConnection

    constructor(elements: Collection<ImplementationViewElement>, index: Int, escKeyHandler: KeyAdapter) : super(
        BorderLayout()
    ) {

        val firstElement = (if (!elements.isEmpty()) elements.iterator().next() else null) ?: return
        this.escKeyHandler = escKeyHandler
        project = firstElement.project
        implementationViewElement = firstElement
        editorFactory = EditorFactory.getInstance()
        preferredSize = JBUI.size(600, 400)
        setupEditor(firstElement)

        val layout = BorderLayout()
        myViewingPanel = JBPanel(layout)

        val containingFile = firstElement.containingFile
        label = createSelectableTextLabel(createLabelText(containingFile))

        var icon: Icon? = getFileIcon(containingFile)
        if (icon is DeferredIcon) {
            icon = icon.evaluate()
        }
        val headerPanel: JBPanel<JBPanel<*>> = JBPanel(BorderLayout())


        iconLabel = JBLabel(icon)
        iconLabel.horizontalAlignment = JBLabel.CENTER
        headerPanel.add(iconLabel, BorderLayout.WEST)
        headerPanel.add(label, BorderLayout.CENTER)
        headerPanel.border = JBEmptyBorder(8)

        myViewingPanel.add(myEditor.component, BorderLayout.CENTER)
        myViewingPanel.add(headerPanel, BorderLayout.NORTH)

        add(myViewingPanel, BorderLayout.CENTER)
        ApplicationManager.getApplication().invokeLater {
            iconLabel.revalidate()
            iconLabel.repaint()
            revalidate();
            repaint();
        }

        DataManager.registerDataProvider(this, this)

//        val actionListner = object : AnActionListener {
//            override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
//                super.beforeActionPerformed(action, event)
//            }
//        }
//
//        val connection: MessageBusConnection = ApplicationManager.getApplication().messageBus.connect()
//        connection.subscribe(AnActionListener.TOPIC, actionListner)
    }


    private fun getFileIcon(file: VirtualFile?): Icon? {
        if (file != null) {
            val psiFile = PsiManager.getInstance(project).findFile(file)
            return psiFile?.getIcon(com.intellij.openapi.util.Iconable.ICON_FLAG_VISIBILITY)
                ?: UIManager.getIcon("FileView.fileIcon");
        }

        return UIManager.getIcon("FileView.fileIcon");
    }

    fun createLabelText(file: VirtualFile?): String {
        if (file == null) {
            return ""
        }
        return file.presentableName;
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

    fun update(implementationElements: List<ImplementationViewElement>, elementIndex: Int) {
        updateWithFunc(
            implementationElements
        ) { elements, _ ->
            if (myEditor.isDisposed) return@updateWithFunc false;
            if (elements.isEmpty()) return@updateWithFunc false;

            implementationViewElement = elements[0]
            project = implementationViewElement.project
            val virtualFile = implementationViewElement.containingFile

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
        myViewingPanel.add(myEditor.component, BorderLayout.CENTER)
        iconLabel.icon = getFileIcon(element.containingFile)
        label.text = createLabelText(element.containingFile)
    }

    private fun createSelectableTextLabel(text: String): JBTextField {
        val labelLikeField = JBTextField(text)
        labelLikeField.isEditable = false
        labelLikeField.border = null
        labelLikeField.setOpaque(false)
        labelLikeField.setForeground(UIManager.getColor("Label.foreground"));
        return labelLikeField
    }

    fun getPreferredFocusableComponent(): JComponent {
        return myEditor.contentComponent
    }


    fun hasElementsToShow(): Boolean {
        return true
    }

    fun cleanup() {
        if (!myEditorReleased) {
            myEditorReleased = true // remove notify can be called several times for popup windows
            EditorFactory.getInstance().releaseEditor(myEditor)
        }

        DataManager.removeDataProvider(this)
//        connection.disconnect()
    }

    override fun getData(dataId: String): Any? {
        when {
            NAVIGATE_IN_EDITOR.`is`(dataId) -> myEditor
            EDITOR.`is`(dataId) -> myEditor
            PROJECT.`is`(dataId) -> project
        }
        return null
    }
}


private data class FileDescriptor(
    val file: VirtualFile,
    val index: Int,
)
