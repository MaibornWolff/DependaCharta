package de.maibornwolff.dependacharta.pipeline.analysis.analyzers

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.javascript.JavascriptAnalyzer
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.FileInfo
import de.maibornwolff.dependacharta.pipeline.analysis.model.NodeType
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.shared.SupportedLanguage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class JavascriptAnalyzerTest {
    @Test
    fun `should convert exported ES6 class to node with type CLASS`() {
        // given
        val javascriptCode = """
            export class MyGreatClass {}
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "TestClass.js",
                javascriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.CLASS, Path(listOf("TestClass", "MyGreatClass")))
            )
    }

    @Test
    fun `should convert exported ES6 function to node with type FUNCTION`() {
        // given
        val javascriptCode = """
            export function myGreatFunction() {}
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "TestClass.js",
                javascriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.FUNCTION, Path(listOf("TestClass", "myGreatFunction")))
            )
    }

    @Test
    fun `should convert exported ES6 const to node with type VARIABLE`() {
        // given
        val javascriptCode = """
            export const MY_CONSTANT = "value"
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "TestClass.js",
                javascriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.VARIABLE, Path(listOf("TestClass", "MY_CONSTANT")))
            )
    }

    @Test
    fun `should add ES6 named imports to dependencies`() {
        // given
        val javascriptCode = """
            import { MyClass, MyFunction } from './MyModule'

            export class MyGreatClass extends MyClass {}
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "MyDirectory/MyGreatClass.js",
                javascriptCode
            )
        ).analyze()

        // then
        val expectedDependencies = listOf(
            Dependency(path = Path(listOf("MyDirectory", "MyModule", "MyClass"))),
            Dependency(path = Path(listOf("MyDirectory", "MyModule", "MyFunction")))
        )
        val node = report.nodes[0]
        assertThat(node.dependencies).containsAll(expectedDependencies)
    }

    @Test
    fun `should add ES6 default import to dependencies`() {
        // given
        val javascriptCode = """
            import MyModule from './MyModule'

            export class MyGreatClass extends MyModule {}
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "MyDirectory/MyGreatClass.js",
                javascriptCode
            )
        ).analyze()

        // then
        val expectedDependency = Dependency(
            path = Path(listOf("MyDirectory", "MyModule", "default"))
        )
        val node = report.nodes[0]
        assertThat(node.dependencies).contains(expectedDependency)
    }

    @Test
    fun `should handle CommonJS require`() {
        // given
        val javascriptCode = """
            const MyModule = require('./MyModule')

            class MyGreatClass extends MyModule {}
            module.exports = MyGreatClass
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "MyDirectory/MyGreatClass.js",
                javascriptCode
            )
        ).analyze()

        // then
        val expectedDependency = Dependency(
            path = Path(listOf("MyDirectory", "MyModule", "default"))
        )
        val node = report.nodes[0]
        assertThat(node.dependencies).contains(expectedDependency)
    }

    @Test
    fun `should handle CommonJS destructured require`() {
        // given
        val javascriptCode = """
            const { MyClass, MyFunction } = require('./MyModule')

            class MyGreatClass extends MyClass {}
            module.exports = MyGreatClass
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "MyDirectory/MyGreatClass.js",
                javascriptCode
            )
        ).analyze()

        // then
        val expectedDependencies = listOf(
            Dependency(path = Path(listOf("MyDirectory", "MyModule", "MyClass"))),
            Dependency(path = Path(listOf("MyDirectory", "MyModule", "MyFunction")))
        )
        val node = report.nodes[0]
        assertThat(node.dependencies).containsAll(expectedDependencies)
    }

    @Test
    fun `should handle CommonJS module exports`() {
        // given
        val javascriptCode = """
            class MyGreatClass {}
            module.exports = MyGreatClass
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "TestClass.js",
                javascriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.CLASS, Path(listOf("TestClass", "MyGreatClass")))
            )
    }

    @Test
    fun `should handle CommonJS named exports`() {
        // given
        val javascriptCode = """
            function myFunction() {}
            exports.myFunction = myFunction
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "TestClass.js",
                javascriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.FUNCTION, Path(listOf("TestClass", "myFunction")))
            )
    }

    @Test
    fun `should handle ES6 re-exports from index file`() {
        // given
        val javascriptCode = """
            export { MyClass } from './MyClass'
            export { MyFunction } from './MyFunction'
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "MyDirectory/index.js",
                javascriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes).hasSize(2)
        assertThat(report.nodes)
            .extracting("nodeType")
            .containsExactly(NodeType.REEXPORT, NodeType.REEXPORT)
    }

    @Test
    fun `should handle ES6 wildcard re-exports`() {
        // given
        val javascriptCode = """
            export * from './MyModule'
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "MyDirectory/index.js",
                javascriptCode
            )
        ).analyze()

        // then
        val expectedDependency = Dependency(
            path = Path(listOf("MyDirectory", "MyModule")),
            isWildcard = true
        )
        val node = report.nodes[0]
        assertThat(node.nodeType).isEqualTo(NodeType.REEXPORT)
        assertThat(node.dependencies).contains(expectedDependency)
    }

    @Test
    fun `should parse JSX files`() {
        // given
        val javascriptCode = """
            import React from 'react'

            export function MyComponent() {
                return <div>Hello World</div>
            }
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "MyComponent.jsx",
                javascriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.FUNCTION, Path(listOf("MyComponent", "MyComponent")))
            )
    }

    @Test
    fun `should handle mixed ES6 and CommonJS in dependencies`() {
        // given
        val javascriptCode = """
            import { ES6Class } from './ES6Module'
            const CommonJSClass = require('./CommonJSModule')

            export class MyClass extends ES6Class {}
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "MyDirectory/MyClass.js",
                javascriptCode
            )
        ).analyze()

        // then
        val expectedDependencies = listOf(
            Dependency(path = Path(listOf("MyDirectory", "ES6Module", "ES6Class"))),
            Dependency(path = Path(listOf("MyDirectory", "CommonJSModule", "default")))
        )
        val node = report.nodes[0]
        assertThat(node.dependencies).containsAll(expectedDependencies)
    }

    @Test
    fun `should handle ES6 namespace imports`() {
        // given
        val javascriptCode = """
            import * as MyModule from './MyModule'

            export class MyClass {}
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "MyDirectory/MyClass.js",
                javascriptCode
            )
        ).analyze()

        // then
        val expectedDependency = Dependency(
            path = Path(listOf("MyDirectory", "MyModule")),
            isWildcard = true
        )
        val node = report.nodes[0]
        assertThat(node.dependencies).contains(expectedDependency)
    }

    @Test
    fun `should resolve relative import paths`() {
        // given
        val javascriptCode = """
            import { MyClass } from '../parent/MyClass'

            export class MyGreatClass extends MyClass {}
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "MyDirectory/child/MyGreatClass.js",
                javascriptCode
            )
        ).analyze()

        // then
        val expectedDependency = Dependency(
            path = Path(listOf("MyDirectory", "parent", "MyClass", "MyClass"))
        )
        val node = report.nodes[0]
        assertThat(node.dependencies).contains(expectedDependency)
    }

    @Test
    @Disabled("Anonymous default exports not yet supported - queries don't match AST structure")
    fun `should handle default export of anonymous class`() {
        // given
        val javascriptCode = """
            export default class {}
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "TestClass.js",
                javascriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.CLASS, Path(listOf("TestClass", "default")))
            )
    }

    @Test
    @Disabled("Anonymous default exports not yet supported - queries don't match AST structure")
    fun `should handle default export of anonymous function`() {
        // given
        val javascriptCode = """
            export default function() {}
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "TestFunction.js",
                javascriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.FUNCTION, Path(listOf("TestFunction", "default")))
            )
    }

    @Test
    fun `should handle default export of declared identifier`() {
        // given
        val javascriptCode = """
            const buildFunction = () => {
              return "hello";
            };

            export default buildFunction;
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "module.js",
                javascriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.VARIABLE, Path(listOf("module", "default")))
            )
    }

    @Test
    fun `should handle exported const with arrow function and external imports`() {
        // given
        val javascriptCode = """
            import { moduleA, moduleB } from 'external-lib';
            import { CONFIG } from '../../shared/constants/config';

            export const validateData = (arg1, arg2, arg3, arg4, arg5, arg6) => {
              let valid = true;
              return valid;
            };

            export const createInstance = (container) => {
              return new moduleA.Instance();
            };
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "src/utils/helpers.js",
                javascriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactlyInAnyOrder(
                tuple(NodeType.VARIABLE, Path(listOf("src", "utils", "helpers", "validateData"))),
                tuple(NodeType.VARIABLE, Path(listOf("src", "utils", "helpers", "createInstance")))
            )

        val expectedDependencies = listOf(
            Dependency(path = Path(listOf("external-lib", "moduleA"))),
            Dependency(path = Path(listOf("external-lib", "moduleB"))),
            Dependency(path = Path(listOf("shared", "constants", "config", "CONFIG")))
        )
        val node = report.nodes[0]
        assertThat(node.dependencies).containsAll(expectedDependencies)
    }

    @Test
    fun `should handle export list syntax with declared constants`() {
        // given
        val javascriptCode = """
            const configObject = {
              key: undefined,
              value: undefined,
            };

            const settingsObject = {
              enabled: undefined,
            };

            export {
              configObject,
              settingsObject,
            };
        """.trimIndent()

        // when
        val report = JavascriptAnalyzer(
            FileInfo(
                SupportedLanguage.JAVASCRIPT,
                "config/data.js",
                javascriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactlyInAnyOrder(
                tuple(NodeType.VARIABLE, Path(listOf("config", "data", "configObject"))),
                tuple(NodeType.VARIABLE, Path(listOf("config", "data", "settingsObject")))
            )
    }
}
