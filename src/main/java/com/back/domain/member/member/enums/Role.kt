package com.back.domain.member.member.enums

enum class Role {
    ADMIN,
    USER;

    companion object {
        @JvmStatic
        fun from(value: String?): Role {
            requireNotNull(value) { "Role is null" }

            return runCatching { valueOf(value.uppercase()) }  // [1] Locale 파라미터 불필요
                .getOrElse { throw IllegalArgumentException("Invalid role: $value") }
        }
    }
}
