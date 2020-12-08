package com.vmware.util.db;

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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.vmware.util.ReflectionUtils;
import com.vmware.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbUtils {

    private Logger log = LoggerFactory.getLogger(DbUtils.class);

    private Driver driver;
    private String databaseUrl;
    private Properties dbProperties;

    public DbUtils(File databaseDriverFile, String databaseDriverClass, String databaseUrl, Properties dbProperties) {
        this.driver = createDatabaseDriver(databaseDriverFile, databaseDriverClass);
        this.databaseUrl = databaseUrl;
        this.dbProperties = dbProperties;
    }

    public Connection createConnection() throws SQLException {
        return driver.connect(databaseUrl, dbProperties);
    }

    public <T> T queryUnique(Class<T> recordClass, String query, Object... parameters) {
        List<T> records = query(recordClass, query, parameters);
        if (records.size() > 1) {
            throw new RuntimeException(records.size() + " records found for query " + query);
        }
        return !records.isEmpty() ? records.get(0) : null;
    }


    public <T> T queryUnique(Connection connection, Class<T> recordClass, String query, Object... parameters) {
        List<T> records = query(connection, recordClass, query, parameters);
        if (records.size() > 1) {
            throw new RuntimeException(records.size() + " records found for query " + query);
        }
        return !records.isEmpty() ? records.get(0) : null;
    }

    public <T> List<T> query(Class<T> recordClass, String query, Object... parameters) {
        if (driver == null) {
            return Collections.emptyList();
        }
        try (Connection connection = createConnection()) {
            return query(connection, recordClass, query, parameters);
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    public <T> List<T> query(Connection connection, Class<T> recordClass, String query, Object... parameters) {
        log.debug("{}{}{}", query, System.lineSeparator(), Arrays.toString(parameters));
        try {
            PreparedStatement statement = createStatement(connection, query, parameters);
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

    public <T> void insert(Connection connection, T record) {
        insertIfNeeded(connection, record, null);
    }

    public <T> void insertIfNeeded(Connection connection, T record, String existsQuery, Object... existsQueryParams) {
        throwExceptionIfNotDbClass(record.getClass());

        if (((BaseDbClass) record).id != null) {
            return;
        }

        if (existsQuery != null) {
            BaseDbClass existingRecord = (BaseDbClass) queryUnique(connection, record.getClass(), existsQuery, existsQueryParams);
            if (existingRecord != null) {
                ((BaseDbClass) record).id = existingRecord.id;
                return;
            }
        }


        PreparedStatement insertStatement = createInsertStatement(connection, record);

        try {
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
        try (Connection connection = createConnection()) {
            update(connection, record);
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    public <T> void update(Connection connection, T record) {
        throwExceptionIfNotDbClass(record.getClass());
        try {
            PreparedStatement updateStatement = createUpdateStatement(connection, record);
            int rowsUpdated = updateStatement.executeUpdate();
            if (rowsUpdated != 1) {
                throw new RuntimeException("Expected 1 row to be updated not " + rowsUpdated);
            }
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    public int delete(Connection connection, String query, Object... parameters) {
        try {
            log.debug("{}{}{}", query, System.lineSeparator(), Arrays.toString(parameters));
            PreparedStatement deleteStatement = createStatement(connection, query, parameters);
            return deleteStatement.executeUpdate();
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    private <T> PreparedStatement createInsertStatement(Connection connection, T record) {
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
            return createStatementWithFieldValues(connection, record, fields, statementText);
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    private <T> PreparedStatement createUpdateStatement(Connection connection, T record) {
        List<Field> fields = ReflectionUtils.getAllFieldsWithoutAnnotation(record.getClass(), DbSaveIgnore.class);
        String tableName = StringUtils.convertToDbName(record.getClass().getSimpleName());
        StringBuilder updateStatement = new StringBuilder("UPDATE ").append(tableName).append(" SET ");

        updateStatement.append(fields.stream().map(field -> convertToColumnName(field) + " = ?").collect(Collectors.joining(", ")));

        updateStatement.append(" WHERE ID = ?");

        String statementText = updateStatement.toString();
        log.debug(statementText);

        try {
            PreparedStatement statement = createStatementWithFieldValues(connection, record, fields, statementText);
            statement.setLong(fields.size() + 1, ((BaseDbClass) record).id);
            return statement;
        } catch (SQLException se) {
            throw new RuntimeException(se);
        }
    }

    private <T> PreparedStatement createStatementWithFieldValues(Connection connection, T record, List<Field> fields, String statementToUse)
            throws SQLException {
        PreparedStatement statement = connection.prepareStatement(statementToUse, Statement.RETURN_GENERATED_KEYS);

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
            valueToUse = valueToUse.toString();
        } else if (valueToUse != null && valueToUse.getClass() == int[].class) {
            valueToUse = Arrays.stream((int[])valueToUse).mapToObj(String::valueOf).collect(Collectors.joining(","));
        }
        return valueToUse;
    }

    private <T> void throwExceptionIfNotDbClass(Class record) {
        if (!BaseDbClass.class.isAssignableFrom(record)) {
            throw new RuntimeException(record.getSimpleName() + " cannot be saved as only objects extending " + BaseDbClass.class.getSimpleName() + " can be saved");
        }
    }

    private <T> PreparedStatement createStatement(Connection connection, String query, Object[] parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
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
            URLClassLoader urlClassloader = new URLClassLoader( new URL[] { databaseDriverFile.toURI().toURL() }, System.class.getClassLoader() );
            Class driverClass = urlClassloader.loadClass(databaseDriverClass);
            driver = (Driver) driverClass.newInstance();
        } catch (MalformedURLException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return driver;
    }

}
