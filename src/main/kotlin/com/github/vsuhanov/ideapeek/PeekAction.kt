package com.github.vsuhanov.ideapeek

import com.intellij.codeInsight.hint.ImplementationViewSession
import com.intellij.codeInsight.hint.ImplementationViewSessionFactory
import com.intellij.codeInsight.hint.PsiImplementationViewSession
import com.intellij.codeInsight.navigation.ImplementationSearcher
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiDocumentManager

class PeekAction : AnAction() {
    override fun actionPerformed(anActionEvent: AnActionEvent) {
        val context = anActionEvent.dataContext

        val project = anActionEvent.project ?: return

        // TODO: don't know if it's necessary, copied from ShowRelatedElementsActionBase
        PsiDocumentManager.getInstance(project).commitAllDocuments();

        val editor = anActionEvent.getData(CommonDataKeys.EDITOR)
                ?: return

//        val psiFile = PsiUtilBase.getPsiFileInEditor(editor, project) ?: return

        val logicalPosition = editor.caretModel.logicalPosition
//        val offset = editor.logicalPositionToOffset(logicalPosition)
//        val elementAtCaret = psiFile.findElementAt(offset) ?: return

//        val definition: PsiElement = findDefinition(elementAtCaret) ?: return

//        val virtualFile = definition.containingFile.virtualFile ?: return

        try {
            val sessionFactories = getSessionFactories()
            for (factory in sessionFactories) {
                val session = factory.createSession(context, project, true, true) ?: continue

                ensureValid(session, context)
                showPeekDefinition(session)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    private fun showPeekDefinition(session: ImplementationViewSession) {
        // TODO("Not yet implemented")
        val impls = session.implementationElements;
        if (impls.isEmpty()) {
            return;
        }

        val project = session.project;
        val virtualFile = session.file
        var index = 0
        // TODO: for some reason it's important to know if it's invoked from editor and or via shortcut
        if (virtualFile != null && impls.size > 1) {
            val containingFile = impls[0].containingFile
            if (virtualFile == containingFile) {
                val secondContainingFile = impls[1].containingFile
                if (secondContainingFile != null && secondContainingFile != containingFile) {
                    index = 1
                }
            }
        }

        PeekDefinitionPopupManager.getInstance()
                .showImplementationsPopup(session, impls, index, "Peek Definition")

    }

//    private fun findDefinition(element: PsiElement): PsiElement? {
//        return PsiTreeUtil.getParentOfType(element, PsiElement::class.java)
//        return element.reference?.resolve() ?: return null
//        // Implement logic to find the definition of the symbol
//        // This is a placeholder and needs to be replaced with actual implementation
//    }

    private fun getSessionFactories(): List<ImplementationViewSessionFactory> {
        return ImplementationViewSessionFactory.EP_NAME.getExtensionList(null);
    }

    private fun createImplementationSearcher(): ImplementationSearcher {
        return PsiImplementationViewSession.createImplementationsSearcher(true);
    }

    private fun ensureValid(session: ImplementationViewSession, context: kotlin.Any?) {
        var contextFile: com.intellij.psi.PsiFile? = null
        if (context is DataContext) {
            contextFile = CommonDataKeys.PSI_FILE.getData(context)
        }
        if (context is com.intellij.psi.PsiElement) {
            contextFile = context.getContainingFile()
        }
        val contextVirtualFile: com.intellij.openapi.vfs.VirtualFile? = if (contextFile != null) contextFile.getVirtualFile() else null
        val sessionVirtualFile: com.intellij.openapi.vfs.VirtualFile? = session.file
        if (contextVirtualFile != null && (contextVirtualFile == sessionVirtualFile)) {
            com.intellij.psi.util.PsiUtilCore.ensureValid((contextFile)!!)
        } else if (sessionVirtualFile != null && !sessionVirtualFile.isValid()) {
            throw com.intellij.openapi.vfs.InvalidVirtualFileAccessException(sessionVirtualFile)
        }
    }

}
