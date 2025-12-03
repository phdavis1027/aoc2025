rootProject.name = "advent_25"

// Include all day subprojects
(1..12).forEach { day ->
    val dayStr = day.toString().padStart(2, '0')
    val projectDir = file("day$dayStr")
    if (projectDir.exists()) {
        include("day$dayStr")
    }
}
