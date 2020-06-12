#!/usr/bin/env groovy
/*
    Build the Android GoVerifyID release files

    job params:
        BUILD_LABEL_SUFFIX     default value: <blank>
            The version label suffix for this build of GoVerifyID
                String parameter

        BRANCH_OR_TAG              default value: master
            The Git branch to checkout and build
                String parameter

*/


//Library includes:
//noinspection GroovyUnusedAssignment
@Library(['globalFunctions']) _  //'beFunctions'


//Jenkins boolean parameters conversion to actual booleans
PRODUCTION_BUILD=PRODUCTION_BUILD.toBoolean()
RUN_FIREBASE_ROBO_TEST=false
RUN_FIREBASE_INTEGRATION_TEST=false

//Global constants:
GIT_REPO = "jumpstart_sample_android"
ANDROID_MODULE_DIR = 'app'
ANDROID_BUILD_TOOLS_VERSION = '29.0.2'
//noinspection GroovyUnusedAssignment
MINS_SLEEP_BEFORE_VM_AUTODESTRUCTION_ON_FAILURE = 90
//noinspection GroovyUnusedAssignment
MINS_SLEEP_BEFORE_VM_AUTODESTRUCTION_ON_SUCCESS = 30
MINS_SLEEP_TO_WAIT_FOR_ANDROID_EMU = 15
SECONDS_SLEEP_BETWEEN_ANDROID_EMU_BOOTUP_CHECKS = 15
VM_ADDITIONAL_SWAPFILE_SIZE='8G'
JENKINS_GITHUB_CREDENTIALS_ID = 'IWSGithub'
BASH_SHEBANG = '#!/usr/bin/env bash'
//noinspection SpellCheckingInspection
SLACK_CHANNEL_NAME = "android-dev-botfeed"
SLACK_COLOR_MAP = ['SUCCESS' : 'good',
                   'FAILURE' : 'danger',
                   'UNSTABLE': 'warning',
                   'ABORTED' : 'warning',
                   'STARTED' : '#1f56ec']
ANDROID_SDK_FOLDER = 'android-sdk'
VAGRANT_HOME = '/home/vagrant'
//noinspection SpellCheckingInspection
JENKINS_LOGIN_URL_SUFFIX = 'view/GoVerifyID/'
LINT_SCAN_HTML_FILE = "lint-results.html"

//noinspection SpellCheckingInspection
MEMORY_CHECK_CMD = 'free --human --lohi --total --wide ; df -h'
GRADLE_TIMEOUT_OPTIONS="-Dorg.gradle.internal.http.socketTimeout=60000 -Dorg.gradle.internal.http.connectionTimeout=60000 "
//noinspection SpellCheckingInspection
ARTIFACTORY_SERVER_HOST = 'artifactory.iwsinc.com:8442'
//noinspection SpellCheckingInspection
ARTIFACTORY_DEPLOY_BASE_PATH = "ArtifactoryFiles/GMI/Applications"
JAVA_HOME = '/usr/lib/jvm/java-8-openjdk-amd64/jre'
ANDROID_SDK_TOOLS_FILENAME = 'sdk-tools-linux-4333796.zip'  //Updates here https://developer.android.com/studio/


//Job-time-constructed constants:
//noinspection GroovyUnusedAssignment,SpellCheckingInspection
ANDROID_INTEGRATION_TEST_SMALL_CMD = 'fastlane smalltests'
//noinspection GroovyUnusedAssignment,SpellCheckingInspection
ANDROID_INTEGRATION_TEST_CMD = 'fastlane alltests'

PROJECT_DISPLAY_TITLE = "${GIT_REPO}"
VM_NAME = "${JOB_NAME}-${BUILD_NUMBER}-Centos7"
HOME_VAGRANT_ANDROID = "${VAGRANT_HOME}/.android"
VERSION_NAME_FOLDER = "${GIT_REPO}/${ANDROID_MODULE_DIR}"//  /${ANDROID_MODULE_DIR}"
VERSION_NAME_FILE = "build.gradle"
VERSION_NAME_PREFIX = 'versionName \"'
BUILD_ARTIFACT_OUTPUT_DIR = "${ANDROID_MODULE_DIR}/build/outputs/apk"
LINT_SCAN_HTML_LOCATION = "${ANDROID_MODULE_DIR}/build/reports/${LINT_SCAN_HTML_FILE}"
TEST_REPORT_FOLDER = "${ANDROID_MODULE_DIR}/build/reports"


