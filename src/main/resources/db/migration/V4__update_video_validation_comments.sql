ALTER TABLE video_assets
    MODIFY COLUMN file_hash VARCHAR(128) NULL COMMENT '完整文件MD5；初始化为客户端声明值，新上传通过完整性检查后写服务端计算值；历史数据未回溯验证',
    MODIFY COLUMN media_status VARCHAR(32) NOT NULL COMMENT '媒体结构检查：PENDING待检查、PROCESSING检查中、READY结构有效、FAILED结构无效；不表示内容审核或转码完成';
