package net.jeena.pipelinerdepot.stages.example

import com.daimler.pipeliner.Logger
import com.daimler.pipeliner.ScriptUtils
import hudson.EnvVars
import hudson.plugins.git.GitSCM
import com.cloudbees.groovy.cps.NonCPS

/**
 * Contains stages that can be reused across pipelines
 */

class CommonStages {
    def script
    def dockerImage
    Map env
    ScriptUtils utils
    
    /**
     * Constructor
     * @param script reference to the Jenkins scripted environment
     * @param env Map for Jenkins environment variables
     */
    CommonStages(script, Map env) {
        this.script = script
        this.utils = new ScriptUtils(script, env)
        this.env = env
    }

    /**
     * stageCheckout checks out the associated git repository inicializes it
     * and updates any submodules contained.
     *
     * @param env Map of Jenkins env variables
     * @param args String array of optional arguments for submodule update
     */
    def stageCheckout(Map env) {
        script.stage("Checkout") {
            /*script.checkout script.scm
            script.sh "git submodule update --init --recursive"*/
            utils.checkout()
        }
    }

    def stageCleanup() {
        script.cleanWs()
    }

    def stageBuildAndTestDocker() {

        if (utils.isDocker()) {
            Logger.info("Running inside Docker")
        } else {
            Logger.info("Running outside Docker")
        }

        script.stage("Build Docker") {
            dockerImage = script.docker.build("halo/yocto-image")
        }
        script.stage("Test Docker") {
            dockerImage.inside {
                script.sh 'echo "Tests passed"'

                if (utils.isDocker()) {
                    Logger.info("Running inside Docker")
                } else {
                    Logger.info("Running outside Docker")
                }
            }
        }

    }

}
