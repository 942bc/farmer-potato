package org.farmer.potato.datasource;

import org.farmer.potato.PotatoConfig;
import org.farmer.potato.pool.PotatoFarmland;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * User: zhangjiajun2
 * Date: 2016/11/7
 * Time: 13:51
 */
public class PotatoDataSource implements DataSource, Closeable{

    private final PotatoFarmland pool;

    public PotatoDataSource() {
        super();
        pool = null;

    }
    public PotatoDataSource(PotatoConfig config) {
        //校验配置是否可以使用
        pool = new PotatoFarmland(config);

    }



    @Override
    public void close() throws IOException {

    }

    @Override
    public Connection getConnection() throws SQLException {
        return null;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {

    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }
}
