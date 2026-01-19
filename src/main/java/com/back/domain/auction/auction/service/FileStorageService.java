package com.back.domain.auction.auction.service;

import com.back.global.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException e) {
            throw new ServiceException("500-2", "파일 저장 디렉토리를 생성할 수 없습니다.");
        }
    }

    public String storeFile(MultipartFile file) {
        // 원본 파일명
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new ServiceException("400-3", "파일 이름이 유효하지 않습니다.");
        }

        // 고유한 파일명 생성 (UUID 사용)
        String fileExtension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFileName.substring(dotIndex);
        }
        String storedFileName = UUID.randomUUID() + fileExtension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 저장된 파일의 URL 반환 (상대 경로)
            return "/uploads/" + storedFileName;
        } catch (IOException e) {
            throw new ServiceException("500-3", "파일 저장에 실패했습니다: " + storedFileName);
        }
    }
}

