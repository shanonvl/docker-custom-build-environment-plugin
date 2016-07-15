package com.cloudbees.jenkins.plugins.docker_build_env;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.Shell;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerEndpoint;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SingleFileSCM;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertThat;


/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class FunctionalTests {

    @Rule  // @ClassRule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void run_inside_pulled_container() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();

        project.getBuildWrappersList().add(
                new DockerBuildWrapper(
                        new PullDockerImageSelector("ubuntu:14.04"),
                        "", new DockerServerEndpoint("", ""), "", true, false, Collections.<Volume>emptyList(), null, "cat", false, "bridge", null, null)
        );
        project.getBuildersList().add(new Shell("lsb_release  -a"));

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        String s = FileUtils.readFileToString(build.getLogFile());
        assertThat(s, containsString("Ubuntu 14.04"));
        jenkins.buildAndAssertSuccess(project);
    }

    @Test
    public void run_inside_built_container() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new SingleFileSCM("Dockerfile", IOUtils.toString(getClass().getResourceAsStream("/Dockerfile"), "UTF-8")));

        project.getBuildWrappersList().add(
                new DockerBuildWrapper(
                        new DockerfileImageSelector(".", "$WORKSPACE/Dockerfile"),
                        "", new DockerServerEndpoint("", ""), "", true, false, Collections.<Volume>emptyList(), null, "cat", false, "bridge", null, null)
        );
        project.getBuildersList().add(new Shell("lsb_release  -a"));

        FreeStyleBuild build = project.scheduleBuild2(0).get();
        jenkins.assertBuildStatus(Result.SUCCESS, build);
        String s = FileUtils.readFileToString(build.getLogFile());
        assertThat(s, containsString("Ubuntu 14.04"));
        jenkins.buildAndAssertSuccess(project);
    }

    @Test
    public void bind_mount_replaces_existing_mount() throws Exception {
        BuiltInContainer container = new BuiltInContainer(null); // don't need docker arg for this test.
        Map<String, String> volumes = container.getVolumes();
        assertThat(volumes.size(), equalTo(0));
        container.bindMount("/var/jenkins_home");
        container.bindMount("/srv/docker/jenkins/var/jenkins_home", "/var/jenkins_home");
        assertThat(volumes.size(), equalTo(1));
        assertThat(volumes.get("/var/jenkins_home"), isEmptyOrNullString());
        assertThat(volumes.get("/srv/docker/jenkins/var/jenkins_home"), equalTo("/var/jenkins_home"));
    }

}
