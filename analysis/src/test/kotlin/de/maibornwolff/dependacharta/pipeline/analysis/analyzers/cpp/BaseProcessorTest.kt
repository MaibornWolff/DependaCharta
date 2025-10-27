package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.model.CppContext
import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing.CppNodeProcessor
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import org.junit.jupiter.api.BeforeEach
import org.treesitter.TSNode
import org.treesitter.TSParser
import org.treesitter.TreeSitterCpp

abstract class BaseProcessorTest {
    protected var cpp = TreeSitterCpp()
    protected var namespace = Path(listOf("MyNamespace"))

    @BeforeEach
    fun before() {
        customSetup()
    }

    protected fun process(
        processor: CppNodeProcessor,
        cppCode: String
    ) = processor.process(createContext(cppCode), parseCode(cppCode, cpp))

    protected fun processCompound(
        processor: CppNodeProcessor,
        cppCode: String
    ) = processor.process(createContext(cppCode), parseCode(cppCode, cpp).getChild(0))

    protected fun createContext(sourceCode: String) = CppContext(FileInfo(SupportedLanguage.CPP, "MyFile.cpp", sourceCode), emptySet(), emptySet(), emptySet(), namespace)

    protected fun appliesTo(
        processor: CppNodeProcessor,
        cppCode: String
    ) = processor.appliesTo(parseCode(cppCode, cpp).getChild(0))

    protected fun parseCode(
        cppCode: String,
        cpp: TreeSitterCpp
    ): TSNode {
        val parser = TSParser()
        parser.language = cpp
        val tree = parser.parseString(null, cppCode)
        return tree.rootNode
    }

    open fun customSetup() {}
}
