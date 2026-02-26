package com.back.global.infra.storage
import org.springframework.web.multipart.MultipartFile

interface FileStorageProvider {
    fun storeFile(file: MultipartFile): String
}