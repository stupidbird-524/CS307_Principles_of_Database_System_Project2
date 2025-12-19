package io.sustc.service.impl;

import io.sustc.dto.AuthInfo;
import io.sustc.dto.PageResult;
import io.sustc.dto.RecipeRecord;
import io.sustc.dto.ReviewRecord;
import io.sustc.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private DataSource dataSource;

    @Override
    public long addReview(AuthInfo auth, long recipeId, int rating, String review) {
        long userId = authenticate(auth);
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating 1-5");

        try (Connection conn = dataSource.getConnection()) {
            // Check Recipe Exists
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM recipes WHERE id = ?")) {
                stmt.setLong(1, recipeId);
                if (!stmt.executeQuery().next()) throw new IllegalArgumentException("Recipe not found");
            }

            // Insert Review
            // 注意：这里同时插入 create_time 作为 dateSubmitted
            String sql = "INSERT INTO reviews (recipe_id, user_id, rating, content, create_time) VALUES (?, ?, ?, ?, NOW()) RETURNING id";
            long reviewId;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, recipeId);
                stmt.setLong(2, userId);
                stmt.setInt(3, rating);
                stmt.setString(4, review);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) reviewId = rs.getLong(1);
                    else throw new SQLException("Insert failed");
                }
            }
            refreshRecipeAggregatedRating(recipeId);
            return reviewId;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void editReview(AuthInfo auth, long recipeId, long reviewId, int rating, String review) {
        long userId = authenticate(auth);
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating 1-5");

        // 更新 content, rating 以及 date_modified (如果有这个字段的话，没有需自行添加列或忽略)
        String sql = "UPDATE reviews SET rating = ?, content = ? WHERE id = ? AND recipe_id = ? AND user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, rating);
            stmt.setString(2, review);
            stmt.setLong(3, reviewId);
            stmt.setLong(4, recipeId);
            stmt.setLong(5, userId);
            if (stmt.executeUpdate() == 0) throw new SecurityException("Edit failed: Not owner or not found");

            refreshRecipeAggregatedRating(recipeId);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void deleteReview(AuthInfo auth, long recipeId, long reviewId) {
        long userId = authenticate(auth);
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Verify Owner
                try (PreparedStatement stmt = conn.prepareStatement("SELECT user_id, recipe_id FROM reviews WHERE id = ?")) {
                    stmt.setLong(1, reviewId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Not found");
                        if (rs.getLong("user_id") != userId) throw new SecurityException("Not owner");
                        if (rs.getLong("recipe_id") != recipeId) throw new IllegalArgumentException("Recipe mismatch");
                    }
                }

                // Delete Likes then Review
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM review_likes WHERE review_id = ?")) {
                    stmt.setLong(1, reviewId);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM reviews WHERE id = ?")) {
                    stmt.setLong(1, reviewId);
                    stmt.executeUpdate();
                }
                conn.commit();
                refreshRecipeAggregatedRating(recipeId);
            } catch (Exception e) { conn.rollback(); throw e; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public long likeReview(AuthInfo auth, long reviewId) {
        long userId = authenticate(auth);
        try (Connection conn = dataSource.getConnection()) {
            // Check self-like
            try (PreparedStatement stmt = conn.prepareStatement("SELECT user_id FROM reviews WHERE id = ?")) {
                stmt.setLong(1, reviewId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) throw new IllegalArgumentException("Review not found");
                    if (rs.getLong(1) == userId) throw new SecurityException("Cannot like own review");
                }
            }
            // Insert like
            try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO review_likes (review_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
                stmt.setLong(1, reviewId);
                stmt.setLong(2, userId);
                stmt.executeUpdate();
            }
            // Count
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM review_likes WHERE review_id = ?")) {
                stmt.setLong(1, reviewId);
                try (ResultSet rs = stmt.executeQuery()) { return rs.next() ? rs.getLong(1) : 0; }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public long unlikeReview(AuthInfo auth, long reviewId) {
        long userId = authenticate(auth);
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM review_likes WHERE review_id = ? AND user_id = ?")) {
                stmt.setLong(1, reviewId);
                stmt.setLong(2, userId);
                stmt.executeUpdate();
            }
            // Count
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM review_likes WHERE review_id = ?")) {
                stmt.setLong(1, reviewId);
                try (ResultSet rs = stmt.executeQuery()) { return rs.next() ? rs.getLong(1) : 0; }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public PageResult<ReviewRecord> listByRecipe(long recipeId, int page, int size, String sort) {
        // SQL: JOIN users 表以获取 authorName，同时子查询 count likes 用于排序
        String sql = "SELECT r.*, u.name as author_name, " +
                "(SELECT COUNT(*) FROM review_likes rl WHERE rl.review_id = r.id) as like_cnt " +
                "FROM reviews r " +
                "JOIN users u ON r.user_id = u.id " +
                "WHERE r.recipe_id = ? ";

        if ("likes_desc".equals(sort)) sql += "ORDER BY like_cnt DESC, r.create_time DESC ";
        else sql += "ORDER BY r.create_time DESC "; // date_desc default

        sql += "LIMIT ? OFFSET ?";

        List<ReviewRecord> list = new ArrayList<>();
        long total = 0;

        try (Connection conn = dataSource.getConnection()) {
            // 1. Get Total Count
            try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM reviews WHERE recipe_id = ?")) {
                stmt.setLong(1, recipeId);
                try (ResultSet rs = stmt.executeQuery()) { if (rs.next()) total = rs.getLong(1); }
            }

            // 2. Get Records
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, recipeId);
                stmt.setInt(2, size);
                stmt.setInt(3, (page - 1) * size);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ReviewRecord rec = new ReviewRecord();
                        // 字段映射：完全对应 ReviewRecord.java
                        rec.setReviewId(rs.getLong("id"));
                        rec.setRecipeId(rs.getLong("recipe_id"));
                        rec.setAuthorId(rs.getLong("user_id"));
                        rec.setAuthorName(rs.getString("author_name")); // 从 JOIN 获取
                        rec.setRating(rs.getFloat("rating"));
                        rec.setReview(rs.getString("content"));
                        rec.setDateSubmitted(rs.getTimestamp("create_time"));
                        // 假设 update 时没有写 date_modified，这里可以置空或同 create_time
                        // rec.setDateModified(rs.getTimestamp("date_modified"));

                        // 获取具体的点赞用户ID列表 (ReviewRecord 要求 long[])
                        rec.setLikes(fetchLikeUserIds(conn, rec.getReviewId()));

                        list.add(rec);
                    }
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }

        // 构造函数：list, page, size, total
        return new PageResult<>(list, page, size, total);
    }

    @Override
    public RecipeRecord refreshRecipeAggregatedRating(long recipeId) {
        String query = "SELECT AVG(rating) as val, COUNT(*) as cnt FROM reviews WHERE recipe_id = ?";
        String update = "UPDATE recipes SET aggregated_rating = ?, review_count = ? WHERE id = ?";

        try (Connection conn = dataSource.getConnection()) {
            double avg = 0;
            int count = 0;
            boolean hasReview = false;

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setLong(1, recipeId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt("cnt") > 0) {
                        hasReview = true;
                        count = rs.getInt("cnt");
                        avg = Math.round(rs.getDouble("val") * 100.0) / 100.0;
                    }
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(update)) {
                if (hasReview) stmt.setDouble(1, avg);
                else stmt.setNull(1, Types.DOUBLE);

                stmt.setInt(2, count);
                stmt.setLong(3, recipeId);
                stmt.executeUpdate();
            }
            return null;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // --- 辅助方法 ---

    // 获取某条评论的所有点赞用户ID
    private long[] fetchLikeUserIds(Connection conn, long reviewId) {
        List<Long> userIds = new ArrayList<>();
        String sql = "SELECT user_id FROM review_likes WHERE review_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, reviewId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    userIds.add(rs.getLong("user_id"));
                }
            }
        } catch (SQLException e) {
            log.error("Error fetching likes", e);
        }
        // 转换 List<Long> -> long[]
        return userIds.stream().mapToLong(Long::longValue).toArray();
    }

    private long authenticate(AuthInfo auth) {
        if (auth == null) throw new SecurityException("No auth");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id FROM users WHERE id = ? AND password = ?")) {
            stmt.setLong(1, auth.getAuthorId());
            stmt.setString(2, auth.getPassword());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        throw new SecurityException("Auth failed");
    }
}