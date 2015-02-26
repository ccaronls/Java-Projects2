package cc.fantasy.hibernate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.Hibernate;
import org.hibernate.MappingException;

public class SQLiteDialect extends Dialect {
    
    Log log = LogFactory.getLog(getClass());
    
    private String driver;
    private String url;
    private String user;
    private String pw;
    
    public SQLiteDialect() {
        
        try {
            Properties props = new Properties();
            props.load(getClass().getClassLoader().getResourceAsStream("fantasy.properties"));
            
            driver = props.getProperty("db.driver");
            url = props.getProperty("db.url");
            user = props.getProperty("db.user");
            pw = props.getProperty("db.pw");
            
            Class.forName(driver);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate SQLiteDialect", e);
        }
        
        registerColumnType(Types.BIT, "bit");
        registerColumnType(Types.TINYINT, "tinyint");
        registerColumnType(Types.SMALLINT, "smallint");
        registerColumnType(Types.INTEGER, "integer");
        registerColumnType(Types.BIGINT, "bigint");
        registerColumnType(Types.FLOAT, "float");
        registerColumnType(Types.REAL, "real");
        registerColumnType(Types.DOUBLE, "double");
        registerColumnType(Types.NUMERIC, "numeric");
        registerColumnType(Types.DECIMAL, "decimal");
        registerColumnType(Types.CHAR, "char");
        registerColumnType(Types.VARCHAR, "varchar");
        registerColumnType(Types.LONGVARCHAR, "longvarchar");
        registerColumnType(Types.DATE, "date");
        registerColumnType(Types.TIME, "time");
        registerColumnType(Types.TIMESTAMP, "timestamp");
        registerColumnType(Types.BINARY, "blob");
        registerColumnType(Types.VARBINARY, "blob");
        registerColumnType(Types.LONGVARBINARY, "blob");
        // registerColumnType(Types.NULL, "null");
        registerColumnType(Types.BLOB, "blob");
        registerColumnType(Types.CLOB, "clob");
        registerColumnType(Types.BOOLEAN, "boolean");
        
        registerFunction( "concat", new VarArgsSQLFunction(Hibernate.STRING, "", "||", "") );
        registerFunction( "mod", new SQLFunctionTemplate( Hibernate.INTEGER, "?1 % ?2" ) );
        registerFunction( "substr", new StandardSQLFunction("substr", Hibernate.STRING) );
        registerFunction( "substring", new StandardSQLFunction( "substr", Hibernate.STRING ) );
    }
    
    public boolean supportsIdentityColumns() {
        return true;
    }
    
    public boolean hasDataTypeInIdentityColumn() {
        return false; // As specify in NHibernate dialect
    }
    
    public String getIdentityColumnString() {
        // return "integer primary key autoincrement";
        return "integer";
    }
    
    public String getIdentitySelectString() {
        return "select last_insert_rowid()";
    }
    
    public boolean supportsLimit() {
        return true;
    }
    
    public String getLimitString(String query, boolean hasOffset) {
        return new StringBuffer(query.length()+20).
        append(query).
        append(hasOffset ? " limit ? offset ?" : " limit ?").
        toString();
    }
    
    public boolean supportsTemporaryTables() {
        return true;
    }
    
    public String getCreateTemporaryTableString() {
        return "create temporary table if not exists";
    }
    
    public boolean dropTemporaryTableAfterUse() {
        return false;
    }
    
    public boolean supportsCurrentTimestampSelection() {
        return true;
    }
    
    public boolean isCurrentTimestampSelectStringCallable() {
        return false;
    }
    
    public String getCurrentTimestampSelectString() {
        return "select current_timestamp";
    }
    
    public boolean supportsUnionAll() {
        return true;
    }
    
    public boolean hasAlterTable() {
        return false; // As specify in NHibernate dialect
    }
    
    public boolean dropConstraints() {
        return false;
    }
    
    public String getAddColumnString() {
        return "add column";
    }
    
    public String getForUpdateString() {
        return "";
    }
    
    public boolean supportsOuterJoinForUpdate() {
        return false;
    }
    
    public String getDropForeignKeyString() {
        throw new UnsupportedOperationException("No drop foreign key syntax supported by SQLiteDialect");
    }
    
    public String getAddForeignKeyConstraintString(String constraintName,
            String[] foreignKey, String referencedTable, String[] primaryKey,
            boolean referencesPrimaryKey) {
        throw new UnsupportedOperationException("No add foreign key syntax supported by SQLiteDialect");
    }
    
    public String getAddPrimaryKeyConstraintString(String constraintName) {
        throw new UnsupportedOperationException("No add primary key syntax supported by SQLiteDialect");
    }
    
    public boolean supportsIfExistsBeforeTableName() {
        return true;
    }
    
    public boolean supportsCascadeDelete() {
        return false;
    }
    
    public String getSequenceNextValString(String arg0) throws MappingException {
    	return "0";
    }
    
    /*
        int value = 0;
        int num = 0;
        try {
            ResultSet result = executeQuery("select VALUE, TABLE_NAME from FAN_SEQUENCE_T where NAME = '" + arg0 + "'");
            value = result.getInt(1);
            String table = result.getString(2);
            result.close();
            // count how many 
            result = executeQuery("select count(*) from " + table);
            num = result.getInt(1);
            result.close();            
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.debug("CC: " + arg0 + "=" + (value+num));
        return String.valueOf(value+num);
    }
    
    ResultSet executeQuery(String q) throws SQLException {
        Connection con = DriverManager.getConnection(url, user, pw);
        log.debug("executing query: " + q);
        return con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).executeQuery(q);
    }
    
    void executeStmt(String s) throws SQLException {
        Connection con = DriverManager.getConnection(url, user, pw);
        log.debug("executing stmt: " + s);
        con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).execute(s);
    }
*/    
}
