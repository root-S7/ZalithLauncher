package com.movtery.zalithlauncher.feature.download.enums

//1 = EmbeddedLibrary
//2 = OptionalDependency
//3 = RequiredDependency
//4 = Tool
//5 = Incompatible
//6 = Include
enum class DependencyType(val curseforge: String?, val modrinth: String?) {
    REQUIRED("3", "required"),
    OPTIONAL("2", "optional"),
    INCOMPATIBLE("5", "incompatible"),
    EMBEDDED("1", "embedded"),
    TOOL("4", null),
    INCLUDE("6", null)
}