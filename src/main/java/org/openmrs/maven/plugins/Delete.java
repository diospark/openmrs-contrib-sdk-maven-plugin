package org.openmrs.maven.plugins;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.openmrs.maven.plugins.utility.AttributeHelper;
import org.openmrs.maven.plugins.utility.DBConnector;
import org.openmrs.maven.plugins.utility.ServerConfig;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * @goal delete
 * @requiresProject false
 */
public class Delete extends AbstractMojo{

    private static final String TEMPLATE_SUCCESS = "Server '%s' removed successfully";

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * @component
     */
    private Prompter prompter;

    public void execute() throws MojoExecutionException, MojoFailureException {
        AttributeHelper helper = new AttributeHelper(prompter);
        File server = helper.getServerPath(serverId);
        try {
            ServerConfig props = ServerConfig.loadServerConfig(server);
            FileUtils.deleteDirectory(server);
            String dbName = props.getParam(SDKConstants.PROPERTY_DB_NAME);
            String dbUser = props.getParam(SDKConstants.PROPERTY_DB_USER);
            String dbPass = props.getParam(SDKConstants.PROPERTY_DB_PASS);
            String dbUri = props.getParam(SDKConstants.PROPERTY_DB_URI);
            DBConnector connector = new DBConnector(dbUri, dbUser, dbPass, dbName);
            connector.dropDatabase();
            connector.close();
            getLog().info(String.format(TEMPLATE_SUCCESS, server.getName()));
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (SQLException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }
}
