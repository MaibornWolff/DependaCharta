package de.maibornwolff.dependacharta.pipeline.analysis.analyzers

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.abl.AblAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AblAnalyzerTest {
    @Test
    fun `analyzes ABL class file with class name`() {
        // Given
        val ablCode = """
            CLASS mypackage.MyClass:
                DEFINE VARIABLE myVar AS CHARACTER NO-UNDO.

                METHOD PUBLIC VOID doSomething():
                END METHOD.
            END CLASS.
        """.trimIndent()

        val fileInfo = FileInfo(
            language = SupportedLanguage.ABL,
            physicalPath = "src/mypackage/MyClass.cls",
            content = ablCode
        )

        // When
        val result = AblAnalyzer(fileInfo).analyze()

        // Then
        assertThat(result.nodes).hasSize(1)
        val node = result.nodes.first()
        assertThat(node.pathWithName.toString()).isEqualTo("mypackage.MyClass")
        assertThat(node.nodeType).isEqualTo(NodeType.CLASS)
        assertThat(node.language).isEqualTo(SupportedLanguage.ABL)
    }

    @Test
    fun `analyzes ABL procedure file`() {
        // Given
        val ablCode = """
            DEFINE VARIABLE cName AS CHARACTER NO-UNDO.

            cName = "Hello".
            MESSAGE cName VIEW-AS ALERT-BOX.
        """.trimIndent()

        val fileInfo = FileInfo(
            language = SupportedLanguage.ABL,
            physicalPath = "src/procedures/myproc.p",
            content = ablCode
        )

        // When
        val result = AblAnalyzer(fileInfo).analyze()

        // Then
        assertThat(result.nodes).hasSize(1)
        val node = result.nodes.first()
        assertThat(node.pathWithName.toString()).isEqualTo("src.procedures.myproc")
        assertThat(node.nodeType).isEqualTo(NodeType.CLASS)
        assertThat(node.language).isEqualTo(SupportedLanguage.ABL)
    }

    @Test
    fun `analyzes ABL window file`() {
        // Given
        val ablCode = """
            DEFINE BUTTON btnOK AUTO-GO LABEL "OK".

            ENABLE btnOK.
        """.trimIndent()

        val fileInfo = FileInfo(
            language = SupportedLanguage.ABL,
            physicalPath = "src/windows/mainwindow.w",
            content = ablCode
        )

        // When
        val result = AblAnalyzer(fileInfo).analyze()

        // Then
        assertThat(result.nodes).hasSize(1)
        val node = result.nodes.first()
        assertThat(node.pathWithName.toString()).isEqualTo("src.windows.mainwindow")
        assertThat(node.nodeType).isEqualTo(NodeType.CLASS)
    }

    @Test
    fun `analyzes ABL include file`() {
        // Given
        val ablCode = """
            DEFINE VARIABLE cSharedVar AS CHARACTER NO-UNDO.

            PROCEDURE SharedProcedure:
                MESSAGE "Shared".
            END PROCEDURE.
        """.trimIndent()

        val fileInfo = FileInfo(
            language = SupportedLanguage.ABL,
            physicalPath = "src/includes/shared.i",
            content = ablCode
        )

        // When
        val result = AblAnalyzer(fileInfo).analyze()

        // Then
        assertThat(result.nodes).hasSize(1)
        val node = result.nodes.first()
        assertThat(node.pathWithName.toString()).isEqualTo("src.includes.shared")
        assertThat(node.nodeType).isEqualTo(NodeType.CLASS)
    }

    @Test
    fun `extracts USING statements as dependencies`() {
        // Given
        val ablCode = """
            USING mypackage.OtherClass.
            USING Progress.Lang.*.

            CLASS mypackage.MyClass:
                DEFINE VARIABLE obj AS OtherClass NO-UNDO.
            END CLASS.
        """.trimIndent()

        val fileInfo = FileInfo(
            language = SupportedLanguage.ABL,
            physicalPath = "src/mypackage/MyClass.cls",
            content = ablCode
        )

        // When
        val result = AblAnalyzer(fileInfo).analyze()

        // Then
        val node = result.nodes.first()
        val dependencyPaths = node.dependencies.map { it.path.toString() }
        assertThat(dependencyPaths).contains("mypackage.OtherClass")
    }

    @Test
    fun `adds USING types to usedTypes for resolution`() {
        // Given
        val ablCode = """
            USING mypackage.OtherClass.
            USING core.Customer.

            CLASS mypackage.MyClass:
                DEFINE VARIABLE obj AS OtherClass NO-UNDO.
            END CLASS.
        """.trimIndent()

        val fileInfo = FileInfo(
            language = SupportedLanguage.ABL,
            physicalPath = "src/mypackage/MyClass.cls",
            content = ablCode
        )

        // When
        val result = AblAnalyzer(fileInfo).analyze()

        // Then
        val node = result.nodes.first()
        val usedTypeNames = node.usedTypes.map { it.name }
        assertThat(usedTypeNames).contains("OtherClass")
        assertThat(usedTypeNames).contains("Customer")
    }

    @Test
    fun `extracts RUN statements as dependencies`() {
        // Given
        val ablCode = """
            DEFINE VARIABLE cResult AS CHARACTER NO-UNDO.

            RUN mypackage/helper.p.
            RUN other/util.p (INPUT "test", OUTPUT cResult).
        """.trimIndent()

        val fileInfo = FileInfo(
            language = SupportedLanguage.ABL,
            physicalPath = "src/procedures/main.p",
            content = ablCode
        )

        // When
        val result = AblAnalyzer(fileInfo).analyze()

        // Then
        val node = result.nodes.first()
        val dependencyPaths = node.dependencies.map { it.path.toString() }
        assertThat(dependencyPaths).contains("mypackage.helper")
        assertThat(dependencyPaths).contains("other.util")
    }

    @Test
    fun `extracts inheritance from INHERITS clause`() {
        // Given
        val ablCode = """
            CLASS mypackage.ChildClass INHERITS mypackage.BaseClass:
            END CLASS.
        """.trimIndent()

        val fileInfo = FileInfo(
            language = SupportedLanguage.ABL,
            physicalPath = "src/mypackage/ChildClass.cls",
            content = ablCode
        )

        // When
        val result = AblAnalyzer(fileInfo).analyze()

        // Then
        val node = result.nodes.first()
        val usedTypeNames = node.usedTypes.map { it.name }
        assertThat(usedTypeNames).contains("BaseClass")
    }

    @Test
    fun `extracts interface implementation from IMPLEMENTS clause`() {
        // Given
        val ablCode = """
            CLASS mypackage.MyClass IMPLEMENTS mypackage.IMyInterface:
            END CLASS.
        """.trimIndent()

        val fileInfo = FileInfo(
            language = SupportedLanguage.ABL,
            physicalPath = "src/mypackage/MyClass.cls",
            content = ablCode
        )

        // When
        val result = AblAnalyzer(fileInfo).analyze()

        // Then
        val node = result.nodes.first()
        val usedTypeNames = node.usedTypes.map { it.name }
        assertThat(usedTypeNames).contains("IMyInterface")
    }

    @Test
    fun `extracts inheritance from INHERITS clause with simple class name`() {
        // Given
        val ablCode = """
            CLASS mypackage.ChildClass INHERITS BaseClass:
            END CLASS.
        """.trimIndent()

        val fileInfo = FileInfo(
            language = SupportedLanguage.ABL,
            physicalPath = "src/mypackage/ChildClass.cls",
            content = ablCode
        )

        // When
        val result = AblAnalyzer(fileInfo).analyze()

        // Then
        val node = result.nodes.first()
        val usedTypeNames = node.usedTypes.map { it.name }
        assertThat(usedTypeNames).contains("BaseClass")
    }

    @Test
    fun `extracts include directives as dependencies`() {
        // Given
        val ablCode = """
            DEFINE VARIABLE cName AS CHARACTER NO-UNDO.
            {include/error-handling.i}
            {shared/constants.i &param=value}
        """.trimIndent()

        val fileInfo = FileInfo(
            language = SupportedLanguage.ABL,
            physicalPath = "src/procedures/main.p",
            content = ablCode
        )

        // When
        val result = AblAnalyzer(fileInfo).analyze()

        // Then
        val node = result.nodes.first()
        val dependencyPaths = node.dependencies.map { it.path.toString() }
        assertThat(dependencyPaths).contains("include.error-handling")
        assertThat(dependencyPaths).contains("shared.constants")
    }
}