//Global fields:
vmIpAddr = ""
changeLogExtract = ""
artifactoryDeployedUrl = ""
artifactoryDirectDownloadUrls = ""
verNum = ""
buildLabel = ""
testMessage = ""
artifactoryDirectLintScanHtmlUrl = ""
htmlReportsUrl = ""
lintScanOutputFilename = ""
artifactoryDirectDownloadFlavorZipUrl = ""
reportArchiveZipUrl = ""
flavorZipFilename = ""
JAVA_HOME = ""
absoluteTestReportFolder = ''


jenkinsHost = ''

ARTIFACTORY_DEPLOY_DEV_PATH_OPTIONAL_SUFFIX = ""
//noinspection SpellCheckingInspection
ARTIFACTORY_BASE_DEPLOYED_URL = ""
ARTIFACTORY_DEPLOY_PATH = ""



//Global shell commands:
java = "java"
//noinspection SpellCheckingInspection
jarsigner = "jarsigner"

ADB = ""
sdkManager = ""
avdManager = ""
avdEmulator = ""

//Functions:

@SuppressWarnings("GrReassignedInClosureLocalVar")
def stageSlackStart() {
    testMessage = "Notifying Slack of job start..."
    timeout(time: 2, unit: 'MINUTES') {
        stage('SlackStart') {
            echo testMessage

            echo "Extracting Jenkins host from job URL ${JOB_URL}..."
            jenkinsHost = getHostFromUrl(JOB_URL)
            echo "jenkinsHost retrieved: " + jenkinsHost

            slackMessage = "*JENKINS PIPELINE GMI ${ANDROID_MODULE_DIR} BUILD JOB STARTED*: " +
                    "<$jenkinsHost|Login> - <$JOB_URL|$JOB_NAME> <$currentBuild.absoluteUrl|build #$currentBuild.number> suffix: ${BUILD_LABEL_SUFFIX} prod: ${PRODUCTION_BUILD}\n" +
                    "Git repo: ${GIT_REPO}, - Branch / Tag: ${BRANCH_OR_TAG}, FBRobo: ${RUN_FIREBASE_ROBO_TEST}, FBInt: ${RUN_FIREBASE_INTEGRATION_TEST}"
            echo "slackMessage to send:\n${slackMessage}"
            slackSend message: slackMessage, color: SLACK_COLOR_MAP['STARTED'], channel: "${SLACK_CHANNEL_NAME}"
        }
    }
}

def stageVmMake() {
    testMessage = "Create the VM"
    timeout(time: 15, unit: 'MINUTES') {
        stage('VmMake') {
            echo testMessage
//            createCentos7Vm label: "${VM_NAME}"
            createCentos7Vm label: "${VM_NAME}"
        }
    }
}

@SuppressWarnings("SpellCheckingInspection")
def vmAddSwapFile(SWAP_FILE_SIZE) {
    echo "Configuring ${SWAP_FILE_SIZE} swapfile..."
    sh """${BASH_SHEBANG}
${MEMORY_CHECK_CMD}
sudo fallocate -l ${SWAP_FILE_SIZE} /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
${MEMORY_CHECK_CMD}
"""
}

def stageVmSwapCfg() {
    testMessage = "Configure the VM and swap"
    timeout(time: 15, unit: 'MINUTES') {
        stage('VmCfg') {
            echo testMessage
            vmAddSwapFile(VM_ADDITIONAL_SWAPFILE_SIZE)

            echo "Boolean parameters from Jenkins:"
            echo "PRODUCTION_BUILD=$PRODUCTION_BUILD"

            echo "Checking env before making changes..."
            sh "env"

            if (PRODUCTION_BUILD) {
                echo "Production build indicated, configuring Sonar and Artifactory..."
                ARTIFACTORY_DEPLOY_DEV_PATH_OPTIONAL_SUFFIX = ""    //Leave blank to deploy to a release folder
            } else {
                echo "Development build indicated, configuring Sonar and Artifactory..."
                ARTIFACTORY_DEPLOY_DEV_PATH_OPTIONAL_SUFFIX = "/dev"    //Leave blank to deploy to a release folder
            }

            //noinspection SpellCheckingInspection
            ARTIFACTORY_BASE_DEPLOYED_URL = "https://${ARTIFACTORY_SERVER_HOST}/artifactory/webapp/#/artifacts/browse/tree/General/GoVerifyID/Android/Applications"
            ARTIFACTORY_DEPLOY_PATH = "${ARTIFACTORY_DEPLOY_BASE_PATH}/${GIT_REPO}${ARTIFACTORY_DEPLOY_DEV_PATH_OPTIONAL_SUFFIX}"

            absoluteTestReportFolder = "${WORKSPACE}/${GIT_REPO}/${TEST_REPORT_FOLDER}"

            ADB = "${WORKSPACE}/${ANDROID_SDK_FOLDER}/platform-tools/adb"
            sdkManager = "${WORKSPACE}/${ANDROID_SDK_FOLDER}/tools/bin/sdkmanager"

            jenkinsHost = getHostFromUrl(JOB_URL)
            echo "jenkinsHost retrieved: " + jenkinsHost

            echo "Checking current user"
            sh "whoami"

            echo 'Checking where tilde ~ is mapped: ' + "~"

            echo "Change home folder to writeable"
            sh "sudo chmod u+w ~"    //Ensuring home folder writable

            echo "Change owner of home folder to current user (prob vagrant)"
            sh "sudo chown -R `whoami` ~"    //Ensuring home folder owned by meeee

            sh 'printenv || echo command failed'

            configEnv()
            configBashEnv()

            echo 'Checking where tilde ~ is mapped: ' + "~"

            showMem()
        }
    }
}


