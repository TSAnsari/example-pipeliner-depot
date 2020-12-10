package net.jeena.pipelinerdepot.pipelines.example

import com.daimler.pipeliner.Logger
import com.daimler.pipeliner.BasePipeline
import net.jeena.pipelinerdepot.stages.example.CommonStages
import net.jeena.pipelinerdepot.stages.example.ComponentStages

/**
 * ComponentPipeline is a generic pipeline to build CMake projects
 * and run tests written in gtest, assuming the project has a
 * standard structure with CMakelists.txt at the root level.
 * <p>Paremeters which can be used to configure the pipeline from a
 * merge request or environmental variables are:
 * <ul>
 *  <li>
 *   <strong>runUnitTests</strong><br>
 *   Flag to enable the running of unit tests<br>
 *   example values: true, false<br>
 *   Default: false
 *  <li>
 *   <strong>cmakeArgs</strong><br>
 *   Set custom cmake arguments<br>
 *   example values: -DFOO=foo -DBAR=bam<br>
 */

class ComponentPipeline extends BasePipeline {
    CommonStages commonStages
    ComponentStages componentStages
    
    /**
     * Constructor
     * 
     * @param script Reference to the jenkins script environment
     * @param message Git merge request message
     * @param env Environment variables
     * @param stageOverrides map for user defined custom stages in Jenkinsfile
     * @param ioMap map which is used to pass parameters between pipelines
     */
    ComponentPipeline(script, String message, Map env, Map stageOverrides, Map ioMap) {
        super(script, [
            // Default values for inputs for the pipeline, can be overwritten
            // be user inputs from eithen MR message or Jenkins env
            defaultInputs: '''
                runUnitTests = false
                cmakeArgs =
            ''',
            // Keys exposed to the user for modification
            exposed: [
                'runUnitTests',
                'cmakeArgs'
            ],
            // Keys for which pipeline should be parallelized
            parallel: []
        ] as Map, message, env, ioMap)

        commonStages = new CommonStages(script, env);
        componentStages = new ComponentStages(script, env);
    }

    @Override
    void stages(Map stageInput) {

        String buildDir = "build"

        commonStages.stageCheckout(env);

        commonStages.stageBuildAndTestDocker();

        componentStages.stageBuild(".", buildDir, stageInput['cmakeArgs'], commonStages.dockerImage);

        Logger.info("JEENA: " + stageInput.inspect())

        if (stageInput['rununittests'] && stageInput['rununittests'].equalsIgnoreCase('true')) {
            componentStages.stageUnitTests(buildDir, commonStages.dockerImage);
        }
    }

    @Override
    void postParallel(Map stageInput) {
        commonStages.stageCleanup();
    }
}
