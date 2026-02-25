package com.back.domain.auction.auction.service

import com.back.global.exception.ServiceException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

@Service
class FileStorageService(
    @Value("\${file.upload-dir}") uploadDir: String
) {
    private val fileStorageLocation: Path = Paths.get(uploadDir).toAbsolutePath().normalize()

    init {
        try {
            Files.createDirectories(fileStorageLocation)
        } catch (_: IOException) {
            throw ServiceException("500-2", "파일 저장 디렉토리를 생성할 수 없습니다.")
        }
    }

    fun storeFile(file: MultipartFile): String {
        val originalFileName = file.originalFilename
        if (originalFileName.isNullOrEmpty()) {
            throw ServiceException("400-3", "파일 이름이 유효하지 않습니다.")
        }

        val dotIndex = originalFileName.lastIndexOf('.')
        val fileExtension = if (dotIndex > 0) originalFileName.substring(dotIndex) else ""
        val storedFileName = UUID.randomUUID().toString() + fileExtension

        return try {
            val targetLocation = fileStorageLocation.resolve(storedFileName)
            Files.copy(file.inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)
            "/uploads/$storedFileName"
        } catch (_: IOException) {
            throw ServiceException("500-3", "파일 저장에 실패했습니다: $storedFileName")
        }
    }
}
