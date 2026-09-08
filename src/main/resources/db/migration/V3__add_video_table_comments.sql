ALTER TABLE video_assets
    COMMENT = '视频上传资产表；记录上传生命周期和MinIO对象，不代表视频已正式发布',
    MODIFY COLUMN video_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '视频资产ID；上传接口返回的videoId，发布时用于引用该资产',
    MODIFY COLUMN account_id BIGINT UNSIGNED NOT NULL COMMENT '资产所属账号ID，用于所有权校验',
    MODIFY COLUMN upload_id VARCHAR(64) NULL COMMENT '分片上传会话ID；关联Redis上传会话，普通直传时为空',
    MODIFY COLUMN object_key VARCHAR(512) NOT NULL COMMENT 'MinIO对象键；服务端访问、删除对象时使用的稳定存储路径',
    MODIFY COLUMN video_url VARCHAR(1024) NULL COMMENT '上传完成时生成的公开视频URL快照；实际访问地址可由object_key重新生成',
    MODIFY COLUMN original_filename VARCHAR(255) NOT NULL COMMENT '用户选择文件时的原始文件名',
    MODIFY COLUMN file_size BIGINT UNSIGNED NOT NULL COMMENT '原始视频文件大小，单位字节',
    MODIFY COLUMN file_hash VARCHAR(128) NULL COMMENT '前端计算并提交的完整文件Hash；当前为MD5，尚未由服务端对合并文件复算校验',
    MODIFY COLUMN upload_status VARCHAR(32) NOT NULL COMMENT '上传状态：UPLOADING、COMPLETED、FAILED',
    MODIFY COLUMN media_status VARCHAR(32) NOT NULL COMMENT '媒体处理状态；当前初始值为PENDING，预留给后续转码或分析流程',
    MODIFY COLUMN created_time DATETIME(3) NOT NULL COMMENT '资产记录创建时间，即上传初始化时间',
    MODIFY COLUMN updated_time DATETIME(3) NOT NULL COMMENT '资产状态或URL最后更新时间';

ALTER TABLE videos
    COMMENT = '已发布视频业务表；仅保存正式发布内容，并通过asset_id关联上传资产',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '已发布视频ID；Feed、点赞、评论等业务统一引用该ID',
    MODIFY COLUMN author_id BIGINT NOT NULL COMMENT '视频发布者账号ID',
    MODIFY COLUMN username VARCHAR(255) NOT NULL COMMENT '发布时记录的作者用户名快照',
    MODIFY COLUMN title VARCHAR(255) NOT NULL COMMENT '视频标题',
    MODIFY COLUMN description VARCHAR(255) NULL COMMENT '视频描述',
    MODIFY COLUMN play_url VARCHAR(255) NOT NULL COMMENT '视频MinIO objectKey；字段名为历史命名，数据库实际保存的不是公网URL',
    MODIFY COLUMN cover_url VARCHAR(255) NOT NULL COMMENT '封面MinIO objectKey或兼容的封面地址',
    MODIFY COLUMN create_time DATETIME(3) NULL COMMENT '视频正式发布时间',
    MODIFY COLUMN likes_count BIGINT NOT NULL COMMENT '点赞数量汇总值',
    MODIFY COLUMN popularity BIGINT NOT NULL COMMENT '视频热度值，用于热门Feed排序',
    MODIFY COLUMN asset_id BIGINT UNSIGNED NULL COMMENT '关联video_assets.video_id；标识该发布记录使用的上传资产';
