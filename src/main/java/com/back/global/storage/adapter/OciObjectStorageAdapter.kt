package com.back.global.storage.adapter

import com.back.global.exception.ServiceException
import com.back.global.storage.port.FileStoragePort
import com.back.global.storage.support.StoragePathGenerator
import com.oracle.bmc.Region
import com.oracle.bmc.auth.InstancePrincipalsAuthenticationDetailsProvider
import com.oracle.bmc.objectstorage.ObjectStorageClient
import com.oracle.bmc.objectstorage.model.CreatePreauthenticatedRequestDetails
import com.oracle.bmc.objectstorage.requests.CreatePreauthenticatedRequestRequest
import com.oracle.bmc.objectstorage.requests.PutObjectRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

@Component
@ConditionalOnProperty(prefix = "file.storage", name = ["provider"], havingValue = "oci")
class OciObjectStorageAdapter(
    @Value("\${file.storage.oci.namespace}") private val namespace: String,
    @Value("\${file.storage.oci.bucket}") private val bucket: String,
    @Value("\${file.storage.oci.region}") private val regionId: String,
    @Value("\${file.storage.oci.par-expire-minutes:30}") private val parExpireMinutes: Long,
    @Value("\${file.storage.path-prefix:\${spring.profiles.active:dev}}") private val pathPrefix: String,
) : FileStoragePort {
    private val authProvider = InstancePrincipalsAuthenticationDetailsProvider.builder().build()
    private val objectStorageClient = ObjectStorageClient(authProvider).apply {
        setRegion(Region.fromRegionId(regionId))
    }

    override fun storeFile(file: MultipartFile, domain: String): String {
        val originalFileName = file.originalFilename
        if (originalFileName.isNullOrEmpty()) {
            throw ServiceException("400-3", "파일 이름이 유효하지 않습니다.")
        }

        val objectName = StoragePathGenerator.generate(pathPrefix, domain, originalFileName)

        try {
            objectStorageClient.putObject(
                PutObjectRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucket)
                    .objectName(objectName)
                    .contentType(file.contentType)
                    .contentLength(file.size)
                    .putObjectBody(file.inputStream)
                    .build()
            )
        } catch (e: Exception) {
            throw ServiceException("500-3", "OCI 업로드에 실패했습니다: ${e.message}")
        }

        return createPresignedUrl(objectName)
    }

    private fun createPresignedUrl(objectName: String): String {
        return try {
            val details = CreatePreauthenticatedRequestDetails.builder()
                .name("upload-$objectName")
                .accessType(CreatePreauthenticatedRequestDetails.AccessType.ObjectRead)
                .objectName(objectName)
                .timeExpires(Date.from(Instant.now().plus(parExpireMinutes, ChronoUnit.MINUTES)))
                .build()

            val response = objectStorageClient.createPreauthenticatedRequest(
                CreatePreauthenticatedRequestRequest.builder()
                    .namespaceName(namespace)
                    .bucketName(bucket)
                    .createPreauthenticatedRequestDetails(details)
                    .build()
            )

            "https://objectstorage.$regionId.oraclecloud.com${response.preauthenticatedRequest.accessUri}"
        } catch (e: Exception) {
            throw ServiceException("500-4", "OCI Presigned URL 생성에 실패했습니다: ${e.message}")
        }
    }
}
