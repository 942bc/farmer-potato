package org.farmer.potato.pool;

import org.farmer.potato.PotatoConfig;
import org.farmer.potato.util.FarmTools;
import org.farmer.potato.datasource.DriverDataSource;
import org.farmer.potato.util.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * User: zhangjiajun2
 * Date: 2016/11/6
 * Time: 14:48
 */
abstract class BasePool {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final PotatoConfig potatoConfig;

    protected final String poolName;

    protected long connectionTimeout;

    protected long validationTimeout;

    private static final String[] RESET_STATES = {"readOnly", "autoCommit", "isolation", "catalog", "netTimeout"};
    private static final byte TRUE = 1;
    private static final byte FALSE = 0;

    private int networkTimeout;
    private byte isNetworkTimeoutSupported;
    private byte isQueryTimeoutSupported;
    private int defaultTransactionIsolation; //默认事务隔离级别
    private int transactionIsolation;        //事务隔离级别
    private Executor netTimeoutExecutor;
    private DataSource dataSource;           //数据源

    private final String catalog;
    private final boolean isReadOnly;        //是否只读
    private final boolean isAutoCommit;      //是否自动提交事务

    private final boolean isUseJdbc4Validation;
    private final boolean isIsolateInternalQueries;
    private final AtomicReference<Throwable> lastConnectionFailure;

    private volatile boolean isValidChecked; //是否做连接有效检查

    BasePool(final PotatoConfig potatoConfig){
        this.potatoConfig = potatoConfig;
        this.networkTimeout = -1;
        this.catalog = potatoConfig.getCatalog();
        this.isReadOnly = potatoConfig.isReadOnly();
        this.isAutoCommit = potatoConfig.isAutoCommit();
        this.transactionIsolation = FarmTools.getTransactionIsolation(potatoConfig.getTransactionIsolationName());

        this.isQueryTimeoutSupported = -1;
        this.isNetworkTimeoutSupported = -1;
        this.isUseJdbc4Validation = potatoConfig.getConnectionTestQuery() == null;
        this.isIsolateInternalQueries = potatoConfig.isolateInternalQueries();

        this.poolName = potatoConfig.getPoolName();
        this.connectionTimeout = potatoConfig.getConnectionTimeout();
        this.validationTimeout = potatoConfig.getValidationTimeout();
        this.lastConnectionFailure = new AtomicReference<Throwable>();
        this.dataSource = this.initDataSource();
    }

    /**
     * 初始化数据源
     */
    private DataSource initDataSource(){
        String jdbcUrl = potatoConfig.getJdbcUrl();
        String username = potatoConfig.getUsername();
        String password = potatoConfig.getPassword();
        String dsClassName = potatoConfig.getDataSourceClassName();
        String driverClassName = potatoConfig.getDriverClassName();
        Properties dataSourceProperties = potatoConfig.getDataSourceProperties();
        DataSource dataSource = potatoConfig.getDataSource();
        if (FarmTools.isNotBlank(dsClassName) && dataSource == null) {
            dataSource = createInstance(dsClassName, DataSource.class);
            FarmTools.setPropToObjectField(dataSourceProperties, dataSource);
        } else if (FarmTools.isNotBlank(jdbcUrl) && dataSource == null) {
            dataSource = new DriverDataSource(jdbcUrl, driverClassName, username, password, dataSourceProperties);
        }

        if (dataSource != null) {
            setLoginTimeout(dataSource);
            createNetworkTimeoutExecutor(dataSource, dsClassName, jdbcUrl);
        }
        return dataSource;
    }

    void closeConnection(Connection connection, String reason) {
        if (connection != null) {
            logger.debug("连接池{}关闭连接{} 原因:{}", poolName, connection, reason);
            try {
                connection.close();
            } catch (SQLException e) {
                logger.debug("连接池{}关闭连接{}失败", poolName, connection, e);
            }
        }
    }

    public static <T> T createInstance(final String className, final Class<T> clazz, final Object... args) {
        if (className == null) {
            return null;
        }
        try {
            Class<?> loadedClass = BasePool.class.getClassLoader().loadClass(className);
            if (args.length == 0) {
                return clazz.cast(loadedClass.newInstance());
            }

            Class<?>[] argClasses = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argClasses[i] = args[i].getClass();
            }
            Constructor<?> constructor = loadedClass.getConstructor(argClasses);
            return clazz.cast(constructor.newInstance(args));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setLoginTimeout(final DataSource dataSource) {
        if (connectionTimeout != Integer.MAX_VALUE) {
            try {
                dataSource.setLoginTimeout(Math.max(1, (int) MILLISECONDS.toSeconds(500L + connectionTimeout)));
            } catch (Throwable e) {
                logger.info("连接池{}中给数据源设置登录超时时间失败", poolName, e.getMessage());
            }
        }
    }

    private void createNetworkTimeoutExecutor(final DataSource dataSource, final String dsClassName, final String jdbcUrl) {
        //处理Mysql中存在的一个bug， MySQL issue: http://bugs.mysql.com/bug.php?id=75615
        if ((dsClassName != null && dsClassName.contains("Mysql")) ||
                (jdbcUrl != null && jdbcUrl.contains("mysql")) ||
                (dataSource != null && dataSource.getClass().getName().contains("Mysql"))) {
            netTimeoutExecutor = new Executor() {
                @Override
                public void execute(Runnable command) {
                    try {
                        command.run();
                    } catch (Throwable t) {
                        LoggerFactory.getLogger(BasePool.class).debug("Failed to execute: {}", command, t);
                    }
                }
            };
        } else {
            ThreadFactory threadFactory = potatoConfig.getThreadFactory();
            threadFactory = threadFactory != null ? threadFactory : new DefaultThreadFactory(poolName + " network timeout executor", true);
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool(threadFactory);
            executor.setKeepAliveTime(15, SECONDS);
            executor.allowCoreThreadTimeOut(true);
            netTimeoutExecutor = executor;
        }
    }

    /**
     * 检查连接是否存活
     * @param connection
     * @return
     */
    boolean checkConnectionAlive(final Connection connection) {
        if (connection != null) {
            Statement statement = null;
            try {
                try {
                    if (isUseJdbc4Validation) {
                        return connection.isValid((int) TimeUnit.MILLISECONDS.toSeconds(validationTimeout));
                    }
                    statement = connection.createStatement();
                } finally {
                    if (isIsolateInternalQueries && !isAutoCommit) {
                        connection.rollback();
                    }
                }
                try {
                    if (isNetworkTimeoutSupported != 0 && isQueryTimeoutSupported != 0) {
                        statement.setQueryTimeout((int) TimeUnit.MILLISECONDS.toSeconds(validationTimeout));
                        isQueryTimeoutSupported = TRUE;
                    }
                } catch (SQLException e) {
                    if (isQueryTimeoutSupported == -1) {
                        isQueryTimeoutSupported = FALSE;
                        logger.info("{}给statement设置超时时间失败。{}", poolName, e.getMessage());
                    }
                }
                try {
                    statement.execute(potatoConfig.getConnectionTestQuery());
                } finally {
                    if (isIsolateInternalQueries && !isAutoCommit) {
                        connection.rollback();
                    }
                }
            } catch (SQLException e) {
                lastConnectionFailure.set(e);
                return false;
            }
        }
        return true;
    }

    public Throwable getLastConnectionFailure() {
        return lastConnectionFailure.get();
    }
}
