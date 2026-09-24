-- 私有附件下载按完整 media_url 定位消息；191 字符前缀已覆盖当前受控路径格式。
ALTER TABLE `consult_message`
    ADD KEY `idx_consult_message_media_status` (`media_url`(191), `status`);

ALTER TABLE `farm_chat_message`
    ADD KEY `idx_farm_chat_message_media_status` (`media_url`(191), `status`);
