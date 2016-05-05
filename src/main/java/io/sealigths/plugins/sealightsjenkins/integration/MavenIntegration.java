package io.sealigths.plugins.sealightsjenkins.integration;

import io.sealigths.plugins.sealightsjenkins.TestingFramework;
import io.sealigths.plugins.sealightsjenkins.entities.FileBackupInfo;

import javax.xml.transform.TransformerException;
import java.io.PrintStream;


/**
 * Created by Nadav on 4/19/2016.
 */
public class MavenIntegration {
    //TODO: Check the SureFire version on the resolved (effective) *.pom file.
    //private final static String SUREFIRE_GROUP_ID = "org.apache.maven.plugins";
    //private final static String SUREFIRE_ARTIFACT_ID = "maven-surefire-plugin";
    private final static String SEALIGHTS_GROUP_ID = "io.sealights.on-premise.agents.plugin";
    private final static String SEALIGHTS_ARTIFACT_ID = "sealights-maven-plugin";


    private MavenIntegrationInfo mavenIntegrationInfo;
    private PrintStream log;

    public MavenIntegration(PrintStream log, MavenIntegrationInfo mavenIntegrationInfo) {
        this.log = log;
        this.mavenIntegrationInfo = mavenIntegrationInfo;
    }

    public void integrate() {
        this.integrate(true);
    }

    public void integrate(boolean shouldBackup) {

        log(log, "MavenIntegration.integrate - starting");

        for (FileBackupInfo fileBackupInfo : mavenIntegrationInfo.getPomFiles()) {
            log(log, "MavenIntegration.integrate - Modifying pom: " + fileBackupInfo.getSourceFile());

            String sourceFile = fileBackupInfo.getSourceFile();
            PomFile pomFile = new PomFile(sourceFile, log);

            try {
                if (pomFile.isPluginExistInEntriePom(SEALIGHTS_GROUP_ID, SEALIGHTS_ARTIFACT_ID)) {
                    log(log, "MavenIntegration.integrate - Skipping the integration since SeaLights plugin is already defined in the the POM file.");
                    return;
                }

//                TODO: Check the SureFire version on the resolved (effective) *.pom file.
//            if (!pomFile.isPluginExistInEntriePom(SUREFIRE_GROUP_ID, SUREFIRE_ARTIFACT_ID))
//            {
//                //Surefire plugin isn't defined.
//                throw new RuntimeException("SeaLights plugin requires Maven Surefire Plugin");
//            }
//
//            String version = pomFile.getPluginVersion(SUREFIRE_GROUP_ID, SUREFIRE_ARTIFACT_ID);
//            String[] tokens = version.split("\\.");
//            int majorVersion = Integer.parseInt(tokens[0]);
//            int minorVersion = Integer.parseInt(tokens[1]);
//            if ((majorVersion < 2) || (majorVersion == 2 && minorVersion < 9))
//            {
//                throw new RuntimeException("Unsupported Maven Surefire plugin. SeaLights requires a version 2.9 or higher.");
//            }

                if (shouldBackup)
                {
                    backupPom(sourceFile);
                }
                integrateToPomFile(fileBackupInfo, pomFile);
            } catch (Exception e) {
                log(log, "MavenIntegration.integrate - Unable to parse pom : " + sourceFile + ". Error:");
                e.printStackTrace(log);
            }
        }

    }

    private void backupPom(String sourceFile) {
        String backupFile = sourceFile + ".slbak";
        log(log, "MavenIntegration.integrate - created back up file: " + backupFile);
        this.savePom(backupFile);
    }

    private void integrateToPomFile(FileBackupInfo fileBackupInfo, PomFile pomFile) {
//        String profileId = info.getProfileId();

        integrateToAllProfiles(fileBackupInfo, pomFile);
        //TODO: Enable the profile integration once done + tested.
//        if (profileId == null || profileId.equals("")) {
//            integrateToAllProfiles();
//        } else {
//            integrateToProfile(profileId);
//        }
    }

