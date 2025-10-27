package de.maibornwolff.codegraph.pipeline.analysis.analyzers.cpp

import de.maibornwolff.codegraph.pipeline.analysis.model.Dependency
import de.maibornwolff.codegraph.pipeline.analysis.model.FileInfo
import de.maibornwolff.codegraph.pipeline.analysis.model.Path
import de.maibornwolff.codegraph.pipeline.analysis.model.Type
import de.maibornwolff.codegraph.pipeline.analysis.model.TypeOfUsage
import de.maibornwolff.codegraph.pipeline.shared.SupportedLanguage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CppAnalyzerTest {
    @Test
    fun `should recognize constructor call correctly`() {
        // given
        val cppCode = """

class Foo{

// 1. Default constructor
Alpha a;

// 2. Parameterized constructor
Beta b(42, "hello");

// 3. Copy constructor
Gamma g(b);

// 4. Move constructor
Delta d(std::move(b));

// 5. Dynamic allocation (default)
Epsilon* e = new Epsilon;

// 6. Dynamic allocation (parameterized)
Zeta* z = new Zeta(99, "world");

// 7. Dynamic allocation (copy)
Eta* eta = new Eta(a);

// 8. List initialization (C++11)
Theta t{1, "list"};

// 9. Uniform initialization (C++11)
Iota i = Iota{5, "uniform"};

// 10. Placement new
char buffer[sizeof(Kappa)];
Kappa* k = new (buffer) Kappa(7, "placement");

// 11. Array of objects
Lambda arr[3] = { Lambda(), Lambda(1, "a"), Lambda(2, "b") };

// 12. Smart pointer initialization
std::unique_ptr<Mu> uptr = std::make_unique<Mu>(123, "smart");
std::shared_ptr<Nu> sptr = std::make_shared<Nu>(456, "shared");
};

        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./path", cppCode)).analyze()

        // then
        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).containsAll(
            listOf(
                Type.simple("Alpha"),
                Type.simple("Beta"),
                Type.simple("Gamma"),
                Type.simple("Delta"),
                Type.simple("Epsilon"),
                Type.simple("Zeta"),
                Type.simple("Eta"),
                Type.simple("Theta"),
                Type.simple("Iota"),
                Type.simple("Kappa"),
                Type.simple("Lambda"),
                Type.simple("Mu"),
                Type.simple("Nu")
            )
        )
    }

    @Test
    fun `should recognize type of static function call correctly`() {
        // given
        val cppCode = """
inline bool Address::doSomething() {
    return Assembler::is_uimm12(offset >> shift);
}
        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./path", cppCode)).analyze()

        // then
        val usedTypes = report.nodes.first { it.name() == "Address" }.usedTypes
        assertThat(usedTypes).contains(
            Type.simple("Assembler"),
        )
    }

    @Test
    fun `should recognize types of function parameters correctly`() {
        // given
        val cppCode = """
inline bool Address::offset_ok_for_immed(int64_t offset, uint shift) {
}
        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./path", cppCode)).analyze()

        // then
        val usedTypes = report.nodes.first { it.name() == "Address" }.usedTypes
        assertThat(usedTypes).containsAll(
            listOf(
                Type.simple("int64_t"),
                Type.simple("uint"),
            )
        )
    }

    @Test
    fun `should set path of node to file path with file name if there is no namespace it resides in`() {
        // given
        val cppCode = """
            class Foo {
                // This class is just a placeholder to test the include statement
            };
        """.trimIndent()

        // when
        val report =
            CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./foo/bar/classes/DummyClasses.cpp", cppCode)).analyze()

        // then
        assertThat(report.nodes.first().pathWithName).isEqualTo(Path.fromStringWithDots("foo.bar.classes.DummyClasses_cpp.Foo"))
    }

    @Test
    fun `should set path of node to namespace if there is a namespace it resides in`() {
        // given
        val cppCode = """
            namespace de::maibornwolff::codegraph::pipeline::analysis {
                class DummyClass {
                    // This class is just a placeholder to test the include statement
                };
            }
        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./foo/bar/classes", cppCode)).analyze()

        // then
        assertThat(
            report.nodes.first().pathWithName
        ).isEqualTo(Path.fromStringWithDots("de.maibornwolff.codegraph.pipeline.analysis.DummyClass"))
    }

    @Test
    fun `should recognize include statements with file path as dependencies`() {
        // given
        val cppCode = """
            #include "dir/subdir/CreatureRepository.h"
            #include"dir/subdir/AnotherCreatureRepository.h"
            
            class DummyClass {
                // This class is just a placeholder to test the include statement
            };
        """
        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./path", cppCode)).analyze()
        // then
        assertThat(report.nodes.first().dependencies).contains(
            Dependency(Path.fromStringWithDots("dir.subdir.CreatureRepository_h"), isWildcard = false),
            Dependency(Path.fromStringWithDots("dir.subdir.AnotherCreatureRepository_h"), isWildcard = false)
        )
    }

    @Test
    fun `should set path of node to namespace it resides in, even when namespace is nested`() {
        // given
        val cppCode = """
            namespace de::maibornwolff::codegraph::pipeline::analysis {
                class FooClass {
                };
                namespace analyzers {
                    class BarClass {
                    };
                }
            }
        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./path", cppCode)).analyze()

        // then
        assertThat(
            report.nodes.first().pathWithName
        ).isEqualTo(Path.fromStringWithDots("de.maibornwolff.codegraph.pipeline.analysis.FooClass"))
        assertThat(
            report.nodes[1].pathWithName
        ).isEqualTo(Path.fromStringWithDots("de.maibornwolff.codegraph.pipeline.analysis.analyzers.BarClass"))
    }

    @Test
    fun `should handle nested namespaces`() {
        // given
        val cppCode = """
            namespace de::maibornwolff::codegraph::pipeline {
                namespace analysis {
                    namespace analyzers {
                        class DummyClass {
                        };
                    }
                }
            }
        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./path", cppCode)).analyze()

        // then
        assertThat(
            report.nodes.first().pathWithName
        ).isEqualTo(Path.fromStringWithDots("de.maibornwolff.codegraph.pipeline.analysis.analyzers.DummyClass"))
    }

    /*
    Add tests that takle all possible include statement variations of c++
     */
    @Test
    fun `should recognize include statements with angle brackets`() {
        // given
        val cppCode = """
                #include <vector>
                #include <myproject/MyHeader.h>
                #include <boost/algorithm/string.hpp>
                #include<no_space.h>
                    
                class DummyClass {
                    // This class is just a placeholder to test the include statement
                };
        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./path", cppCode)).analyze()

        // then
        val dependencies = report.nodes
            .first()
            .dependencies
            .map { it.path.toString() }
        assertThat(dependencies).containsAll(
            listOf(
                "vector",
                "myproject.MyHeader_h",
                "boost.algorithm.string_hpp",
                "no_space_h",
            )
        )
    }

    @Test
    fun `should recognize multiline include statements`() {
        // given
        val cppCode = """
                #include "dir/subdir/CreatureRepository.h"
                #include "dir/\
                    subdir/AnotherCreatureRepository.h"
                    
                class DummyClass {
                    // This class is just a placeholder to test the include statement
                };
        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./path", cppCode)).analyze()

        // then
        assertThat(report.nodes.first().dependencies).contains(
            Dependency(Path.fromStringWithDots("dir.subdir.CreatureRepository_h"), isWildcard = false),
            Dependency(Path.fromStringWithDots("dir.subdir.AnotherCreatureRepository_h"), isWildcard = false),
        )
    }

    @Test
    fun `should extract field types correctly`() {
        // given
        val cppCode = """
           #include "CreatureRepository.h"

// Since CreatureRepository is an interface, it's typically implemented by a derived class.
// For example purposes, here's a possible implementation:

#include <unordered_set>

namespace de::sots::cellarsandcentaurs::adapter::persistence {

template<typename TEntity, typename TKey>
class ConcreteCreatureRepository : public CreatureRepository<TEntity, TKey> {
private:
    std::unordered_set<std::shared_ptr<TEntity>> entities;

public:
    std::set<std::shared_ptr<TEntity>> Entities() const override {
        return std::set<std::shared_ptr<TEntity>>(entities.begin(), entities.end());
    }
/*
    std::shared_ptr<TEntity> Find(const TKey& id) override {
        for (const auto& entity : entities) {
            if (entity->Id == id) {
                return entity;
            }
        }
        return nullptr;
    }

    void Add(const std::shared_ptr<TEntity>& entity) override {
        entities.insert(entity);
    }

    void Update(const std::shared_ptr<TEntity>& entity) override {
        Remove(entity);
        Add(entity);
    }

    void Remove(const std::shared_ptr<TEntity>& entity) override {
        entities.erase(entity);
    }
    */
};

} // namespace de::sots::cellarsandcentaurs::adapter::persistence

// It's mandatory to provide implementations for model classes 
// such as TEntity with uuid_t as the Id member, similar to the CreatureEntity class.
        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./path", cppCode)).analyze()

        // then
        val usedTypes = report.nodes.first().usedTypes
        val sharedPtr = Type.generic("shared_ptr", listOf(Type.simple("TEntity")))

        assertThat(usedTypes).containsAll(
            listOf(
                Type.generic("unordered_set", listOf(sharedPtr)),
                Type.generic("set", listOf(sharedPtr)),
                sharedPtr
            )
        )
    }

    @Test
    fun `should extract method return types correctly`() {
        // given
        val cppCode = """
           #include "PersistedCreatures.h"

namespace de::sots::cellarsandcentaurs::adapter::persistence {

PersistedCreatures::PersistedCreatures(std::shared_ptr<CreatureRepository<CreatureEntity, std::string>> repository)
    : repository(repository) {}

void PersistedCreatures::Save(const Creature& creature) {
    repository->Add(std::make_shared<CreatureEntity>(creature.GetId().GetId()));
}

Creature PersistedCreatures::Find(const CreatureId& id) {
    auto creatureEntity = repository->Find(id.GetId());
    if (!creatureEntity) {
        throw NoSuchCreatureException(id);
    }
    return Creature(CreatureId(creatureEntity->Id));
}

} // namespace de::sots::cellarsandcentaurs::adapter::persistence
        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./path", cppCode)).analyze()

        // then
        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).containsAll(
            listOf(
                Type.generic(
                    "shared_ptr",
                    listOf(
                        Type.generic(
                            "CreatureRepository",
                            listOf(
                                Type.simple("CreatureEntity"),
                                Type.simple("string")
                            )
                        )
                    )
                ),
                Type.simple("string"),
                Type.simple("CreatureEntity"),
                Type.generic("make_shared", listOf(Type.simple("CreatureEntity"))),
                Type.simple("Creature"),
                Type.simple("void"),
                Type.simple("CreatureId")
            )
        )
    }

    @Test
    fun `should extract constructor parameter types correctly`() {
        // given
        val cppCode = """
        #include "Creature.h"

using namespace de::sots::cellarsandcentaurs::application;
namespace de::sots::cellarsandcentaurs::domain::model {

Creature::Creature(const std::string& name,
                   CreatureType type,
                   const HitPoints& hitPoints,
                   const ArmorClass& armorClass,
                   const std::unordered_map<SpeedType, Speed>& speeds)
    : name(name), type(CreatureFacade::STANDARD_CREATURE_TYPE), hitPoints(hitPoints), armorClass(armorClass), speeds(speeds) {}

std::string Creature::GetName() const {
    return name;
}

void Creature::SetName(const std::string& name) {
    this->name = name;
}

CreatureType Creature::GetType() const {
    return type;
}

void Creature::SetType(CreatureType type) {
    this->type = type;
}

HitPoints Creature::GetHitPoints() const {
    return hitPoints;
}

void Creature::SetHitPoints(const HitPoints& hitPoints) {
    this->hitPoints = hitPoints;
}

ArmorClass Creature::GetArmorClass() const {
    return armorClass;
}

void Creature::SetArmorClass(const ArmorClass& armorClass) {
    this->armorClass = armorClass;
}

Speed Creature::GetSpeed(SpeedType speedType) const {
    auto it = speeds.find(speedType);
    if (it != speeds.end()) {
        return it->second;
    }
    // Handle the case where the speedType is not found, throw exception or return a default value
    return Speed(); // Assuming Speed has a default constructor
}

void Creature::SetSpeed(SpeedType speedType, const Speed& speed) {
    speeds[speedType] = speed;
}

} // namespace de::sots::cellarsandcentaurs::domain::model
        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.C_SHARP, "./path", cppCode)).analyze()

        // then
        val usedTypes = report.nodes.first().usedTypes
        assertThat(usedTypes).containsAll(
            listOf(
                Type.simple("string", typeOfUsage = TypeOfUsage.ARGUMENT),
                Type.simple("CreatureType", typeOfUsage = TypeOfUsage.ARGUMENT),
                Type.simple("HitPoints", typeOfUsage = TypeOfUsage.ARGUMENT),
                Type.simple("ArmorClass", typeOfUsage = TypeOfUsage.ARGUMENT),
                Type.simple("CreatureFacade", typeOfUsage = TypeOfUsage.ARGUMENT),
                Type.simple("void", typeOfUsage = TypeOfUsage.ARGUMENT),
                Type.simple("SpeedType", typeOfUsage = TypeOfUsage.ARGUMENT),
                Type.simple("Speed", typeOfUsage = TypeOfUsage.ARGUMENT),
                Type.generic(
                    "unordered_map",
                    listOf(
                        Type.simple("SpeedType"),
                        Type.simple("Speed"),
                    ),
                    typeOfUsage = TypeOfUsage.ARGUMENT
                )
            )
        )
    }

    @Test
    fun `should recognize unsigned statement without type as int`() {
        // given
        val cppCode = """
            class A { 
                unsigned foo() {}
            }
        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./path", cppCode)).analyze()

        // then
        assertEquals(
            report.nodes
                .first()
                .usedTypes
                .first()
                .name,
            "int"
        )
    }

    @Test
    fun `should recognize signed statement without type as int`() {
        // given
        val cppCode = """
            class B {
                signed bar() {}
            }
        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./path", cppCode)).analyze()

        // then
        assertEquals(
            report.nodes
                .first()
                .usedTypes
                .first()
                .name,
            "int"
        )
    }

    @Test
    fun `should recognize type declared inside of class as own node and with namespace of declared class`() {
        // given
        val cppCode = """
            class B {
                enum Foo {
                    BAR,
                    BAZ
                };
            }
        """.trimIndent()

        // when
        val report = CppAnalyzer(FileInfo(SupportedLanguage.CPP, "./path", cppCode)).analyze()

        // then
        assertThat(report.nodes.map { it.name() }).containsExactlyInAnyOrder("Foo", "B")
        val fooNode = report.nodes.first { it.name() == "Foo" }
        assertThat(fooNode.pathWithName.withoutName().last()).isEqualTo("B")
    }
}
