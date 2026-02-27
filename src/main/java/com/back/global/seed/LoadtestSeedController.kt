package com.back.global.seed

import com.back.global.controller.BaseController
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Profile("loadtest|loadtest-cloud")
@RestController
@RequestMapping("/api/v1/loadtest")
class LoadtestSeedController(
    rq: Rq,
    private val loadtestSeeder: LoadtestSeeder
) : BaseController(rq) {

    @PostMapping("/reset")
    fun reset(): RsData<Void?> {
        loadtestSeeder.reset(authenticatedMemberId)
        return RsData("200-1", "부하테스트 데이터 재시딩 완료")
    }
}
