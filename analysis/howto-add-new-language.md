# How To write a language analyzer
To write an analyzer for a programming language, you need to implement the `LanguageAnalyzer` interface and add a dictionary for the language that implements `LanguageDictionary`.
## Step 1) Add the [tree-sitter-ng](https://github.com/bonede/tree-sitter-ng) dependency for your language
We are usually using [tree-sitter-ng](https://github.com/bonede/tree-sitter-ng) to parse the source code. A lot of precompiled tree-sitter grammars for this library are available as maven dependencies, so add the latest dependency for the language you want to analyze to the `build.gradle.kts` file.
```kotlin
dependencies {
    implementation("io.github.bonede:tree-sitter-[language]:[version]")
}
```
## Step 2) Implement the `LanguageAnalyzer` interface
Create a new class that implements the `LanguageAnalyzer` interface.
The purpose of the `analyze()` method is to extract dependency and type information for each declaration (e.g. classes, interfaces, etc.).
Each declaration will be turned into a `Node` object and added to the `FileReport` that is returned by the function.

The most important information to extract is:
* The name of the declaration and its complete path (e.g. what is used in other files to reference this declaration)
* The dependencies that get declared in the analyzed files (e.g. the `import` statements in Java or `using` directives in C#)
* Types that are actually used in your declaration (e.g. the types of variables or fields, the return type or parameter types of a method, inherited types, etc.)

You can use [tree-sitter queries](https://tree-sitter.github.io/tree-sitter/using-parsers/queries/1-syntax.html) to extract the information you need from the source code.

Use the [tree-sitter playground](https://tree-sitter.github.io/tree-sitter/7-playground.html) to find out how tree-sitter represents the code element you are looking at. You can use the query feature on the site to test your queries.

For examples on how to write and use queries, you can look at the existing analyzers in the [analyzers](./src/main/kotlin/de/maibornwolff/dependacharta/pipeline/analysis/analyzers) package.

### Handling language specifica
The LanguageAnalyzer should be the only part of the code that handles language specific details. The rest of the code should be language agnostic.
This means that the LanguageAnalyzer should be responsible for details like:
* Adding implicit imports (e.g. a wildcard import for a class's own package in Java)
* Handling different declarations within the same file (e.g. declaring multiple classes in one file in Java)
* Handling different types of dependencies (e.g. static imports in Java, using directives with alias in C#)
* Specifying what to use as the path of the declaration (e.g. the package name in Java, the namespace in C#, a physical file path, etc.)

## Step 3) Implement the `LanguageDictionary` interface
Create a new class that implements the `LanguageDictionary` interface.
The `get()` method should return a map of language specific, auto-imported classes. This is useful for languages that have a lot of implicit imports (e.g. Java with `java.lang.*`).

Types that appear in the LanguageDirectory will not be added as "real" dependencies to the output file.

## Step 4) Add the language to the `Language` enum
Add a new entry to the `Language` enum. Handle that new entry in `LanguageAnalyzerFactory`.