@SuppressWarnings("SpellCheckingInspection")
def stageTestSyntax() {
    testMessage = 'Testing out important shell syntax nuances on the VM before proceeding...'
    stage('VmTest') {
        echo testMessage

        echo "Testing string substitution to extract a character"
        STRING_SUBSTITUTION_TEST_CMD = 'IND=0 ; ' +
                'EMU_BOOT_COMPLETED=2345 ; ' +
                'while [ $IND -le ' + (MINS_SLEEP_TO_WAIT_FOR_ANDROID_EMU * 60) + ' ] ' +
                '&& [ \"${EMU_BOOT_COMPLETED:0:1}\" != \"1\" ] ; ' +
                'do EMU_BOOT_COMPLETED=12345 ; ' +
                'IND=$(( $IND + ' + SECONDS_SLEEP_BETWEEN_ANDROID_EMU_BOOTUP_CHECKS + ' )) ; ' +
                'echo \"Seconds passed: $IND, EMU_BOOT_COMPLETED=$EMU_BOOT_COMPLETED\" ; done'
        sh """${BASH_SHEBANG}
echo 'Showing which shell we are using, bash hopefully not dash'
readlink -f \$(which sh)
${STRING_SUBSTITUTION_TEST_CMD}
"""

        echo "Testing host retrieval from job URL: ${JOB_URL}"
        jenkinsHost = getHostFromUrl(JOB_URL)
        echo "jenkinsHost retrieved: " + jenkinsHost

        echo "Testing file name listing into variable array..."
        def testFileName = "testfile.txt"
        sh "touch ${testFileName}"
        sh "echo 'Hello I am test' >> ${testFileName}"
        sh "cat ${testFileName}"
        sh 'cp ' + testFileName + ' "test file 2.txt"'
        def testEmptyFolderName = "testemptyfolder"
        sh "ls -l"
        def  FILES_LIST = sh (script: "ls .", returnStdout: true).trim()
        echo "FILES_LIST : ${FILES_LIST}"
        for(String ele : FILES_LIST.split("\\r?\\n")){
            if (ele.trim().length() > 0) {
                println ">>>${ele}<<<"
                sh "cat '${ele}'"
            }
        }

        echo "Testing with empty folder..."
        sh 'mkdir ' + testEmptyFolderName
        sh "ls -l"
        FILES_LIST = sh (script: "ls ${testEmptyFolderName}", returnStdout: true).trim()
        echo "FILES_LIST : ${FILES_LIST}"
        for(String ele : FILES_LIST.split("\\r?\\n")){
            if (ele.trim().length() > 0) {
                println ">>>${ele}<<<"
                sh "cat '${ele}'"
            }
        }
    }
}

@SuppressWarnings("SpellCheckingInspection")
def stageInstallPackages() {
    testMessage = "Install required packages..."
    timeout(time: 10, unit: 'MINUTES') {
        stage('PkgInst') {
            echo testMessage
            sh "sudo rm -f /var/cache/yum/x86_64/7/timedhosts.txt"
            sh "sudo yum -y install epel-release"   //Must install epel-release repo before jq, cannot be in same command - https://stackoverflow.com/a/45745410/3686125
            sh 'sudo yum -y install zip unzip nano jq' //mesa-libGL pulseaudio expect cifs-utils pulseaudio java-1.8.0-openjdk-devel
            sh 'jq --version || true'
            javaCfg()

            echo "Checking env after changes..."
            sh "env"

            showMem()
        }
    }
}

