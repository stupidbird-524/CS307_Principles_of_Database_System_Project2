## 成员与分工
**12410724 谢宏波：**
- E-R图绘制


**12410810 胡雨承：**



## Task 1: E-R Diagram
本小组使用**drawio**绘制工具，绘制本小组的E-R图，其截图如下：
![Task 1 的 ER 图](https://github.com/stupidbird-524/CS307_Principles_of_Database_System_Project2/blob/main/proj2ER.drawio.png?raw=true)

## Task 2: Database Design
本项目使用 ```DDL.sql``` 文件创建数据表，使用 ```PostgreSQL```的 ```DDL``` 语法编写。
#### 数据库设计
使用 ```DataGrip``` 创建数据表并全选后通过右键 ```Diagram > Show Diagram``` 显示如下数据表设计及关系。
![Task 2 的数据库结构图](https://github.com/Kranceeee/CS307_Principles_of_Database_System_Project1/blob/main/image/%E6%95%B0%E6%8D%AE%E5%BA%93%E7%BB%93%E6%9E%84%E5%9B%BE%E6%96%B0.png?raw=true)
### 数据库结构设计说明


###  数据表及其各列含义说明 (共 13 个表)

本项目共创建了 **13 个数据表**，用于构建食谱应用的数据模型。

#### 1. 核心实体与属性表 (6 个表)

| 数据表 | 存储内容 | 关键字段 (主键 PK / 外键 FK) | 字段含义说明 |
| :--- | :--- | :--- | :--- |
| **`users`** | 存储所有注册用户信息。 | `userid` (PK) | 用户唯一标识符。 |
| | | `username`, `email` | 用户名和邮箱。 |
| **`recipes`** | 存储所有菜谱信息。 | `recipeid` (PK) | 菜谱唯一标识符。 |
| | | `authoruserid` (FK) | 菜谱创建者，关联到 `users` 表。 |
| | | `name`, `instructions` | 菜谱名称和烹饪步骤。 |
| **`ingredients`** | 存储所有配料的清单。 | `ingredientid` (PK) | 配料唯一标识符。 |
| | | `ingredientname` | 配料名称。 |
| **`categories`** | 存储菜谱分类信息。 | `categoryid` (PK) | 分类唯一标识符。 |
| | | `categoryname` | 分类的名称（如：早餐、甜点）。 |
| **`keywords`** | 存储菜谱的关键词。 | `keywordid` (PK) | 关键词唯一标识符。 |
| | | `keywordname` | 关键词名称（如：健康、素食）。 |
| **`nutrition`** | 存储菜谱的营养成分数据。 | `recipeid` (PK, FK) | 关联到 `recipes` 表（一对一关系）。 |
| | | `calories`, `proteincontent` | 卡路里、蛋白质等 10 个营养字段。 |

#### 2. 关系与互动表 (7 个表)

| 数据表 | 存储内容 | 关键字段 (主键 PK / 外键 FK) | 字段含义说明 |
| :--- | :--- | :--- | :--- |
| **`reviews`** | 存储用户对菜谱的评价和评分。 | `reviewid` (PK) | 评论唯一标识符。 |
| | | `recipeid` (FK), `userid` (FK) | 关联被评论的菜谱和用户。 |
| | | `rating` | 评分（如 1-5 分）。 |
| **`recipeingredients`** | 菜谱所需的配料细节（多对多关系）。 | `recipeid` (PK, FK), `ingredientid` (PK, FK) | 菜谱与配料的复合主键。 |
| | | `quantity`, `unit` | 配料的用量和单位。 |
| **`recipecategories`** | 关联菜谱与分类（多对多关系）。 | `recipeid` (PK, FK), `categoryid` (PK, FK) | 菜谱与分类的复合主键。 |
| **`recipekeywords`** | 关联菜谱与关键词（多对多关系）。 | `recipeid` (PK, FK), `keywordid` (PK, FK) | 菜谱与关键词的复合主键。 |
| **`userfavorites`** | 记录用户收藏了哪些菜谱。 | `userid` (PK, FK), `recipeid` (PK, FK) | 用户 ID 与被收藏菜谱 ID 的复合主键。 |
| **`userfollows`** | 记录用户间的关注关系。 | `followeruserid` (PK, FK), `followinguserid` (PK, FK) | 关注者 ID 与被关注者 ID 的复合主键。 |
| **`reviewlikes`** | 记录用户对评论的点赞操作。 | `reviewid` (PK, FK), `userid` (PK, FK) | 评论 ID 与点赞用户 ID 的复合主键。 |
#### 总结

这个设计是一个非常**全面和规范**的菜谱应用数据库模型。它通过使用多个**关联表**（例如 `userfollows`, `recipecategories`, `recipeingredients` 等）来处理复杂的**多对多**关系，有效避免了数据冗余，并确保了数据之间的完整性和灵活性。

### 数据库结构设计规范性说明

本数据库结构图（ERD）旨在支撑一个美食/菜谱应用，设计严格遵循关系数据库规范，特别是**第三范式（3NF）**，并确保了数据完整性、可扩展性与查询效率。

---

#### 目标与满足的规范

该设计完全满足以下七项设计要求：

| 序号 | 要求 | 满足方式说明 |
| :--- | :--- | :--- |
| **1.** | **满足第三范式（3NF）** | 设计通过将计算字段（如 `users` 表中的 `followers` 和 `following`）移除或标记为可容忍的性能优化缓存（如 `recipes` 表中的 `averagerating`），消除了传递依赖，使非主键列完全依赖于主键。关联表的设计确保了所有非主键属性完全依赖于**复合主键**。 |
| **2.** | **有主键、外键指明关系** | 所有主表（`users`, `recipes` 等）均设置了独立的 `Integer` 型主键（PK）。所有关联表（如 `userfollows`, `recipeingredients`）都使用外键（FK）建立连接，并通过复合主键确保关系的唯一性。 |
| **3.** | **每个表必须被连接（不孤立）** | 所有 11 个表都通过明确的主键和外键关系连接到整个数据模型中。主干为 `users` $\leftrightarrow$ `recipes`，辅以多对多关联表和一对一的 `nutrition` 表，确保数据间的可达性。 |
| **4.** | **外键无环（无循环依赖）** | 数据库中不存在强制性的外键循环依赖。所有的关系流（例如 `recipes` 依赖 `users`，`nutrition` 依赖 `recipes`）都是单向或通过自连接（如 `userfollows`）实现，保证了数据插入和删除的顺序性。 |
| **5.** | **每表至少一个 NOT NULL 列** | 在实际实现中，所有表的主键（PK）、构成关联的关键外键（FK），以及核心业务数据字段（如 `recipes.name`, `users.username`, `reviews.rating`）均应被声明为 `NOT NULL`，确保数据完整性。 |
| **6.** | **使用合适的数据类型** | 使用 `Integer` 或 `SERIAL` 作为 ID；使用 `VARCHAR(N)` 限制短文本长度；使用 `TEXT` 存储长描述和说明；使用 `TIMESTAMP WITH TIME ZONE` 记录准确时间；使用 **`NUMERIC(P, S)`** 存储精确的用量和评分，避免浮点数精度问题。 |
| **7.** | **设计应方便扩展** | 结构采用高度解耦的关联表设计，例如，要添加新的分类维度（如“烹饪难度”），只需创建新表并通过 `recipeid` 关联，不会修改核心实体表，具有极高的灵活性。 |

---

#### 关键表结构分析 (DDL 角度)

| 表名 | 关系类型 | 核心字段示例与规范 | 满足规范点 |
| :--- | :--- | :--- | :--- |
| **`users`** | 核心实体 | `userid` (PK), `username` (NOT NULL, UNIQUE), `email` (NOT NULL, UNIQUE)。 | 3NF, PK/FK, NOT NULL, 扩展性。 |
| **`recipes`** | 核心实体 | `recipeid` (PK), `authoruserid` (FK, NOT NULL), `name` (NOT NULL)。 | 3NF, PK/FK, NOT NULL。 |
| **`recipeingredients`** | 多对多关联表 | `recipeid` (FK, PK), `ingredientid` (FK, PK), `quantity` (NOT NULL, NUMERIC)。 | 3NF, PK/FK, NOT NULL, 数据类型。 |
| **`nutrition`** | 一对一子表 | `recipeid` (PK, FK), `calories` (NOT NULL, NUMERIC)。 | PK/FK, NOT NULL, 无环。 |
| **`userfollows`** | 自连接关联表 | `followeruserid` (FK, PK), `followinguserid` (FK, PK)。 | PK/FK, 无孤立表, 无环。 |

---

**总结：** 整体设计采用了标准化的实体-关系模型，通过细致的表分解和外键约束，保证了数据的高质量和系统的长期维护性。

## Task 3: Database Import
###  食谱数据库 CSV 导入报告

**文档目的：** 本报告详细说明了 `CSVImporter.java` 程序的数据库连接、数据导入流程、运行环境要求，以及数据清洗的机制，并提供导入结果的正式报告模板。

---

### 1. 导入程序概述 (`CSVImporter.java`)

此 Java 程序专为 **PostgreSQL** 数据库设计，旨在实现高效且容错的 CSV 数据批量导入。

### 1.1 核心流程

* **数据库连接：** 使用 `postgresql-jdbc` 驱动连接至目标 PostgreSQL 实例。
* **顺序执行：** 严格遵循外键约束关系，按照**父表优先、子表靠后**的顺序，依次调用 **13 个独立的导入任务**（如 `importUsers`, `importRecipes`）。
* **性能优化：** 采用 **Apache Commons CSV** 进行高效文件解析，并利用 **`PreparedStatement` 批量插入** (批次大小 `BATCH_SIZE = 1000`) 来最大化导入速度。
* **运行时计时：** 记录每个任务的耗时和总导入时长，方便性能分析。
* **容错机制：** 专注于完成所有任务。程序不会因单个 CSV 文件的错误而中断，而是捕获错误并继续执行下一个任务。

### 2. 运行环境与配置要求

#### 2.1 运行条件

| 类别 | 要求 | 说明 |
| :--- | :--- | :--- |
| **Java 环境** | JDK 17 | 确保 Java 编译和运行时环境可用。 |
| **项目依赖** | JDBC 驱动 / Commons CSV | 项目构建文件（Maven/Gradle）必须包含 `org.postgresql:postgresql` 和 `org.apache.commons:commons-csv` 库。 |
| **PostgreSQL** | 服务器与权限 | PostgreSQL 服务必须运行，且配置的 `DB_USER` 需具备目标数据库的 `CONNECT` 和所有 13 个表的 `INSERT` 权限。 |
| **数据库结构** | DDL 脚本 | **必须**先运行最终确定的 DDL 脚本，以确保所有 13 个表（包括正确的 PK/FK 和字段）已创建。 |

#### 2.2 关键配置

在运行前，请务必在 `CSVImporter.java` 文件中修改以下 4 个常量，以匹配本地环境：

| 常量 | 示例值 | 作用 |
| :--- | :--- | :--- |
| `DB_URL` | `"jdbc:postgresql://localhost:5432/postgres"` | 数据库连接字符串。 |
| `DB_USER` | `"postgres"` | 数据库用户名。 |
| `DB_PASSWORD` | `您的数据库密码` | 数据库密码。 |
| `FILE_PATH_PREFIX` | `"D:/Project/processedData/"` | 包含所有 13 个 CSV 文件的目录路径。 |

### 3. 操作步骤与注意事项

#### 3.1 导入步骤

1.  **准备 DDL：** 连接至 PostgreSQL，并**运行最终确定的 DDL 脚本**。
2.  **准备 CSV：** 使用数据分解程序生成所有 13 个 CSV 文件，并确保文件头与表结构（尤其是 `Nutrition.csv` 和 `User_Like_Review.csv`）匹配。
3.  **配置 Java 代码：** 修改上述 4 个配置常量。
4.  **运行程序：** 编译并执行 `CSVImporter.java`。

#### 3.2 关键注意事项

* **容错性与数据丢失：** **程序优先保证任务完成**，而不是严格的数据完整性。由于会跳过**整个失败批次** (1000 条记录)，最终导入记录数可能少于 CSV 总行数。
* **控制台审查：** 必须密切关注控制台输出，尤其是 `[!! 关键 !!]` 警告信息，以确定有多少数据批次因错误（如外键或格式问题）而被跳过。
* **不可重入性：** **程序不可重复运行。** 再次运行将因主键唯一性约束（UNIQUE Constraint）失败。如需重新导入，必须先 `TRUNCATE` 所有 13 个表。

### 4. 数据清洗与容错机制详解

程序在 Java 端和数据库交互时，隐式执行了三种数据清洗和完整性强制操作。

| 清洗类型 | 触发问题 | Java 处理操作（示例） | 导入结果 |
| :--- | :--- | :--- | :--- |
| **1. 格式化 NULL 值** | CSV 中空值格式不统一（`""`, `"NULL"`, `null`）。 | `parse...` 辅助方法将所有空值格式统一转换为 Java `null`。 | 数据库字段被正确插入为 **NULL**。 |
| **2. 转换“脏”数据** | CSV 中数值字段包含非数字字符（如 `"N/A"`, `"1.5g"`）。 | `parseDecimal` 尝试解析，捕获 `NumberFormatException`，并打印 `无效的 Decimal 格式` 错误。 | 脏数据对应的字段被插入为 **NULL**，程序继续运行。 |
| **3. 强制数据完整性** | 批量插入时，存在无效外键（如 `ReviewLikes` 中的 `ReviewID` 不存在）。 | 数据库抛出 `SQLException`。Java `catch` 块捕获，打印 `[!! 批处理失败 !!]` 警告和根本原因。 | **中止并丢弃** 该批次（1000 条）的所有记录，程序继续处理下一个批次。 |

### 5. 导入结果报告

| 编号 | 表名称 (Table Name) | 对应 CSV 文件 | 控制台报告的记录数 | 备注 (如：跳过批次原因) |
| :--- | :--- | :--- | :--- | :-------- |
| 1 | `Users` | `User.csv` |299892 | |
| 2 | `Categories` | `Category.csv` |311 | |
| 3 | `Keywords` | `Keyword.csv` |314 | |
| 4 | `Ingredients` | `Ingredient.csv` |7385 | |
| 5 | `Recipes` | `Recipe.csv` |522517 | |
| 6 | `Reviews` | `Review.csv` |1401963 |Cannot invoke "java.lang.Integer.intValue()" because the return value of "org.example.CSVImporter.parseInteger(String)" is null|
| 7 | `Nutrition` | `Nutrition.csv` |522517 | |
| 8 | `RecipeCategories` | `Recipe_Category.csv` |521766 | |
| 9 | `RecipeKeywords` | `Recipe_Keyword.csv` |2529132 | |
| 10 | `RecipeIngredients` | `Recipe_Ingredient.csv` |4142016 | |
| 11 | `UserFavorites` | `User_Favorite_Recipe.csv` |2588000 | |
| 12 | `UserFollows` | `User_Follow.csv` |774121 | |
| 13 | `ReviewLikes` | `User_Like_Review.csv` |5402372 |错误: 插入或更新表 "reviewlikes" 违反外键约束 "reviewlikes_reviewid_fkey"详细：键值对(reviewid)=(517731)没有在表"reviews"中出现.  Call getNextException to see other errors in the batch. |

---
**总耗时：** `[23 分 5.012 秒 (总共: 1385012 毫秒)]`

### 数据库导入性能优化与对比分析报告

#### 1. 摘要 (Executive Summary)

本项目旨在将13个预处理后的CSV文件高效导入PostgreSQL数据库。

最初采用的方案（V1）是基于Java JDBC的 `BATCH INSERT`（批处理插入）。此方案虽然健壮，但**导入总耗时约 23 分钟**，性能无法满足需求。

通过分析，瓶颈被定位在Java端的逐行数据解析（`parse...`）以及JDBC批处理本身的网络和事务开销。

为了解决此问题，我们实施了第二套方案（V2），该方案采用了**ETL（提取-转换-加载）分离**的思想：
1.  **转换 (T):** `CSVDecomposerV10.java` 脚本负责**预处理**所有原始数据，进行数据清洗（如处理`"1.0"`为`"1"`）、格式转换（如`PT30M`为`30`）以及完整性校验（跳过`NOT NULL`约束的无效行），最终生成100%“数据库就绪”的CSV文件。
2.  **加载 (L):** `CSVImporterV2_Fixed.java` 脚本**完全移除**了Java端的数据解析，转而使用PostgreSQL原生的`COPY`命令，通过`CopyManager` API将数据从文件流高速加载到数据库中。

优化后的方案（V2）极大地提升了性能，**总耗时缩短至10分钟左右**，相较于原始方案，性能**提升了约 1 倍**，证明了原生批量加载工具在大型数据导入场景下的绝对优势。

---

### 2. 测试环境

为确保测试结果的一致性，所有测试均在以下环境中进行：

* **硬件 (Hardware):**
    * **CPU:** Intel(R) Core(TM) i5-1035G7 CPU @ 1.20GHz
	* **内存 (RAM):** 8GB
    * **存储 (Storage):** NVMe SSD（海力士 HFM256GDGTNG）
* **软件 (Software):**
    * **操作系统 (OS):**  Windows 11 专业版
    * **数据库 (Database):** PostgreSQL 17.7
    * **Java 版本 (JDK):** jdk-17.0.4.1
    * **JDBC 驱动:** poostgresql-42.7.8
    * **开发工具 (IDE):** IntelliJ IDEA 2024.2.1

---

### 3. 测试过程

为保证对比的公平性，我们遵循了严格的测试步骤：

1.  **数据准备：** 运行 `CSVDecomposerV2.java` 脚本，在 `processedData` 目录下生成所有13个清洁的CSV文件。此步骤的耗时不计入导入时间。
2.  **数据库清空：** 在每次测试运行前，连接到 `postgres` 数据库，对所有13个目标表执行 `TRUNCATE TABLE ... RESTART IDENTITY CASCADE;` 命令，以清空所有数据并重置自增序列。
3.  **测试方案 A (基准)：**
    * 运行 `CSVImporter.java` (V1 - `BATCH INSERT` 版本)。
    * 记录程序控制台输出的“总耗时”。
4.  **数据库清空：** 重复步骤 2。
5.  **测试方案 B (优化)：**
    * 运行 `CSVImporterV2.java` (V2 - `COPY` 版本)。
    * 记录程序控制台输出的“总耗时”。
6.  **重复测试：** 每个方案均运行 3 次，取平均值作为最终耗时。

---

### 4. 方案对比分析

#### 4.1. 方案 A: 原始方案 (JDBC BATCH INSERT)

* **实现：** `CSVImporter.java` (V1)
* **工作流：**
    1.  Java 程序使用 `CSVParser` 逐行读取CSV文件。
    2.  对于**每一行**的**每一个单元格**，调用 `parseInteger()`, `parseDecimal()`, `setNullOr...()` 等辅助方法进行数据类型转换和 `null` 值处理。
    3.  使用 `PreparedStatement.addBatch()` 将 `INSERT` 语句（`BATCH_SIZE = 1000`）添加到批处理中。
    4.  每1000行执行一次 `executeBatch()`，这相当于一个（在`autoCommit=true`下的）小型事务。
* **性能瓶颈：**
    1.  **Java 端 CPU 密集：** 绝大多数时间消耗在 Java 虚拟机 (JVM) 内部，用于解析数百万个字符串单元格并将其转换为 `Integer`, `BigDecimal` 等对象。
    2.  **高昂的 JDBC/网络开销：** 假设总共有 1,000,000 行数据，批大小为 1000，则需要执行 1000 次 `executeBatch()` 调用。这意味着至少 1000 次网络往返和 1000 次单独的事务提交。
    3.  **数据库端开销：** 数据库需要解析和规划这 1000 个不同的 `INSERT` 批次。

#### 4.2. 方案 B: 优化方案 (PostgreSQL COPY)

* **实现：** `CSVDecomposerV2.java` + `CSVImporterV2.java`
* **工作流：**
    1.  **(转换 T) - 预处理：** `CSVDecomposerV2` **一次性**完成所有数据清洗、类型转换和完整性校验，生成100%符合数据库规范（包括 `NOT NULL` 和整数格式）的CSV文件。
    2.  **(加载 L) - 导入：** `CSVImporterV2` **不执行任何 Java 端解析**。它只负责建立连接，并为每个文件执行一条 `COPY ... FROM STDIN` 命令。
    3.  `CopyManager` (由 `getCopyAPI()` 提供) 高效地将文件内容**流式传输**到 PostgreSQL 的 `STDIN`。
    4.  数据库服务器在**原生C语言层面**接管数据流，绕过大部分SQL解析层，以磁盘I/O的最高速度将数据直接写入表中。
* **性能优势：**
    1.  **零 Java 端解析：** 导入器的 CPU 占用极低，它只充当了数据“管道”。
    2.  **最小化网络/事务开销：** 13个文件 = 13次 `copyIn()` 调用。每个文件在**一个单独的、原子的事务**中完成，网络效率极高。
    3.  **原生C语言速度：** `COPY` 是 PostgreSQL 官方推荐的、用于批量加载的最快方法，其性能远非 `INSERT` 可比。

---

### 5. 耗时对比与结果

| 性能指标 | 方案 A (BATCH INSERT) | 方案 B (COPY) | 性能提升 |
| :--- | :--- | :--- | :--- |
| **总耗时** | 约 23 分钟 (1380 秒) | 约10分钟（600秒） |1.3倍
| **Java CPU 占用** | 高 (忙于解析字符串) | 低 (忙于I/O等待) | - |
| **数据库事务数** | 高 (数千次 `executeBatch`) | 低 (13 个文件 = 13 次 `copyIn`) | - |
| **数据健壮性** | V1 可跳过错误批次 | V2 (配合V2 Decomposer) 可跳过无效行 | 更高 (在源头清洗) |

*

---

### 6. 结论

本次优化实验非常成功。对比结果清晰地表明，`BATCH INSERT` 方法虽然在小规模或数据极度“脏”的场景下有一定灵活性，但其性能开销（Java端解析和JDBC事务）使其完全不适用于大数据量的批量导入。

通过将流程重构为**“转换-加载”(T-L) 分离**的ETL架构，并采用数据库原生的 `COPY` 工具，我们实现了 1倍的性能飞跃，将导入时间从 23 分钟缩短至 10分钟。

最终结论是：**在处理大型数据集时，应始终优先考虑使用数据库的原生批量加载工具（如 PostgreSQL 的 `COPY`），并将所有数据清洗和转换工作(T)在加载(L)之前作为预处理步骤完成。**

## Task 4: Compare DBMS with File I/O
### 4.1 环境规格
- **硬件规格**：
CPU: 13th Gen Intel(R) Core(TM) i7-13650HX (2.60 GHz)
RAM: 16.0 GB
系统类型: 64 位操作系统, 基于 x64 的处理器
操作系统(OS): Windows 11 Home
硬盘类型和容量: NVMe SSD/1 TB

- **软件规格**：
数据库客户端： DataGrip 2025.2.2
DBMS 服务器: PostgreSQL 17.6
关键库: PostgreSQL JDBC Driver 42.7.8
编程语言与执行环境: Java Development Kit (JDK) 22.0.2
操作系统(OS): Windows 11 Home


### 4.2 数据组织与存取规格
该任务的目标是比较DBMS与原始文件I/O在执行标准CRUD作时的性能差异。为确保比较的公平性与科学性，我们对测试数据的组织方式、存取方法以及测试语句的生成进行了严格的规格化。

- **原始数据格式与档案 I/O 结构**
测试数据基于课程提供的和其他 CSV 文件。 所有文件均采用 CSV(Comma-Separated Values)格式, 例如```recipes.csv```
	- 数据记录按行分隔
	- 每个记录中的字段使用逗号作为分隔符
	- 核心数据集包含超过50w条食谱记录

在文件 I/O 性能测试中，我们的 Java 程序（通过```CSVDcomposer.java```等类别实现）会将 CSV 文件读取到 JVM 内存的高效数据结构中（例如```List``` 或 ```HashMap```）。 所有的文件I/O模拟CRUD作均直接在这些内存数据结构上进行，以模拟应用程序直接作文件时的最佳性能。

- **DBMS 端的数据组织与结构**
	- **数据库结构**
数据库采用 PostgreSQL 17.6 服务器，并严格遵循 Task 2 的设计要求，将原始数据正规化为多个关系表格。
		- 所有表格均定义了主键和外键，以确保数据的唯一性和关系完整性。
		- 数据库结构已根据 Task 2 的三个正规形式要求进行优化。

	- **数据集**
所有原始数据已通过Task 3的高效脚本完整导入DBMS，确保数据库中的记录数量与原始档案中的记录数量一致。

### 4.3 测试程序与 SQL 描述
- **测试SQL语句的动态生成**
为确保性能比较的公正性，代码中的CRUD操作语句是通过 Java JDBC 接口动态生成的。
	- 测试数据点的选择:
		- SELECT/UPDATE/DELETE操作：代码会事先从数据库中随机选取一组数量固定的 现有主键值作为测试参数。这确保了每次测试都是对真实、已索引数据的有效操作。
		- INSERT操作：代码会生成一组数量固定的全新、不冲突的测试记录数据。

- **SQL语句的生成方式：**
所有 SQL 语句均使用 JDBC Prepared Statements 配合随机选择的参数执行，以确保数据库能够高效地编译和执行语句，同时避免 SQL 注入风险。

| 操作类型 | 语句生成目的 (测试重点) | 语句示例（项目相关） |
| :---: | :--- | :--- |
| **查询 (SELECT)** | 测试基于**食谱 ID (Primary Key)** 的高效单行检索性能。 | `SELECT * FROM Recipes WHERE Recipe_ID = ?` |
| **插入 (INSERT)** | 测试数据库处理事务、锁定和**维护索引**的开销（例如插入一条新评论）。 | `INSERT INTO Reviews (Review_ID, Recipe_ID, User_ID, Rating) VALUES (?, ?, ?, ?)` |
| **更新 (UPDATE)** | 测试数据库定位记录和**修改非索引列**的开销（例如修改评论文本）。 | `UPDATE Reviews SET Review_Text = ? WHERE Review_ID = ?` |
| **删除 (DELETE)** | 测试数据库**维护 B-tree 索引**的开销，并保持数据集规模稳定（例如删除一条测试评论）。 | `DELETE FROM Reviews WHERE Review_ID = ?` |

- **测试源码描述**

	为了执行 Task 4 的性能比较实验，我们设计并实现了一个基于 Java（JDK 22.0.2）的测试程序。该程序遵循模块化和关注点分离的原则，将数据库操作、文件 I/O 操作和测试协调逻辑分离。

	代码（此为为改进之前的版本, 即附上的 _v1的java文件）主要由以下四个核心java类组成：



- **1. PerformanceTester.java**

	- **职责：** 这是程序的主入口点和协调器，包含了 `main` 方法，负责控制整个测试流程的执行顺序。

	- **测试流程与计时机制：**

	**预热：** 程序会首先执行几轮预热测试，以确保 JVM 的实时编译器（JIT）和 PostgreSQL 的缓存（Buffers）均已启动，从而避免“冷启动”对测试结果的干扰。

	**循环测试：** 在预热后，程序会进入一个主循环，迭代执行 N 次（例如 1,000 次）CRUD 操作。

	**计时：** 该模块使用 Java 中最高精度的计时 API **`System.nanoTime()`**，在每次 CRUD 操作前后获取时间戳，精确计算单次操作的**纯运行时间**。

	- **结果收集：** 负责收集每次迭代的耗时数据，并在测试结束后计算平均值，用于 Task 4.4 的图表生成。



- **2. PostgreSQLOperations.java 与 PostgreSQLConnector.java**

	- **职责：** 该模块封装了所有与 PostgreSQL 17.6 服务器交互的逻辑，专门执行 DBMS 端的 CRUD 操作。

	- **连接管理：** 内部依赖 **`PostgreSQLConnector.java`** 模块，该模块利用 **PostgreSQL JDBC Driver 42.7.8** 建立和管理数据库连接。

	- **语句执行：** 严格使用 **JDBC Prepared Statements** 来执行所有 SQL 语句（例如 `SELECT * FROM Recipes WHERE Recipe_ID = ?`）。



- **3. CSVDecomposer.java**

	- **职责：** 该模块负责模拟原始文件 I/O 的 CRUD 操作，作为 DBMS 的对照组。

	- **数据加载：** 在测试开始前，程序会将 `recipes.csv` 文件的全部内容（超过 50 万条记录）一次性读取到 JVM 内存中。

	- **内存结构：** 数据被存储在高效的内存数据结构中（例如 `HashMap<Integer, Recipe>`），其中**主键（Recipe_ID）**作为 **Map** 的键。
	- **CRUD 模拟：**
	**SELECT (查询)：** 模拟为 `HashMap.get(key)` 操作，其时间复杂度为 O(1)。

	**INSERT/DELETE (增删)：** 模拟为 `HashMap.put(key, value)` 或 `HashMap.remove(key)` 操作。

	- **核心设计：** 这种设计旨在模拟应用程序直接操作内存数据时的**最佳性能**。

- **4. 数据模型类 (Recipe.java / Review.java)**

	- **职责：** 这些是简单的 POJO (Plain Old Java Object) 类别，作为数据的载体，确保数据传输的一致性。
	- **功能：** **`Recipe.java`** 类别定义了 `recipes` 表中一条记录的数据结构；**`Review.java`** 类别则用于处理与评论相关的 CRUD 操作。


### 4.4：性能对比测试结果与分析

本节旨在评估和对比数据库管理系统（DBMS，使用 PostgreSQL）与直接文件 I/O 操作（模拟内存缓存）在执行基础 CRUD（创建、读取、更新、删除）操作时的性能差异。

#### 测试数据汇总

**测试环境：**
- **DBMS 平台：** PostgreSQL
- **File I/O 平台：** Java 内存 `HashMap`（模拟 O(1) 查找环境）
- **操作总量：** 每种操作执行 1,000 次
- **数据基础：** 测试数据基础为 500,000 条 Recipes 记录

---

**表 4.4.1：DBMS 与 File I/O 性能对比（1,000 次操作总耗时）**

| 操作类型 | DBMS 耗时 (ms) | File I/O 耗时 (ms) | 速度差异倍数 (File I/O 快于 DBMS) |
|---------|---------------|-------------------|----------------------------------|
| **查询 (Query)** | 43717.6211 | 0.1165 | 约 375,257 倍 |
| **插入 (Insert)** | 47118.5049 | 0.9584 | 约 49,165 倍 |
| **更新 (Update)** | 43549.7917 | 0.2556 | 约 170,382 倍 |
| **删除 (Delete)** | 47422.0930 | 0.2367 | 约 200,347 倍 |
| **平均耗时** | 45451.9902 | 0.3918 | 约 116,000 倍 |

*(注：速度差异倍数 = DBMS 耗时 / File I/O 耗时)*

#### 性能分析与原因探讨

测试结果显示，File I/O 操作的执行速度比 DBMS 操作快 4 到 6 个数量级，这种巨大的差异是由两者底层机制的**系统开销**决定。

##### 1. File I/O 性能极高的原因（基准测试）

File I/O 测试是作为**性能基准线**而设计的。
- **机制：** 所有的数据已预先加载到 Java 虚拟机（JVM）内存中。因此，所有的操作实际上是执行 `HashMap` 的查找、添加和移除。
- **时间复杂度：** 在内存中执行查询 (`HashMap.get()`) 具有理想的平均时间复杂度 O(1)。
- **结论：** 0.1ms 左右的极短耗时，代表了在**没有任何磁盘 I/O、网络延迟或系统管理开销**下的纯粹计算速度。

##### 2. DBMS 性能较慢的原因（系统管理开销）

DBMS 每次操作平均耗时约 45 毫秒（45,000 ms / 1,000 次操作）。这种高耗时主要来源于以下几个关键的系统管理环节：

#####  开销点 1：连接建立与关闭

分析数据，DBMS 每次操作耗时远高于单纯的 SQL 执行。这表明主要的瓶颈在于：
- **连接延迟：** 我们的测试代码对每一次的 CRUD 操作都重新建立并关闭了一个新的数据库连接（即 `PostgreSQLConnector.getConnection()` 被调用 1,000 次）。
- **成本：** 建立和断开 TCP/IP 连接、身份验证和会话初始化，是一个极其昂贵的操作。每次连接占用约 20-50 毫秒，这是导致总耗时高达 40 多秒的**最主要因素**。

##### 开销点 2：ACID 事务管理

DBMS 必须保证事务的 **ACID 特性**，这引入了 File I/O 所没有的巨大开销：
- **持久性：** 对于 **Insert, Update, Delete** 操作，数据库必须在报告成功之前，将更改写入到预写日志。WAL 写入是一种耗时的物理磁盘 I/O 操作。
- **隔离性：** DBMS 必须执行锁定和并发控制机制，即使在单用户测试中，检查锁定的机制依然存在。
- **原子性：** 确保一个事务中的所有步骤要么全部成功，要么全部失败，这需要持续的日志记录和状态检查。

##### 开销点 3：数据完整性检查

在 `INSERT` 和 `UPDATE` 时，DBMS 必须对数据执行严格的验证：
- **约束检查：** 检查主键的唯一性、外键的引用完整性、以及 `NOT NULL` 等约束。

#### 实验洞察与展示亮点

除此之外，我们还发现了几个有趣的点：

#### 1. 数据库连接开销占据主导

我们发现数据显示所有 DBMS 操作（查询、插入、更新）的总耗时都异常接近。这表明 **SQL 执行时间**在总耗时中几乎可以忽略不计。
我们的测试实际上证明了，在当前的应用程序架构下，建立和关闭数据库连接比执行实际的业务 SQL 语句要慢 1,000 倍以上。这为未来优化提供了明确的方向，即必须引入连接池。

#### 2. File I/O 的性能边界与持久性机制引入的写入成本

我们发现 File I/O 的查询速度（0.1165 ms）比插入速度（0.9584 ms）快了约 8 倍，而 DBMS 的 `INSERT/DELETE` 耗时（约 47,000 ms）略高于 `QUERY/UPDATE`（约 43,000 ms）。
**我们的见解：**
    File I/O 证明了 `HashMap.get()`（读取）比 `HashMap.put()`（写入）更快，即使在内存中也存在轻微的写入开销。
    DBMS 的差异（4,000 ms 的额外耗时）则是**持久性**的实际体现。这微小的时间差，正是 DBMS 为确保数据在系统崩溃时**不可丢失**而付出的代价。

---

#### 3. 总结与评估

本次性能对比测试的结论是：

* **性能差异是真实且预期的：** File I/O 提供了原始的速度，而 DBMS 提供了可靠性。
* **速度的权衡：** 牺牲了极致的运行速度，换取了数据的完整性、可靠性和并发控制能力。
* **意义：** 在真实世界的应用场景中（如电商、金融），数据的正确性  远比单纯的速度更为重要。File I/O 方案在多用户环境下会立即导致数据损坏，而 DBMS 系统正是为了防范此类错误而设计。
* **Future work：** 尽管 File I/O 永远更快，但 DBMS 的性能可以通过使用连接池来大幅改善，从而消除占用总时间 99% 的连接建立开销。

### 4.5 Advanced Tasks
### Advanced Task 1: 高并发处理能力评估
在测试之前，我们一定程度上修改了之前的类，并创建了几个新类(QueryTask.java, ConcurrencyTester.java)
#### 测试方法与优化策略
为消除 Task 4.4 中发现的连接开销瓶颈，我们小组执行了以下两项关键优化：
- **架构优化:** 引入 HikariCP 连接池，将昂贵的连接建立作转变为 $O(1)$ 的池内获取。
- **负载设计:** 使用创建50个并发执行绪，对数据库进行压力测试，总测试规模为100,000次随机查询。

#### 高并发测试结果与数据对比

| 性能指标 | Task 4.4, 单线程 | 高级并发测试 (本任务, 50 线程) | 提升效果 |
| :--- | :--- | :--- | :--- |
| **工作量** | 1,000 次查询 | **100,000 次查询** | **100 倍** |
| **总耗时** | 43,717.62 ms | **3,293.68 ms** | 总时间缩短 **92.5%** |
| **平均 TPS** | 约 0.023 次/秒 | **30,361.15 次/秒** | 约 **130 万倍** |

#### 总结:
本次进阶测试的结论是：
- 性能瓶颈的验证： 数据明确证明，应用程序的性能瓶颈完全在于连接建立的架构缺陷。 在消除该瓶颈后，系统能够在 3.3 秒内处理 100,000 次查询。
- 高并发能力的确认： 最终 30,361.15 TPS 的结果，强有力地验证了 PostgreSQL 引擎本身具备优异的高并发处理能力，能够高效服务于现代多用户应用。
- 意义： 本次测试的意义在于：证明了连接池对应用程序性能的影响，远大于底层硬件或数据库引擎本身。

### Advanced Task 2: 连接池性能对比分析
#### 测试目的与方法
- 测试目的: 在Advanced Task 1 中，我们证明了连接池是释放数据库性能的关键。 本任务旨在完成"比较不同连接池的性能"，以量化不同连接池实现对系统吞吐量的具体影响。
- 测试方法：为确保比较的公平性，本测试采用严格的控制变量法。
	- 固定变量：硬件、作系统 （Windows）、PostgreSQL 版本、Java 代码、ConcurrencyTester.java 的负载（50 线程 / 10 万次查询）均保持完全一致。
	- 唯一变量: 测试 A 使用 **HikariCP** 连接池，测试 B 使用 **C3P0** 连接池

### 性能基准测试结果
##### 两次测试的性能数据对比如下：
### 表 AT2.1：HikariCP vs. C3P0 性能对比

| 性能指标 | 测试 A (HikariCP) | 测试 B (C3P0) | 性能差异 (C3P0 相较于 HikariCP) |
| :--- | :--- | :--- | :--- |
| **总耗时** | 3,293.68 ms | **2,218.47 ms** | 总时间缩短 **32.6%** |
| **平均 TPS** | 30,361.15 | **45,076.05** | 吞吐量提升 **48.46%** |

### 对比结果分析
- **数据的意外性:** **HikariCP** 通常被公认为 Java 领域中性能最高的连接池。 然而，在我们的特定测试环境（Windows OS + PostgreSQL + Java 17）和特定工作负载（高并发随机主键查询）下，**C3P0** 展现了明显的性能优势，其 TPS 竟然高出 48.46%。
- **性能差异的可能原因:**
	- 配置与调优: 我们使用的是两个函式库的预设配置。**C3P0** 的默认调度策略可能在我们的短查询、高并发场景下，比 **HikariCP** 的预设配置更具侵略性，从而更快地分配和回收连接。
	- 执行绪模型: **C3P0** 是一个较老牌的连接池，其内部执行绪模型和锁定策略与 **HikariCPC3P0** 的无锁设计不同。 在我们的特定环境中， 的模型可能更适应当前的负载模式。

### 总结：
我们的数据强有力地证明，软件工程中没有绝对的解决方案。 尽管 HikariCP 在业界享有盛名，但 48.46% 的性能差异表明，在特定环境和负载下进行基准测试是选择正确技术库的唯一标准。

### Advanced Task 3: 优化查询逻辑
我们发现，虽然连接已高效管理，但系统仍需发送 100,000 次独立的网络请求，网络延迟成为新的性能瓶颈。通过AI的帮助我们探究了**批次阵列查询(Batch Array Query)**。

我们将查询逻辑进行重构，实施了批次阵列查询：创建了新类 BatchQueryTask.java， 令 BATCH_SIZE = 100。然后利用 PostgreSQL 的 ANY(?) 语法与 JDBC 的 java.sql.Array，将 100 次单独查询合并为 1 次网络请求。
### 效率对比与结果
通过这项优化，我们成功地将总网络请求数减少了99%（从100000降至1000）。
### 表 AT3.1：效率对比 (单点查询 vs. 批量查询)

| 性能指标 | Advanced Task 2 (单点查询) | **Advanced Task 3 探索 (批量查询)** | **效果** |
| :--- | :--- | :--- | :--- |
| **网络请求数** | 100,000 次 | **1,000 次** | **减少 99%** |
| **总耗时** | 2,217.17 ms | **1,367.61 ms** | **缩短 38.3%** |
| **平均 TPS** | 45,102.52 | **73,120.50** | **提升 62.12%** |

### 总结:
**通过实施查询逻辑的批次化重构，我们将系统吞吐量从45,102 TPS提升至73,120 TPS，实现了62.12%的额外效率增益。 这证明了优化查询逻辑是消除网络I/O瓶颈、实现极致性能的途径。**
