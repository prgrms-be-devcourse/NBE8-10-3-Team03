package com.back.domain.member.member.service

import com.back.domain.auction.auction.repository.AuctionRepository
import com.back.domain.image.image.entity.Image
import com.back.domain.image.image.repository.ImageRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.enums.MemberStatus
import com.back.domain.member.member.enums.Role
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.reputation.entity.Report
import com.back.domain.member.reputation.entity.Reputation
import com.back.domain.member.reputation.entity.ReputationEvent
import com.back.domain.member.reputation.enums.EventType
import com.back.domain.member.reputation.enums.RefType
import com.back.domain.member.reputation.repository.ReportRepository
import com.back.domain.member.reputation.repository.ReputationEventRepository
import com.back.domain.member.reputation.repository.ReputationRepository
import com.back.domain.member.review.entity.Review
import com.back.domain.member.review.repository.ReviewRepository
import com.back.global.audit.enums.AuditType
import com.back.global.audit.service.SecurityAuditService
import com.back.global.exception.ServiceException
import com.back.global.rsData.RsData
import com.back.global.storage.port.FileStoragePort
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.util.*
import kotlin.math.abs

@Service
class MemberService(
    private val authTokenService: AuthTokenService,
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder,
    private val reputationRepository: ReputationRepository,
    private val eventRepository: ReputationEventRepository,
    private val auctionRepository: AuctionRepository,
    private val reviewRepository: ReviewRepository,
    private val reportRepository: ReportRepository,
    private val loginFailService: LoginFailService,
    private val auditService: SecurityAuditService,
    private val servletRequest: HttpServletRequest,
    private val fileStoragePort: FileStoragePort,
    private val imageRepository: ImageRepository
) {


    fun count(): Long = memberRepository.count()

    @Transactional
    fun join(username: String, password: String?, name: String, profileImgUrl: String?): Member {
        val nickname = name.trim()

        memberRepository.findByUsername(username)
            .ifPresent { throw ServiceException("409-1", "이미 존재하는 아이디입니다.") }

        memberRepository.findByUsername(username)
            .ifPresent{ throw ServiceException("409-1", "이미 존재하는 아이디입니다.")}

        val encodedPassword = password
            ?.takeIf { it.isNotBlank() }
            ?.also { passwordValidation(username, it) }
            ?.let { passwordEncoder.encode(it) }

        // (임시) system이나 admin으로 가입시 ADMIN ROLE 부여
        val role = if (username == "system" || username == "admin") Role.ADMIN else Role.USER
        val member = Member(username, encodedPassword, nickname, role, profileImgUrl)

        // (임시) 최초 신용도 50.0
        val reputation = Reputation(member, 50.0)
        reputationRepository.save(reputation)

        return memberRepository.save(member)
    }

    // OAuth 회원가입 / 로그인
    @Transactional
    fun modifyOrJoin(username: String, password: String?, nickname: String, profileImgUrl: String?): RsData<Member?> {
        val member = findByUsername(username).orElse(null)
            ?: return RsData("201-1", "회원가입이 완료되었습니다.", join(username, password, nickname, profileImgUrl))

        modify(member, nickname, profileImgUrl)
        return RsData("200-1", "회원 정보가 수정되었습니다.", member)
    }

    // OAuth 수정
    @Transactional
    private fun modify(member: Member, nickname: String, profileImgUrl: String?) {
        member.modify(nickname, profileImgUrl)
    }

    // 로그인
    @Transactional
    fun login(member: Member, password: String?) {
        when (member.status) {                              // if-else 체인 → when
            MemberStatus.BANNED -> throw ServiceException("403-3", "계정이 영구 정지되었습니다. 관리자에게 문의해주세요.")
            MemberStatus.WITHDRAWN -> throw ServiceException("403-4", "탈퇴한 계정입니다.")
            else -> Unit
        }
        // 계정 잠김이 만료되었는지 확인
        member.unlockIfExpired()

        // 계정이 잠겨있으면 에러
        if (member.isLocked && member.lockedUntil?.isAfter(LocalDateTime.now()) == true) {  // [9] isLocked() → isLocked (프로퍼티)
            throw ServiceException("400-5", "계정이 일시적으로 잠겼습니다.")
        }

        password?.let { checkPassword(member, it) }

        member.resetFailCount()
    }

    @Transactional
    fun checkPassword(member: Member, password: String?) {
        val now = LocalDateTime.now()
        // 패스워드가 일치하지 않으면
        if (!passwordEncoder.matches(password, member.password)) {
            loginFailService.record(member.id, now)

            if (member.loginFailCount >= 5) {
                loginFailService.lock(member.id, now)
                auditService.log(
                    member.id,
                    AuditType.LOCK,
                    servletRequest.remoteAddr,
                    servletRequest.getHeader("User-Agent")
                )
                throw ServiceException("401-1", "비밀번호 입력 횟수를 초과하였습니다. 10분 뒤에 다시 시도해주세요.")
            }

            throw ServiceException("401-1", "비밀번호가 일치하지 않습니다.")
        }
    }


    // 회원 탈퇴
    @Transactional
    fun withdraw(actor: Member) {
        val member = memberRepository.findById(actor.id)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 회원입니다.") }

        if (member.status == MemberStatus.WITHDRAWN) {
            throw ServiceException("400-1", "이미 탈퇴한 회원입니다.")
        }

        member.withdraw()
        auditService.log(
            member.id,
            AuditType.WITHDRAW,
            servletRequest.remoteAddr,
            servletRequest.getHeader("User-Agent")
        )
    }


    fun findByUsername(username: String): Optional<Member> = memberRepository.findByUsername(username)

    fun findByApiKey(apiKey: String): Optional<Member> = memberRepository.findByApiKey(apiKey)

    fun genAccessToken(member: Member): String = authTokenService.genAccessToken(member)

    fun payload(accessToken: String): Map<String, Any>? = authTokenService.payload(accessToken)

    fun findById(id: Int): Optional<Member> = memberRepository.findById(id)

    fun findAll(): MutableList<Member> = memberRepository.findAll()


    @Transactional
    fun modifyNickname(member: Member, nickname: String) = member.modifyName(nickname)

    @Transactional
    fun modifyPassword(member: Member, password: String, newPassword: String, checkPassword: String) {
        if (!passwordEncoder.matches(password, member.password)) throw ServiceException(
            "401-1",
            "현재 비밀번호와 일치하지 않습니다."
        )

        if (newPassword != checkPassword) throw ServiceException("401-1", "비밀번호가 일치하지 않습니다.")

        member.modifyPassword(passwordEncoder.encode(newPassword))
    }


    // 신고에 의한 신용도 감소
    @Transactional
    fun decreaseByNofiy(target: Member, reporter: Member) {
        val targetId = target.id
        val reporterId = reporter.id

        val count = reportRepository.countByReporterIdAndCreateDateAfter(reporterId, LocalDateTime.now().minusDays(1))

        // 신고는 하루에 3번만 가능
        if (count >= 3) throw ServiceException("400-6", "신고는 하루에 3번만 가능합니다.")


        val exists = reportRepository.existsByReporterAndTargetAndCreateDateAfter(
            reporter,
            target,
            LocalDateTime.now().minusDays(1)
        )

        // A 회원 대상 B 회원의 최대 신고 횟수 하루에 1번 제한
        if (exists) throw ServiceException("400-6", "이미 신고한 회원입니다.")

        reportRepository.save(Report(target, reporter))

        // 이미 정지/탈퇴/영구정지 상태면 신고 누적 X
        when (target.status) {                              // [8] if 연속 → when
            MemberStatus.SUSPENDED, MemberStatus.BANNED -> throw ServiceException("400-2", "해당 회원은 정지된 회원입니다.")
            MemberStatus.WITHDRAWN -> throw ServiceException("400-3", "해당 회원은 탈퇴한 회원입니다.")
            else -> Unit
        }

        val reputation = reputationRepository.findById(target.id).get()
        reputation.increaseNotify()

        // 신고 10회 이상 => 평판 감소
        if (reputation.notifyCount % 10 == 0) reputation.decrease()

        // 신고 100회 이상 => 계정 정지
        if (reputation.notifyCount >= 100) {
            target.suspend()
            reputation.notifyCount = 0
        }

        // 신고 10000회 이상 => 영구 정지
        if (reputation.totalNotifyCount >= 10000) target.banned()
    }

    // 경매 취소 시 신용도 감소 (if 입찰 O)
    @Transactional
    fun decreaseByCancel(auctionId: Int, actorId: Int) {
        val auction = auctionRepository.findById(auctionId).get()
        val seller = memberRepository.findById(actorId).get()

        // if 입찰 O
        if (auction.bidCount > 0) {
            val reputation = reputationRepository.findById(seller.id).get()

            // 증감 계산
            val before = reputation.score
            reputation.decrease()
            val after = reputation.score

            val event = ReputationEvent(seller, EventType.CANCEL, RefType.AUCTION, auctionId, abs(before - after))
            eventRepository.save(event)
        }
    }

    // 낙찰 & 거래 완료 시 신용도 증가
    @Transactional
    fun increaseByDeal(dealId: Int) {
        val auction = auctionRepository.findById(dealId).get()
        val seller = memberRepository.findById(auction.seller.id).get()

        val reputation = reputationRepository.findById(seller.id).get()
        // 증감 계산
        val before = reputation.score
        reputation.increase()
        val after = reputation.score

        val event = ReputationEvent(seller, EventType.CANCEL, RefType.AUCTION, dealId, abs(before - after))
        eventRepository.save(event)
    }


    // 리뷰 생성
    @Transactional
    fun createReview(star: Int, msg: String?, member: Member, reviewerId: Int): Review {
        if (findById(reviewerId).get().status == MemberStatus.SUSPENDED) {
            throw ServiceException("403-3", "정지된 회원은 해당 기능을 사용할 수 없습니다.")
        }
        val review = Review(member, star, msg, reviewerId)
        return reviewRepository.save(review)
    }


    fun passwordValidation(username: String?, password: String?) {
        password ?: return

        // 길이 검증 (이미 @Size로 체크되지만 명시적으로)
        if (password.length !in 8..20)
            throw ServiceException("400-1", "비밀번호는 8-20자여야 합니다")


        // 복잡도 검증 (영문 대/소문자, 숫자, 특수문자 중 3가지 이상)
        val complexityCount = listOf(                       // [15] 복잡도 체크 간결화
            Regex("[a-z]"),
            Regex("[A-Z]"),
            Regex("[0-9]"),
            Regex("[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]"),
        ).count { it.containsMatchIn(password) }

        if (complexityCount < 3)
            throw ServiceException("400-1", "비밀번호는 영문 대/소문자, 숫자, 특수문자 중 3가지 이상 조합이어야 합니다")


        // 연속된 문자 검증
        if (hasConsecutiveChars(password))
            throw ServiceException("400-1", "연속된 문자 또는 숫자 3개 이상 사용할 수 없습니다")

        // 동일 문자 반복 검증
        if (Regex("(.)\\1\\1").containsMatchIn(password))  // Pattern.compile → Regex
            throw ServiceException("400-1", "동일한 문자를 3번 이상 연속 사용할 수 없습니다")

        // 비밀번호에 아이디 포함되는지 검증
        if (!username.isNullOrEmpty() && password.lowercase().contains(username.lowercase()))
            throw ServiceException("400-1", "비밀번호에 아이디를 포함할 수 없습니다")
    }

    private fun hasConsecutiveChars(password: String): Boolean {
        val lowerPassword = password.lowercase(Locale.getDefault())

        // 연속된 숫자 체크
        val consecutiveNumbers = listOf(
            "012", "123", "234", "345", "456", "567", "678", "789", "890"
        )
        if (consecutiveNumbers.any { lowerPassword.contains(it) }) return true

        // 연속된 알파벳 체크
        for (i in 0..<lowerPassword.length - 2) {
            val c1 = lowerPassword.get(i)
            val c2 = lowerPassword.get(i + 1)
            val c3 = lowerPassword.get(i + 2)

            if (Character.isLetter(c1) && Character.isLetter(c2) && Character.isLetter(c3)) {
                if (c2.code == c1.code + 1 && c3.code == c2.code + 1) {
                    return true
                }
            }
        }

        return false
    }

    // 프로필 사진 변경
    @Transactional
    fun modifyProfile(memberId: Int, profileImg: MultipartFile) {
        if (profileImg.isEmpty()) throw ServiceException("400-1", "업로드할 이미지 파일이 없습니다.")


        // 트랜잭션 내에서 managed entity를 조회해야 dirty checking이 동작함
        val managedMember = memberRepository.findById(memberId)
            .orElseThrow { ServiceException("404-1", "존재하지 않는 회원입니다.") }

        val imageUrl = fileStoragePort.storeFile(profileImg, "member")

        imageRepository.save(Image(imageUrl))

        managedMember.modify(managedMember.getName(), imageUrl)
    }
}
