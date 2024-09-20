package edu.taladro;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension;
import org.gradle.testing.jacoco.tasks.JacocoReport;
import org.gradle.testing.jacoco.tasks.rules.JacocoViolationRulesContainer;
import com.diffplug.gradle.spotless.SpotlessExtension;

import java.math.BigDecimal;

public class TaladroPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        configureJacoco(project);
        configureCheckstyle(project);
        configureSpotless(project);
    }

    private static void configureJacoco(Project project) {
        project.getPluginManager().apply("jacoco");

        project.getExtensions().configure(JacocoPluginExtension.class, jacoco -> {
            jacoco.setToolVersion("0.8.12");
        });

        project.getTasks().named("jacocoTestReport", JacocoReport.class, jacocoTestReport -> {
            jacocoTestReport.dependsOn(project.getTasks().named("test"));
            jacocoTestReport.getReports().getXml().getRequired().set(true);
            jacocoTestReport.getReports().getHtml().getRequired().set(true);
            jacocoTestReport.getReports().getHtml().getOutputLocation().set(project.file(project.getBuildDir() + "/jacocoHtml"));
            jacocoTestReport.doLast(task -> System.out.println("HTML report generated: " + jacocoTestReport.getReports().getHtml().getOutputLocation()));
        });

        project.getTasks().named("jacocoTestCoverageVerification", task -> {
            JacocoViolationRulesContainer violationRules = ((JacocoViolationRulesContainer) task);
            violationRules.rule(r -> {
                r.limit(l -> {
                    l.setCounter("LINE");
                    l.setMinimum(BigDecimal.valueOf(0.80));
                });
            });
        });
    }

    private static void configureCheckstyle(Project project) {
        project.getPluginManager().apply("checkstyle");
        project.getExtensions().configure(CheckstyleExtension.class, checkstyle -> {
            checkstyle.setToolVersion("10.3.3");
            checkstyle.setIgnoreFailures(false);
            checkstyle.setMaxWarnings(0);
            checkstyle.setConfigFile(project.file(project.getRootDir() + "/config/checkstyle/checkstyle.xml"));
        });
    }

    private static void configureSpotless(Project project) {
        project.getPluginManager().apply("com.diffplug.spotless");
        project.getExtensions().configure(SpotlessExtension.class, spotless -> {
            spotless.format("misc", format -> {
                format.target("**/*.java");
                format.targetExclude("**/build*/**");
                format.trimTrailingWhitespace();
                format.indentWithTabs();
                format.endWithNewline();
            });
        });
    }
}