@SuppressWarnings(["SpellCheckingInspection", "GrReassignedInClosureLocalVar"])
def javaCfg() {
//                    def JAVA_HOME = sh(script: 'sudo update-alternatives --config java | grep current | sed \'s/link currently points to //\' | sed \'s|/bin/java||\'', returnStdout: true).toString().trim()

    //In Ubuntu, this provides the following output when one java is installed:
    //There is only one alternative in link group java (providing /usr/bin/java): /usr/lib/jvm/java-7-openjdk-amd64/jre/bin/java
    //Nothing to configure.

    JAVA_HOME = sh(script: 'sudo update-alternatives --config java | grep \'(providing /usr/bin/java): \'  | sed \'s/There is only one alternative in link group java (providing \\/usr\\/bin\\/java): //\' | sed \'s|/bin/java||\'', returnStdout: true).toString().trim()
    echo "JAVA_HOME is now set to ${JAVA_HOME}"

    echo "Exporting java home to bash environment..."
    env.JAVA_HOME = JAVA_HOME

    sh 'env'

    sh 'printenv'

    echo "Checking java version..."
    sh "${java} -version"   // || echo command failed

    jarsigner = sh(script: 'locate bin/jarsigner | head -n 1', returnStdout: true).toString().trim()
    echo "jarsigner is now ${jarsigner}"

    echo "testing out jarsigner binary at ${jarsigner} ..."
    sh "${jarsigner}"   //|| echo command failed
}

@SuppressWarnings("SpellCheckingInspection")
def stageInstallAndroidSdk() {
    testMessage = "Installing Android SDK via direct zip download..."
    timeout(time: 120, unit: 'MINUTES') {
        stage('AndSdkInst') {
            echo testMessage
            sh "mkdir -p ${ANDROID_SDK_FOLDER}"
            dir("${ANDROID_SDK_FOLDER}") {
                echo 'Look here (bottom of the page) for an updated zip file in case this one gets too outdated:' + ANDROID_SDK_TOOLS_FILENAME
                echo 'https://developer.android.com/studio/'
                sh 'wget --output-document=android-sdk.zip https://dl.google.com/android/repository/' + ANDROID_SDK_TOOLS_FILENAME
                sh 'unzip android-sdk.zip'
                sh 'rm -f android-sdk.zip'
            }
            sh 'echo ANDROID_SDK_ROOT is now: $ANDROID_SDK_ROOT'
            echo "Attempting to run ${sdkManager}..."
            sh "${sdkManager}"
            showMem()
        }
    }
}

def stageInstallAndroidApis() {
    testMessage = "Install Android APIs..."
    timeout(time: 30, unit: 'MINUTES') {
        stage('AndApiInst') {
            echo testMessage
            sh "yes | ${sdkManager} --licenses"
            sh "yes | ${sdkManager} --install \"build-tools;${ANDROID_BUILD_TOOLS_VERSION}\" \"tools\" \"platform-tools\" "    //\"extras;google;m2repository\""
            showMem()
        }
    }
}



@SuppressWarnings("GrReassignedInClosureLocalVar")
def stageGitCheckout() {
    testMessage = "Checkout ${GIT_REPO} from github..."
    timeout(time: 5, unit: 'MINUTES') {
        stage('GetGit') {
            echo testMessage

            checkout([$class                           : 'GitSCM',
                      branches                         : [[name: "${BRANCH_OR_TAG}"]],
                      doGenerateSubmoduleConfigurations: false,
                      extensions                       : [[$class: 'RelativeTargetDirectory', relativeTargetDir: GIT_REPO]],
                      gitTool                          : 'Default', submoduleCfg: [],
                      userRemoteConfigs                : [[credentialsId: JENKINS_GITHUB_CREDENTIALS_ID, url: "https://github.com/ImageWare/${GIT_REPO}.git"]]])
        }
    }
}

@SuppressWarnings("GrReassignedInClosureLocalVar")
def stageGetVersionAndChanges() {
    testMessage = "Get the version number and construct other strings..."
    timeout(time: 2, unit: 'MINUTES') {
        stage('GetVer') {
            echo testMessage
            dir(VERSION_NAME_FOLDER) {
                def grepVerNum = sh(returnStdout: true, script: "grep '${VERSION_NAME_PREFIX}' ${VERSION_NAME_FILE}")
                grepVerNum = grepVerNum.trim()
                echo "grepVerNum = ${grepVerNum}"
                def beginIndex = grepVerNum.indexOf(VERSION_NAME_PREFIX) + VERSION_NAME_PREFIX.length()
                echo "beginIndex = ${beginIndex}"
                def endIndex = grepVerNum.lastIndexOf('"')
                echo "endIndex = ${endIndex}"
                verNum = grepVerNum.substring(beginIndex, endIndex)
                echo "verNum = ${verNum}"
                buildLabel = "${verNum}${BUILD_LABEL_SUFFIX}"
                echo "buildLabel = ${buildLabel}"
                artifactoryDeployedUrl = ARTIFACTORY_BASE_DEPLOYED_URL + "/${GIT_REPO}${ARTIFACTORY_DEPLOY_DEV_PATH_OPTIONAL_SUFFIX}/${buildLabel}"
                echo "artifactoryDeployedUrl = ${artifactoryDeployedUrl}"
//                artifactoryDirectLintScanHtmlUrl = "${artifactoryDeployedUrl}/${LINT_SCAN_HTML_FILE}"
                artifactoryDirectLintScanHtmlUrl = "${currentBuild.absoluteUrl}androidLintResult/"
                htmlReportsUrl = "${currentBuild.absoluteUrl}HTML_20Reports/"
                reportArchiveZipUrl = "${htmlReportsUrl}*zip*/HTML_20Reports.zip"
                flavorZipFilename = "${GIT_REPO}-${buildLabel}-apks.zip"
                artifactoryDirectDownloadFlavorZipUrl = "${artifactoryDeployedUrl}/${flavorZipFilename}"

            }
//            changeLogExtract = sh(script: "awk -v RS=  'NR==1{ print }' ${CHANGE_LOG_LOCATION}", returnStdout: true).toString().trim()
//            changeLogExtract += "\n\n"
//            changeLogExtract += sh(script: "awk -v RS=  'NR==2{ print }' ${CHANGE_LOG_LOCATION}", returnStdout: true).toString().trim()
            echo "changeLogExtract set to: \n${changeLogExtract}"
        }
    }
}

