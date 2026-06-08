package com.travel.platform.mapper;

import com.travel.platform.entity.DataQualityResult;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DataQualityResultMapper {

    @Insert("""
            INSERT INTO data_quality_check_result
            (table_name, rule_name, rule_desc, check_result, error_count, check_time, error_message)
            VALUES
            (#{tableName}, #{ruleName}, #{ruleDesc}, #{checkResult}, #{errorCount}, #{checkTime}, #{errorMessage})
            """)
    int insert(DataQualityResult result);

    @Select("""
            SELECT
                id, table_name, rule_name, rule_desc, check_result,
                error_count, check_time, error_message, created_at
            FROM data_quality_check_result
            ORDER BY check_time DESC
            LIMIT 100
            """)
    List<DataQualityResult> selectLatest();
}