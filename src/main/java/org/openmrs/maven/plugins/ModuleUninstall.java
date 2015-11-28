package org.openmrs.maven.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.utility.AttributeHelper;
import org.openmrs.maven.plugins.utility.ServerConfig;
import org.openmrs.maven.plugins.utility.SDKConstants;

import java.io.File;

/**
 * @goal uninstall
 * @requiresProject false
 */
public class ModuleUninstall extends AbstractMojo {

    /**
     * @parameter expression="${serverId}"
     */
    private String serverId;

    /**
     * @parameter expression="${artifactId}"
     */
    private String artifactId;

    /**
     * @parameter expression="${groupId}" default-value="org.openmrs.module"
     */
    private String groupId;

    /**
     * @component
     */
    private Prompter prompter;

    public void execute() throws MojoExecutionException, MojoFailureException {
        ModuleInstall installer = new ModuleInstall(prompter);
        AttributeHelper helper = new AttributeHelper(prompter);
        if (serverId == null) {
            File currentProperties = helper.getCurrentServerPath();
            if (currentProperties != null) serverId = currentProperties.getName();
        }
        File serverPath = helper.getServerPath(serverId);
        Artifact artifact = installer.getArtifactForSelectedParameters(helper, groupId, artifactId, "default");
        File modules = new File(serverPath, SDKConstants.OPENMRS_SERVER_MODULES);
        File[] listOfModules = modules.listFiles();
        for (File mod : listOfModules) {
            if (mod.getName().startsWith(artifact.getArtifactId())) {
                boolean deleted = mod.delete();
                if (deleted) {
                    ServerConfig properties = ServerConfig.loadServerConfig(serverPath);
                    properties.removeFromValueList(SDKConstants.PROPERTY_USER_MODULES, artifact.getArtifactId());
                    properties.save();
                    getLog().info(String.format("Module with groupId: '%s', artifactId: '%s' was successfully removed from server.",
                            artifact.getGroupId(), artifact.getArtifactId()));
                    return;
                }
                else {
                    throw new MojoExecutionException(String.format("Error during removing Module with groupId: '%s', artifactId: '%s'.",
                            artifact.getGroupId(), artifact.getArtifactId()));
                }
            }
        }
        throw new MojoExecutionException(String.format("There no module with groupId: '%s', artifactId: '%s' on server.", artifact.getGroupId(), artifact.getArtifactId()));
    }
}