def stageBuildCfgNoGradleLibs() {
    testMessage = "Configure the Gradle build system for this project"
    timeout(time: 15, unit: 'MINUTES') {
        stage('BuildCfg') {
            echo testMessage
            dir(GIT_REPO) {
                sh "echo 'sdk.dir=$WORKSPACE/${ANDROID_SDK_FOLDER}' > local.properties"
                sh 'chmod +x gradlew'
                showMem()
            }
        }
    }
}

@SuppressWarnings("GrReassignedInClosureLocalVar")
def stageBuildTestScan() {
    testMessage = "Build / Test / Scan the releases of ${ANDROID_MODULE_DIR}"
    timeout(time: 5, unit: 'HOURS') {
        stage('BuildTestScan') {
            echo testMessage
            dir("${GIT_REPO}") {
                sh "mkdir -vp ${absoluteTestReportFolder}"
                sh './gradlew assembleDebug lint ' + GRADLE_TIMEOUT_OPTIONS
                showMem()
            }
        }
    }
}

def stageLintPublish() {
    testMessage = "Publish LINT scan report"
    timeout(time: 15, unit: 'MINUTES') {
        stage('LintPub') {
            echo testMessage
            androidLint canComputeNew: false, defaultEncoding: '', healthy: '', pattern: '', unHealthy: ''
        }
    }
}

def stageOrganizeApks() {
    testMessage = "Organizing apks..."
    timeout(time: 30, unit: 'MINUTES') {
        //noinspection SpellCheckingInspection
        stage('OrgApks') {
            echo testMessage
            withCredentials([string(credentialsId: 'AndroidStorePwd', variable: 'STORE_PWD'),
                             string(credentialsId: 'AndroidKeyPwd', variable: 'KEY_PWD')]) {

                echo "Listing build folder structure..."
                sh "ls -r ${GIT_REPO}/*"

                //deploy the -debug.apk's
                def apkDebugPath = "./${GIT_REPO}/${BUILD_ARTIFACT_OUTPUT_DIR}/debug/${ANDROID_MODULE_DIR}-debug.apk"

                echo "Organizing for Artifactory deploy..."

                def currArtifactoryDeployPathForFlavor = "${ARTIFACTORY_DEPLOY_PATH}/${buildLabel}"
                def currOutputDebugFilenameForFlavor = "${GIT_REPO}-${buildLabel}-debug.apk"

                sh "mkdir -p \"${currArtifactoryDeployPathForFlavor}\""
                sh "cp -v ${apkDebugPath} \"./${currArtifactoryDeployPathForFlavor}/${currOutputDebugFilenameForFlavor}\""

                sh "ls -r  ./${ARTIFACTORY_DEPLOY_PATH}/${buildLabel}/*"

                //noinspection GrReassignedInClosureLocalVar
                artifactoryDirectDownloadUrls += "${artifactoryDeployedUrl}/${currOutputDebugFilenameForFlavor}\n"

                sh "cp ${GIT_REPO}/${LINT_SCAN_HTML_LOCATION} ${ARTIFACTORY_DEPLOY_PATH}/${buildLabel}/${lintScanOutputFilename}"
            }
        }
    }
}

