package ru.grishankov.modpack_creator.core

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun zipFolder(folder: File, outputZip: File) {
    ZipOutputStream(FileOutputStream(outputZip)).use { zipOut ->

        val files = folder.walkTopDown()

        val size = files.count()

        files.forEachIndexed { index, file ->
            print("\r📂 Архивация... ${index.inc() * 100 / size}%")

            val entryName = file.relativeTo(folder).path.replace("\\", "/") // Относительный путь

            if (entryName.isEmpty()) return@forEachIndexed

            if (file.isDirectory) {
                zipOut.putNextEntry(ZipEntry("$entryName/")) // Добавляем папку
                zipOut.closeEntry()
            } else {
                zipOut.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }
    }
    println("\n✅ Архив создан: ${outputZip.absolutePath}")
}
