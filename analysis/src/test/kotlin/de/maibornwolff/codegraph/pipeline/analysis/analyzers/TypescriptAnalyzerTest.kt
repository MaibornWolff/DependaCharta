package de.maibornwolff.codegraph.pipeline.analysis.analyzers

import de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript.DEFAULT_EXPORT_NODE_NAME
import de.maibornwolff.codegraph.pipeline.analysis.analyzers.typescript.TypescriptAnalyzer
import de.maibornwolff.codegraph.pipeline.analysis.model.Dependency
import de.maibornwolff.codegraph.pipeline.analysis.model.FileInfo
import de.maibornwolff.codegraph.pipeline.analysis.model.NodeType
import de.maibornwolff.codegraph.pipeline.analysis.model.Path
import de.maibornwolff.codegraph.pipeline.analysis.model.Type
import de.maibornwolff.codegraph.pipeline.shared.SupportedLanguage
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import java.io.File

class TypescriptAnalyzerTest {
    @Test
    fun `should convert path to correct path with names`() {
        // given
        val typescriptCode = """            
            export class Person {
                private name: string
                private age: number
            }
        """.trimIndent()
        val physicalPath = File("MyExample/Path/TypescriptAnalyzerTest.ts").path

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                physicalPath,
                typescriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes).extracting("pathWithName").containsExactly(
            Path(listOf("MyExample", "Path", "TypescriptAnalyzerTest", "Person"))
        )
    }

    @Test
    fun `should convert exported class to node with type CLASS`() {
        // given
        val typescriptCode = """            
            export class MyGreatClass {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "TestClass.ts",
                typescriptCode
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
    fun `should convert exported function to node with type FUNCTION`() {
        // given
        val typescriptCode = """            
            export function myGreatFunction() {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "TestClass.ts",
                typescriptCode
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
    fun `should convert exported interface to node with type INTERFACE`() {
        // given
        val typescriptCode = """            
            export interface MyGreatInterface {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "TestClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.INTERFACE, Path(listOf("TestClass", "MyGreatInterface")))
            )
    }

    @Test
    fun `should convert exported enum to node with type ENUM`() {
        // given
        val typescriptCode = """            
            export enum MyGreatEnum {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "TestClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.ENUM, Path(listOf("TestClass", "MyGreatEnum")))
            )
    }

    @Test
    fun `should convert exported type to node with type CLASS`() {
        // given
        val typescriptCode = """            
            export type MyGreatType = {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "TestClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.CLASS, Path(listOf("TestClass", "MyGreatType")))
            )
    }

    @Test
    fun `should convert exported variable to node with type VARIABLE`() {
        // given
        val typescriptCode = """            
            export var myGreatVariable = ""
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "TestClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.VARIABLE, Path(listOf("TestClass", "myGreatVariable")))
            )
    }

    @Test
    fun `should convert exported constant to node with type VARIABLE`() {
        // given
        val typescriptCode = """            
            export const MY_GREAT_CONSTANT = ""
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "TestClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes)
            .extracting("nodeType", "pathWithName")
            .containsExactly(
                tuple(NodeType.VARIABLE, Path(listOf("TestClass", "MY_GREAT_CONSTANT")))
            )
    }

    @Test
    fun `should add named imports to dependencies of a node`() {
        // given
        val typescriptCode = """
            import { MyGreatInterface, AnotherGreatInterface } from './MyGreatInterface';
            
            export class MyGreatClass implements MyGreatInterface {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyGreatClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedDependencies = listOf(
            Dependency(
                path = Path(listOf("MyDirectory", "MyGreatInterface", "MyGreatInterface")),
            ),
            Dependency(
                path = Path(listOf("MyDirectory", "MyGreatInterface", "AnotherGreatInterface")),
            )
        )
        val node = report.nodes[0]
        assertThat(node.dependencies).containsAll(expectedDependencies)
    }

    @Test
    fun `should add default import to dependencies of a node`() {
        // given
        val typescriptCode = """
            import MyGreatInterface from 'MyGreatInterface';
            
            export class MyGreatClass implements MyGreatInterface {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "TestClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedDependency = Dependency(
            path = Path(listOf("MyGreatInterface", "MyGreatInterface_$DEFAULT_EXPORT_NODE_NAME")),
        )
        val expectedType = Type.simple("MyGreatInterface_$DEFAULT_EXPORT_NODE_NAME")
        val node = report.nodes[0]
        assertThat(node.dependencies).contains(expectedDependency)
        assertThat(node.usedTypes).containsExactly(expectedType)
    }

    @Test
    fun `should resolve relative import on same directory`() {
        // given
        val typescriptCode = """
            import { MyGreatInterface } from './MyGreatInterface';
            
            export class MyGreatClass implements MyGreatInterface {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyGreatClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedDependency = Dependency(
            path = Path(listOf("MyDirectory", "MyGreatInterface", "MyGreatInterface")),
        )
        val expectedType = Type.simple("MyGreatInterface")
        val node = report.nodes[0]
        assertThat(node.dependencies).contains(expectedDependency)
        assertThat(node.usedTypes).containsExactly(expectedType)
    }

    @Test
    fun `should resolve relative import on nested directory`() {
        // given
        val typescriptCode = """
            import { MyGreatInterface } from '../MyGreatInterface';
            
            export class MyGreatClass implements MyGreatInterface {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyRoot/MyDirectory/MyGreatClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedDependency = Dependency(
            path = Path(listOf("MyRoot", "MyGreatInterface", "MyGreatInterface")),
        )
        val expectedType = Type.simple("MyGreatInterface")
        val node = report.nodes[0]
        assertThat(node.dependencies).contains(expectedDependency)
        assertThat(node.usedTypes).containsExactly(expectedType)
    }

    @Test
    fun `should trim file endings in imports`() {
        // given
        val typescriptCode = """
            import { MyGreatInterface } from 'MyGreatInterface.ts';
            
            export class MyGreatClass implements MyGreatInterface {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyGreatClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedDependency = Dependency(
            path = Path(listOf("MyGreatInterface", "MyGreatInterface")),
        )
        val node = report.nodes[0]
        assertThat(node.dependencies).contains(expectedDependency)
    }

    @Test
    fun `should handle index ts`() {
        // given
        val typescriptCode = """
             export { MyReexportedClass } from './MyInternalClass'
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/index.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedDependency = Dependency(
            path = Path(listOf("MyDirectory", "MyInternalClass", "MyReexportedClass")),
        )
        val node = report.nodes[0]
        assertThat(node.pathWithName).isEqualTo(Path(listOf("MyDirectory", "index", "MyReexportedClass")))
        assertThat(node.dependencies).contains(expectedDependency)
        assertThat(node.usedTypes).containsExactly(Type.simple("MyReexportedClass"))
    }

    @Test
    fun `should handle multiple reexports of same file in index ts`() {
        // given
        val typescriptCode = """
             export { AClass, AnotherClass } from './MyInternalModule'
             export { AThirdClass } from './AnotherInternalModule'
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/index.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedDependency1 = Dependency(path = Path(listOf("MyDirectory", "MyInternalModule", "AClass")))
        val expectedDependency2 = Dependency(path = Path(listOf("MyDirectory", "MyInternalModule", "AnotherClass")))
        val expectedDependency3 = Dependency(path = Path(listOf("MyDirectory", "AnotherInternalModule", "AThirdClass")))

        assertThat(report.nodes).hasSize(3)
        val nodeOfAClass = report.nodes[0]
        assertThat(nodeOfAClass.dependencies).contains(expectedDependency1)
        assertThat(nodeOfAClass.dependencies).doesNotContain(expectedDependency2, expectedDependency3)
        assertThat(nodeOfAClass.usedTypes).containsExactly(Type.simple("AClass"))

        val nodeOfAnotherClass = report.nodes[1]
        assertThat(nodeOfAnotherClass.dependencies).contains(expectedDependency2)
        assertThat(nodeOfAnotherClass.dependencies).doesNotContain(expectedDependency1, expectedDependency3)
        assertThat(nodeOfAnotherClass.usedTypes).containsExactly(Type.simple("AnotherClass"))

        val nodeOfAThirdClass = report.nodes[2]
        assertThat(nodeOfAThirdClass.dependencies).contains(expectedDependency3)
        assertThat(nodeOfAThirdClass.dependencies).doesNotContain(expectedDependency1, expectedDependency2)
        assertThat(nodeOfAThirdClass.usedTypes).containsExactly(Type.simple("AThirdClass"))
    }

    @Test
    fun `should add implicit dependency on index`() {
        // given
        val typescriptCode = """
            import { MyGreatInterface } from 'MyGreatInterface';
            
            export class MyGreatClass implements MyGreatInterface {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "TestClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedDependencies = listOf(
            Dependency(path = Path(listOf("MyGreatInterface", "MyGreatInterface"))),
            Dependency(path = Path(listOf("MyGreatInterface", "index", "MyGreatInterface")))
        )
        val expectedType = Type.simple("MyGreatInterface")
        val node = report.nodes[0]
        assertThat(node.dependencies).containsAll(expectedDependencies)
        assertThat(node.usedTypes).containsExactly(expectedType)
    }

    @Test
    fun `resulting node should be named after alias`() {
        val typescriptCode = """
             export { MyReexportedClass as MRC } from './MyInternalClass'
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/index.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedDependency = Dependency(
            path = Path(listOf("MyDirectory", "MyInternalClass", "MyReexportedClass")),
        )
        val node = report.nodes[0]
        assertThat(node.pathWithName).isEqualTo(Path(listOf("MyDirectory", "index", "MRC")))
        assertThat(node.dependencies).contains(expectedDependency)
        assertThat(node.usedTypes).containsExactly(Type.simple("MyReexportedClass"))
    }

    @Test
    fun `should add used type identifiers to usedTypes of a Node`() {
        // given
        val typescriptCode = """
            export class MyClass {
                private myType: MyType
            }
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedTypes = listOf(
            Type.simple("MyType")
        )
        val node = report.nodes[0]
        assertThat(node.usedTypes).containsAll(expectedTypes)
    }

    @Test
    fun `should only include types that are used in a Node in it`() {
        // given
        val typescriptCode = """
            export class MyFirstClass {
                private myType: MyFirstType
            }
            export class MySecondClass {
                private myType: MySecondType
            }
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyClasses.ts",
                typescriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes).extracting("pathWithName", "usedTypes").containsExactlyInAnyOrder(
            tuple(Path(listOf("MyClasses", "MyFirstClass")), setOf(Type.simple("MyFirstType"))),
            tuple(Path(listOf("MyClasses", "MySecondClass")), setOf(Type.simple("MySecondType")))
        )
    }

    @Test
    fun `should add used type identifier with alias to usedTypes of a Node`() {
        // given
        val typescriptCode = """
            import { MyType as MyRenamedType } from './MyType'
            export class MyClass {
                private myType: MyRenamedType
            }
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedTypes = listOf(
            Type.simple("MyType")
        )
        val node = report.nodes[0]
        assertThat(node.usedTypes).containsAll(expectedTypes)
    }

    @Test
    fun `should add constructor type to usedTypes of a Node`() {
        // given
        val typescriptCode = """
            export const myConst = new MyConst()
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedTypes = listOf(
            Type.simple("MyConst")
        )
        val node = report.nodes[0]
        assertThat(node.usedTypes).containsAll(expectedTypes)
    }

    @Test
    fun `should add constant type to usedTypes of a Node`() {
        // given
        val typescriptCode = """
            export const myConst = Utils.someRandomConst
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedTypes = listOf(
            Type.simple("Utils")
        )
        val node = report.nodes[0]
        assertThat(node.usedTypes).containsAll(expectedTypes)
    }

    @Test
    fun `should add implemented interface to usedTypes of a node`() {
        // given
        val typescriptCode = """
            export class MyInterfaceImplementation implements MyInterface {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedTypes = listOf(
            Type.simple("MyInterface")
        )
        val node = report.nodes[0]
        assertThat(node.usedTypes).containsAll(expectedTypes)
    }

    @Test
    fun `should add extended type to usedTypes of a node`() {
        // given
        val typescriptCode = """
            export class MyExtendedClass extends MyClass {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedTypes = listOf(
            Type.simple("MyClass")
        )
        val node = report.nodes[0]
        assertThat(node.usedTypes).containsAll(expectedTypes)
    }

    @Test
    fun `should add imported identifiers to used types of a node`() {
        // given
        val typescriptCode = """
        import { SCT } from 'bla'
        export class Creature {
            id: CreatureId;
            type: CreatureType;
        
            constructor(id: CreatureId, type: CreatureType = SCT) {
                this.id = id;
                this.type = SCT;
            }
        }        
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedTypes = listOf(Type.simple("SCT"))
        val node = report.nodes[0]
        assertThat(node.usedTypes).containsAll(expectedTypes)
        assertThat(node.usedTypes).doesNotContain(Type.simple("id"))
    }

    @Test
    fun `should add default export of a file as separate node`() {
        // given
        val typescriptCode = """
            export default MyDefaultExport;      
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val node = report.nodes[0]
        assertThat(node.dependencies).containsExactly(
            Dependency(
                Path(listOf("MyDirectory", "MyClass")),
                isWildcard = true
            )
        )
        assertThat(node.pathWithName).isEqualTo(Path(listOf("MyDirectory", "MyClass", "MyDirectory_MyClass_$DEFAULT_EXPORT_NODE_NAME")))
        assertThat(node.usedTypes).containsExactly(Type.simple("MyDefaultExport"))
    }

    @Test
    fun `should add non-exported class as a node`() {
        // given
        val typescriptCode = """
            class MyPrivateClass {}      
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val node = report.nodes[0]
        assertThat(node.pathWithName).isEqualTo(Path(listOf("MyDirectory", "MyClass", "MyPrivateClass")))
    }

    @Test
    fun `should not add the node itself to used types`() {
        // given
        val typescriptCode = """
            class MyPrivateClass {}      
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val node = report.nodes[0]
        assertThat(node.usedTypes).isEmpty()
    }

    @Test
    fun `should not add a node for declarations inside other declarations`() {
        // given
        val typescriptCode = """
            class MyOuterClass {
                myFunction() {
                    const nestedDeclaration = "Nested"
                }
            }  
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes).hasSize(1)
        assertThat(report.nodes[0].pathWithName).isEqualTo(Path(listOf("MyDirectory", "MyClass", "MyOuterClass")))
    }

    @Test
    fun `should add default imports and named imports from same file to node dependencies`() {
        // given
        val typescriptCode = """
            import MyGreatInterface, { SomeNamedImport } from 'MyGreatInterface';
            
            export class MyGreatClass implements MyGreatInterface<SomeNamedImport> {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes[0].dependencies).contains(
            Dependency(Path(listOf("MyGreatInterface", "MyGreatInterface_$DEFAULT_EXPORT_NODE_NAME"))),
            Dependency(Path(listOf("MyGreatInterface", "SomeNamedImport")))
        )
    }

    @Test
    fun `should add commonjs imports to node dependencies`() {
        // given
        val typescriptCode = """
            const myModule = require('myModule');
            export class MyGreatClass {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes[0].dependencies).contains(
            Dependency(Path(listOf("myModule", "myModule_$DEFAULT_EXPORT_NODE_NAME")))
        )
    }

    @Test
    fun `should add named commonjs imports to node dependencies`() {
        // given
        val typescriptCode = """
            const { myMethod } = require('myModule');
            export class MyGreatClass {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes[0].dependencies).contains(Dependency(Path(listOf("myModule", "myMethod"))))
    }

    @Test
    fun `should add alias for named commonjs imports to node dependencies`() {
        // given
        val typescriptCode = """
            const { myMethod: alias } = require('myModule');
            export class MyGreatClass {
                alias()
            }
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes[0].dependencies).contains(Dependency(Path(listOf("myModule", "myMethod"))))
        assertThat(report.nodes[0].usedTypes).contains(Type.simple("myMethod"))
    }

    @Test
    fun `should not crash on declare module statement`() {
        // given
        val typescriptCode = """
            declare module "*.md" {}   
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        assertThat(report.nodes).isEmpty()
    }

    @Test
    fun `should add identifiers used in an annotation on an exported node to usedTypes of that node`() {
        // given
        val typescriptCode = """
            import { MyComponentImport } from './MyComponentImport';
            
            @Component({
              imports: [MyComponentImport],
            })
            export class MyClass {}
        """.trimIndent()

        // when
        val report = TypescriptAnalyzer(
            FileInfo(
                SupportedLanguage.TYPESCRIPT,
                "MyDirectory/MyClass.ts",
                typescriptCode
            )
        ).analyze()

        // then
        val expectedTypes = listOf(
            Type.simple("MyComponentImport")
        )
        val node = report.nodes[0]
        assertThat(node.usedTypes).containsAll(expectedTypes)
    }
}