@SuppressWarnings(["SpellCheckingInspection", "GrReassignedInClosureLocalVar"])
def stageDeployApks() {
    testMessage = "Deploy ${buildLabel} release files to Artifactory..."
    timeout(time: 10, unit: 'MINUTES') {

        //noinspection SpellCheckingInspection
        stage('DeployArt') {
            echo testMessage
            def repoName = "GoVerifyID" // 'test-repo-local'
            def version = "${buildLabel}"
            target = "${repoName}/Android/"

            //noinspection SpellCheckingInspection
            build job: 'p_upload_files_to_artifactory',
                    parameters: [string(name: 'NODE', value: "${VM_NAME}"),
                                 string(name: 'SERVERNAME', value: 'IWSArtifactoryServer'),
                                 string(name: 'REPONAME', value: "${repoName}"),
                                 string(name: 'VERSION', value: "${version}"),
                                 string(name: 'FILES_ROOT_DIR', value: "$WORKSPACE/ArtifactoryFiles/GMI/Applications"),
                                 string(name: 'PATTERN', value: "Applications/**"),
                                 string(name: 'TARGET', value: "${target}"),
                                 string(name: 'PROPS', value: ''),
                                 string(name: 'RECURSIVE', value: 'true'),
                                 string(name: 'FLAT', value: 'false'),
                                 string(name: 'REGEXP', value: 'false'),
                                 string(name: 'APPLNAME', value: "${GIT_REPO}"),
                                 string(name: 'UPLOAD_DIR', value: "Applications")]
        }
    }
}

@SuppressWarnings("GrMethodMayBeStatic")
def slackReportLineGitHub() {
    return "GitHub: <https://github.com/ImageWare/${GIT_REPO}/releases/tag/${buildLabel}|Tag ${buildLabel}> - <https://github.com/ImageWare/${GIT_REPO}|master>\n"
}

@SuppressWarnings(["SpellCheckingInspection", "GrMethodMayBeStatic"])
def slackReportLineArtifactory() {
    return "Artifactory: <${artifactoryDeployedUrl}|${"${PROJECT_DISPLAY_TITLE}-${buildLabel}"}>\n"
}

@SuppressWarnings(["SpellCheckingInspection", "GrMethodMayBeStatic"])
def slackReportLineDocumentation() {
    return "Documentation: <https://imageware.atlassian.net/wiki/spaces/MA/pages/755204122/How+To+Integrate+Sample+App+Details|How To Use Sample App>" +
            " - <https://imageware.atlassian.net/wiki/spaces/MA/pages/704938246/Integration.txt+for+adding+GMI+SDK+libraries+to+an+app|SDK integration.txt>" +
            " - <https://artifactory.iwsinc.com:8442/artifactory/webapp/#/artifacts/browse/tree/General/GoVerifyID/Android/SDKs/7.6.18/ims_android_7.6.19_JavadocAPI.zip|SDK 7.6.19 JavaDoc API>\n"
}

def slackReportLineJenkins(boolean includeTests = true) {
    def testLink = ''
    if (includeTests) {
        testLink = "<${htmlReportsUrl}|Tests>/<${reportArchiveZipUrl}|.ZIP> - "
    }
    return "Jenkins: <$jenkinsHost|Login> - <$JOB_URL|$JOB_NAME> - <$currentBuild.absoluteUrl|build #$currentBuild.number> ${testLink}prod: ${PRODUCTION_BUILD}\n"
}

def stageSlackSuccess() {
    testMessage = "Notifying Slack of SmallTest and release success..."
    timeout(time: 2, unit: 'MINUTES') {
        stage('SlackDone') {
            echo testMessage
            slackMessage = "*NEW Android GMI ${PROJECT_DISPLAY_TITLE} release available: ${buildLabel}*\n"
            slackMessage += slackReportLineDocumentation()
            slackMessage += slackReportLineJenkins(false)
            slackMessage += slackReportLineArtifactory()
            slackMessage += slackReportLineGitHub()

            echo "++ slack message post: ${slackMessage}"
            echo "++ slack metadata result: ${currentBuild.currentResult}"
            echo "++ slack metadata result: ${SLACK_CHANNEL_NAME}"
            slackSend message: slackMessage, color: SLACK_COLOR_MAP[currentBuild.currentResult], channel: "${SLACK_CHANNEL_NAME}"
        }
    }
}

