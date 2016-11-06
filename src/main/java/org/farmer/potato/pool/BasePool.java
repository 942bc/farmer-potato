package org.farmer.potato.pool;

import org.farmer.potato.PotatoConfig;
import org.farmer.potato.util.PotatoUtil;
import org.farmer.potato.datasource.DriverDataSource;
import org.farmer.potato.util.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created with IntelliJ IDEA.
 * User: zhangjiajun2
 * Date: 2016/11/6
 * Time: 14:48
 */
abstract class BasePool {

    private final Logger logger = LoggerFactory.getLogger(BasePool.class);

    protected final PotatoConfig potatoConfig;

    protected final String poolName;

    protected long connectionTimeout;

    protected long validationTimeout;

    private static final String[] RESET_STATES = {"readOnly", "autoCommit", "isolation", "catalog", "netTimeout"};
    private static final int UNINITIALIZED = -1;

    private int networkTimeout;
    private int isNetworkTimeoutSupported;
    private int isQueryTimeoutSupported;
    private int defaultTransactionIsolation;
    private int transactionIsolation;
    private Executor netTimeoutExecutor;
    private DataSource dataSource;

    private final String catalog;
    private final boolean isReadOnly;
    private final boolean isAutoCommit;

    private final boolean isUseJdbc4Validation;
    private final boolean isIsolateInternalQueries;
    private final AtomicReference<Throwable> lastConnectionFailure;

    private volatile boolean isValidChecked;

    BasePool(final PotatoConfig potatoConfig){
        this.potatoConfig = potatoConfig;


        this.networkTimeout = UNINITIALIZED;
        this.catalog = potatoConfig.getCatalog();
        this.isReadOnly = potatoConfig.isReadOnly();
        this.isAutoCommit = potatoConfig.isAutoCommit();
        this.transactionIsolation = PotatoUtil.getTransactionIsolation(potatoConfig.getTransactionIsolationName());

        this.isQueryTimeoutSupported = UNINITIALIZED;
        this.isNetworkTimeoutSupported = UNINITIALIZED;
        this.isUseJdbc4Validation = potatoConfig.getConnectionTestQuery() == null;
        this.isIsolateInternalQueries = potatoConfig.isolateInternalQueries();

        this.poolName = potatoConfig.getPoolName();
        this.connectionTimeout = potatoConfig.getConnectionTimeout();
        this.validationTimeout = potatoConfig.getValidationTimeout();
        this.lastConnectionFailure = new AtomicReference<Throwable>();

        this.initDataSource();
    }

    private void initDataSource(){
        final String jdbcUrl = potatoConfig.getJdbcUrl();
        final String username = potatoConfig.getUsername();
        final String password = potatoConfig.getPassword();
        final String dsClassName = potatoConfig.getDataSourceClassName();
        final String driverClassName = potatoConfig.getDriverClassName();
        final Properties dataSourceProperties = potatoConfig.getDataSourceProperties();
        DataSource dataSource = potatoConfig.getDataSource();
        if (dsClassName != null && dataSource == null) {
            dataSource = createInstance(dsClassName, DataSource.class);
            PotatoUtil.setPropToObjectField(dataSourceProperties, dataSource);
        }
        else if (jdbcUrl != null && dataSource == null) {
            dataSource = new DriverDataSource(jdbcUrl, driverClassName, username, password, dataSourceProperties);
        }

        if (dataSource != null) {
            setLoginTimeout(dataSource);
            createNetworkTimeoutExecutor(dataSource, dsClassName, jdbcUrl);
        }

        this.dataSource = dataSource;
    }

    public static <T> T createInstance(final String className, final Class<T> clazz, final Object... args) {
        if (className == null) {
            return null;
        }

        try {
            Class<?> loaded = BasePool.class.getClassLoader().loadClass(className);
            if (args.length == 0) {
                return clazz.cast(loaded.newInstance());
            }

            Class<?>[] argClasses = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argClasses[i] = args[i].getClass();
            }
            Constructor<?> constructor = loaded.getConstructor(argClasses);
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
                logger.info("{} - Failed to set login timeout for data source. ({})", poolName, e.getMessage());
            }
        }
    }

    private void createNetworkTimeoutExecutor(final DataSource dataSource, final String dsClassName, final String jdbcUrl) {
        // Temporary hack for MySQL issue: http://bugs.mysql.com/bug.php?id=75615
        //Special executor used only to work around a MySQL issue that has not been addressed.
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
}
