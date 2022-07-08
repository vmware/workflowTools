package com.vmware.util.db;

import com.vmware.util.ReflectionUtils;
import com.vmware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DbUtils {

    private Logger log = LoggerFactory.getLogger(DbUtils.class);

    private Driver driver;
    private String databaseUrl;
    private Properties dbProperties;

    private Connection currentConnection;

    public DbUtils(File databaseDriverFile, String databaseDriverClass, String databaseUrl, Properties dbProperties) {
        this.driver = createDatabaseDriver(databaseDriverFile, databaseDriverClass);
        this.databaseUrl = databaseUrl;
        this.dbProperties = dbProperties;
    }

    public void createConnection() {
        try {
            currentConnection = driver.connect(databaseUrl, dbProperties);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeConnection() {
        if (currentConnection != null) {
            try {
                currentConnection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            currentConnection = null;
        }
    }

    public int executeUpdate(String sql) {
        try (Statement statement = currentConnection.createStatement()) {
            return statement.executeUpdate(sql);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void executeSqlScript(String sqlScript) {
        Scanner s = new Scanner(sqlScript);
        s.useDelimiter("(;(\r)?\n)|(--\n)");
        try (Statement statement = currentConnection.createStatement()) {
            while (s.hasNext()) {
                String line = s.next();
                if (line.startsWith("/*!") && line.endsWith("*/")) {
                    int i = line.indexOf(' ');
                    line = line.substring(i + 1, line.length() - " */".length());
                }

                if (line.trim().length() > 0) {
                    statement.execute(line);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T queryUnique(Class<T> recordClass, String query, Object... parameters) {
        List<T> records = query(recordClass, query, parameters);
        if (records.size() > 1) {
            throw new RuntimeException(records.size() + " records found for query " + query);
        }
        return !records.isEmpty() ? records.get(0) : null;
    }

    public <T> List<T> query(Class<T> recordClass, String query, Object... parameters) {
        log.debug("{}{}{}", query, System.lineSeparator(), Arrays.toString(parameters));
        try (PreparedStatement statement = createStatement(query, parameters)) {
            ResultSet results = statement.executeQuery();
            ResultSetMetaData resultMetaData = results.getMetaData();
            if (!BaseDbClass.class.isAssignableFrom(recordClass) && resultMetaData.getColumnCount() == 1) {
                List<T> values = new ArrayList<>();
                while (results.next()) {
                    values.add((T) results.getObject(1));
                }
                return values;
            } else if (!BaseDbClass.class.isAssignableFrom(recordClass)) {
               throw new RuntimeException("Cannot query for multiple values without using a class that extends " + BaseDbClass.class.getSimpleName());
            }

            List<Field> fieldsForClass = ReflectionUtils.getAllFieldsWithoutAnnotation(recordClass, null);

            Map<Field, Integer> fieldColumnMappings = new HashMap<>();
            for (int i = 1; i <= resultMetaData.getColumnCount(); i++) {
                String columnName = resultMetaData.getColumnName(i);
                String expectedFieldName = StringUtils.convertFromDbName(columnName);
                Optional<Field> matchingField = fieldsForClass.stream().filter(field -> field.getName().equals(expectedFieldName)).findFirst();
                if (matchingField.isPresent()) {
                    fieldColumnMappings.put(matchingField.get(), i);
                }
            }

            List<T> records = new ArrayList<>();
            while (results.next()) {
                T record = (T) ReflectionUtils.newInstance(recordClass);
                fieldColumnMappings.forEach((key, value) -> {
                    try {
                        Object valueToUse = results.getObject(value);
                        if (key.getType().isEnum() && valueToUse instanceof String) {
                            valueToUse = createEnumValue(key.getType(), valueToUse.toString());
                        }
                        ReflectionUtils.setValue(key, record, valueToUse);
                    } catch (SQLException se) {
                        throw new RuntimeException(se);
                    }
                });
                ReflectionUtils.invokeAllMethodsWithAnnotation(record, AfterDbLoad.class);
                records.add(record);
            }
            return records;

        } catch (SQLException se) {
            throw new RuntimeException(se);
        }

    }

    public <T> void insert(T record) {
        insertIfNeeded(record, null);
    }

    public <T> void insertIfNeeded(T record, String existsQuery, Object... existsQueryParams) {
        throwExceptionIfNotDbClass(record.getClass());

        if (((BaseDbClass) record).id != null) {
            return;
        }

        if (existsQuery != null) {
            BaseDbClass existingRecord = (BaseDbClass) queryUnique(record.getClass(), existsQuery, existsQueryParams);
            if (existingRecord != null) {
                ((BaseDbClass) record).id = existingRecord.id;
                return;
            }
        }

        try (PreparedStatement insertStatement = createInsertStatement(record)) {
            insertStatement.executeUpdate();

            try (ResultSet keys = insertStatement.getGeneratedKeys()) {
                if (keys.next()) {
                    ((BaseDbClass) record).id = keys.getLong(1);
                }
            }

        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    public <T> void update(T record) {
        throwExceptionIfNotDbClass(record.getClass());
        try (PreparedStatement updateStatement = createUpdateStatement(record)) {
            int rowsUpdated = updateStatement.executeUpdate();
            if (rowsUpdated != 1) {
                throw new RuntimeException("Expected 1 row to be updated not " + rowsUpdated);
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    public int delete(BaseDbClass record) {
        if (record.id == null) {
            return 0;
        }
        String tableName = StringUtils.convertToDbName(record.getClass().getSimpleName());
        String query = "DELETE FROM " + tableName + " WHERE id = ?";
        return delete(query, record.id);
    }

    public int delete(String query, Object... parameters) {
        log.debug("{}{}{}", query, System.lineSeparator(), Arrays.toString(parameters));
        try (PreparedStatement deleteStatement = createStatement(query, parameters)) {
            return deleteStatement.executeUpdate();
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    private <T> PreparedStatement createInsertStatement(T record) {
        List<Field> fields = ReflectionUtils.getAllFieldsWithoutAnnotation(record.getClass(), DbSaveIgnore.class);
        String tableName = StringUtils.convertToDbName(record.getClass().getSimpleName());
        StringBuilder insertStatement = new StringBuilder("INSERT INTO ").append(tableName).append(" (");

        insertStatement.append(fields.stream().map(this::convertToColumnName).collect(Collectors.joining(", ")));

        insertStatement.append(") VALUES (");

        insertStatement.append(IntStream.range(0, fields.size()).mapToObj(i -> "?").collect(Collectors.joining(", ")));
        insertStatement.append(")");

        String statementText = insertStatement.toString();
        log.debug(statementText);

        try {
            return createStatementWithFieldValues(record, fields, statementText);
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    private <T> PreparedStatement createUpdateStatement(T record) {
        List<Field> fields = ReflectionUtils.getAllFieldsWithoutAnnotation(record.getClass(), DbSaveIgnore.class);
        TableName tableNameAnnotation = record.getClass().getAnnotation(TableName.class);
        String tableName = tableNameAnnotation != null ? tableNameAnnotation.value()
                : StringUtils.convertToDbName(record.getClass().getSimpleName());
        StringBuilder updateStatement = new StringBuilder("UPDATE ").append(tableName).append(" SET ");

        updateStatement.append(fields.stream().map(field -> convertToColumnName(field) + " = ?").collect(Collectors.joining(", ")));

        updateStatement.append(" WHERE ID = ?");

        String statementText = updateStatement.toString();
        log.debug(statementText);

        try {
            PreparedStatement statement = createStatementWithFieldValues(record, fields, statementText);
            statement.setLong(fields.size() + 1, ((BaseDbClass) record).id);
            return statement;
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    private <T> PreparedStatement createStatementWithFieldValues(T record, List<Field> fields, String statementToUse)
            throws SQLException {
        PreparedStatement statement = currentConnection.prepareStatement(statementToUse, Statement.RETURN_GENERATED_KEYS);

        IntStream.range(0, fields.size()).forEach(i -> {
            try {
                Object valueToUse = getValue(record, fields.get(i));
                statement.setObject(i + 1, valueToUse);
            } catch (SQLException se) {
                throw new RuntimeException(se);
            }
        });
        return statement;
    }

    private <T> Object getValue(T record, Field field) {
        Object valueToUse = ReflectionUtils.getValue(field, record);
        if (valueToUse != null && valueToUse.getClass().isEnum()) {
            return valueToUse.toString();
        } else {
            return valueToUse;
        }
    }

    private <T> void throwExceptionIfNotDbClass(Class record) {
        if (!BaseDbClass.class.isAssignableFrom(record)) {
            throw new RuntimeException(record.getSimpleName() + " cannot be saved as only objects extending " + BaseDbClass.class.getSimpleName() + " can be saved");
        }
    }

    private <T> PreparedStatement createStatement(String query, Object[] parameters) throws SQLException {
        PreparedStatement statement = currentConnection.prepareStatement(query);
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
        return statement;
    }

    private Object createEnumValue(Class type, String text)
    {
        for (Object candidate : type.getEnumConstants()) {
            if (candidate.toString().equals(text)) {
                return candidate;
            }
        }

        return null;
    }

    private String convertToColumnName(Field field) {
        return StringUtils.convertToDbName(field.getName());
    }

    private Driver createDatabaseDriver(File databaseDriverFile, String databaseDriverClass) {
        Driver driver;
        try {
            log.debug("Loading driver {} in file {}", databaseDriverClass, databaseDriverFile.getPath());
            URLClassLoader urlClassloader = new URLClassLoader( new URL[] { databaseDriverFile.toURI().toURL() }, this.getClass().getClassLoader() );
            Class driverClass = urlClassloader.loadClass(databaseDriverClass);
            driver = (Driver) driverClass.newInstance();
        } catch (MalformedURLException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return driver;
    }
}
