package org.openmrs.maven.plugins.utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

/**
 * Class for attribute helper functions
 */
public class AttributeHelper {
    private static final String EMPTY_STRING = "";
    private static final String NONE = "(none)";
    private static final String DEFAULT_SERVER_NAME = "server";
    private static final String DEFAULT_VALUE_TMPL = "Define value for property '%s'";
    private static final String DEFAULT_VALUE_TMPL_WITH_DEFAULT = "Define value for property '%s': (default: '%s')";
    private static final String DEFAULT_FAIL_MESSAGE = "Server with such serverId is not exists";
    private static final String INVALID_SERVER = "Invalid server Id";
    private static final String YESNO = " [Y/n]";

    private Prompter prompter;

    public AttributeHelper(Prompter prompter) {
        this.prompter = prompter;
    }

    /**
     * Prompt for serverId, and get default serverId which is not exists,
     * if serverId is not set before
     * @param omrsPath
     * @param serverId
     * @return
     * @throws PrompterException
     */
    public String promptForNewServerIfMissing(String omrsPath, String serverId) throws PrompterException {
        String defaultServerId = DEFAULT_SERVER_NAME;
        int indx = 0;
        while (new File(omrsPath, defaultServerId).exists()) {
            indx++;
            defaultServerId = DEFAULT_SERVER_NAME + String.valueOf(indx);
        }
        return promptForValueIfMissingWithDefault(serverId, "serverId", defaultServerId);
    }

    /**
     * Prompt for a value if it not set, and default value is set
     * @param value
     * @param parameterName
     * @param defValue
     * @return value
     * @throws PrompterException
     */
    public String promptForValueIfMissingWithDefault(String value, String parameterName, String defValue) throws PrompterException {
        if (value != null) return value;
        String textToShow = null;
        // check if there no default value
        if (defValue.equals(EMPTY_STRING)) textToShow = String.format(DEFAULT_VALUE_TMPL, parameterName);
        else textToShow = String.format(DEFAULT_VALUE_TMPL_WITH_DEFAULT, parameterName, defValue);
        String val = prompter.prompt(textToShow);
        if (val.equals(EMPTY_STRING)) val = defValue;
        return val;
    }

    /**
     * Prompt for a value with list of proposed values
     * @param value
     * @param parameterName
     * @param values
     * @return value
     * @throws PrompterException
     */
    public String promptForValueWithDefaultList(String value, String parameterName, List<String> values) throws PrompterException {
        if (value != null) return value;
        String defaultValue = values.size() > 0 ? values.get(0) : NONE;
        final String text = DEFAULT_VALUE_TMPL_WITH_DEFAULT + " (possible: %s)";
        String val = prompter.prompt(String.format(text, parameterName, defaultValue, StringUtils.join(values.toArray(), ", ")));
        if (val.equals(EMPTY_STRING)) val = defaultValue;
        return val;
    }

    /**
     * Prompt for a value if it not set, and default value is NOT set
     * @param value
     * @param parameterName
     * @return
     * @throws PrompterException
     */
    public String promptForValueIfMissing(String value, String parameterName) throws PrompterException {
        return promptForValueIfMissingWithDefault(value, parameterName, EMPTY_STRING);
    }

    /**
     * Print dialog Yes/No
     * @param text - text to display
     * @return
     */
    public boolean dialogYesNo(String text) throws PrompterException {
        String yesNo = prompter.prompt(text.concat(YESNO));
        return yesNo.equals("") || yesNo.toLowerCase().equals("y");
    }

    /**
     * Check if value is submit
     * @param value
     * @return
     */
    public boolean checkYes(String value) {
        String val = value.toLowerCase();
        return val.equals("true") || val.equals("yes");
    }

    /**
     * Get path to server by serverId and prompt if missing
     * @return
     * @throws MojoFailureException
     */
    public File getServerPath(String serverId, String failureMessage) throws MojoFailureException {
        File omrsHome = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        String resultServerId = null;
        try {
            List<String> servers = getListOf5RecentServers();
            resultServerId = promptForValueWithDefaultList(serverId, "serverId", servers);
        } catch (PrompterException e) {
            throw new MojoFailureException(e.getMessage());
        }
        if (resultServerId.equals(NONE)) {
            throw new MojoFailureException(INVALID_SERVER);
        }
        File serverPath = new File(omrsHome, resultServerId);
        if (!serverPath.exists()) {
            throw new MojoFailureException(failureMessage);
        }
        return serverPath;
    }

    /**
     * Check if we are currenly inside "server" folder and get path
     * @return
     */
    public File getCurrentServerPath() throws MojoExecutionException {
        File currentFolder = new File(System.getProperty("user.dir"));
        File openmrsHome = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        File current = new File(currentFolder, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        File parent = new File(currentFolder.getParent(), SDKConstants.OPENMRS_SERVER_PROPERTIES);
        File propertiesFile = null;
        if (current.exists()) propertiesFile = current;
        else if (parent.exists()) propertiesFile = parent;
        if (propertiesFile != null) {
            File server = propertiesFile.getParentFile();
            if (!server.getParentFile().equals(openmrsHome)) return null;
            ServerConfig properties = ServerConfig.loadServerConfig(server);
            if (properties.getParam(SDKConstants.PROPERTY_SERVER_ID) != null) return propertiesFile.getParentFile();
        }
        return null;
    }

    /**
     * Get server with default failure message
     * @param serverId
     * @return
     * @throws MojoFailureException
     */
    public File getServerPath(String serverId) throws MojoFailureException {
        return getServerPath(serverId, DEFAULT_FAIL_MESSAGE);
    }

    /**
     * Get 5 last modified servers
     * @return
     */
    public List<String> getListOf5RecentServers() {
        final int count = 5;
        String home = System.getProperty("user.home");
        File openMRS = new File(home, SDKConstants.OPENMRS_SERVER_PATH);
        Map<Long, String> sortedMap = new TreeMap<Long, String>(Collections.reverseOrder());
        File [] list = (openMRS.listFiles() == null) ? new File[0] : openMRS.listFiles();
        for (File f: list) {
            if (f.isDirectory()) sortedMap.put(f.lastModified(), f.getName());
        }
        int length = sortedMap.size() < count ? sortedMap.size() : count;
        return new ArrayList<String>(sortedMap.values()).subList(0, length);
    }
}
