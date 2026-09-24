-- Revoke only the legacy administrator credential that was distributed in an old
-- manual initialization script. Administrators whose password has already been
-- changed are deliberately left untouched.
--
-- The SHA-256 fingerprint avoids retaining the reusable BCrypt credential in the
-- deployable artifact. A disabled account cannot pass AdminAuthServiceImpl's
-- status check, and the random replacement prevents the old password from ever
-- matching again.
UPDATE `user`
SET `password` = CONCAT('{revoked}', REPLACE(UUID(), '-', ''), REPLACE(UUID(), '-', '')),
    `status` = 0,
    `remark` = CONCAT_WS('; ', NULLIF(`remark`, ''),
                        'Legacy default administrator credential revoked; reset through an approved provisioning process')
WHERE `username` = 'admin'
  AND SHA2(`password`, 256) = '55347fa06c7e81b67bd142b804f7cef38975cd579021bc35b8ef13279d47afc5';
