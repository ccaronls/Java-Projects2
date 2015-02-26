---------------------------------------------
-- SEQUENCE TABLES TABLE
---------------------------------------------
CREATE TABLE FAN_SEQUENCE_T (
	NAME VARCHAR(64) NOT NULL,
	VALUE INTEGER NOT NULL,
	TABLE_NAME VARCHAR(64) NOT NULL
);

INSERT INTO FAN_SEQUENCE_T(name, value, table_name)
   values ('FAN_FRANCHISE_ID', '1000', 'FAN_FRANCHISE_T');   
INSERT INTO FAN_SEQUENCE_T(name, value, table_name)
   values ('FAN_LEAGUE_ID', '5000', 'FAN_LEAGUE_T');
INSERT INTO FAN_SEQUENCE_T(name, value, table_name)
   values ('FAN_TEAM_ID', '10000', 'FAN_TEAM_T');      
INSERT INTO FAN_SEQUENCE_T(name, value, table_name)
   values ('FAN_USER_ID', '10000', 'FAN_USER_T');
INSERT INTO FAN_SEQUENCE_T(name, value, table_name)
   values ('FAN_PLAYER_ID', '20000', 'FAN_PLAYER_T');   
INSERT INTO FAN_SEQUENCE_T(name, value, table_name)
   values ('FAN_POSITION_ID', '30000', 'FAN_POSITION_T'); 
INSERT INTO FAN_SEQUENCE_T(name, value, table_name)
   values ('FAN_STAT_ID', '40000', 'FAN_STAT_T'); 
INSERT INTO FAN_SEQUENCE_T(name, value, table_name)
   values ('FAN_LEAGUE_STAT_ID', '50000', 'FAN_LEAGUE_STAT_T'); 
INSERT INTO FAN_SEQUENCE_T(name, value, table_name)
   values ('FAN_LEAGUE_POSITION_ID', '60000', 'FAN_LEAGUE_POSITION_T'); 
INSERT INTO FAN_SEQUENCE_T(name, value, table_name)
   values ('FAN_TEAM_PLAYER_ID', '70000', 'FAN_TEAM_PLAYER_T'); 
   
---------------------------------------------
-- USER TABLE
---------------------------------------------
CREATE TABLE FAN_USER_T (
	ID INTEGER NOT NULL PRIMARY KEY,
	FIRST_NAME VARCHAR(128),
	LAST_NAME VARCHAR(128),
	USER_NAME VARCHAR(128) NOT NULL,
	PASSWORD VARCHAR(128) NOT NULL,
	ACCESS VARCHAR(64),
	EMAIL VARCHAR(128),
	ACTIVE CHAR(1),
	LAST_LOGIN DATE	
);
   
---------------------------------------------
-- FRANCHISE TABLE
---------------------------------------------
CREATE TABLE FAN_FRANCHISE_T (
	ID INTEGER NOT NULL PRIMARY KEY,
	NAME VARCHAR(128) NOT NULL,
	CATEGORY VARCHAR(128),
	ACTIVE CHAR(1)
);

---------------------------------------------
-- LEAGUE TABLE
---------------------------------------------
CREATE TABLE FAN_LEAGUE_T (
	ID INTEGER NOT NULL PRIMARY KEY,
	NAME VARCHAR(128) NOT NULL,
	USER_ID INTEGER NOT NULL,
	FRANCHISE_ID INTEGER NOT NULL,
	MAX_PLAYERS INTEGER,
	CREATED DATE DEFAULT SYSDATE,
	DRAFT DATE NOT NULL,
	ENDING DATE NOT NULL,
	UPDATED DATE NOT NULL
);

---------------------------------------------
-- TEAM TABLE
---------------------------------------------
CREATE TABLE FAN_TEAM_T (
	ID INTEGER NOT NULL PRIMARY KEY,
	NAME VARCHAR(128) NOT NULL,
	USER_ID INTEGER NOT NULL,
	POINTS INTEGER,
	LEAGUE_ID INTEGER NOT NULL
);

---------------------------------------------
-- PLAYER TABLE (many-to-one with with franchise)
---------------------------------------------
CREATE TABLE FAN_PLAYER_T (
	ID INTEGER NOT NULL PRIMARY KEY,
	FIRST_NAME VARCHAR(128),
	LAST_NAME VARCHAR(128),
	INFO VARCHAR(128),
	POSITIONS VARCHAR(128),
	STATUS VARCHAR(128),
	FRANCHISE_ID INTEGER NOT NULL
);

---------------------------------------------
-- POSITION TABLE (many-to-one with with franchise)
---------------------------------------------
CREATE TABLE FAN_POSITION_T (
	ID INTEGER NOT NULL PRIMARY KEY,
	NAME VARCHAR(32) NOT NULL,
	LONG_NAME VARCHAR(128),
	PLAYER_COLUMN VARCHAR(64),
	SPREADSHEET_FILE VARCHAR(256),
	OMIT_STATS VARCHAR(256),
	FRANCHISE_ID INTEGER NOT NULL
);
	
---------------------------------------------
-- STAT TABLE 
---------------------------------------------
CREATE TABLE FAN_STAT_T (
	ID INTEGER NOT NULL PRIMARY KEY,
	NAME VARCHAR(32) NOT NULL,
	DESCRIPTION VARCHAR(128)
);

---------------------------------------------
-- LEAGUE STAT TABLE 
---------------------------------------------
CREATE TABLE FAN_LEAGUE_STAT_T (
	ID INTEGER NOT NULL PRIMARY KEY,
	NAME VARCHAR(32) NOT NULL,
	MULTIPLIER NUMBER
);

---------------------------------------------
-- LEAGUE POSITION TABLE 
---------------------------------------------
CREATE TABLE FAN_LEAGUE_POSITION_T (
	ID INTEGER NOT NULL PRIMARY KEY,
	POSITION_ID INTEGER NOT NULL,
	NUM INTEGER
);

---------------------------------------------
-- TEAM PLAYER TABLE 
---------------------------------------------
CREATE TABLE FAN_TEAM_PLAYER_T (
	ID INTEGER NOT NULL PRIMARY KEY,
	PLAYER_ID INTEGER NOT NULL,
	POSITION VARCHAR(32) NOT NULL,
	RANK INTEGER
);
