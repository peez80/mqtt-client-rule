package de.stiffi.testing.junit.rules.postgres;

import de.stiffi.testing.junit.rules.kubectl.KubeCtlTunnelRule;
import org.junit.rules.ExternalResource;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgresRule extends ExternalResource {


    private int dbPort;
    private String dbName;
    private String dbHost;
    private String username;
    private String password;

    private String jdbcUrl;

    private Connection connection;

    private KubeCtlTunnelRule tunnelRule;

    public static PostgresRule newPostgresRule(String dbHost, int dbPort, String dbName, String username, String password) {
        PostgresRule rule = new PostgresRule();
        rule.dbHost = dbHost;
        rule.dbPort = dbPort;
        rule.dbName = dbName;
        rule.username = username;
        rule.password = password;

        return rule;
    }

    public static PostgresRule newPostgresRuleByJdbcUrl(String jdbcUri, String username, String password) {
        URI uri = URI.create(jdbcUri);

        PostgresRule rule = new PostgresRule();
        rule.jdbcUrl = jdbcUri;
        rule.username = username;
        rule.password = password;

        return rule;
    }

    public static PostgresRule newTunneledPostgresRule(KubeCtlTunnelRule tunnelRule, String dbName, String username, String password) {
        PostgresRule rule = new PostgresRule();
        rule.dbHost = "localhost";
        rule.dbName = dbName;
        rule.username = username;
        rule.password = password;

        rule.tunnelRule = tunnelRule;
        return rule;
    }

    protected PostgresRule() {

    }

    @Override
    protected void before() throws Throwable {
        Class.forName("org.postgresql.Driver");


        if (tunnelRule != null) {
            tunnelRule.before();
            dbPort = tunnelRule.getLocalPort();
        }

        String jdbcConnectionUrl = (
                jdbcUrl != null
                        ? jdbcUrl
                        : "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName);


        System.out.println("Establish Postgres connection: " + jdbcConnectionUrl);
        connection = DriverManager.getConnection(jdbcConnectionUrl, username, password);
    }

    @Override
    protected void after() {
        try {
            connection.close();
            System.out.println("JDBC Connection Closed: " + connection.isClosed());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (tunnelRule != null) {
            tunnelRule.after();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
