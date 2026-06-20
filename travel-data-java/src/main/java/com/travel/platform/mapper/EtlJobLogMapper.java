package com.travel.platform.mapper;

import com.travel.platform.entity.EtlJobLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EtlJobLogMapper {

    @Insert("""
            INSERT INTO etl_job_log
            (job_name, job_type, job_desc, start_time, status, row_count, data_date, request_id, trigger_type)
            VALUES
            (#{jobName}, #{jobType}, #{jobDesc}, #{startTime}, #{status}, #{rowCount}, #{dataDate}, #{requestId}, #{triggerType})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EtlJobLog log);

    @Update("""
            UPDATE etl_job_log
            SET end_time = #{endTime},
                status = #{status},
                row_count = #{rowCount},
                duration_seconds = TIMESTAMPDIFF(SECOND, start_time, #{endTime}),
                error_message = #{errorMessage}
            WHERE id = #{id}
            """)
    int updateFinish(EtlJobLog log);

    @Select("""
            SELECT
                id, job_name, job_type, job_desc, start_time, end_time,
                status, row_count, data_date, request_id, duration_seconds, error_message, trigger_type,
                created_at, updated_at
            FROM etl_job_log
            ORDER BY start_time DESC
            LIMIT 50
            """)
    List<EtlJobLog> selectLatest();
}
