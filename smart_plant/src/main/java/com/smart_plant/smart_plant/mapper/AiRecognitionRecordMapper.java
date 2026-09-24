package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.AiRecognitionRecord;
import com.smart_plant.smart_plant.entity.AiRecognitionResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AiRecognitionRecordMapper {

    /** 新增AI识别记录，并回填自增主键，供手工识别和自动识别流程复用。 */
    int insertRecord(AiRecognitionRecord record);

    /** 新增AI识别结果，并回填自增主键，结果表承载具体识别结论。 */
    int insertResult(AiRecognitionResult result);

    int deleteById(Long id);

    int deleteBatchByIds(@Param("ids") List<Long> ids);

    int deleteResultsByRecordId(Long recordId);

    int deleteResultsByRecordIds(@Param("ids") List<Long> ids);

    int clearChatSessionResultLinksByRecordId(Long recordId);

    int clearChatSessionResultLinksByRecordIds(@Param("ids") List<Long> ids);

    AiRecognitionRecord selectById(Long id);

    List<AiRecognitionRecord> selectList(@Param("userId") Long userId,
                                         @Param("username") String username,
                                         @Param("sourceType") Integer sourceType,
                                         @Param("status") Integer status,
                                         @Param("resultStatus") Integer resultStatus,
                                         @Param("cameraName") String cameraName,
                                         @Param("plotName") String plotName,
                                         @Param("recognitionType") Long recognitionType,
                                         @Param("resultName") String resultName,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    Map<String, Object> selectStatistics(@Param("userId") Long userId,
                                         @Param("username") String username,
                                         @Param("sourceType") Integer sourceType,
                                         @Param("status") Integer status,
                                         @Param("resultStatus") Integer resultStatus,
                                         @Param("cameraName") String cameraName,
                                         @Param("plotName") String plotName,
                                         @Param("recognitionType") Long recognitionType,
                                         @Param("resultName") String resultName,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
}
