def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def agent = config.agent ?: 'android'


    // now build, based on the configuration provided
    node(agent) {

    def workspace = pwd()

    def pipeline = new cz.ackee.Pipeline()

    config.pipelineType = 'android'

    println "pipeline config from Jenkinsfile ==> ${config}"

    pipeline.setEnv(config)
    println "flattened pipeline config ==> ${config}"

    env.CHANGELOG_PATH = "outputs/changelog.txt"
    def slackChannel = config.slackChannel ?: 'ci-android'
    def hockeyID = config.hockeyID
    def defChoices = ["DevApiBeta", "DevApiRelease"]
    def paramChoices = config.buildVariants ?: defChoices
    def hockeyAppApiToken = config.hockeyAppApiToken

    def reason = 'start'

    try {

        properties([
                disableConcurrentBuilds(),
                parameters([
                        choice(choices: paramChoices.join("\n"), description: 'Build variant of the build', name: 'buildVariant')
                ])
        ])

        stage('Checkout') {
            reason = 'checkout'
            // Checkout code from repository and update any submodules
            pipeline.checkoutScm()

            sh 'git submodule update --init'
            sh "mkdir -p outputs"

            // generate changelog
            sh "git log --pretty='format:- %s [%ce]' ${env.GIT_COMMIT}...${env.GIT_PREVIOUS_COMMIT} > ${env.CHANGELOG_PATH}"
            openTasks high: 'FIXME', normal: 'TODO', pattern: '**/*.kt'
        }

        stage('Build') {
            reason = 'build'
            //branch name from Jenkins environment variables
            sh "touch outputs/mapping.txt"
            sh "chmod +x gradlew"
            withCredentials([[$class           : 'AmazonWebServicesCredentialsBinding', credentialsId: 'android-gradle-aws',
                              accessKeyVariable: 'AWS_ACCESS_KEY', secretKeyVariable: 'AWS_SECRET_KEY']]) {
                //build your gradle flavor, passes the current build number as a parameter to gradle
                sh "./gradlew -Pkotlin.incremental=false -Porg.gradle.parallel=false -Porg.gradle.daemon=false -PAWS_ACCESS_KEY=$AWS_ACCESS_KEY -PAWS_SECRET_KEY=$AWS_SECRET_KEY clean assemble${params.buildVariant}"
            }
        }

        stage('Test') {
            reason = 'test'
            withCredentials([[$class           : 'AmazonWebServicesCredentialsBinding', credentialsId: 'android-gradle-aws',
                              accessKeyVariable: 'AWS_ACCESS_KEY', secretKeyVariable: 'AWS_SECRET_KEY']]) {
                sh "./gradlew -Pkotlin.incremental=false -Porg.gradle.parallel=false -Porg.gradle.daemon=false -PAWS_ACCESS_KEY=$AWS_ACCESS_KEY -PAWS_SECRET_KEY=$AWS_SECRET_KEY test${params.buildVariant}UnitTest"
            }
            step $class: 'JUnitResultArchiver', testResults: "app/build/test-results/**/*.xml"
        }

        stage('Archive') {
            reason = 'archive'
            //tell Jenkins to archive the apks
            archiveArtifacts artifacts: 'outputs/*.apk', fingerprint: true
            archiveArtifacts artifacts: 'outputs/mapping.txt', fingerprint: true
        }

        stage('Upload to HockeyApp') {
            reason = 'hockeyapp'
            //archive 'outputs/*.apk'
            step([
                    $class      : 'HockeyappRecorder',
                    applications: [
                            [apiToken          : hockeyAppApiToken,
                             downloadAllowed   : true,
                             filePath          : 'outputs/App.apk',
                             dsymPath          : 'outputs/mapping.txt',
                             tags              : 'android, internal, ios',
                             mandatory         : false,
                             notifyTeam        : false,
                             releaseNotesMethod: [$class: 'FileReleaseNotes', fileName: env.CHANGELOG_PATH],
                             uploadMethod      : [$class: 'VersionCreation', appId: hockeyID]]
                    ],
                    debugMode   : false,
                    failGracefully: false])
        }
        currentBuild.result = 'SUCCESS'
    } catch (e) {
        currentBuild.result = 'FAILURE'
        throw e
    } finally {
        pipeline.notifySlack(slackChannel: slackChannel, reason: reason)
    }
    }
}
