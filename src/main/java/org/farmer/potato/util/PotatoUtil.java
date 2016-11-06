package org.farmer.potato.util;

import org.farmer.potato.PotatoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: zhangjiajun2
 * Date: 2016/11/6
 * Time: 15:03
 */
public class PotatoUtil {
    private static final Logger logger = LoggerFactory.getLogger(PotatoUtil.class);

    public static void setPropToObjectField(final Properties prop, final Object obj) {
        if (prop == null || obj == null) {
            return;
        }
        if (obj instanceof PotatoConfig) {
            String key, value;
            PotatoConfig potatoConfig = (PotatoConfig) obj;
            for (Map.Entry<Object, Object> entry : prop.entrySet()) {
                key = String.valueOf(entry.getKey());
                value = String.valueOf(entry.getValue());
                potatoConfig.addDataSourceProperty(key.toString().substring("dataSource.".length()), value);
            }
        } else {
            setPropertyToObject(prop, obj);
        }
    }

    private static void setPropertyToObject(final Properties prop, final Object obj) {
        Method [] methods = obj.getClass().getMethods();
        String key, value;
        StringBuilder setMethodName = new StringBuilder();
        Method setMethod = null;
        for (Map.Entry<Object, Object> entry : prop.entrySet()) {
            key = String.valueOf(entry.getKey());
            value = String.valueOf(entry.getValue());
            setMethodName.append("set").append(key.substring(0,1).toLowerCase()).append(key.substring(1));
            for (Method method : methods) {
                if (setMethodName.equals(method.getName())) {
                    setMethod = method;
                    break;
                }
            }
            if (null == setMethod) {
                logger.error("Property {} does not exist on obj {}", key, obj.getClass());
                throw new RuntimeException(String.format("Property %s does not exist on obj %s", key, obj.getClass()));
            }
            try {
                Class<?> paramClass = setMethod.getParameterTypes()[0];
                if (paramClass == int.class) {
                    setMethod.invoke(obj, Integer.parseInt(key));
                } else if (paramClass == String.class) {
                    setMethod.invoke(obj, value);
                } else if (paramClass == long.class) {
                    setMethod.invoke(obj, Long.parseLong(value));
                } else if (paramClass == boolean.class) {
                    setMethod.invoke(obj, Boolean.parseBoolean(value));
                } else if(paramClass == double.class) {
                    setMethod.invoke(obj, Double.valueOf(value));
                } else if(paramClass == float.class) {
                    setMethod.invoke(obj, Float.valueOf(value));
                } else if (paramClass == byte.class) {
                    setMethod.invoke(obj, Byte.valueOf(value));
                } else if (paramClass == short.class) {
                    setMethod.invoke(obj, Short.valueOf(value));
                }
            } catch (Exception e) {
                logger.error("Failed to set property {} on obj {}", key, obj.getClass(), e);
                throw new RuntimeException(e);
            }
            setMethodName.setLength(0);
        }
    }

    public static String getNullIsBlank(final String str) {
        return str == null ? null : str.trim() == "" ? null : str.trim();
    }

    /**
     * 获取事务隔离级别
     * @param transactionIsolationName
     * @return
     */
    public static int getTransactionIsolation(final String transactionIsolationName){
        if (transactionIsolationName == null) {
            return  -1;
        }
        final String isolationName = transactionIsolationName.toUpperCase();
        try {
            if (isolationName.startsWith("TRANSACTION_")) {
                Field field = Connection.class.getField(isolationName);
                return field.getInt(null);
            }
            final int level = Integer.parseInt(transactionIsolationName);
            switch (level) {
                case Connection.TRANSACTION_READ_UNCOMMITTED:
                case Connection.TRANSACTION_READ_COMMITTED:
                case Connection.TRANSACTION_REPEATABLE_READ:
                case Connection.TRANSACTION_SERIALIZABLE:
                case Connection.TRANSACTION_NONE:
                    return level;
                default:
                    throw new IllegalArgumentException("Invalid transaction isolation value: " + transactionIsolationName);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid transaction isolation value: " + transactionIsolationName);
        }
    }
}