    private String getEventListenerPackage(TestingFramework testingFramework) {
        if ("testng".equalsIgnoreCase(testingFramework.name())) {
            return "io.sealights.onpremise.agents.java.agent.integrations.testng.TestListener";
        } else if ("junit".equalsIgnoreCase(testingFramework.name())) {
            return "io.sealights.onpremise.agents.java.agent.integrations.junit.SlRunListener";
        }
        return "";
    }

    private void integrateToProfile(String profileId, PomFile pomFile, FileBackupInfo fileBackupInfo) {
//        List<String> profileIdfiles = pomFile.getProfileIds();
        if (profileId.length() == 0) {
            throw new RuntimeException("The specified POM file does not contain any profiles.");
        }

//        if (!profiles.contains(profileId))
//        {
//            throw new RuntimeException("The specified POM file does not contain a profile with id of '" + profileId + "'.");
//        }

        TestingFramework testingFramework = mavenIntegrationInfo.getTestingFramework();
        SeaLightsPluginInfo seaLightsPluginInfo = this.mavenIntegrationInfo.getSeaLightsPluginInfo();
        String xml = toPluginText(seaLightsPluginInfo, testingFramework);
        pomFile.addPlugin(xml);

        String testingFrameworkListeners = getEventListenerPackage(testingFramework);
        String apiAgentPath = mavenIntegrationInfo.getSeaLightsPluginInfo().getApiJar();

        if (testingFramework.equals(TestingFramework.AUTO_DETECT)) {
            testingFrameworkListeners = null;   //Used to pass control to the maven plugin.
        }

        pomFile.updateSurefirePlugin(testingFrameworkListeners, apiAgentPath);
        savePom(fileBackupInfo, pomFile);

    }

    private void integrateToAllProfiles(FileBackupInfo fileBackupInfo, PomFile pomFile) {
        SeaLightsPluginInfo seaLightsPluginInfo = this.mavenIntegrationInfo.getSeaLightsPluginInfo();
        TestingFramework testingFramework = mavenIntegrationInfo.getTestingFramework();
        String xml = toPluginText(seaLightsPluginInfo, testingFramework);
        pomFile.addPlugin(xml);

        String testingFrameworkListeners = getEventListenerPackage(testingFramework);
        String apiAgentPath = mavenIntegrationInfo.getSeaLightsPluginInfo().getApiJar();

        if (testingFramework.equals(TestingFramework.AUTO_DETECT)) {
            testingFrameworkListeners = null; //Used to pass control to the maven plugin.
        }

        pomFile.updateSurefirePlugin(testingFrameworkListeners, apiAgentPath);

        savePom(fileBackupInfo, pomFile);
    }


    private void savePom(String filename) {
        savePom(filename, new PomFile(filename, this.log));
    }

    private void savePom(FileBackupInfo fileBackupInfo, PomFile pomFile) {
        String targetFile = fileBackupInfo.getTargetFile();
        if (targetFile == null || targetFile.equals("")) {
            targetFile = fileBackupInfo.getSourceFile();
        }
        savePom(targetFile, pomFile);
    }

    private void savePom(String filename, PomFile pomFile) {
        try {
            pomFile.save(filename);
        } catch (TransformerException e) {
            log.println("Failed saving POM file. Error:");
            e.printStackTrace(this.log);
        }
    }

    private void log(PrintStream logger, String message) {
        message = "[SeaLights Jenkins Plugin] " + message;
        logger.println(message);
    }

