CREATE TABLE IF NOT EXISTS video_assets (
    video_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '视频资产ID；上传接口返回的videoId，发布时用于引用该资产',
    account_id BIGINT UNSIGNED NOT NULL COMMENT '资产所属账号ID，用于所有权校验',
    upload_id VARCHAR(64) COMMENT '分片上传会话ID；关联Redis上传会话，普通直传时为空',
    object_key VARCHAR(512) NOT NULL COMMENT 'MinIO对象键；服务端访问、删除对象时使用的稳定存储路径',
    video_url VARCHAR(1024) COMMENT '上传完成时生成的公开视频URL快照；实际访问地址可由object_key重新生成',
    original_filename VARCHAR(255) NOT NULL COMMENT '用户选择文件时的原始文件名',
    file_size BIGINT UNSIGNED NOT NULL COMMENT '原始视频文件大小，单位字节',
    file_hash VARCHAR(128) COMMENT '前端计算并提交的完整文件Hash；当前为MD5，尚未由服务端对合并文件复算校验',
    upload_status VARCHAR(32) NOT NULL COMMENT '上传状态：UPLOADING、COMPLETED、FAILED',
    media_status VARCHAR(32) NOT NULL COMMENT '媒体处理状态；当前初始值为PENDING，预留给后续转码或分析流程',
    created_time DATETIME(3) NOT NULL COMMENT '资产记录创建时间，即上传初始化时间',
    updated_time DATETIME(3) NOT NULL COMMENT '资产状态或URL最后更新时间',
    PRIMARY KEY (video_id),
    UNIQUE KEY uk_video_assets_upload_id (upload_id),
    UNIQUE KEY uk_video_assets_object_key (object_key),
    KEY idx_video_assets_account_hash (account_id, file_hash),
    KEY idx_video_assets_status (upload_status, media_status),
    CONSTRAINT fk_video_assets_account FOREIGN KEY (account_id) REFERENCES accounts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频上传资产表；记录上传生命周期和MinIO对象，不代表视频已正式发布';

ALTER TABLE videos
    ADD COLUMN asset_id BIGINT UNSIGNED NULL,
    ADD UNIQUE KEY uk_videos_asset_id (asset_id),
    ADD CONSTRAINT fk_videos_asset FOREIGN KEY (asset_id) REFERENCES video_assets(video_id);