def stageEmailSuccess() {
    testMessage = "Sending release email..."
    echo testMessage
    timeout(time: 2, unit: 'MINUTES') {
        //noinspection SpellCheckingInspection
        SendResultsEmail('EmailDone',
                "The ${PROJECT_DISPLAY_TITLE} ${buildLabel} SmallTest pass was SUCCESSFUL",
                "NEW Android GMI ${PROJECT_DISPLAY_TITLE} available: ${buildLabel}\n\n" +
                        "Jenkins: $JOB_URL $currentBuild.absoluteUrl build #$currentBuild.number\n" +
                        "Artifactory: ${artifactoryDeployedUrl}\n" +
                        "GitHub: https://github.com/ImageWare/${GIT_REPO}/releases/tag/${buildLabel}\n" +
                        "Android ${ANDROID_MODULE_DIR} LINT scan results available at: ${artifactoryDirectLintScanHtmlUrl}\n" +
                        "\n\n\n\n" +
                        "---===Additional resources and direct links===---\n\n" +
                        "Artifactory direct .apk flavor zip archive download link:" + artifactoryDirectDownloadFlavorZipUrl + " \n\n" +
                        "Artifactory direct .apk download links: " + artifactoryDirectDownloadUrls + " \n\n" +
                        "GitHub main repository: https://github.com/ImageWare/${GIT_REPO}\n\n" +
                        "---===Changelog===---\n" + changeLogExtract + " \n\n",
                'cpaiano@iwsinc.com', 'jenkins@iwsinc.com',
                '', '', '')
    }
}

def stageSlackFail() {
    timeout(time: 2, unit: 'MINUTES') {
        stage('SlackFail') {

            echo "Notifying Slack of failure..."
            slackMessage = "*GMI ${PROJECT_DISPLAY_TITLE} FAILED: repo ${GIT_REPO}, branch ${"${BRANCH_OR_TAG}"}, BUILD_LABEL_SUFFIX: ${BUILD_LABEL_SUFFIX}, build label ${buildLabel}*\n"
            slackMessage += slackReportLineJenkins()
            slackMessage += slackReportLineGitHub()

            echo "++ slack message post: ${slackMessage}"
            echo "++ slack metadata result: ${currentBuild.currentResult}"
            echo "++ slack metadata result: ${SLACK_CHANNEL_NAME}"
            slackSend message: slackMessage, color: SLACK_COLOR_MAP[currentBuild.currentResult], channel: "${SLACK_CHANNEL_NAME}"
        }
    }
}

def stageEmailFail() {
    timeout(time: 5, unit: 'MINUTES') {
        SendResultsEmail('EmailFail',
                "The ${PROJECT_DISPLAY_TITLE} ${buildLabel} build job FAILED",
                "The ${PROJECT_DISPLAY_TITLE} ${buildLabel} build job failed.  Job: ${JOB_NAME}-${BUILD_NUMBER}, ${BUILD_URL}.  The job was attempting to do:  '${testMessage}'",
                'cpaiano@iwsinc.com', 'jenkins@iwsinc.com',
                '', '', '')
    }
}

def stageVmDestroy() {
    timeout(time: 10, unit: 'MINUTES') {
        stage('VmDestroy') {
            echo "Destroying the Linux VM ${VM_NAME}"
//            destroyUbuntuVm label: "${VM_NAME}"
            destroyCentosVm label: "${VM_NAME}"
        }
    }
}

@SuppressWarnings("SpellCheckingInspection")
def stagesVmDebugEnvCfgAndDebugSleep(VM_NAME, MINS_SLEEP_BEFORE_VM_AUTODESTRUCT) {
//    stageVmIp(VM_NAME)
    node("${VM_NAME}") { //Debug VM node closure
        timeout(time: 5, unit: 'MINUTES') {
            stage('VmDebugEnvCfg') {
                echo "Attempting to configure the VM's environment for manual debug..."
                configEnv()
            }
        }

        stageDebugSleep(MINS_SLEEP_BEFORE_VM_AUTODESTRUCT)
    }
}

@SuppressWarnings("SpellCheckingInspection")
def stageDebugSleep(MINS_SLEEP_BEFORE_VM_AUTODESTRUCT) {
    stage('DebugSleep') {
        //noinspection SpellCheckingInspection
        echo "VM Predestruction sleep: Sleeping for " + MINS_SLEEP_BEFORE_VM_AUTODESTRUCT + " mins so devs can access VM named ${VM_NAME} at IP ${vmIpAddr}"
        try {
            sleep(time: MINS_SLEEP_BEFORE_VM_AUTODESTRUCT, unit: "MINUTES")
        } catch (Exception e) {
            //noinspection SpellCheckingInspection
            echo "VM Predestruction IP access sleep aborted! " + e.getMessage()
        }
    }
}

@SuppressWarnings("GrReassignedInClosureLocalVar")
def stageVmIp(VM_NAME) {
    timeout(time: 10, unit: 'MINUTES') {
        stage('VmIP') {
            echo "Retrieve the VM's IP"
            vmIpAddr = getVmIpAddr node: "${VM_NAME}", ipAddrFilter: '192.168'
            echo "VM IP address retrieved: " + vmIpAddr
        }
    }
}

