# 视频完整性及媒体结构校验

## 当前流程

分片上传到后端 → 合并 → 校验实际大小和完整 MD5 → FFprobe 返回元数据 → 事务保存元数据及 READY → 上传 MinIO → COMPLETED。
这次 FFprobe 是上传请求内的有界同步检查，不是持久化异步任务；前端 Pinia 上传任务允许站内跳转，但刷新浏览器仍可能中断请求。

- 初始化的 file_hash 必须是 32 位十六进制 MD5；完整文件比对不区分大小写。
- 完整性失败：upload_status=FAILED，不上传 MinIO，清理失效分片和 Redis 会话。前端重试会初始化新会话。
- FFprobe 通过：media_status=READY，然后才上传 MinIO。
- FFprobe 识别不到有效视频结构：media_status=FAILED、upload_status=FAILED，不上传 MinIO。
- 程序缺失、超时或服务繁忙：返回 503；media_status=PENDING、upload_status=FAILED，可以重试。
- MinIO 失败：upload_status=FAILED，已完成的媒体校验状态 READY 保留。
- 普通直传也执行媒体校验、计算并记录 MD5，但接口没有客户端 Hash 参数，因此不执行客户端 Hash 比对。
- 已有历史记录不回填、不声称已验证；新上传开始时 file_hash 仍是声明值，通过完整性检查后写入服务端计算值。

## FFprobe 环境

安装 FFmpeg 发行包中的 ffprobe，在运行 Java 服务的环境设置 FFPROBE_PATH 为可执行文件绝对路径，或将 ffprobe 加入 PATH。
例如 Windows 可配置 FFPROBE_PATH=D:/tools/ffmpeg/bin/ffprobe.exe（替换为真实路径）。
在 IDEA 启动时，环境变量应配置到 Java 服务的运行配置中；修改后重启 Java 服务。

也可设置 Spring 参数 feedsystem.media.ffprobe-path。
feedsystem.media.probe-timeout-seconds 默认 20 秒；单应用实例最多同时运行两个探测进程。
未安装不会静默跳过校验。当前不自动下载或安装外部程序。

使用独立 ProcessBuilder 参数，不经过 shell；限制输入为本地 file 协议和 MOV/MP4 demuxer，不允许远程媒体/播放列表。
JSON 输出上限 64 KiB，超时终止子进程。

校验内容：存在非封面图片的视频流、已识别编码、正宽高、有限且为正的时长。
允许没有音频流，不做音量/静音检查，不做内容合规审核，也不保证全文件每帧可解码。
官方说明：https://ffmpeg.org/ffprobe.html

## 数据库

video_media_metadata 使用已创建的 V5 表结构，以 video_id 为主键关联资产，重复检测更新同一条记录。
保存时长（秒，三位小数）、首个有效视频流宽高和编码、是否存在音轨、第一条音轨编码、FFprobe JSON 输出及保存时刻。
probe_json 是当前 show_entries 指定字段的原始输出，并非 FFprobe 全部可用字段。
无音轨时 has_audio=false、audio_codec=NULL；有音轨不代表存在语音或非静音声音。
VideoMediaMetadataService.saveReady 在一个事务中 upsert 元数据并更新媒体状态 READY，失败回滚，不继续上传 MinIO。
若数据库失败，先前的 PROCESSING 状态可能保留，但上传不会被标记为 COMPLETED；重试会重新检测。
MinIO 上传失败时已保存元数据仍然保留。后续业务须同时检查 upload_status=COMPLETED 与 media_status=READY。
现有 media_status VARCHAR 可保存 PENDING / PROCESSING / READY / FAILED。
V4__update_video_validation_comments.sql 仅更新 video_assets 的字段注释，已有数据不会自动重新校验。
Flyway 关闭时可手动执行 V4，无需重复执行 V2/V3。
已创建 video_media_metadata 的环境无需重新建表；旧视频不会自动回填元数据。本次没有新增 Agent 工具或查询接口。

重启后上传新视频，可按资产 ID 验证：
```sql
SELECT a.video_id, a.upload_status, a.media_status, m.*
FROM video_assets a LEFT JOIN video_media_metadata m ON m.video_id = a.video_id
WHERE a.video_id = 6;
```

## 验证

运行 mvn -q test。测试覆盖完整 MD5、大小、无音轨、纯音频、封面流、非法尺寸/时长、
程序缺失、超时、失败不进入 MinIO、成功校验后上传等路径。
元数据测试使用 H2 MySQL 模式运行真实 MyBatis upsert 和 Spring 事务，验证重复更新、无音轨以及保存失败回滚；不替代真实 MySQL 验收。
进程边界采用模拟测试；部署后需用真实 ffprobe 和真实有声/无声 MP4 做环境验收。
