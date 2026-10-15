package ru.grishankov.modpack_creator.core

import io.ktor.client.HttpClient
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.jar.JarFile

/** Only publishes complete downloads. An existing partial/corrupt JAR is never accepted as a cache hit. */
internal class ModFileDownloader(
    private val client: HttpClient,
    private val username: String? = null,
    private val password: String? = null,
) {
    suspend fun download(url: String, output: File, checksums: Map<String, String> = emptyMap()) {
        if (output.isFile && runCatching { validate(output, checksums) }.isSuccess) {
            println("⚠️ ${output.name} уже был скачан и проверен")
            return
        }
        Files.createDirectories(output.toPath().toAbsolutePath().parent)
        val temporary = Files.createTempFile(output.toPath().toAbsolutePath().parent, output.name + ".", ".part")
        try {
            val response = client.get(url) {
                expectSuccess = true
                timeout { connectTimeoutMillis = 20000; socketTimeoutMillis = 60000; requestTimeoutMillis = 300000 }
                if (username != null && password != null) basicAuth(username, password)
            }
            val expectedLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
            val copied = response.bodyAsChannel().toInputStream().use { input ->
                Files.newOutputStream(temporary).use { input.copyTo(it) }
            }
            check(expectedLength == null || copied == expectedLength) { "Неполная загрузка ${output.name}" }
            validate(temporary.toFile(), checksums, output.extension.equals("jar", ignoreCase = true))
            try { Files.move(temporary, output.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(temporary, output.toPath(), StandardCopyOption.REPLACE_EXISTING) }
            println("✅ ${output.name} успешно скачан и проверен")
        } finally { Files.deleteIfExists(temporary) }
    }

    private fun validate(file: File, checksums: Map<String, String>, jar: Boolean = file.extension.equals("jar", ignoreCase = true)) {
        check(file.length() > 0) { "Пустой файл ${file.name}" }
        if (jar) JarFile(file).use { archive ->
            check(archive.entries().hasMoreElements()) { "Пустой JAR ${file.name}" }
            archive.entries().asSequence().filter { !it.isDirectory }.forEach { entry ->
                val crc = java.util.zip.CRC32()
                archive.getInputStream(entry).use { input ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        crc.update(buffer, 0, count)
                    }
                }
                check(crc.value == entry.crc) { "Повреждён JAR ${file.name}" }
            }
        }
        val checksum = checksums.entries.firstOrNull { it.key.equals("sha256", true) }
            ?: checksums.entries.firstOrNull { it.key.equals("sha1", true) }
        if (checksum != null) {
            val digest = MessageDigest.getInstance(if (checksum.key.equals("sha256", true)) "SHA-256" else "SHA-1")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
            check(actual.equals(checksum.value, ignoreCase = true)) { "Неверная контрольная сумма ${file.name}" }
        }
    }
}
