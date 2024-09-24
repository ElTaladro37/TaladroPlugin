package edu.taladro;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class GitHookTask extends DefaultTask {

    @TaskAction
    public void yourTask() throws Exception {
        System.out.println("Copying pre-commit hook");

        File gitHooksDir = new File(getProject().getRootDir(), ".git/hooks");
        if (!gitHooksDir.exists()) {
            throw new GradleException(".git/hooks directory not found. Is this a Git repository?");
        }
        try (InputStream preCommitStream = getClass().getClassLoader().getResourceAsStream("pre-commit")) {
            if (preCommitStream == null) {
                throw new GradleException("Pre-commit hook file not found in plugin resources.");
            }
            File preCommitFile = new File(gitHooksDir, "pre-commit");
            Files.copy(preCommitStream, preCommitFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            String os = System.getProperty("os.name").toLowerCase();

            if (!os.contains("windows")) {
                getProject().exec(execSpec -> {
                    execSpec.commandLine("chmod", "+x", preCommitFile.getAbsolutePath());
                }).assertNormalExitValue().rethrowFailure();
            }

            System.out.println("Pre-commit hook installed successfully.");
        } catch (Exception e) {
            throw new GradleException("Failed to copy pre-commit hook", e);
        }
    }
}
