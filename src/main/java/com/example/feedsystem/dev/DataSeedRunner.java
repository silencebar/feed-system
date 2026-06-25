package com.example.feedsystem.dev;

import com.example.feedsystem.account.service.AccountTokenCache;
import com.example.feedsystem.account.service.JwtService;
import com.example.feedsystem.comment.service.CommentEventPublisher;
import com.example.feedsystem.feed.service.TimelineService;
import com.example.feedsystem.like.service.LikeEventPublisher;
import com.example.feedsystem.like.service.PopularityCacheService;
import com.example.feedsystem.notification.mapper.NotificationMapper;
import com.example.feedsystem.social.service.SocialEventPublisher;
import com.example.feedsystem.video.model.VideoDO;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "data.seed", name = "enabled", havingValue = "true")
public class DataSeedRunner implements ApplicationRunner {
    private static final int SEED = 20260624;

    private final DataSeedProperties properties;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AccountTokenCache accountTokenCache;
    private final TimelineService timelineService;
    private final PopularityCacheService popularityCacheService;
    private final LikeEventPublisher likeEventPublisher;
    private final CommentEventPublisher commentEventPublisher;
    private final SocialEventPublisher socialEventPublisher;
    private final NotificationMapper notificationMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (properties.isCleanup()) {
            cleanup();
        } else if (existsUser001()) {
            SeedData existing = loadExistingSeedData();
            writeCsv(existing);
            logStats(existing, 0, 0, 0);
            log.info("Seed user user001 already exists. Skipped inserts. Set data.seed.cleanup=true to rebuild test data.");
            return;
        }

