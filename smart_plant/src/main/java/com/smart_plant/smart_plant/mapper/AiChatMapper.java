package com.smart_plant.smart_plant.mapper;

import com.smart_plant.smart_plant.entity.AiChat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI对话记录 Mapper。
 *
 * <p>该接口负责 ai_chat 表写入、查询、删除和统计。</p>
 */
@Mapper
public interface AiChatMapper {

    /** 新增AI对话记录，并回填自增主键。 */
    int insert(AiChat chat);

    /** 根据主键删除单条对话记录。 */
    int deleteById(Long id);

    /** 根据主键集合批量删除对话记录。 */
    int deleteBatchByIds(@Param("ids") List<Long> ids);

    /** 根据主键查询对话详情。 */
    AiChat selectById(Long id);

    /** 按条件分页查询AI对话记录列表。 */
    List<AiChat> selectList(@Param("userId") Long userId,
                            @Param("username") String username,
                            @Param("modelName") String modelName,
                            @Param("status") Integer status,
                            @Param("startTime") LocalDateTime startTime,
                            @Param("endTime") LocalDateTime endTime);

    /** 按当前用户和游标分页查询成功对话，避免把其他用户的消息或失败记录带到 farm。 */
    List<AiChat> selectClientHistory(@Param("userId") Long userId,
                                     @Param("beforeId") Long beforeId,
                                     @Param("limit") int limit);

    /** 统计对话总数、成功数、失败数、图片对话数和时间范围。 */
    Map<String, Object> selectStatistics(@Param("userId") Long userId,
                                         @Param("username") String username,
                                         @Param("modelName") String modelName,
                                         @Param("status") Integer status,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
}
