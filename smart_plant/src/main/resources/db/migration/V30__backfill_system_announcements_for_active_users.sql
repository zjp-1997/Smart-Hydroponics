-- 为升级前已经存在的有效系统公告补齐所有启用用户的接收明细。
-- 后续新注册用户仍由移动端查询时的幂等同步逻辑补齐历史公告。
INSERT IGNORE INTO `notification`
    (`user_id`, `title`, `content`, `notice_type`, `ref_type`, `ref_id`, `level`, `is_read`,
     `send_time`, `publisher_id`, `publisher_name`, `status`, `remark`)
SELECT
    target.id,
    source.title,
    source.content,
    1,
    'system_announcement',
    grouped.announcement_id,
    source.level,
    0,
    source.send_time,
    source.publisher_id,
    source.publisher_name,
    1,
    source.remark
FROM `user` target
CROSS JOIN (
    SELECT COALESCE(n.ref_id, n.id) AS announcement_id, MIN(n.id) AS source_id
    FROM `notification` n
    WHERE n.status = 1
      AND n.notice_type = 1
      AND COALESCE(n.ref_type, 'system_announcement') = 'system_announcement'
    GROUP BY COALESCE(n.ref_id, n.id)
) grouped
INNER JOIN `notification` source ON source.id = grouped.source_id
LEFT JOIN `notification` existing
  ON existing.user_id = target.id
 AND existing.notice_type = 1
 AND existing.status = 1
 AND COALESCE(existing.ref_type, 'system_announcement') = 'system_announcement'
 AND COALESCE(existing.ref_id, existing.id) = grouped.announcement_id
WHERE target.status = 1
  AND existing.id IS NULL;