        Random random = new Random(SEED);
        List<AccountSeed> users = createUsers();
        List<VideoSeed> videos = createVideos(users, random);
        List<FollowSeed> follows = createFollows(users, random);
        List<LikeSeed> likes = createLikes(users, videos, random);
        List<CommentSeed> comments = createComments(users, videos, random);
        SeedData data = new SeedData(users, videos, likes, comments, follows);
        writeCsv(data);
        logStats(data, likes.size(), comments.size(), follows.size());
        triggerAsyncPaths(videos, likes, comments, follows);
    }

    private List<AccountSeed> createUsers() {
        String encoded = passwordEncoder.encode(properties.getPassword());
        List<AccountSeed> users = new ArrayList<>();
        for (int i = 1; i <= properties.getUserCount(); i++) {
            String username = "user%03d".formatted(i);
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO accounts (username, password) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, username);
                ps.setString(2, encoded);
                return ps;
            }, keyHolder);
            long id = keyHolder.getKey().longValue();
            String token = jwtService.generateAccessToken(id, username);
            jdbcTemplate.update("UPDATE accounts SET token = ? WHERE id = ?", token, id);
            accountTokenCache.saveAccessToken(id, token);
            users.add(new AccountSeed(id, username, token));
        }
        return users;
    }

    private List<VideoSeed> createVideos(List<AccountSeed> users, Random random) {
        List<VideoSeed> videos = new ArrayList<>();
        OffsetDateTime base = OffsetDateTime.now().minusDays(30);
        int sequence = 0;
        for (AccountSeed user : users) {
            int count = between(random, properties.getMinVideosPerUser(), properties.getMaxVideosPerUser());
            for (int i = 0; i < count; i++) {
                sequence++;
                int videoNumber = sequence;
                long popularity = 5L + random.nextInt(300);
                OffsetDateTime createdAt = base.plusMinutes(videoNumber * 17L);
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement("""
                            INSERT INTO videos (author_id, username, title, description, play_url, cover_url,
                                                create_time, likes_count, popularity)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """, Statement.RETURN_GENERATED_KEYS);
                    ps.setLong(1, user.id());
                    ps.setString(2, user.username());
                    ps.setString(3, "JMeter Seed Video " + videoNumber);
                    ps.setString(4, "seed video for pressure test #" + videoNumber);
                    ps.setString(5, properties.getVideoObjectKey());
                    ps.setString(6, properties.getCoverObjectKey());
                    ps.setTimestamp(7, Timestamp.from(createdAt.toInstant()));
                    ps.setLong(8, 0);
                    ps.setLong(9, popularity);
                    return ps;
                }, keyHolder);
                videos.add(new VideoSeed(keyHolder.getKey().longValue(), user.id(), createdAt, popularity));
            }
        }
        return videos;
    }

    private List<FollowSeed> createFollows(List<AccountSeed> users, Random random) {
        List<FollowSeed> follows = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (AccountSeed follower : users) {
            List<AccountSeed> candidates = new ArrayList<>(users);
            Collections.shuffle(candidates, random);
            int target = between(random, properties.getMinFollowsPerUser(), properties.getMaxFollowsPerUser());
            for (AccountSeed vlogger : candidates) {
                if (follower.id() == vlogger.id()) continue;
                String key = follower.id() + ":" + vlogger.id();
                if (!seen.add(key)) continue;
                jdbcTemplate.update("INSERT INTO socials (follower_id, vlogger_id) VALUES (?, ?)",
                        follower.id(), vlogger.id());
                follows.add(new FollowSeed(follower, vlogger.id()));
                if (followsFor(follows, follower.id()) >= target) break;
            }
        }
        return follows;
    }

    private List<LikeSeed> createLikes(List<AccountSeed> users, List<VideoSeed> videos, Random random) {
        List<LikeSeed> likes = new ArrayList<>();
        Map<Long, Long> likeCounts = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (AccountSeed user : users) {
            List<VideoSeed> candidates = new ArrayList<>(videos);
            Collections.shuffle(candidates, random);
            int target = between(random, properties.getMinLikesPerUser(), properties.getMaxLikesPerUser());
            int done = 0;
            for (VideoSeed video : candidates) {
                String key = user.id() + ":" + video.id();
                if (!seen.add(key)) continue;
                jdbcTemplate.update("INSERT INTO likes (video_id, account_id, created_at) VALUES (?, ?, ?)",
                        video.id(), user.id(), Timestamp.from(OffsetDateTime.now().minusMinutes(random.nextInt(2000)).toInstant()));
                likeCounts.merge(video.id(), 1L, Long::sum);
                likes.add(new LikeSeed(user, video.id()));
                done++;
                if (done >= target) break;
            }
        }
        likeCounts.forEach((videoId, count) ->
                jdbcTemplate.update("UPDATE videos SET likes_count = likes_count + ?, popularity = popularity + ? WHERE id = ?",
                        count, count, videoId));
        return likes;
    }

    private List<CommentSeed> createComments(List<AccountSeed> users, List<VideoSeed> videos, Random random) {
        List<CommentSeed> comments = new ArrayList<>();
        Map<Long, Long> commentCounts = new LinkedHashMap<>();
        for (AccountSeed user : users) {
            List<VideoSeed> candidates = new ArrayList<>(videos);
            Collections.shuffle(candidates, random);
            int target = between(random, properties.getMinCommentsPerUser(), properties.getMaxCommentsPerUser());
            for (int i = 0; i < target; i++) {
                VideoSeed video = candidates.get(i % candidates.size());
                String content = "seed comment userId=%d videoId=%d uuid=%s"
                        .formatted(user.id(), video.id(), UUID.randomUUID());
                jdbcTemplate.update("""
                        INSERT INTO comments (username, video_id, author_id, content, created_at)
                        VALUES (?, ?, ?, ?, ?)
                        """, user.username(), video.id(), user.id(), content,
                        Timestamp.from(OffsetDateTime.now().minusMinutes(random.nextInt(1000)).toInstant()));
                commentCounts.merge(video.id(), 1L, Long::sum);
                comments.add(new CommentSeed(user, video.id(), content));
            }
        }
        commentCounts.forEach((videoId, count) ->
                jdbcTemplate.update("UPDATE videos SET popularity = popularity + ? WHERE id = ?", count, videoId));
        if (hasColumn("videos", "comments_count")) {
            commentCounts.forEach((videoId, count) ->
                    jdbcTemplate.update("UPDATE videos SET comments_count = comments_count + ? WHERE id = ?", count, videoId));
        } else {
            log.info("videos.comments_count column not found; skipped comments_count update.");
        }
        return comments;
    }

    private void triggerAsyncPaths(List<VideoSeed> videos, List<LikeSeed> likes, List<CommentSeed> comments,
            List<FollowSeed> follows) {
        try {
            refreshRedis(videos);
        } catch (RuntimeException ex) {
            log.warn("Seed data has been written, but Redis feed/hot refresh failed.", ex);
        }
        try {
            publishSampleEvents(likes, comments, follows);
        } catch (RuntimeException ex) {
            log.warn("Seed data has been written, but RabbitMQ sample event publishing failed.", ex);
        }
    }

    private void refreshRedis(List<VideoSeed> videos) {
        for (VideoSeed video : videos) {
            VideoDO videoDO = new VideoDO();
            videoDO.setId(video.id());
            videoDO.setCreateTime(video.createdAt());
            timelineService.addVideo(videoDO);
            popularityCacheService.changeVideoPopularity(video.id(), Math.max(1, video.popularity().intValue()));
        }
    }

    private void publishSampleEvents(List<LikeSeed> likes, List<CommentSeed> comments, List<FollowSeed> follows) {
        likes.stream().limit(50).forEach(like -> likeEventPublisher.publishLike(like.user().id(), like.videoId()));
        comments.stream().limit(50).forEach(comment -> {
            // Publish through the same exchange/routing key shape used by the application.
            commentEventPublisher.publishComment(toCommentDo(comment));
        });
        follows.stream().limit(50).forEach(follow -> socialEventPublisher.publishFollow(follow.user().id(), follow.vloggerId()));
    }

    private com.example.feedsystem.comment.model.CommentDO toCommentDo(CommentSeed seed) {
        com.example.feedsystem.comment.model.CommentDO comment = new com.example.feedsystem.comment.model.CommentDO();
        comment.setId(0L);
        comment.setAuthorId(seed.user().id());
        comment.setUsername(seed.user().username());
        comment.setVideoId(seed.videoId());
        comment.setContent(seed.content());
        comment.setCreatedAt(OffsetDateTime.now());
        return comment;
    }

    private void cleanup() {
        List<Long> accountIds = testAccountIds();
        if (accountIds.isEmpty()) return;
        List<Long> videoIds = jdbcTemplate.queryForList(
                "SELECT id FROM videos WHERE author_id IN (%s)".formatted(placeholders(accountIds.size())),
                Long.class, accountIds.toArray());
        if (!videoIds.isEmpty()) {
            jdbcTemplate.update("DELETE FROM video_tags WHERE video_id IN (%s)".formatted(placeholders(videoIds.size())), videoIds.toArray());
            jdbcTemplate.update("DELETE FROM outbox_msgs WHERE video_id IN (%s)".formatted(placeholders(videoIds.size())), videoIds.toArray());
            jdbcTemplate.update("DELETE FROM likes WHERE video_id IN (%s)".formatted(placeholders(videoIds.size())), videoIds.toArray());
            jdbcTemplate.update("DELETE FROM comments WHERE video_id IN (%s)".formatted(placeholders(videoIds.size())), videoIds.toArray());
            notificationMapper.deleteByVideoIds(videoIds);
        }
        jdbcTemplate.update("DELETE FROM likes WHERE account_id IN (%s)".formatted(placeholders(accountIds.size())), accountIds.toArray());
        jdbcTemplate.update("DELETE FROM socials WHERE follower_id IN (%s) OR vlogger_id IN (%s)"
                .formatted(placeholders(accountIds.size()), placeholders(accountIds.size())), merge(accountIds, accountIds));
        jdbcTemplate.update("DELETE FROM comments WHERE author_id IN (%s)".formatted(placeholders(accountIds.size())), accountIds.toArray());
        notificationMapper.deleteByAccountIds(accountIds);
        jdbcTemplate.update("DELETE FROM videos WHERE author_id IN (%s)".formatted(placeholders(accountIds.size())), accountIds.toArray());
        jdbcTemplate.update("DELETE FROM accounts WHERE id IN (%s)".formatted(placeholders(accountIds.size())), accountIds.toArray());
    }

    private void writeCsv(SeedData data) throws IOException {
        Path dir = Path.of(properties.getOutputDir());
        Files.createDirectories(dir);
        write(dir.resolve("users.csv"), "username,password\n",
                data.users().stream().map(user -> user.username() + "," + properties.getPassword()).toList());
        write(dir.resolve("tokens.csv"), "token,account_id\n",
                data.users().stream().map(user -> user.token() + "," + user.id()).toList());
        write(dir.resolve("video_ids.csv"), "video_id\n",
                data.videos().stream().map(video -> String.valueOf(video.id())).toList());
        write(dir.resolve("like_pairs.csv"), "token,account_id,video_id\n",
                data.likes().stream().map(like -> like.user().token() + "," + like.user().id() + "," + like.videoId()).toList());
        write(dir.resolve("comment_pairs.csv"), "token,account_id,video_id,content\n",
                data.comments().stream().map(comment -> csv(comment.user().token(), comment.user().id(), comment.videoId(), comment.content())).toList());
        write(dir.resolve("follow_pairs.csv"), "token,account_id,vlogger_id\n",
                data.follows().stream().map(follow -> follow.user().token() + "," + follow.user().id() + "," + follow.vloggerId()).toList());
    }

    private void write(Path path, String header, List<String> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(header.stripTrailing());
        lines.addAll(rows);
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    private SeedData loadExistingSeedData() {
        List<AccountSeed> users = jdbcTemplate.query("SELECT id, username, token FROM accounts WHERE username LIKE 'user%'",
                (rs, rowNum) -> new AccountSeed(rs.getLong("id"), rs.getString("username"), rs.getString("token")))
                .stream().filter(user -> user.username().matches("user\\d{3}")).toList();
        List<Long> ids = users.stream().map(AccountSeed::id).toList();
        List<VideoSeed> videos = ids.isEmpty() ? List.of() : jdbcTemplate.query(
                "SELECT id, author_id, create_time, popularity FROM videos WHERE author_id IN (%s)".formatted(placeholders(ids.size())),
                (rs, rowNum) -> new VideoSeed(rs.getLong("id"), rs.getLong("author_id"),
                        rs.getTimestamp("create_time").toInstant().atOffset(OffsetDateTime.now().getOffset()),
                        rs.getLong("popularity")),
                ids.toArray());
        Map<Long, AccountSeed> userById = new LinkedHashMap<>();
        users.forEach(user -> userById.put(user.id(), user));
        Set<Long> videoIds = videos.stream().map(VideoSeed::id).collect(java.util.stream.Collectors.toSet());
        List<LikeSeed> likes = ids.isEmpty() ? List.of() : jdbcTemplate.query("""
                SELECT account_id, video_id FROM likes
                WHERE account_id IN (%s)
                """.formatted(placeholders(ids.size())),
                (rs, rowNum) -> new LikeSeed(userById.get(rs.getLong("account_id")), rs.getLong("video_id")),
                ids.toArray()).stream()
                .filter(like -> like.user() != null && videoIds.contains(like.videoId()))
                .toList();
        List<CommentSeed> comments = ids.isEmpty() ? List.of() : jdbcTemplate.query("""
                SELECT author_id, video_id, content FROM comments
                WHERE author_id IN (%s)
                """.formatted(placeholders(ids.size())),
                (rs, rowNum) -> new CommentSeed(userById.get(rs.getLong("author_id")),
                        rs.getLong("video_id"), rs.getString("content")),
                ids.toArray()).stream()
                .filter(comment -> comment.user() != null && videoIds.contains(comment.videoId()))
                .toList();
        List<FollowSeed> follows = ids.isEmpty() ? List.of() : jdbcTemplate.query("""
                SELECT follower_id, vlogger_id FROM socials
                WHERE follower_id IN (%s)
                """.formatted(placeholders(ids.size())),
                (rs, rowNum) -> new FollowSeed(userById.get(rs.getLong("follower_id")), rs.getLong("vlogger_id")),
                ids.toArray()).stream()
                .filter(follow -> follow.user() != null && userById.containsKey(follow.vloggerId()))
                .toList();
        return new SeedData(users, videos, likes, comments, follows);
    }

    private List<Long> testAccountIds() {
        return jdbcTemplate.query("SELECT id, username FROM accounts WHERE username LIKE 'user%'",
                (rs, rowNum) -> Map.entry(rs.getLong("id"), rs.getString("username")))
                .stream().filter(entry -> entry.getValue().matches("user\\d{3}")).map(Map.Entry::getKey).toList();
    }

    private boolean existsUser001() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM accounts WHERE username = 'user001'", Integer.class);
        return count != null && count > 0;
    }

    private boolean hasColumn(String table, String column) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, Integer.class, table, column);
        return count != null && count > 0;
    }

    private int between(Random random, int min, int max) {
        return min + random.nextInt(Math.max(1, max - min + 1));
    }

    private long followsFor(List<FollowSeed> follows, long followerId) {
        return follows.stream().filter(follow -> follow.user().id() == followerId).count();
    }

    private String placeholders(int size) {
        return String.join(",", Collections.nCopies(size, "?"));
    }

    private Object[] merge(List<Long> first, List<Long> second) {
        List<Long> merged = new ArrayList<>(first);
        merged.addAll(second);
        return merged.toArray();
    }

    private String csv(Object... values) {
        List<String> escaped = new ArrayList<>();
        for (Object value : values) {
            String text = String.valueOf(value).replace("\"", "\"\"");
            escaped.add("\"" + text + "\"");
        }
        return String.join(",", escaped);
    }

    private void logStats(SeedData data, int likes, int comments, int follows) {
        log.info("JMeter seed completed: users={}, videos={}, likes={}, comments={}, follows={}, csvDir={}",
                data.users().size(), data.videos().size(), likes, comments, follows,
                Path.of(properties.getOutputDir()).toAbsolutePath());
    }

    private record AccountSeed(long id, String username, String token) {}
    private record VideoSeed(long id, long authorId, OffsetDateTime createdAt, Long popularity) {}
    private record LikeSeed(AccountSeed user, long videoId) {}
    private record CommentSeed(AccountSeed user, long videoId, String content) {}
    private record FollowSeed(AccountSeed user, long vloggerId) {}
    private record SeedData(List<AccountSeed> users, List<VideoSeed> videos, List<LikeSeed> likes,
                            List<CommentSeed> comments, List<FollowSeed> follows) {}
}