    public String toPluginText(SeaLightsPluginInfo pluginInfo, TestingFramework testingFramework) {

        StringBuilder plugin = new StringBuilder();
        plugin.append("<groupId>io.sealights.on-premise.agents.plugin</groupId>");
        plugin.append("<artifactId>sealights-maven-plugin</artifactId>");
        plugin.append("<version>1.0.0</version>");
        plugin.append("<configuration>");

        if (!pluginInfo.isEnabled()) {
            plugin.append("<enable>false</enable>");
        }

        tryAppendValue(plugin, pluginInfo.getCustomerId(), "customerid");
        tryAppendValue(plugin, pluginInfo.getServerUrl(), "server");
        tryAppendValue(plugin, pluginInfo.getProxy(), "proxy");


        String appName = pluginInfo.getAppName();
        if ("Build Per Module".equalsIgnoreCase(pluginInfo.getBuildStrategy().getDisplayName())) {
            appName = "[" + pluginInfo.getAppName() + "] - " + pluginInfo.getModuleName();
        }

        tryAppendValue(plugin, appName, "appName");
        tryAppendValue(plugin, pluginInfo.getModuleName(), "moduleName");
        tryAppendValue(plugin, pluginInfo.getWorkspacepath(), "workspacepath");
        tryAppendValue(plugin, pluginInfo.getBuildName(), "build");
        tryAppendValue(plugin, pluginInfo.getBranchName(), "branch");
        tryAppendValue(plugin, pluginInfo.getPackagesIncluded(), "packagesincluded");

        if (!isNullOrEmpty(pluginInfo.getPackagesExcluded())) {
            plugin.append("<packagesexcluded>*FastClassByGuice*, *ByCGLIB*, *EnhancerByMockitoWithCGLIB*, *EnhancerBySpringCGLIB*, " + pluginInfo.getPackagesExcluded() + "</packagesexcluded>");
        }

        tryAppendValue(plugin, pluginInfo.getFilesIncluded(), "filesincluded");
        tryAppendValue(plugin, pluginInfo.getApiJar(), "apiJar");
        tryAppendValue(plugin, pluginInfo.getScannerJar(), "buildScannerJar");
        tryAppendValue(plugin, pluginInfo.getListenerJar(), "testListenerJar");
        tryAppendValue(plugin, pluginInfo.getListenerConfigFile(), "testListenerConfigFile");

        if (!pluginInfo.isRecursive()) {
            plugin.append("<recursive>false</recursive>");
        }

        if (pluginInfo.isLogEnabled()) {
            plugin.append("<logEnabled>true</logEnabled>");
        }
        String logLevel = pluginInfo.getLogLevel().name();
        tryAppendValue(plugin, logLevel, "logLevel");

        if (pluginInfo.getLogDestination() != null && "file".equalsIgnoreCase(pluginInfo.getLogDestination().name())) {
            plugin.append("<logToFile>true</logToFile>");
        }

        tryAppendValue(plugin, pluginInfo.getLogFolder(), "logFolder");
        plugin.append("<enableTestListenerInitialization>" + testingFramework.equals(TestingFramework.AUTO_DETECT) + "</enableTestListenerInitialization>");


        plugin.append("</configuration>");
        plugin.append("<executions>");
        appendExecution(plugin, "a1", "build-scanner");
        appendExecution(plugin, "a2", "test-listener");
        appendExecution(plugin, "a3", "initialize-test-listener");
        plugin.append("</executions>");


        //        if(!isNullOrEmpty(environment)){
//            plugin.append("<environment>" + environment + "</environment>");
//        }

//        if(!isNullOrEmpty(filesExcluded)){
//            plugin.append("<filesexcluded>" + filesExcluded + "</filesexcluded>");
//        }

        return plugin.toString();
    }

    private void appendExecution(StringBuilder stringBuilder, String executionId, String goal) {
        stringBuilder.append("<execution>");
        stringBuilder.append("<id>" + executionId + "</id>");
        stringBuilder.append("<goals>");
        stringBuilder.append("<goal>" + goal + "</goal>");
        stringBuilder.append("</goals>");
        stringBuilder.append("</execution>");
    }

    private void tryAppendValue(StringBuilder stringBuilder, String value, String elementName) {
        if (!isNullOrEmpty(value)) {
            stringBuilder.append("<" + elementName + ">");
            stringBuilder.append(value);
            stringBuilder.append("</" + elementName + ">");
        }
    }

    private boolean isNullOrEmpty(String str) {
        if (str == null || "".equals(str))
            return true;
        return false;
    }

}