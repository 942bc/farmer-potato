package org.farmer.potato;
import org.farmer.potato.util.FarmTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * User: zhangjiajun2
 * Date: 2016/11/6
 * Time: 14:33
 */
public class PotatoConfig implements PotatoFarmlandMXBean {

    private final Logger logger = LoggerFactory.getLogger(PotatoConfig.class);

    private static final long CONNECTION_TIMEOUT = SECONDS.toMillis(30);
    private static final long VALIDATION_TIMEOUT = SECONDS.toMillis(5);
    private static final long IDLE_TIMEOUT = MINUTES.toMillis(10);
    private static final long MAX_LIFETIME = MINUTES.toMillis(30);
    private static final int DEFAULT_POOL_SIZE = 10;

    private volatile long connectionTimeout;
    private volatile long validationTimeout;
    private volatile long idleTimeout;            //空闲时间
    private volatile long sickDetectionThreshold; //生病检测阀值
    private volatile long maxLifetime;
    private volatile int maxPoolSize;
    private volatile int minIdle;

    private String catalog;
    private String connectionInitSql;
    private String connectionTestQuery;
    private String dataSourceClassName;
    private String dataSourceJndiName;
    private String driverClassName;
    private String jdbcUrl;
    private String password;
    private String poolName;
    private String transactionIsolationName;
    private String username;
    private boolean isAutoCommit;
    private boolean isReadOnly;
    private boolean isInitializationFailFast;
    private boolean isIsolateInternalQueries;
    private boolean isRegisterMbeans;
    private boolean isAllowPoolPause;
    private DataSource dataSource;
    private Properties dataSourceProperties;
    private ThreadFactory threadFactory;
    private ScheduledThreadPoolExecutor scheduledExecutor;
    //private MetricsTrackerFactory metricsTrackerFactory;
    //private Object metricRegistry;
    //private Object healthCheckRegistry;
    //private Properties healthCheckProperties;

    public PotatoConfig(){
        dataSourceProperties = new Properties();

        minIdle = -1;
        maxPoolSize = -1;
        maxLifetime = MAX_LIFETIME;
        connectionTimeout = CONNECTION_TIMEOUT;
        validationTimeout = VALIDATION_TIMEOUT;
        idleTimeout = IDLE_TIMEOUT;
        isAutoCommit = false;
        isInitializationFailFast = true;

        String systemProp = System.getProperty("potato.configFilePath");
        if (systemProp != null) {
            loadConfig(systemProp);
        }
    }

    public PotatoConfig(Properties prop) {
        this();
        FarmTools.setPropToObjectField(prop, this);
    }

    public PotatoConfig(String configFilePath) {
        this();
        loadConfig(configFilePath);
    }


    protected void loadConfig(String configFilePath) {
        final File propFile = new File(configFilePath);
        try {
            InputStream in = propFile.isFile() ? new FileInputStream(propFile) : this.getClass().getResourceAsStream(configFilePath);
            if (in == null) {
                throw new IllegalArgumentException("Cannot find property file: " + configFilePath);
            }
            Properties prop = new Properties();
            prop.load(in);
            FarmTools.setPropToObjectField(prop, this);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read property file", e);
        }
    }

    public void addDataSourceProperty(String propertyName, String value) {
        dataSourceProperties.put(propertyName, value);
    }


    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        if (connectionTimeout == 0) {
            this.connectionTimeout = Integer.MAX_VALUE;
        }
        this.connectionTimeout = connectionTimeout;
    }

    public long getValidationTimeout() {
        return validationTimeout;
    }

    public void setValidationTimeout(long validationTimeout) {
        this.validationTimeout = validationTimeout;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        if (idleTimeout < 0) {
            throw new IllegalArgumentException("idleTimeout cannot be negative");
        }
        this.idleTimeout = idleTimeout;
    }

    public long getSickDetectionThreshold() {
        return sickDetectionThreshold;
    }

    public void setSickDetectionThreshold(long sickDetectionThreshold) {
        this.sickDetectionThreshold = sickDetectionThreshold;
    }

    public long getMaxLifetime() {
        return maxLifetime;
    }

    public void setMaxLifetime(long maxLifetime) {
        this.maxLifetime = maxLifetime;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        if (maxPoolSize < 1) {
            throw new IllegalArgumentException("maxPoolSize cannot be less than 1");
        }
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        if (minIdle < 0) {
            throw new IllegalArgumentException("minIdle cannot be negative");
        }
        this.minIdle = minIdle;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getConnectionInitSql() {
        return connectionInitSql;
    }

    public void setConnectionInitSql(String connectionInitSql) {
        this.connectionInitSql = connectionInitSql;
    }

    public String getConnectionTestQuery() {
        return connectionTestQuery;
    }

    public void setConnectionTestQuery(String connectionTestQuery) {
        this.connectionTestQuery = connectionTestQuery;
    }

    public String getDataSourceClassName() {
        return dataSourceClassName;
    }

    public void setDataSourceClassName(String dataSourceClassName) {
        this.dataSourceClassName = dataSourceClassName;
    }

    public String getDataSourceJndiName() {
        return dataSourceJndiName;
    }

    public void setDataSourceJndiName(String dataSourceJndiName) {
        this.dataSourceJndiName = dataSourceJndiName;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        try {
            Class<?> driverClass = this.getClass().getClassLoader().loadClass(driverClassName);
            driverClass.newInstance();
            this.driverClassName = driverClassName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load class of driverClassName " + driverClassName, e);
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getTransactionIsolationName() {
        return transactionIsolationName;
    }

    public void setTransactionIsolationName(String transactionIsolationName) {
        this.transactionIsolationName = transactionIsolationName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAutoCommit() {
        return isAutoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        isAutoCommit = autoCommit;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setReadOnly(boolean readOnly) {
        isReadOnly = readOnly;
    }

    public boolean isInitializationFailFast() {
        return isInitializationFailFast;
    }

    public void setInitializationFailFast(boolean initializationFailFast) {
        isInitializationFailFast = initializationFailFast;
    }

    public boolean isolateInternalQueries() {
        return isIsolateInternalQueries;
    }

    public void setIsolateInternalQueries(boolean isolateInternalQueries) {
        isIsolateInternalQueries = isolateInternalQueries;
    }

    public boolean isRegisterMbeans() {
        return isRegisterMbeans;
    }

    public void setRegisterMbeans(boolean registerMbeans) {
        isRegisterMbeans = registerMbeans;
    }

    public boolean isAllowPoolPause() {
        return isAllowPoolPause;
    }

    public void setAllowPoolPause(boolean allowPoolPause) {
        isAllowPoolPause = allowPoolPause;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Properties getDataSourceProperties() {
        return dataSourceProperties;
    }

    public void setDataSourceProperties(Properties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    public ScheduledThreadPoolExecutor getScheduledExecutor() {
        return scheduledExecutor;
    }

    public void setScheduledExecutor(ScheduledThreadPoolExecutor scheduledExecutor) {
        this.scheduledExecutor = scheduledExecutor;
    }



    @Override
    public int getIdleConnections() {
        return 0;
    }
    @Override
    public int getActiveConnections() {
        return 0;
    }
    @Override
    public int getTotalConnections() {
        return 0;
    }
    @Override
    public int getThreadsAwaitingConnection() {
        return 0;
    }
    @Override
    public void softEvictConnections() {

    }
    @Override
    public void pausePool() {

    }

    @Override
    public void resumePool() {

    }
}
