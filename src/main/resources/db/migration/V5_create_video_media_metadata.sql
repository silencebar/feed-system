CREATE TABLE video_media_metadata (
    video_id BIGINT UNSIGNED NOT NULL COMMENT '关联视频资产ID',
    duration_seconds DECIMAL(12, 3) NOT NULL COMMENT '用于处理的视频时长，单位秒',
    width INT NOT NULL COMMENT '主视频流宽度',
    height INT NOT NULL COMMENT '主视频流高度',
    video_codec VARCHAR(64) NOT NULL COMMENT '主视频流编码',
    has_audio BOOLEAN NOT NULL COMMENT '是否存在音频流',
    audio_codec VARCHAR(64) NULL COMMENT '第一条音频流编码',
    probe_json JSON NOT NULL COMMENT 'FFprobe原始JSON结果',
    probed_at DATETIME(3) NOT NULL COMMENT '成功探测时间',

    PRIMARY KEY (video_id),

    CONSTRAINT fk_video_metadata_asset
        FOREIGN KEY (video_id)
        REFERENCES video_assets(video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
COMMENT='视频技术元数据，不包含画面语义或内容审核结果';