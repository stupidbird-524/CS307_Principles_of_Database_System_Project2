package io.sustc.service.impl;

import io.sustc.dto.AuthInfo;
import io.sustc.dto.PageResult;
import io.sustc.dto.RecipeRecord;
import io.sustc.service.RecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RecipeServiceImpl implements RecipeService {

    @Autowired
    private DataSource dataSource;

    @Override
    public String getNameFromID(long id) {
        String sql = "SELECT name FROM recipes WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getString("name");
            }
        } catch (SQLException e) {
            log.error("Error getting name", e);
        }
        return null;
    }

    @Override
    public RecipeRecord getRecipeById(long recipeId) {
        String sql = "SELECT r.*, u.name as author_name, n.* " +
                "FROM recipes r " +
                "JOIN users u ON r.owner_id = u.id " +
                "LEFT JOIN nutrition n ON r.id = n.recipe_id " +
                "WHERE r.id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, recipeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRecipe(conn, rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public PageResult<RecipeRecord> searchRecipes(String keyword, String category, Double minRating, Integer page, Integer size, String sort) {
        StringBuilder sql = new StringBuilder(
                "SELECT r.*, u.name as author_name, n.* " +
                        "FROM recipes r " +
                        "JOIN users u ON r.owner_id = u.id " +
                        "LEFT JOIN nutrition n ON r.id = n.recipe_id " +
                        "WHERE 1=1 "
        );

        List<Object> args = new ArrayList<>();

        if (keyword != null && !keyword.isEmpty()) {
            sql.append("AND (r.name ILIKE ? OR r.description ILIKE ?) ");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }
        if (category != null && !category.isEmpty()) {
            sql.append("AND r.category = ? ");
            args.add(category);
        }
        if (minRating != null) {
            sql.append("AND r.aggregated_rating >= ? ");
            args.add(minRating);
        }

        long total = 0;
        String countSql = "SELECT COUNT(*) " + sql.substring(sql.indexOf("FROM"));

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(countSql)) {
            for (int i = 0; i < args.size(); i++) stmt.setObject(i + 1, args.get(i));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) total = rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (sort != null) {
            switch (sort) {
                case "rating_desc": sql.append("ORDER BY r.aggregated_rating DESC NULLS LAST, r.id ASC "); break;
                case "date_desc": sql.append("ORDER BY r.create_time DESC, r.id ASC "); break;
                case "calories_asc": sql.append("ORDER BY r.calories ASC NULLS LAST, r.id ASC "); break;
                default: sql.append("ORDER BY r.id ASC ");
            }
        } else {
            sql.append("ORDER BY r.id ASC ");
        }

        sql.append("LIMIT ? OFFSET ?");
        args.add(size);
        args.add((page - 1) * size);

        List<RecipeRecord> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) stmt.setObject(i + 1, args.get(i));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToRecipe(conn, rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // 【核心修复点】：这里传入 items, page, size, total 四个参数
        return new PageResult<>(list, page, size, total);
    }

    @Override
    public long createRecipe(RecipeRecord dto, AuthInfo auth) {
        long userId = authenticate(auth);

        String sqlRecipe = "INSERT INTO recipes (owner_id, name, description, category, cook_time, prep_time, create_time, difficulty, calories) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), 1, ?) RETURNING id";

        String sqlNutrition = "INSERT INTO nutrition (recipe_id, calories, fat, sugar, protein, carbohydrates) VALUES (?, ?, ?, ?, ?, ?)";

        String sqlIngred = "INSERT INTO recipe_ingredients (recipe_id, ingredient_name, amount) VALUES (?, ?, '1 unit')";

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long recipeId;
                try (PreparedStatement stmt = conn.prepareStatement(sqlRecipe)) {
                    stmt.setLong(1, userId);
                    stmt.setString(2, dto.getName());
                    stmt.setString(3, dto.getDescription());
                    stmt.setString(4, dto.getRecipeCategory());
                    stmt.setString(5, dto.getCookTime());
                    stmt.setString(6, dto.getPrepTime());
                    stmt.setFloat(7, dto.getCalories());

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) recipeId = rs.getLong(1);
                        else throw new SQLException("Create failed");
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(sqlNutrition)) {
                    stmt.setLong(1, recipeId);
                    stmt.setFloat(2, dto.getCalories());
                    stmt.setFloat(3, dto.getFatContent());
                    stmt.setFloat(4, dto.getSugarContent());
                    stmt.setFloat(5, dto.getProteinContent());
                    stmt.setFloat(6, dto.getCarbohydrateContent());
                    stmt.executeUpdate();
                }

                if (dto.getRecipeIngredientParts() != null) {
                    try (PreparedStatement stmt = conn.prepareStatement(sqlIngred)) {
                        for (String ing : dto.getRecipeIngredientParts()) {
                            stmt.setLong(1, recipeId);
                            stmt.setString(2, ing);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                    }
                }

                conn.commit();
                return recipeId;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteRecipe(long recipeId, AuthInfo auth) {
        long userId = authenticate(auth);
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT owner_id FROM recipes WHERE id = ?")) {
                    stmt.setLong(1, recipeId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) return;
                        if (rs.getLong(1) != userId) throw new SecurityException("Not owner");
                    }
                }

                execute(conn, "DELETE FROM review_likes WHERE review_id IN (SELECT id FROM reviews WHERE recipe_id = ?)", recipeId);
                execute(conn, "DELETE FROM reviews WHERE recipe_id = ?", recipeId);
                execute(conn, "DELETE FROM recipe_ingredients WHERE recipe_id = ?", recipeId);
                execute(conn, "DELETE FROM nutrition WHERE recipe_id = ?", recipeId);
                execute(conn, "DELETE FROM recipes WHERE id = ?", recipeId);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateTimes(AuthInfo auth, long recipeId, String cookTimeIso, String prepTimeIso) {
        long userId = authenticate(auth);
        try {
            if (cookTimeIso != null) Duration.parse(cookTimeIso);
            if (prepTimeIso != null) Duration.parse(prepTimeIso);
        } catch (Exception e) { throw new IllegalArgumentException("Invalid ISO format"); }

        String sql = "UPDATE recipes SET cook_time = COALESCE(?, cook_time), prep_time = COALESCE(?, prep_time) WHERE id = ? AND owner_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cookTimeIso);
            stmt.setString(2, prepTimeIso);
            stmt.setLong(3, recipeId);
            stmt.setLong(4, userId);
            if (stmt.executeUpdate() == 0) {
                throw new SecurityException("Update failed: Not owner or recipe not found");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> getClosestCaloriePair() {
        String sql = "SELECT n1.recipe_id as id1, n2.recipe_id as id2, n1.calories as cal1, n2.calories as cal2, " +
                "ABS(n1.calories - n2.calories) as diff " +
                "FROM nutrition n1 JOIN nutrition n2 ON n1.recipe_id < n2.recipe_id " +
                "ORDER BY diff ASC, id1 ASC, id2 ASC LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("RecipeA", rs.getLong("id1"));
                map.put("RecipeB", rs.getLong("id2"));
                map.put("CaloriesA", rs.getDouble("cal1"));
                map.put("CaloriesB", rs.getDouble("cal2"));
                map.put("Difference", rs.getDouble("diff"));
                return map;
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return null;
    }

    @Override
    public List<Map<String, Object>> getTop3MostComplexRecipesByIngredients() {
        String sql = "SELECT r.id, r.name, COUNT(*) as cnt " +
                "FROM recipes r JOIN recipe_ingredients ri ON r.id = ri.recipe_id " +
                "GROUP BY r.id, r.name " +
                "ORDER BY cnt DESC, r.id ASC LIMIT 3";
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("RecipeId", rs.getLong("id"));
                map.put("Name", rs.getString("name"));
                map.put("IngredientCount", rs.getInt("cnt"));
                list.add(map);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    private RecipeRecord mapResultSetToRecipe(Connection conn, ResultSet rs) throws SQLException {
        RecipeRecord r = new RecipeRecord();
        long recipeId = rs.getLong("id");

        r.setRecipeId(recipeId);
        r.setAuthorId(rs.getLong("owner_id"));
        r.setName(rs.getString("name"));
        r.setDescription(rs.getString("description"));
        r.setRecipeCategory(rs.getString("category"));
        r.setCookTime(rs.getString("cook_time"));
        r.setPrepTime(rs.getString("prep_time"));
        r.setDatePublished(rs.getTimestamp("create_time"));
        r.setReviewCount(rs.getInt("review_count"));
        r.setAggregatedRating(rs.getFloat("aggregated_rating"));
        r.setCalories(rs.getFloat("calories"));

        try { r.setAuthorName(rs.getString("author_name")); } catch (Exception e) { r.setAuthorName("Unknown"); }

        try {
            r.setFatContent(rs.getFloat("fat"));
            r.setSugarContent(rs.getFloat("sugar"));
            r.setProteinContent(rs.getFloat("protein"));
            r.setCarbohydrateContent(rs.getFloat("carbohydrates"));
        } catch (Exception e) {}

        r.setTotalTime(calculateTotalTime(r.getCookTime(), r.getPrepTime()));
        r.setRecipeIngredientParts(fetchIngredients(conn, recipeId));

        return r;
    }

    private String[] fetchIngredients(Connection conn, long recipeId) {
        String sql = "SELECT ingredient_name FROM recipe_ingredients WHERE recipe_id = ? ORDER BY ingredient_name";
        List<String> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, recipeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {}
        return list.toArray(new String[0]);
    }

    private String calculateTotalTime(String cook, String prep) {
        try {
            Duration d1 = (cook != null) ? Duration.parse(cook) : Duration.ZERO;
            Duration d2 = (prep != null) ? Duration.parse(prep) : Duration.ZERO;
            return d1.plus(d2).toString();
        } catch (Exception e) { return null; }
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

    private void execute(Connection conn, String sql, long id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }
}