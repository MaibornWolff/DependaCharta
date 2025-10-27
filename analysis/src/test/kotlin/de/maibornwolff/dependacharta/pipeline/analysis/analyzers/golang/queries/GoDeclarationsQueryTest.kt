package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.golang.queries

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.treesitter.TSParser
import org.treesitter.TreeSitterGo
import org.junit.jupiter.api.Test

class GoDeclarationsQueryTest {
    private val golang = TreeSitterGo()
    private val query = GoDeclarationsQuery(golang)

    private fun parseGoCode(code: String) =
        TSParser()
            .apply {
                language = golang
            }.parseString(null, code)
            .rootNode

    @Test
    fun `should detect function declarations`() {
        // Given
        val goCode = """
            package main
            
            func main() {}
            func helper() {}
        """.trimIndent()

        // When
        val rootNode = parseGoCode(goCode)
        val declarations = query.execute(rootNode)

        // Then
        assertEquals(2, declarations.size)
        val declarationTypes = declarations.map { it.type }
        assertTrue(declarationTypes.all { it == "function_declaration" })
    }

    @Test
    fun `should detect type declarations`() {
        // Given
        val goCode = """
            package main
            
            type User struct {
                Name string
            }
            
            type Writer interface {
                Write([]byte) error
            }
        """.trimIndent()

        // When
        val rootNode = parseGoCode(goCode)
        val declarations = query.execute(rootNode)

        // Then
        assertEquals(2, declarations.size)
        val declarationTypes = declarations.map { it.type }
        assertTrue(declarationTypes.all { it == "type_declaration" })
    }

    @Test
    fun `should detect method declarations`() {
        // Given - This test should FAIL initially
        val goCode = """
            package main
            
            type User struct {
                Name string
            }
            
            func (u *User) GetName() string {
                return u.Name
            }
            
            func (u *User) SetName(name string) {
                u.Name = name
            }
        """.trimIndent()

        // When
        val rootNode = parseGoCode(goCode)
        val declarations = query.execute(rootNode)

        // Then
        // Should find: 1 type_declaration + 2 method_declarations = 3 total
        assertEquals(3, declarations.size, "Should detect type declaration AND method declarations")

        val declarationTypes = declarations.map { it.type }
        assertTrue(declarationTypes.contains("type_declaration"), "Should contain type_declaration")
        assertTrue(declarationTypes.contains("method_declaration"), "Should contain method_declaration")

        // Should have 2 method declarations
        val methodCount = declarationTypes.count { it == "method_declaration" }
        assertEquals(2, methodCount, "Should detect both method declarations")
    }

    @Test
    fun `should detect mixed declarations`() {
        // Given - This test should FAIL initially
        val goCode = """
            package main
            
            type Config struct {
                Name string
            }
            
            func NewConfig() *Config {
                return &Config{}
            }
            
            func (c *Config) Load() error {
                return nil
            }
            
            func (c *Config) Save() error {
                return nil
            }
        """.trimIndent()

        // When
        val rootNode = parseGoCode(goCode)
        val declarations = query.execute(rootNode)

        // Then
        // Should find: 1 type + 1 function + 2 methods = 4 total
        assertEquals(4, declarations.size, "Should detect all declaration types")

        val declarationTypes = declarations.map { it.type }
        assertEquals(1, declarationTypes.count { it == "type_declaration" })
        assertEquals(1, declarationTypes.count { it == "function_declaration" })
        assertEquals(2, declarationTypes.count { it == "method_declaration" })
    }
}