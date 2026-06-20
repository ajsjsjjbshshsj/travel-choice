package com.travel.platform.controller;

import com.travel.platform.common.result.ApiResponse;
import com.travel.platform.entity.EtlJobLog;
import com.travel.platform.service.JobLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/etl/jobs")
public class EtlJobController {

    private final JobLogService jobLogService;

    public EtlJobController(JobLogService jobLogService) {
        this.jobLogService = jobLogService;
    }

    @GetMapping
    public ApiResponse<List<EtlJobLog>> listLatestJobs() {
        return ApiResponse.success(jobLogService.listLatest());
    }
}
