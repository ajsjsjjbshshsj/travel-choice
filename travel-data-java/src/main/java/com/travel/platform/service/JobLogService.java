package com.travel.platform.service;

import com.travel.platform.entity.EtlJobLog;
import com.travel.platform.mapper.EtlJobLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobLogService {

    private final EtlJobLogMapper etlJobLogMapper;

    public JobLogService(EtlJobLogMapper etlJobLogMapper) {
        this.etlJobLogMapper = etlJobLogMapper;
    }

    public EtlJobLog startJob(String jobName, String jobType, String jobDesc, LocalDate dataDate) {
        EtlJobLog log = new EtlJobLog();
        log.setJobName(jobName);
        log.setJobType(jobType);
        log.setJobDesc(jobDesc);
        log.setStartTime(LocalDateTime.now());
        log.setStatus("RUNNING");
        log.setRowCount(0);
        log.setDataDate(dataDate);
        log.setTriggerType("API");

        etlJobLogMapper.insert(log);
        return log;
    }

    public void finishSuccess(Long id, Integer rowCount) {
        EtlJobLog log = new EtlJobLog();
        log.setId(id);
        log.setEndTime(LocalDateTime.now());
        log.setStatus("SUCCESS");
        log.setRowCount(rowCount);
        log.setErrorMessage(null);

        etlJobLogMapper.updateFinish(log);
    }

    public void finishFailed(Long id, String errorMessage) {
        EtlJobLog log = new EtlJobLog();
        log.setId(id);
        log.setEndTime(LocalDateTime.now());
        log.setStatus("FAILED");
        log.setRowCount(0);
        log.setErrorMessage(errorMessage);

        etlJobLogMapper.updateFinish(log);
    }

    public List<EtlJobLog> listLatest() {
        return etlJobLogMapper.selectLatest();
    }
}