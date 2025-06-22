package io.github.egorkor.webutils.queryparam.utils;

import java.sql.DriverManager;
import java.util.Enumeration;


public class DriverUtils {

    /**
     * Определяет тип активной СУБД на основе доступных драйверов
     *
     * @return тип СУБД или UNKNOWN, если не удалось определить
     */
    public static DatabaseType getActiveDatabaseType() {
        // Сначала проверяем зарегистрированные драйверы
        try {
            Enumeration<java.sql.Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                java.sql.Driver driver = drivers.nextElement();
                for (DatabaseType type : DatabaseType.values()) {
                    if (type != DatabaseType.OTHER &&
                            driver.getClass().getName().equals(type.getDriverClass())) {
                        return type;
                    }
                }
            }
        } catch (Exception ignore) {
        }

        // Если через DriverManager не получилось, проверяем через Class.forName
        for (DatabaseType type : DatabaseType.values()) {
            if (type != DatabaseType.OTHER && isDriverAvailable(type.getDriverClass())) {
                return type;
            }
        }

        return DatabaseType.OTHER;
    }

    /**
     * Проверяет доступность драйвера СУБД
     *
     * @param driverClassName полное имя класса драйвера
     * @return true если драйвер доступен
     */
    public static boolean isDriverAvailable(String driverClassName) {
        try {
            Class.forName(driverClassName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Проверяет доступность конкретного типа СУБД
     *
     * @param type тип СУБД
     * @return true если драйвер доступен
     */
    public static boolean isDatabaseAvailable(DatabaseType type) {
        if (type == DatabaseType.OTHER) {
            return false;
        }
        return isDriverAvailable(type.getDriverClass());
    }

    /**
     * Выводит список всех доступных JDBC драйверов
     */
    public static void printAvailableDrivers() {
        try {
            System.out.println("Available JDBC Drivers:");
            Enumeration<java.sql.Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                java.sql.Driver driver = drivers.nextElement();
                System.out.println(" - " + driver.getClass().getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}