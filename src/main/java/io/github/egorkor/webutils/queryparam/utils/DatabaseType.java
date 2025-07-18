package io.github.egorkor.webutils.queryparam.utils;

import lombok.Getter;

/**
 * @author EgorKor
 * @version 1.0
 * @since 2025
 */
@Getter
public enum DatabaseType {
    POSTGRESQL("org.postgresql.Driver"),
    MYSQL("com.mysql.cj.jdbc.Driver"),
    ORACLE("oracle.jdbc.OracleDriver"),
    SQL_SERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    DB2("com.ibm.db2.jcc.DB2Driver"),
    H2("org.h2.Driver"),
    SQLITE("org.sqlite.JDBC"),
    MARIADB("org.mariadb.jdbc.Driver"),
    OTHER("");

    private final String driverClass;

    DatabaseType(String driverClass) {
        this.driverClass = driverClass;
    }

    public static DatabaseType fromDriverClass(String driverClassName) {
        for (DatabaseType type : values()) {
            if (type.driverClass.equals(driverClassName)) {
                return type;
            }
        }
        return OTHER;
    }
}
