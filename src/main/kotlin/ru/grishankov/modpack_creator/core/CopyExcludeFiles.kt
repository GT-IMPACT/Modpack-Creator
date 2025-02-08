package ru.grishankov.modpack_creator.core

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

fun copyFilesWithStructure(srcDir: File, destDir: File, excludeNames: List<String>) {
    // Пройдем по всем файлам и папкам в исходной директории рекурсивно
    srcDir.walkTopDown()
        .filterNot { file ->
            // Фильтруем файлы по условию
            excludeNames.any { file.name.contains(it, ignoreCase = true) }
        }
        .forEach { file ->
            // Составляем новый путь для копирования
            val targetFile = File(destDir, file.relativeTo(srcDir).path)

            if (file.isDirectory) {
                // Если это папка, создаем соответствующую папку в целевой директории
                targetFile.mkdirs()
            } else {
                // Если это файл, копируем его
                targetFile.parentFile.mkdirs() // Убедимся, что родительская папка существует
                Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
}
