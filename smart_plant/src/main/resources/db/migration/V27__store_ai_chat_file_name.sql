-- AI 文件咨询的历史消息需要保留用户可见的原始文件名，旧记录保持 NULL 并由页面显示通用名称。
ALTER TABLE `ai_chat`
    ADD COLUMN `file_name` varchar(180) DEFAULT NULL COMMENT 'AI咨询附件原始文件名' AFTER `image_url`;
