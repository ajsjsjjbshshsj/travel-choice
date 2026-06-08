package com.travel.platform.controller;

import com.travel.platform.entity.DataQualityResult;
import com.travel.platform.service.DataQualityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quality")
public class DataQualityController {

    private final DataQualityService dataQualityService;

    public DataQualityController(DataQualityService dataQualityService) {
        this.dataQualityService = dataQualityService;
    }

    @PostMapping("/run")
    public String runQualityChecks() {
        dataQualityService.runChecks();
        return "数据质量检查完成";
    }

    @GetMapping("/run")
    public String runQualityChecksGet() {
        dataQualityService.runChecks();
        return "数据质量检查完成";
    }

    @GetMapping("/results")
    public List<DataQualityResult> listResults() {
        return dataQualityService.listLatest();
    }
}