@SuppressWarnings(["SpellCheckingInspection", "GroovyUnusedDeclaration"])
def configBashEnv() {
//    node(VM_NAME) {
    echo "Configuring BASH env..."

    def androidHome = WORKSPACE + "/" + ANDROID_SDK_FOLDER
    echo "androidHome is now " + androidHome

    def exportPathTo = 'echo "export PATH=$PATH' +
            ":${WORKSPACE}/" + ANDROID_SDK_FOLDER + '/platform-tools' +
            ":${WORKSPACE}/" + ANDROID_SDK_FOLDER + '/tools' +
            ":${WORKSPACE}/" + ANDROID_SDK_FOLDER + '/tools/bin' +
            ":${WORKSPACE}/" + ANDROID_SDK_FOLDER + '/emulator' +
            '\nexport HOME=' + VAGRANT_HOME +
            '\nexport ANDROID_HOME=' + androidHome +
            '\nexport ANDROID_SDK_ROOT=' + androidHome +
            '\nexport ANDROID_EMULATOR_HOME=' + HOME_VAGRANT_ANDROID +
            '\nexport ADB=' + ADB +
            '\nexport sdkManager=' + sdkManager +
            '\nexport avdManager=' + avdManager +
            '\nexport avdEmulator=' + avdEmulator +
            '" >> '
    sh exportPathTo + VAGRANT_HOME + '/.bashrc'
    sh 'sudo ' + exportPathTo + '~/.bashrc'

/*
    echo "Resetting PATH and resourcing .bashrc to reconstruct..."
//    sh 'PATH=/bin:/usr/bin && source ' + VAGRANT_HOME + '/.bashrc'
    sh 'exec bash'
*/

//    }
}

@SuppressWarnings("GroovyUnusedDeclaration")
def showMem() {
    echo "Showing memory statistics..."
    sh MEMORY_CHECK_CMD
}

@SuppressWarnings(["SpellCheckingInspection", "GroovyUnusedDeclaration"])
def configEnv() {

    def androidHome = WORKSPACE + "/" + ANDROID_SDK_FOLDER
    echo "androidHome is now " + androidHome

    echo "Setting global env vars..."
    env.HOME = VAGRANT_HOME
    env.ANDROID_HOME = androidHome
    env.ANDROID_SDK_ROOT = androidHome
    env.ADB = ADB
    env.sdkManager = sdkManager
    env.avdManager = avdManager
    env.avdEmulator = avdEmulator
    env.ANDROID_EMULATOR_HOME = HOME_VAGRANT_ANDROID
}

@SuppressWarnings("GroovyUnusedDeclaration, SpellCheckingInspection")
def probeJavaStatus(JAVA_HOME) {
    echo "Listing all installed packages..."
    sh "sudo yum list"

    echo "Listing installed java packages..."
    sh "sudo yum list | grep java"

    echo "Listing installed oracle packages..."
    sh "sudo yum list | grep oracle"

    sh "update-alternatives --config java || true"
    echo "JAVA_HOME is set to ${JAVA_HOME}"
    sh "locate jarsigner || true"
    sh "which jarsigner || true"
    sh "locate java || true"
    sh "which java || true"
}

def getHostFromUrl(url) {
    //Using bash because we can't trust Jenkins
    def host = sh (
            script: """${BASH_SHEBANG}
echo "`echo ${url} | awk -F/ '{print \$1}'`//`echo ${url} | awk -F/ '{print \$3}'`/${JENKINS_LOGIN_URL_SUFFIX}"
""",
            returnStdout: true
    ).trim()
    echo "Host ${host} retrieved from url ${url}"
    return host
}

@SuppressWarnings("GroovyUnusedDeclaration, SpellCheckingInspection")
def UpdateJiraIssue(stageName, issueToUpdate, updateMsg) {
    stage("${stageName}") {
        // log a successful system check message
        build job: 'p_add_jira_comment',
                parameters: [[$class: 'StringParameterValue', name: 'JIRA_ISSUE', value: "${issueToUpdate}"],
                             [$class: 'StringParameterValue', name: 'JIRA_COMMENT',
                              value : "${updateMsg}"]]
    }
}

def SendResultsEmail(stageName, subject, body, to, from, replyTo, cc, bcc) {
    stage("${stageName}") {
        try {
            sendEmail(bcc, body, cc, subject, from, replyTo, to)
        } catch (Exception e) {
            echo "Failed to send email because ${e.getMessage()}, retrying..."
            try {
                sendEmail(bcc, body, cc, subject, from, replyTo, to)
            } catch (Exception e1) {
                echo "Failed to send email AGAIN, this time because ${e1.getMessage()}, giving up..."
            }
        }
    }
}

private sendEmail(bcc, body, cc, subject, from, replyTo, to) {
    mail bcc: "${bcc}",
            body: "${body}",
            cc: "${cc}",
            subject: "${subject}",
            from: "${from}",
            replyTo: "${replyTo}",
            to: "${to}"
}

return this
