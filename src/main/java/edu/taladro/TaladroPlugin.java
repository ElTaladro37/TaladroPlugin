package edu.taladro;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification;
import org.gradle.testing.jacoco.tasks.JacocoReport;
import com.diffplug.gradle.spotless.SpotlessExtension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class TaladroPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        configureJacoco(project);
        configureCheckstyle(project);
        configureSpotless(project);

        TaskProvider<GitHookTask> gitHookTask = project.getTasks().register("addPreCommitGitHook", GitHookTask.class);

        project.getTasks().named("build").configure(buildTask -> buildTask.dependsOn(gitHookTask));

    }

    private static void configureJacoco(Project project) {
        project.getPluginManager().apply("jacoco");

        project.getExtensions().configure(JacocoPluginExtension.class, jacoco -> jacoco.setToolVersion("0.8.12"));

        project.getTasks().named("jacocoTestReport", JacocoReport.class, jacocoTestReport -> {
            jacocoTestReport.dependsOn(project.getTasks().named("test"));
            jacocoTestReport.getReports().getXml().getRequired().set(true);
            jacocoTestReport.getReports().getHtml().getRequired().set(true);
            jacocoTestReport.getReports().getHtml().getOutputLocation().set(project.file(project.getBuildDir() + "/jacocoHtml"));
            jacocoTestReport.doLast(task -> System.out.println("HTML report generated: " + jacocoTestReport.getReports().getHtml().getOutputLocation()));
        });

        JacocoCoverageVerification verificationTask = (JacocoCoverageVerification) project.getTasks().getByName("jacocoTestCoverageVerification");


        verificationTask.getViolationRules().rule(rule -> {
            rule.limit(limit -> {
                limit.setCounter("LINE");
                limit.setMinimum(BigDecimal.valueOf(0.80));
            });
        });

        project.getTasks().named("check").configure(task -> task.dependsOn(verificationTask));


    }

    private static void configureCheckstyle(Project project) {
        project.getPluginManager().apply("checkstyle");
        project.getExtensions().configure(CheckstyleExtension.class, checkstyle -> {
            checkstyle.setToolVersion("10.3.3");
            checkstyle.setIgnoreFailures(false);
            checkstyle.setMaxWarnings(0);
            try {
                InputStream configStream = TaladroPlugin.class.getClassLoader().getResourceAsStream("checkstyle.xml");
                if (configStream != null) {
                    File tempConfigFile = File.createTempFile("checkstyle", ".xml");
                    Files.copy(configStream, tempConfigFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    checkstyle.setConfigFile(tempConfigFile);
                } else {
                    throw new RuntimeException("No se pudo cargar el archivo de configuración predeterminado");
                }
            } catch (IOException e) {
                throw new RuntimeException("Error al copiar el archivo de configuración predeterminado", e);
            }        });
    }

    private static void configureSpotless(Project project) {
        project.getPluginManager().apply("com.diffplug.spotless");
        project.getExtensions().configure(SpotlessExtension.class, spotless -> {
            spotless.java(format -> {
                format.target("**/*.java");
                format.targetExclude("**/build*/**");
                format.googleJavaFormat();
                format.indentWithTabs(2);
                format.indentWithSpaces(4);
            });
        });
    }
}
