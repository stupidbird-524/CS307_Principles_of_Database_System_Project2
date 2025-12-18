import io.sustc.dto.*;
import io.sustc.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Statement;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }


    @Override
    public long register(String username, String password) {
        // 1. 构造 DTO 对象
        RegisterUserReq req = new RegisterUserReq();

        // 2. 映射字段：接口的 username 对应 DTO 的 name
        req.setName(username);
        req.setPassword(password);

        // 3. 设置默认值
        req.setGender(RegisterUserReq.Gender.UNKNOWN); // 使用类内部定义的枚举
        req.setBirthday(null); // 基础注册没有生日信息

        // 4. 调用主逻辑
        return this.register(req);
    }

    // ==========================================
    // 方法 2：全量注册 (RegisterUserReq)
    // ==========================================
    @Override
    public long register(RegisterUserReq req) {
        // 1. 校验必填项 (Name 和 Password)
        if (req == null || !StringUtils.hasText(req.getName()) || !StringUtils.hasText(req.getPassword())) {
            log.warn("注册失败：参数不完整");
            throw new IllegalArgumentException("用户名和密码不能为空");
        }

        // 2. 密码加密
        String hashedPassword = passwordEncoder.encode(req.getPassword());

        // 3. 处理 Gender (枚举转字符串)
        String genderStr;
        if (req.getGender() == null) {
            genderStr = "Unknown"; // 默认值
        } else {
            // 将 MALE/FEMALE 转换成字符串
            genderStr = req.getGender().name();
        }

        // 4. ★★★ 处理 Age (核心逻辑：从 birthday 字符串计算年龄)
        int ageVal = calculateAgeFromBirthday(req.getBirthday());

        // 5. SQL 插入
        // 注意：DTO 的 name -> 数据库的 AuthorName
        String sql = "INSERT INTO \"User\" (\"AuthorName\", \"HashedPassword\", \"Gender\", \"Age\") VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, req.getName()); // 取 Name
                ps.setString(2, hashedPassword);
                ps.setString(3, genderStr);
                ps.setInt(4, ageVal); // 存计算后的 Age
                return ps;
            }, keyHolder);

            long newAuthorId = keyHolder.getKey().longValue();

            // 6. 分配默认角色
            assignDefaultRole(newAuthorId, "RegisteredUser");

            log.info("用户注册成功: {} (ID: {})", req.getName(), newAuthorId);
            return newAuthorId;

        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.error("注册失败：用户名 '{}' 已存在", req.getName());
            throw new RuntimeException("用户名已存在");
        } catch (Exception e) {
            log.error("注册异常", e);
            throw new RuntimeException("系统错误");
        }
    }


    private int calculateAgeFromBirthday(String birthdayStr) {
        if (!StringUtils.hasText(birthdayStr)) {
            return 0;
        }
        try {
            // 尝试解析各种常见格式，这里假设是 "yyyy-MM-dd" 或 "MM月dd日" 等
            // 项目通常会约定格式，这里以标准格式 "yyyy-MM-dd" 为例
            // 如果您的测试用例格式不同，请修改 pattern

            // 简单的处理逻辑：只尝试解析 yyyy-MM-dd
            LocalDate birthDate;
            if (birthdayStr.contains("-")) {
                birthDate = LocalDate.parse(birthdayStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            } else {
                // 如果格式太复杂，为了不报错，直接返回 0 或者只解析年份
                return 0;
            }

            // 计算年龄
            return java.time.Period.between(birthDate, LocalDate.now()).getYears();

        } catch (DateTimeParseException | NullPointerException e) {
            log.warn("生日格式解析失败: {}，年龄将默认为 0", birthdayStr);
            return 0;
        }
    }


    private void assignDefaultRole(long userId, String roleName) {
        String selectRoleSql = "SELECT \"RoleID\" FROM \"Role\" WHERE \"RoleName\" = ?";
        try {
            Long roleId = jdbcTemplate.queryForObject(selectRoleSql, Long.class, roleName);
            if (roleId != null) {
                jdbcTemplate.update("INSERT INTO \"UserAssignedRole\" (\"UserID\", \"RoleID\") VALUES (?, ?)", userId, roleId);
            }
        } catch (Exception e) {
            log.error("角色分配失败", e);
        }
    }


    @Override
    public long login(AuthInfo auth) {
        // 1. 基础校验
        // 注意：long 类型的默认值是 0，所以要判断 id > 0
        if (auth == null || auth.getAuthorId() <= 0 || !StringUtils.hasText(auth.getPassword())) {
            throw new IllegalArgumentException("ID 或密码无效");
        }

        // 2. 准备 SQL
        // 这次我们根据主键 "AuthorID" 查密码
        String sql = "SELECT \"HashedPassword\" FROM \"User\" WHERE \"AuthorID\" = ?";

        try {
            // 3. 查询数据库
            // queryForObject 用于只查询一列数据 (这里只需要查密码)
            String dbHashedPassword = jdbcTemplate.queryForObject(sql, String.class, auth.getAuthorId());

            // 4. 核心验证：比对密码
            // 参数1：用户输入的明文 (auth.getPassword())
            // 参数2：数据库里的哈希值 (dbHashedPassword)
            if (dbHashedPassword != null && passwordEncoder.matches(auth.getPassword(), dbHashedPassword)) {
                log.info("用户 ID {} 登录成功", auth.getAuthorId());
                return auth.getAuthorId(); // 登录成功，返回用户的 ID
            } else {
                // 密码不匹配
                log.warn("用户 ID {} 登录失败：密码错误", auth.getAuthorId());
                throw new RuntimeException("ID 或密码错误");
            }

        } catch (EmptyResultDataAccessException e) {
            // 5. 处理 ID 不存在的情况
            log.warn("登录失败：ID {} 不存在", auth.getAuthorId());
            throw new RuntimeException("ID 或密码错误");
        } catch (Exception e) {
            log.error("登录异常", e);
            throw new RuntimeException("系统内部错误");
        }
    }

    @Override
    public boolean deleteAccount(AuthInfo auth, long userId) {
// 1. 基础参数校验
        if (auth == null || auth.getAuthorId() <= 0 || !StringUtils.hasText(auth.getPassword())) {
            return false;
        }

        try {
            // ==========================================
            // 步骤 1: 验证操作者 (Requester) 的身份
            // ==========================================
            String pwdSql = "SELECT \"HashedPassword\" FROM \"User\" WHERE \"AuthorID\" = ?";
            // 查出操作者的密码
            String dbHashedPassword = jdbcTemplate.queryForObject(pwdSql, String.class, auth.getAuthorId());

            // 验证密码是否正确
            if (dbHashedPassword == null || !passwordEncoder.matches(auth.getPassword(), dbHashedPassword)) {
                return false; // 密码错误，拒绝操作
            }

            // ==========================================
            // 步骤 2: 验证权限 (Authorization)
            // ==========================================
            boolean canDelete = false;

            if (auth.getAuthorId() == userId) {
                // 情况 A: 自己删自己 -> 允许
                canDelete = true;
            } else {
                // 情况 B: 删别人 -> 检查操作者是否是管理员
                // 注意：这里需要关联查询 UserAssignedRole 和 Role 表
                String roleSql = "SELECT COUNT(*) FROM \"UserAssignedRole\" uar " +
                        "JOIN \"Role\" r ON uar.\"RoleID\" = r.\"RoleID\" " +
                        "WHERE uar.\"UserID\" = ? AND r.\"RoleName\" = 'Administrator'";

                Integer count = jdbcTemplate.queryForObject(roleSql, Integer.class, auth.getAuthorId());
                if (count != null && count > 0) {
                    canDelete = true; // 是管理员 -> 允许
                }
            }

            // 如果没有权限，直接返回 false
            if (!canDelete) {
                return false;
            }

            // ==========================================
            // 步骤 3: 执行删除
            // ==========================================
            String deleteSql = "DELETE FROM \"User\" WHERE \"AuthorID\" = ?";
            int rowsAffected = jdbcTemplate.update(deleteSql, userId);

            // 如果 rowsAffected > 0，说明删除成功；如果为 0，说明目标用户本来就不存在
            return rowsAffected > 0;

        } catch (EmptyResultDataAccessException e) {
            // 操作者 ID 不存在
            return false;
        } catch (Exception e) {
            log.error("删除用户异常", e);
            return false;
        }    }

    @Override
    public boolean follow(AuthInfo auth, long followeeId) {
        // 1. 基础校验
        if (auth == null || auth.getAuthorId() <= 0 || !StringUtils.hasText(auth.getPassword())) {
            return false;
        }

        // 2. 不能关注自己
        if (auth.getAuthorId() == followeeId) {
            return false; // 或者抛出异常，看具体要求
        }

        try {
            // ==========================================
            // 步骤 3: 验证操作者身份 (Authentication)
            // ==========================================
            String pwdSql = "SELECT \"HashedPassword\" FROM \"User\" WHERE \"AuthorID\" = ?";
            String dbHashedPassword = jdbcTemplate.queryForObject(pwdSql, String.class, auth.getAuthorId());

            if (dbHashedPassword == null || !passwordEncoder.matches(auth.getPassword(), dbHashedPassword)) {
                return false; // 密码错误
            }

            // ==========================================
            // 步骤 4: 验证目标用户是否存在
            // ==========================================
            String checkUserSql = "SELECT COUNT(*) FROM \"User\" WHERE \"AuthorID\" = ?";
            Integer userCount = jdbcTemplate.queryForObject(checkUserSql, Integer.class, followeeId);
            if (userCount == null || userCount == 0) {
                return false; // 目标用户不存在
            }

            // ==========================================
            // 步骤 5: 检查当前关注状态 (是关注还是取关？)
            // ==========================================
            String checkFollowSql = "SELECT COUNT(*) FROM \"Follow\" WHERE \"FollowerID\" = ? AND \"FollowingID\" = ?";
            Integer followCount = jdbcTemplate.queryForObject(checkFollowSql, Integer.class, auth.getAuthorId(), followeeId);

            boolean isFollowing = (followCount != null && followCount > 0);

            if (isFollowing) {
                // 情况 A: 已经关注了 -> 执行【取关】逻辑

                // 1. 删除关联
                jdbcTemplate.update("DELETE FROM \"Follow\" WHERE \"FollowerID\" = ? AND \"FollowingID\" = ?", auth.getAuthorId(), followeeId);

                // 2. 我 (Follower) 的关注数 -1
                jdbcTemplate.update("UPDATE \"User\" SET \"Following\" = \"Following\" - 1 WHERE \"AuthorID\" = ?", auth.getAuthorId());

                // 3. 他 (Followee) 的粉丝数 -1
                jdbcTemplate.update("UPDATE \"User\" SET \"Followers\" = \"Followers\" - 1 WHERE \"AuthorID\" = ?", followeeId);

                log.info("用户 {} 取消关注了 {}", auth.getAuthorId(), followeeId);
                return true;

            } else {
                // 情况 B: 还没关注 -> 执行【关注】逻辑

                // 1. 插入关联
                jdbcTemplate.update("INSERT INTO \"Follow\" (\"FollowerID\", \"FollowingID\") VALUES (?, ?)", auth.getAuthorId(), followeeId);

                // 2. 我 (Follower) 的关注数 +1
                jdbcTemplate.update("UPDATE \"User\" SET \"Following\" = \"Following\" + 1 WHERE \"AuthorID\" = ?", auth.getAuthorId());

                // 3. 他 (Followee) 的粉丝数 +1
                jdbcTemplate.update("UPDATE \"User\" SET \"Followers\" = \"Followers\" + 1 WHERE \"AuthorID\" = ?", followeeId);

                log.info("用户 {} 关注了 {}", auth.getAuthorId(), followeeId);
                return true;
            }

        } catch (EmptyResultDataAccessException e) {
            return false;
        } catch (Exception e) {
            log.error("关注操作异常", e);
            return false;
        }
    }


    @Override
    public UserRecord getById(long userId) {
        if (userId <= 0) {
            return null;
        }

        String sql = "SELECT * FROM \"User\" WHERE \"AuthorID\" = ?";

        try {
            // 2. 查询主表并映射
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                UserRecord record = new UserRecord();

                record.setAuthorId(rs.getLong("AuthorID"));
                record.setAuthorName(rs.getString("AuthorName"));
                record.setGender(rs.getString("Gender"));
                record.setAge(rs.getInt("Age"));
                record.setPassword(rs.getString("HashedPassword"));
                record.setFollowers(rs.getInt("Followers"));
                record.setFollowing(rs.getInt("Following"));
                record.setDeleted(false); // 能查到说明未删除

                // 3. 填充数组字段 (调用下方的辅助方法)
                record.setFollowerUsers(getFollowerIds(userId));
                record.setFollowingUsers(getFollowingIds(userId));

                return record;
            }, userId);

        } catch (EmptyResultDataAccessException e) {
            return null; // 用户不存在
        }
    }

    /**
     * 辅助方法：获取粉丝ID数组
     */
    private long[] getFollowerIds(long userId) {
        String sql = "SELECT \"FollowerID\" FROM \"Follow\" WHERE \"FollowingID\" = ?";
        List<Long> list = jdbcTemplate.queryForList(sql, Long.class, userId);
        return list.stream().mapToLong(Long::longValue).toArray();
    }

    /**
     * 辅助方法：获取关注ID数组
     */
    private long[] getFollowingIds(long userId) {
        String sql = "SELECT \"FollowingID\" FROM \"Follow\" WHERE \"FollowerID\" = ?";
        List<Long> list = jdbcTemplate.queryForList(sql, Long.class, userId);
        return list.stream().mapToLong(Long::longValue).toArray();
    }

    @Override
    public void updateProfile(AuthInfo auth, String gender, Integer age) {
        // 1. 基础校验
        if (auth == null || auth.getAuthorId() <= 0 || !StringUtils.hasText(auth.getPassword())) {
            throw new IllegalArgumentException("参数错误");
        }

        // 2. 验证身份 (这一步是所有修改操作的标配)
        try {
            String pwdSql = "SELECT \"HashedPassword\" FROM \"User\" WHERE \"AuthorID\" = ?";
            String dbHash = jdbcTemplate.queryForObject(pwdSql, String.class, auth.getAuthorId());

            if (dbHash == null || !passwordEncoder.matches(auth.getPassword(), dbHash)) {
                throw new RuntimeException("密码错误");
            }
        } catch (EmptyResultDataAccessException e) {
            throw new RuntimeException("用户不存在");
        }

        // 3. 动态构建 SQL (关键逻辑)
        // 我们只更新那些非空的值

        // 用于存放 SQL 参数
        java.util.List<Object> params = new java.util.ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("UPDATE \"User\" SET ");
        boolean needUpdate = false;

        // --- 处理 Gender ---
        if (gender != null && !gender.isEmpty()) {
            sqlBuilder.append("\"Gender\" = ?");
            params.add(gender);
            needUpdate = true;
        }

        // --- 处理 Age ---
        if (age != null) {
            // 如果前面已经拼接了 gender，这里需要加一个逗号
            if (needUpdate) {
                sqlBuilder.append(", ");
            }
            sqlBuilder.append("\"Age\" = ?");
            params.add(age);
            needUpdate = true;
        }

        // 如果两个参数都是 null，说明没什么好更新的，直接结束
        if (!needUpdate) {
            return;
        }

        // 4. 加上 WHERE 条件
        sqlBuilder.append(" WHERE \"AuthorID\" = ?");
        params.add(auth.getAuthorId());

        // 5. 执行更新
        // params.toArray() 会把 List 转成 Object[] 传给 JDBC
        jdbcTemplate.update(sqlBuilder.toString(), params.toArray());

        log.info("用户 {} 更新了个人资料", auth.getAuthorId());
    }

    @Override
    public PageResult<FeedItem> feed(AuthInfo auth, int page, int size, String category) {
// 1. 基础校验
        if (auth == null || auth.getAuthorId() <= 0) {
            return null;
        }

        // 2. 修正分页参数
        if (page <= 0) page = 1;
        if (size <= 0) size = 10;
        int offset = (page - 1) * size;

        // 3. 准备 SQL 构建器
        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM \"Recipes\" r");
        // 联查 User 表获取作者名
        StringBuilder querySql = new StringBuilder("SELECT r.*, u.\"AuthorName\" FROM \"Recipes\" r JOIN \"User\" u ON r.\"AuthorID\" = u.\"AuthorID\"");

        List<Object> args = new ArrayList<>();

        // 4. 动态拼接筛选条件 (Category)
        if (category != null && !category.isEmpty()) {
            String joinPart = " JOIN \"RecipeCategory\" rc ON r.\"RecipeID\" = rc.\"RecipeID\" " +
                    " JOIN \"Category\" c ON rc.\"CategoryID\" = c.\"CategoryID\" ";
            String wherePart = " WHERE c.\"Name\" = ? ";

            countSql.append(joinPart).append(wherePart);
            querySql.append(joinPart).append(wherePart);

            args.add(category);
        }

        try {
            // 5. 查总数 (Total)
            Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, args.toArray());
            if (total == null || total == 0) {
                return new PageResult<>(new ArrayList<>(), page, size, 0L);
            }

            // 6. 查列表 (Items)
            // 排序：按发布时间倒序 (最新的在最前)
            querySql.append(" ORDER BY r.\"TimeCreated\" DESC LIMIT ? OFFSET ?");
            args.add(size);
            args.add(offset);

            List<FeedItem> items = jdbcTemplate.query(querySql.toString(), (rs, rowNum) -> {
                // 使用 Builder 构建，代码更清晰
                return FeedItem.builder()
                        .recipeId(rs.getLong("RecipeID"))
                        .name(rs.getString("Title"))             // DB: Title -> DTO: name
                        .authorId(rs.getLong("AuthorID"))
                        .authorName(rs.getString("AuthorName"))  // 联查得到的作者名
                        .datePublished(rs.getTimestamp("TimeCreated").toInstant()) // Timestamp -> Instant
                        .aggregatedRating(rs.getDouble("Rating")) // DB: Rating
                        // 假设 DB 有 ReviewCount 字段，如果没有，可能需要额外子查询或设为 0
                        // .reviewCount(rs.getInt("ReviewCount"))
                        .build();
            }, args.toArray());

            // 7. 返回结果
            return new PageResult<>(items, page, size, total);

        } catch (Exception e) {
            log.error("Feed flow error", e);
            return null;
        }
    }

    @Override
    public Map<String, Object> getUserWithHighestFollowRatio() {
        try {
            // 1. 编写 SQL
            // 技巧说明：
            // - CAST("Followers" AS NUMERIC): 强制转为小数，保证除法结果有小数位
            // - WHERE "Following" > 0: 排除分母为0的情况，避免报错
            // - ORDER BY ratio DESC: 按比率从大到小排
            // - LIMIT 1: 只要第一名
            String sql = "SELECT \"AuthorName\" AS name, " +
                    "CAST(\"Followers\" AS NUMERIC) / \"Following\" AS cnt " +
                    "FROM \"User\" " +
                    "WHERE \"Following\" > 0 " +
                    "ORDER BY cnt DESC LIMIT 1";

            // 2. 执行查询
            // queryForMap 刚好返回 Map<String, Object>，符合接口要求
            // 结果 Map 里会有两个 key: "name" 和 "cnt"
            return jdbcTemplate.queryForMap(sql);

        } catch (EmptyResultDataAccessException e) {
            // 如果表里没人，或者所有人 Following 都是 0
            return null;
        } catch (Exception e) {
            log.error("查询最高关注比用户失败", e);
            return null;
        }
    }

}
