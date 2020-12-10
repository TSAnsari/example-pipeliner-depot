package net.jeena.pipelinerdepot.stages.example

import com.daimler.pipeliner.Logger
import com.daimler.pipeliner.ScriptUtils

/**
 * Contains stages that are used by the ComponentPipeline
 */

class ComponentStages {
    def script
    Map env
    ScriptUtils utils
    
    /**
     * Constructor
     * @param script reference to the Jenkins scripted environment
     * @param env Map for Jenkins environment variables
     */
    ComponentStages(script, Map env) {
        this.script = script
        this.utils = new ScriptUtils(script, env)
        this.env = env
    }

    /**
     * stageCheckout checks out the associated git repository inicializes it
     * and updates any submodules contained.
     *
     * @param sourceDir String path to the directory containing sources
     * @param buildDir String path to the build directory
     * @param cmakeArgs String additional cmake arguments
     */
    def stageBuild(String sourceDir, String buildDir, String cmakeArgs, def dockerImage) {
        script.stage("Build") {
        dockerImage.inside {

            if (utils.isDocker()) {
                Logger.info("Running inside Docker in component stage")
            } else {
                Logger.info("Running outside Docker in component stage")
            }

            script.sh "cmake" + " -H" + sourceDir + " -B" + buildDir +
                " -DCMAKE_BUILD_TYPE=Debug " + (cmakeArgs ? cmakeArgs : "")
            script.sh "cmake --build ${buildDir} -- -j\$(nproc)"
        }
        }
    }

    /**
     * Runs the unit tests and reports the result
     *
     * @param 
     */
    def stageUnitTests(String buildDir, def dockerImage) {
        script.stage("Unit tests") {
            dockerImage.inside {

                if (utils.isDocker()) {
                    Logger.info("Running inside Docker in component stage")
                } else {
                    Logger.info("Running outside Docker in component stage")
                }

                String env = "GTEST_OUTPUT=\"xml:`pwd`/${buildDir}/unit-test/\" CTEST_OUTPUT_ON_FAILURE=1"
                script.sh "${env} cmake --build ${buildDir} --target unit-test"
                script.junit "${buildDir}/unit-test/*.xml"
            }
        }
    }
}
