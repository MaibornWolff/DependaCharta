package de.maibornwolff.dependacharta.pipeline.analysis.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TypeOfUsage(
    val rawValue: String
) {
    @SerialName("usage")
    USAGE("usage"),

    @SerialName("inheritance")
    INHERITANCE("inheritance"),

    @SerialName("implementation")
    IMPLEMENTATION("implementation"),

    @SerialName("constant_access")
    CONSTANT_ACCESS("constant_access"),

    @SerialName("return_value")
    RETURN_VALUE("return_value"),

    @SerialName("instantiation")
    INSTANTIATION("instantiation"),

    @SerialName("argument")
    ARGUMENT("argument"),
}
