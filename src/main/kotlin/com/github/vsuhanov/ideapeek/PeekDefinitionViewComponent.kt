package com.github.vsuhanov.ideapeek

import com.intellij.codeInsight.hint.ImplementationViewElement
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import java.util.*
import javax.swing.JPanel

class PeekDefinitionViewComponent : JPanel {
    constructor(elements: Collection<out ImplementationViewElement>, index: Int) : super(BorderLayout()) {

        val firstElement = (if (!elements.isEmpty()) elements.iterator().next() else null) ?: return

        val project = firstElement.project

        val editorFactory = EditorFactory.getInstance()

        val document = getDocument(firstElement.containingFile) ?: return

        val editor = editorFactory.createEditor(document, project)

        val binarySwitch = CardLayout()

        val panel = JPanel(binarySwitch)

        // WTF is "constraints"
        panel.add(editor.component, "Text")


        add(panel, BorderLayout.CENTER)

        preferredSize = JBUI.size(600, 400)
//        switcher = DefinitionSwitcher

//        update(elements, )

    }

    fun getDocument(virtualFile: VirtualFile?): Document? {
        virtualFile ?: return null

        return FileDocumentManager.getInstance().getDocument(virtualFile)
    }

    private fun isImplementationReadOnly(file: VirtualFile?): Boolean {
        if (file != null) {
            val doc = getDocument(file)
            if (doc != null && doc.isWritable) {
                return true
            }
        }

        return false
    }

    private fun update(
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
                files.add(FileDescriptor(file, candidates.size, createTargetPresentation(element)))
            } else {
                files.add(FileDescriptor(file, candidates.size, createTargetPresentation(element.containingMemberOrSelf)))
            }
            candidates.add(element)
        }

        func(candidates, files)
    }

    private fun createTargetPresentation(element: ImplementationViewElement): TargetPresentation {
        return TargetPresentation.builder(element.presentableText)
                .locationText(element.locationText, element.locationIcon)
                .containerText(element.containerPresentation)
                .icon(element.locationIcon)
                .presentation()
    }

    public fun cleanup() {
        // TODO: implement cleanup
//        if (!myEditorReleased) {
//            myEditorReleased = true // remove notify can be called several times for popup windows
//            EditorFactory.getInstance().releaseEditor(myEditor)
//            disposeNonTextEditor()
//        }
    }

}


private data class FileDescriptor(
        val file: VirtualFile,
        val index: Int,
        val element: TargetPresentation
)
