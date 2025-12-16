DROP TABLE IF EXISTS "UserAssignedRole" CASCADE;
DROP TABLE IF EXISTS "RoleIncludesPermission" CASCADE;
DROP TABLE IF EXISTS "ReviewLikes" CASCADE;
DROP TABLE IF EXISTS "RecipeFavorites" CASCADE;
DROP TABLE IF EXISTS "Favorites" CASCADE;
DROP TABLE IF EXISTS "Review" CASCADE;
DROP TABLE IF EXISTS "Nutrition" CASCADE;
DROP TABLE IF EXISTS "Requires" CASCADE;
DROP TABLE IF EXISTS "WithKeyword" CASCADE;
DROP TABLE IF EXISTS "CategorizedAs" CASCADE;
DROP TABLE IF EXISTS "Recipes" CASCADE;
DROP TABLE IF EXISTS "Keyword" CASCADE;
DROP TABLE IF EXISTS "Category" CASCADE;
DROP TABLE IF EXISTS "Ingredient" CASCADE;
DROP TABLE IF EXISTS "Permission" CASCADE;
DROP TABLE IF EXISTS "Role" CASCADE;
DROP TABLE IF EXISTS "User" CASCADE;
DROP TABLE IF EXISTS "Follow" CASCADE;




CREATE TABLE "Role" (
    "RoleID"        BIGSERIAL PRIMARY KEY,
    "RoleName"      VARCHAR(100) NOT NULL UNIQUE,
    "Description"   VARCHAR(255),
    "CreatingTime"  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "Permission" (
    "PermissionID"  BIGSERIAL PRIMARY KEY,
    "GroupName"     VARCHAR(100) NOT NULL,
    "Description"   VARCHAR(255),
    "PermissionKey" VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE "User" (
    "AuthorID"      BIGSERIAL PRIMARY KEY,
    "AuthorName"    VARCHAR(255) NOT NULL UNIQUE,
    "HashedPassword" VARCHAR(255) NOT NULL,
    "Gender"        VARCHAR(10),
    "Age"           SMALLINT CHECK ("Age" >= 0),
    "Followers"     INT DEFAULT 0 CHECK ("Followers" >= 0),
    "Following"     INT DEFAULT 0 CHECK ("Following" >= 0)
);

CREATE TABLE "Category" (
    "CategoryID"    BIGSERIAL PRIMARY KEY,
    "CategoryName"  VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE "Keyword" (
    "KeywordID"     BIGSERIAL PRIMARY KEY,
    "KeywordName"   VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE "Ingredient" (
    "IngredientID"     BIGSERIAL PRIMARY KEY,
    "IngredientName"   VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE "Recipes" (
    "RecipeID"              BIGSERIAL PRIMARY KEY,
    "AuthorID"              BIGINT NOT NULL,
    "Name"                  VARCHAR(255) NOT NULL,
    "CookTime"              SMALLINT CHECK ("CookTime" >= 0),
    "PrepTime"              SMALLINT CHECK ("PrepTime" >= 0),
    "TotalTime"             SMALLINT CHECK ("TotalTime" >= 0),
    "DatePublished"         TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    "Description"           TEXT,
    "AggregatedRating"      NUMERIC(3, 2) CHECK ("AggregatedRating" >= 0 AND "AggregatedRating" <= 5),
    "ReviewCount"           INT DEFAULT 0 CHECK ("ReviewCount" >= 0),
    "RecipeServings"        NUMERIC(4, 1) CHECK ("RecipeServings" > 0),
    "RecipeYield"           VARCHAR(100),
    "RecipeInstructions"    TEXT,

    FOREIGN KEY ("AuthorID") REFERENCES "User"("AuthorID") ON DELETE CASCADE
);

CREATE TABLE "Review" (
    "ReviewID"      BIGSERIAL PRIMARY KEY,
    "RecipeID"      BIGINT NOT NULL,
    "AuthorID"      BIGINT NOT NULL,
    "Rating"        NUMERIC(3, 2) CHECK ("Rating" >= 0 AND "Rating" <= 5),
    "Review"        TEXT,
    "DateSubmitted" TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    "DateModified"  TIMESTAMP WITH TIME ZONE,
    "Likes"         INT DEFAULT 0 CHECK ("Likes" >= 0),

    FOREIGN KEY ("RecipeID") REFERENCES "Recipes"("RecipeID") ON DELETE CASCADE,
    FOREIGN KEY ("AuthorID") REFERENCES "User"("AuthorID") ON DELETE CASCADE
);

CREATE TABLE "Nutrition" (
    "RecipeID"          BIGINT PRIMARY KEY,
    "Calories"          NUMERIC(7, 2) CHECK ("Calories" >= 0),
    "FatContent"        NUMERIC(6, 2) CHECK ("FatContent" >= 0),
    "SaturatedFatContent" NUMERIC(6, 2) CHECK ("SaturatedFatContent" >= 0),
    "CholesterolContent" NUMERIC(6, 2) CHECK ("CholesterolContent" >= 0),
    "SodiumContent"     NUMERIC(6, 2) CHECK ("SodiumContent" >= 0),
    "CarbohydrateContent" NUMERIC(6, 2) CHECK ("CarbohydrateContent" >= 0),
    "FiberContent"      NUMERIC(6, 2) CHECK ("FiberContent" >= 0),
    "SugarContent"      NUMERIC(6, 2) CHECK ("SugarContent" >= 0),
    "ProteinContent"    NUMERIC(6, 2) CHECK ("ProteinContent" >= 0),

    FOREIGN KEY ("RecipeID") REFERENCES "Recipes"("RecipeID") ON DELETE CASCADE
);



CREATE TABLE "CategorizedAs" (
    "RecipeID"      BIGINT NOT NULL,
    "CategoryID"    BIGINT NOT NULL,
    PRIMARY KEY ("RecipeID", "CategoryID"),
    FOREIGN KEY ("RecipeID") REFERENCES "Recipes"("RecipeID") ON DELETE CASCADE,
    FOREIGN KEY ("CategoryID") REFERENCES "Category"("CategoryID") ON DELETE CASCADE
);

CREATE TABLE "WithKeyword" (
    "RecipeID"      BIGINT NOT NULL,
    "KeywordID"     BIGINT NOT NULL,
    PRIMARY KEY ("RecipeID", "KeywordID"),
    FOREIGN KEY ("RecipeID") REFERENCES "Recipes"("RecipeID") ON DELETE CASCADE,
    FOREIGN KEY ("KeywordID") REFERENCES "Keyword"("KeywordID") ON DELETE CASCADE
);

CREATE TABLE "Requires" (
    "RecipeID"        BIGINT NOT NULL,
    "IngredientID"    BIGINT NOT NULL,
    "Quantity"        VARCHAR(50),
    PRIMARY KEY ("RecipeID", "IngredientID"),
    FOREIGN KEY ("RecipeID") REFERENCES "Recipes"("RecipeID") ON DELETE CASCADE,
    FOREIGN KEY ("IngredientID") REFERENCES "Ingredient"("IngredientID") ON DELETE CASCADE
);

CREATE TABLE "Follow" (
    "FollowerID"    BIGINT NOT NULL,
    "FollowingID"   BIGINT NOT NULL,
    PRIMARY KEY ("FollowerID", "FollowingID"),
    FOREIGN KEY ("FollowerID") REFERENCES "User"("AuthorID") ON DELETE CASCADE,
    FOREIGN KEY ("FollowingID") REFERENCES "User"("AuthorID") ON DELETE CASCADE,
    CHECK ("FollowerID" <> "FollowingID")
);

CREATE TABLE "Favorites" (
    "UserID"        BIGINT NOT NULL,
    "RecipeID"      BIGINT NOT NULL,
    PRIMARY KEY ("UserID", "RecipeID"),
    FOREIGN KEY ("UserID") REFERENCES "User"("AuthorID") ON DELETE CASCADE,
    FOREIGN KEY ("RecipeID") REFERENCES "Recipes"("RecipeID") ON DELETE CASCADE
);

CREATE TABLE "RecipeFavorites" (
    "UserID"        BIGINT NOT NULL,
    "RecipeID"      BIGINT NOT NULL,
    PRIMARY KEY ("UserID", "RecipeID"),
    FOREIGN KEY ("UserID") REFERENCES "User"("AuthorID") ON DELETE CASCADE,
    FOREIGN KEY ("RecipeID") REFERENCES "Recipes"("RecipeID") ON DELETE CASCADE
);

CREATE TABLE "ReviewLikes" (
    "UserID"        BIGINT NOT NULL,
    "ReviewID"      BIGINT NOT NULL,
    PRIMARY KEY ("UserID", "ReviewID"),
    FOREIGN KEY ("UserID") REFERENCES "User"("AuthorID") ON DELETE CASCADE,
    FOREIGN KEY ("ReviewID") REFERENCES "Review"("ReviewID") ON DELETE CASCADE
);

CREATE TABLE "RoleIncludesPermission" (
    "RoleID"        BIGINT NOT NULL,
    "PermissionID"  BIGINT NOT NULL,
    PRIMARY KEY ("RoleID", "PermissionID"),
    FOREIGN KEY ("RoleID") REFERENCES "Role"("RoleID") ON DELETE CASCADE,
    FOREIGN KEY ("PermissionID") REFERENCES "Permission"("PermissionID") ON DELETE CASCADE
);

CREATE TABLE "UserAssignedRole" (
    "UserID"        BIGINT NOT NULL,
    "RoleID"        BIGINT NOT NULL,
    PRIMARY KEY ("UserID", "RoleID"),
    FOREIGN KEY ("UserID") REFERENCES "User"("AuthorID") ON DELETE CASCADE,
    FOREIGN KEY ("RoleID") REFERENCES "Role"("RoleID") ON DELETE CASCADE
);



DO
$do$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'sustc') THEN
        CREATE ROLE sustc LOGIN PASSWORD 'sustec';
    END IF;
END
$do$;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO sustc;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO sustc;

CREATE INDEX idx_recipes_authorid ON "Recipes" ("AuthorID");
CREATE INDEX idx_review_recipeid ON "Review" ("RecipeID");
CREATE INDEX idx_review_authorid ON "Review" ("AuthorID");
CREATE INDEX idx_follow_followerid ON "Follow" ("FollowerID");
CREATE INDEX idx_follow_followingid ON "Follow" ("FollowingID");
CREATE INDEX idx_favorites_userid ON "Favorites" ("UserID");
CREATE INDEX idx_favorites_recipeid ON "Favorites" ("RecipeID");
CREATE INDEX idx_recipes_name ON "Recipes" ("Name");

INSERT INTO "Permission" ("GroupName", "PermissionKey") VALUES
('Recipe', 'recipe:create'),
('Recipe', 'recipe:edit_own'),
('Recipe', 'recipe:delete_own'),
('Review', 'review:submit'),
('Review', 'review:delete_own'),
('User', 'user:follow'),
('Admin', 'recipe:delete_any');

INSERT INTO "Role" ("RoleName") VALUES
('Administrator'),
('RegisteredUser'),
('Guest');

INSERT INTO "RoleIncludesPermission" ("RoleID", "PermissionID") VALUES
(1, 7),
(2, 1),
(2, 2),
(2, 3),
(2, 4),
(2, 5),
(2, 6);

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO sustc;
