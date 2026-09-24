package com.smart_plant.smart_plant.service;

import com.smart_plant.smart_plant.dto.FarmChatDetailResponse;
import com.smart_plant.smart_plant.dto.FarmChatSendRequest;
import com.smart_plant.smart_plant.dto.FarmChatSendResponse;
import com.smart_plant.smart_plant.dto.FarmChatSessionResponse;

import java.util.List;

/** 农场主、普通用户与技术人员的成员聊天服务。 */
public interface FarmChatService {
    /** 校验当前账号可以使用成员聊天，附件上传入口也复用该校验。 */
    void requireCurrentChatRole();

    /** 返回当前账号按绑定关系可联系的全部人员及已有会话摘要。 */
    List<FarmChatSessionResponse> listSessions();

    /** 使用会话ID或联系人ID读取聊天记录，并将当前账号收到的消息标记为已读。 */
    FarmChatDetailResponse getDetail(Long sessionId, Long peerUserId, Long beforeId, Long afterId,
                                     Integer pageSize);

    /** 校验有效绑定后保存消息、更新未读数并实时推送。 */
    FarmChatSendResponse send(FarmChatSendRequest request);
}
