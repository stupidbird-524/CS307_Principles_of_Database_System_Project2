package io.sustc.service.impl;

import io.sustc.dto.RecipeRecord;
import io.sustc.dto.ReviewRecord;
import io.sustc.dto.UserRecord;
import io.sustc.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DatabaseServiceImpl implements DatabaseService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Integer> getGroupMembers() {
        // TODO: 这里填入你的真实学号
        return Arrays.asList(12410724);
    }

    @Override
    public void drop() {
        // 使用 TRUNCATE 清理所有表，速度快且彻底
        String sql = "TRUNCATE TABLE " +
                "review_likes, recipe_ingredients, user_follows, " +
                "reviews, nutrition, recipes, users, ingredients, roles " +
                "CASCADE";
        try {
            jdbcTemplate.execute(sql);
            log.info("All tables truncated successfully.");
        } catch (Exception e) {
            log.warn("Truncate failed (tables might not exist yet): {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void importData(
            List<ReviewRecord> reviewRecords,
            List<UserRecord> userRecords,
            List<RecipeRecord> recipeRecords) {

        long start = System.currentTimeMillis();
        log.info("Starting data import...");

        // 1. 清理旧数据
        drop();

        // 2. 导入 Users
        if (userRecords != null && !userRecords.isEmpty()) {
            importUsers(userRecords);
            // 导入用户关注关系 (User Follows)
            importUserFollows(userRecords);
        }

        // 3. 导入 Recipes (包含 Ingredients, Nutrition, Category)
        if (recipeRecords != null && !recipeRecords.isEmpty()) {
            // 先导入字典表 Ingredients
            importIngredients(recipeRecords);
            // 导入主表 Recipes
            importRecipes(recipeRecords);
            // 导入 Nutrition (1:1)
            importNutrition(recipeRecords);
            // 导入 Recipe-Ingredient 关联 (M:N)
            importRecipeIngredients(recipeRecords);
        }

        // 4. 导入 Reviews (包含 Likes)
        if (reviewRecords != null && !reviewRecords.isEmpty()) {
            importReviews(reviewRecords);
            // 导入 Review Likes
            importReviewLikes(reviewRecords);
        }

        long end = System.currentTimeMillis();
        log.info("Data import finished in {} ms", (end - start));
    }

    // ----------------------------------------------------------------------
    //                           Users Module
    // ----------------------------------------------------------------------

    private void importUsers(List<UserRecord> users) {
        String sql = "INSERT INTO users (author_id, author_name, password, gender, age, role_id) VALUES (?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, users, 1000, (ps, user) -> {
            ps.setLong(1, user.getAuthorId()); // 修正：UserRecord 用的是 authorId
            ps.setString(2, user.getAuthorName());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getGender());

            // DTO里 age 是 int，如果数据里没有 null 的情况，直接设值
            // 如果 age 可能为 -1 代表空，可以加判断
            ps.setInt(5, user.getAge());

            // 默认设置为 USER 角色 (假设 ID 1 是 USER)
            ps.setInt(6, 1);
        });
        log.info("Imported {} users.", users.size());
    }

    private void importUserFollows(List<UserRecord> users) {
        // 需要将 "User -> Following[]" 扁平化为 "(Follower, Following)" 对
        // 这是一个内存换速度的操作
        List<long[]> relations = new ArrayList<>();

        for (UserRecord user : users) {
            long followerId = user.getAuthorId();
            if (user.getFollowingUsers() != null) {
                for (long followingId : user.getFollowingUsers()) {
                    relations.add(new long[]{followerId, followingId});
                }
            }
        }

        String sql = "INSERT INTO user_follows (follower_id, following_id) VALUES (?, ?)";
        jdbcTemplate.batchUpdate(sql, relations, 1000, (ps, relation) -> {
            ps.setLong(1, relation[0]);
            ps.setLong(2, relation[1]);
        });
        log.info("Imported {} user follow relations.", relations.size());
    }

    // ----------------------------------------------------------------------
    //                           Recipes Module
    // ----------------------------------------------------------------------

    private void importIngredients(List<RecipeRecord> recipes) {
        // 提取去重后的食材名
        Set<String> uniqueIngredients = new HashSet<>();
        for (RecipeRecord r : recipes) {
            if (r.getRecipeIngredientParts() != null) {
                Collections.addAll(uniqueIngredients, r.getRecipeIngredientParts());
            }
        }

        List<String> ingredientList = new ArrayList<>(uniqueIngredients);
        String sql = "INSERT INTO ingredients (ingredient_name) VALUES (?)";

        // 使用 JDBC Batch 插入
        jdbcTemplate.batchUpdate(sql, ingredientList, 1000, (ps, name) -> {
            ps.setString(1, name);
        });
        log.info("Imported {} unique ingredients.", ingredientList.size());
    }

    private void importRecipes(List<RecipeRecord> recipes) {
        // 注意：字段名需与你数据库完全一致
        String sql = "INSERT INTO recipes (recipe_id, author_id, recipe_name, cook_time, prep_time, total_time, date_published, description, recipe_category) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, recipes, 1000, (ps, r) -> {
            ps.setLong(1, r.getRecipeId()); // Lombok 生成的 getter
            ps.setLong(2, r.getAuthorId());
            ps.setString(3, r.getName());
            ps.setString(4, r.getCookTime());
            ps.setString(5, r.getPrepTime());
            ps.setString(6, r.getTotalTime());
            ps.setTimestamp(7, r.getDatePublished()); // DTO 里已经是 Timestamp 了
            ps.setString(8, r.getDescription());
            ps.setString(9, r.getRecipeCategory());
        });
        log.info("Imported {} recipes.", recipes.size());
    }

    private void importNutrition(List<RecipeRecord> recipes) {
        String sql = "INSERT INTO nutrition (recipe_id, calories, fat_content, saturated_fat_content, cholesterol_content, sodium_content, carbohydrate_content, fiber_content, sugar_content, protein_content) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, recipes, 1000, (ps, r) -> {
            ps.setLong(1, r.getRecipeId());
            // DTO 中是直接平铺的字段
            ps.setFloat(2, r.getCalories());
            ps.setFloat(3, r.getFatContent());
            ps.setFloat(4, r.getSaturatedFatContent());
            ps.setFloat(5, r.getCholesterolContent());
            ps.setFloat(6, r.getSodiumContent());
            ps.setFloat(7, r.getCarbohydrateContent());
            ps.setFloat(8, r.getFiberContent());
            ps.setFloat(9, r.getSugarContent());
            ps.setFloat(10, r.getProteinContent());
        });
    }

    private void importRecipeIngredients(List<RecipeRecord> recipes) {
        // 扁平化处理：RecipeID <-> IngredientName
        List<Object[]> relations = new ArrayList<>();

        for (RecipeRecord r : recipes) {
            if (r.getRecipeIngredientParts() != null) {
                for (String ingredientName : r.getRecipeIngredientParts()) {
                    relations.add(new Object[]{r.getRecipeId(), ingredientName});
                }
            }
        }

        String sql = "INSERT INTO recipe_ingredients (recipe_id, ingredient_name) VALUES (?, ?)";
        jdbcTemplate.batchUpdate(sql, relations, 1000, (ps, rel) -> {
            ps.setLong(1, (Long) rel[0]);
            ps.setString(2, (String) rel[1]);
        });
    }

    // ----------------------------------------------------------------------
    //                           Reviews Module
    // ----------------------------------------------------------------------

    private void importReviews(List<ReviewRecord> reviews) {
        String sql = "INSERT INTO reviews (review_id, recipe_id, author_id, rating, review_text, date_submitted, date_modified) VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, reviews, 1000, (ps, r) -> {
            ps.setLong(1, r.getReviewId());
            ps.setLong(2, r.getRecipeId());
            ps.setLong(3, r.getAuthorId());
            ps.setInt(4, (int) r.getRating()); // DTO是float，DB通常是int，强转一下
            ps.setString(5, r.getReview());    // 修正：ReviewRecord 里叫 getReview()
            ps.setTimestamp(6, r.getDateSubmitted());
            ps.setTimestamp(7, r.getDateModified());
        });
        log.info("Imported {} reviews.", reviews.size());
    }

    private void importReviewLikes(List<ReviewRecord> reviews) {
        // 扁平化处理：ReviewID <-> UserID (点赞人)
        List<long[]> likes = new ArrayList<>();

        for (ReviewRecord r : reviews) {
            if (r.getLikes() != null) {
                for (long userId : r.getLikes()) {
                    likes.add(new long[]{r.getReviewId(), userId});
                }
            }
        }

        String sql = "INSERT INTO review_likes (review_id, author_id) VALUES (?, ?)";
        jdbcTemplate.batchUpdate(sql, likes, 1000, (ps, like) -> {
            ps.setLong(1, like[0]);
            ps.setLong(2, like[1]);
        });
    }

    @Override
    public Integer sum(int a, int b) {
        String sql = "SELECT ? + ?";
        try {
            return jdbcTemplate.queryForObject(sql, Integer.class, a, b);
        } catch (Exception e) {
            log.error("Sum error", e);
            return null;
        }
    }
}