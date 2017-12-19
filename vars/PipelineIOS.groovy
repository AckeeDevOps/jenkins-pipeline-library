#!/usr/bin/groovy

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def agent = config.agent ?: 'ios'


    // now build, based on the configuration provided
    node(agent) {

    def workspace = pwd()

    def pipeline = new cz.ackee.Pipeline()

    config.pipelineType = 'ios'

    println "pipeline config from Jenkinsfile ==> ${config}"

    pipeline.setEnv(config)
    println "flattened pipeline config ==> ${config}"

    env.FASTLANE_SKIP_UPDATE_CHECK = 1
    env.FASTLANE_DISABLE_COLORS = 1
    env.CHANGELOG_PATH = "outputs/changelog.txt"
    def slackChannel = config.slackChannel ?: 'ci-ios' // don't care if it exists
    def hockeyID = config.hockeyID
    def hockeyAppApiToken = config.hockeyAppApiToken

    def reason = 'start'

    try {

        properties([
            disableConcurrentBuilds(),
        ])

        stage('Checkout') {
            reason = 'checkout'
            // Checkout code from repository and update any submodules
            pipeline.checkoutScm()
            sh "mkdir -p outputs"

            // generate changelog
            pipeline.generateChangelog path: env.CHANGELOG_PATH, format: 'format:- %s [%ce]'
        }

        stage('Prepare') {
            reason = 'prepare'
            sh "security unlock -p ${env.MACHINE_PASSWORD} ~/Library/Keychains/login.keychain"
            reason = 'bundler'
            sh "bundle install --path ~/.bundle"
        }

        stage('Carthage') {
            reason = 'carthage'
            sh "bundle exec fastlane cart"
        }

        stage('Pods') {
            reason = 'cocoapods'
            sh "bundle exec fastlane pods"
        }

        stage('Test') {
            reason = 'test'
            sh "bundle exec fastlane test type:unit"
            junit allowEmptyResults: true, testResults: 'fastlane/test_output/report.junit'
        }

        stage('Provisioning') {
            reason = 'provisioning'
            sh "bundle exec fastlane provisioning configuration:AdHoc"
        }

        stage('Build') {
            reason = 'build'
            sh "bundle exec fastlane beta"
        }

        stage('Upload to HockeyApp') {
            reason = 'hockeyapp'

            step([
                    $class      : 'HockeyappRecorder',
                    applications: [
                            [apiToken          : hockeyAppApiToken,
                             downloadAllowed   : true,
                             filePath          : 'outputs/App.ipa',
                             dsymPath          : 'outputs/App.app.dSYM.zip',
                             tags              : 'android, internal, ios',
                             mandatory         : false,
                             notifyTeam        : false,
                             releaseNotesMethod: [$class: 'FileReleaseNotes', fileName: env.CHANGELOG_PATH],
                             uploadMethod      : [$class: 'VersionCreation', appId: hockeyID]]
                    ],
                    debugMode   : false, failGracefully: false])
        }
        currentBuild.result = 'SUCCESS'
    } catch (e) {
        currentBuild.result = 'FAILURE'
        throw e
    } finally {
        pipeline.notifySlack(channel: slackChannel, reason: reason)
    }
    }
}
