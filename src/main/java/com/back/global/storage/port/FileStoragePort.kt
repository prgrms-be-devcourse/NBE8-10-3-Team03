package com.back.global.storage.port

import org.springframework.web.multipart.MultipartFile

interface FileStoragePort {
    fun storeFile(file: MultipartFile, domain: String = "common"): String
}
