package org.treesitter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TreeSitterTsxTest {
    @Test
    fun `should load TSX grammar from native library`() {
        val tsx = TreeSitterTsx()
        assertThat(tsx).isNotNull()
    }

    @Test
    fun `should parse simple JSX without errors`() {
        val tsx = TreeSitterTsx()
        val parser = TSParser()
        parser.language = tsx

        val code = """
            const Component = () => {
              return <div>Hello World</div>;
            };
        """.trimIndent()

        val tree = parser.parseString(null, code)
        val rootNode = tree.rootNode

        assertThat(rootNode.hasError())
            .withFailMessage("TSX grammar should parse JSX without errors")
            .isFalse()
    }

    @Test
    fun `should parse complex React component with JSX`() {
        val tsx = TreeSitterTsx()
        val parser = TSParser()
        parser.language = tsx

        val code = """
            import { useRouter } from 'next/router';

            interface ExamplePageProps {
              resetForm: () => void;
            }

            const ExamplePage: React.FC<ExamplePageProps> = () => {
              return (
                <div>
                  <span>Test</span>
                </div>
              );
            };

            export default ExamplePage;
        """.trimIndent()

        val tree = parser.parseString(null, code)
        val rootNode = tree.rootNode

        println("Has error: ${rootNode.hasError()}")
        println("Type: ${rootNode.type}")
        println("Child count: ${rootNode.childCount}")

        assertThat(rootNode.hasError())
            .withFailMessage("TSX grammar should parse React component without errors")
            .isFalse()
    }
}
