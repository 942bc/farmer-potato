package org.farmer.potato.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: zhangjiajun2
 * Date: 2016/11/6
 * Time: 16:34
 */
public class DriverDataSource implements DataSource {

    private final Logger logger = LoggerFactory.getLogger(DriverDataSource.class);
    private final String jdbcUrl;
    private final Properties driverProperties;
    private Driver driver;

    public DriverDataSource(String jdbcUrl, String driverClassName, String username, String password, Properties properties) {
        this.jdbcUrl = jdbcUrl;
        this.driverProperties = new Properties();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            driverProperties.setProperty(entry.getKey().toString(), entry.getValue().toString());
        }
        if (username != null) {
            driverProperties.put("user", driverProperties.getProperty("user", username));
        }
        if (password != null) {
            driverProperties.put("password", driverProperties.getProperty("password", password));
        }
        /*****************
         * 实例化驱动对象 *
         *****************/
        if (driverClassName != null) {
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                if (driverClassName.equals(driver.getClass().getName())) {
                    this.driver = driver;
                    break;
                }
            }
        }
        if (driver == null) {
            logger.warn("没有在DriverManager中找到驱动类{}, 开始尝试直接实例化驱动类的对象", driverClassName);
            try {
                Class<?> driverClass = this.getClass().getClassLoader().loadClass(driverClassName);
                driver = (Driver) driverClass.newInstance();
            } catch (Exception e) {
                logger.warn("实例化驱动类{}的对象失败, 开始尝试使用jdbcUrl从驱动管理器中获取", driverClassName, e);
            }
        }

        try {
            if (driver == null) {
                driver = DriverManager.getDriver(jdbcUrl);
            } else if (!driver.acceptsURL(jdbcUrl)) {
                throw new RuntimeException("驱动类 " + driverClassName + " 不支持jdbcUrl, " + jdbcUrl);
            }
        } catch (SQLException e) {
            throw new RuntimeException("根据jdbcUrl从驱动管理器中获取驱动失败, jdbcUrl=" + jdbcUrl, e);
        }

    }

    @Override
    public Connection getConnection() throws SQLException {
        return driver.connect(jdbcUrl, driverProperties);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getConnection();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        DriverManager.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }
}
