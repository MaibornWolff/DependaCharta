package de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp

import de.maibornwolff.dependacharta.pipeline.analysis.analyzers.cpp.processing.BodyProcessor
import de.maibornwolff.dependacharta.pipeline.analysis.model.Dependency
import de.maibornwolff.dependacharta.pipeline.analysis.model.Path
import de.maibornwolff.dependacharta.pipeline.analysis.model.Type
import de.maibornwolff.dependacharta.pipeline.analysis.model.TypeOfUsage
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BodyProcessorTest : BaseProcessorTest() {
    @Test
    fun `should extract class type and used types correctly`() {
        // given
        val cppCode = """
std::Foobar<bar> method2(int param){
      char* foo;
      List<int> bla;
      }
      
class SampleClass {
public:
    Foobar method2(int param){
      char* foo;
      List<int> bla;
      }
 private:
   string boo;
};
        """.trimIndent()

        val sut = BodyProcessor()
        // when

        val result = process(sut, cppCode)
        // then
    }

    @Test
    fun `should extract exception type correctly`() {
        // given
        val cppCode = """
void foo() {
  if (1==1) throw FooBarException();
      }
        """.trimIndent()

        val sut = BodyProcessor()
        // when

        val result = process(sut, cppCode)
        Assertions
            .assertThat(result.context.usedTypes)
            .containsAll(listOf(Type.simple("void"), Type.simple("FooBarException")))
        // then
    }

    @Test
    fun `should extract used type of inheritance`() {
        // given
        val cppCode = """
            class PersistedFoo : public a::b::c::Foo {};
        """.trimIndent()
        val sut = BodyProcessor()

        // when
        val result = process(sut, cppCode)

        // then
        val persistedFoo = result.nodes.first()
        assertThat(persistedFoo.usedTypes).contains(Type.simple("Foo", typeOfUsage = TypeOfUsage.INHERITANCE))
        assertThat(persistedFoo.dependencies).contains(Dependency(Path("a", "b", "c"), isWildcard = true))
    }
}
