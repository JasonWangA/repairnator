package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.apache.maven.model.Plugin;

import java.util.Optional;
import java.util.Properties;

/**
 * Created by bloriot97 on 04/10/2018.
 * The idea is to run a maven build with checkstyle:checkstyle
 * to check for CS errors.
 */
public class Checkstyle extends AbstractStep {

    public Checkstyle(ProjectInspector inspector) {
        super(inspector, true);
    }

    public Checkstyle(ProjectInspector inspector, boolean blockingStep, String stepName) {
        super(inspector, blockingStep, stepName);
    }

    private Optional<Plugin> getCheckstylePlugin(){
        for ( Plugin plugin : this.getInspector().getJobStatus().getPlugins()){
            if ( plugin.getArtifactId().equals("maven-checkstyle-plugin") ){
                return Optional.of(plugin);
            }
        }
        return Optional.empty();
    }

    protected StepStatus businessExecute() {
        this.getLogger().debug("Run checkstyle on the project");

        Properties properties = new Properties();
        properties.setProperty(MavenHelper.SKIP_TEST_PROPERTY, "true");

        MavenHelper helper = new MavenHelper(this.getPom(), "checkstyle:checkstyle", properties, this.getClass().getSimpleName(), this.getInspector(), true, false);

        Optional<Plugin> checkstylePlugin = this.getCheckstylePlugin();

        if ( !checkstylePlugin.isPresent() ){
            this.addStepError("Repository " + this.getInspector().getRepoSlug() + " don't have maven-checkstyle-plugin.");
            return StepStatus.buildError(this, PipelineState.NO_CHECKSTYLE_FOUND);
        }

        int result;
        try {
            result = helper.run();
        } catch (InterruptedException e) {
            this.addStepError("Error while running checkstyle", e);
            result = MavenHelper.MAVEN_ERROR;
        }
        if (result == MavenHelper.MAVEN_SUCCESS) {
            return StepStatus.buildError(this, PipelineState.NOTFAILING);
        } else {
            this.addStepError("Repository " + this.getInspector().getRepoSlug() + " has (maybe ?) checkstyle errors.");
            return StepStatus.buildError(this, PipelineState.CHECKSTYLE_ERRORS);
        }
    }

}
