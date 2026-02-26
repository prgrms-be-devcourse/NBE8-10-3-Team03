package com.back.global.storage.adapter

import com.back.global.exception.ServiceException
import com.back.global.storage.port.FileStoragePort
import com.back.global.storage.support.StoragePathGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

@Component
@ConditionalOnProperty(
    prefix = "file.storage",
    name = ["provider"],
    havingValue = "local",
    matchIfMissing = true
)
class LocalFileStorageAdapter(
    @Value("\${file.upload-dir}") uploadDir: String,
    @Value("\${file.storage.path-prefix:\${spring.profiles.active:local}}") private val pathPrefix: String,
) : FileStoragePort {
    private val fileStorageLocation: Path = Paths.get(uploadDir).toAbsolutePath().normalize()

    init {
        try {
            Files.createDirectories(fileStorageLocation)
        } catch (_: IOException) {
            throw ServiceException("500-2", "파일 저장 디렉토리를 생성할 수 없습니다.")
        }
    }

    override fun storeFile(file: MultipartFile, domain: String): String {
        val originalFileName = file.originalFilename
        if (originalFileName.isNullOrEmpty()) {
            throw ServiceException("400-3", "파일 이름이 유효하지 않습니다.")
        }

        val objectPath = StoragePathGenerator.generate(pathPrefix, domain, originalFileName)

        return try {
            val targetLocation = fileStorageLocation.resolve(objectPath).normalize()
            Files.createDirectories(targetLocation.parent)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)
            "/uploads/$objectPath"
        } catch (_: IOException) {
            throw ServiceException("500-3", "파일 저장에 실패했습니다.")
        }
    }
}
