// vars/PipelineNodejs.groovy

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    properties([disableConcurrentBuilds()])
    def agent = config.agent ?: ''

    def pipeline = new cz.ackee.Pipeline()

    config.pipelineType = 'nodejs'

    // now build, based on the configuration provided
    node(agent) {

    def workspace = pwd()

    println "pipeline config from Jenkinsfile ==> ${config}"

    pipeline.setEnv(config)
    println "flattened pipeline config ==> ${config}"

    def projectName = config.projectName
    def appName = config.appName
    def branch = config.branch
    def cloudProject = config.cloudProject

    def appDir = '/usr/src/app'

    // nodeEnv based on branch
    def nodeEnv = config.nodeEnv
    //add rest of params
    nodeEnv =  nodeEnv + " -v ${workspace}:${appDir} -w ${appDir}"

    def nodeTestEnv = "${config.nodeTestEnv} -v ${workspace}:${appDir} -w ${appDir}"
    def imageTag = "eu.gcr.io/${cloudProject}/${projectName}/${appName}:${branch}.${env.BUILD_NUMBER}"

    def namespace = config.namespace

    def slackChannel = config.slackChannel

    // defaults
    def appRole = config.appRole ?: 'server'  // server  | client | apiserver
    def appTier = config.appTier ?: 'backend' // backend | frontend

   // buildCommand
    def buildCommand = config.buildCommand

    def nodeImage = config.nodeImage
    def cloverReportDir = config.cloverReportDir ?: 'coverage'
    def cloverReportFileName = config.cloverReportFileName ?: 'clover.xml'
    def pattern = config.pattern ?: 'checkstyle-result.xml'
    def usePreviousBuildAsReference = config.usePreviousBuildAsReference ?: true
    def dryRun = config.dryRun ?: false
    def reason = 'checkout'

    try {

      stage('Checkout'){

        pipeline.checkoutScm()
      }

      stage('Build') {

        reason='build'
        docker.image(nodeImage).inside(nodeEnv) {
            sh buildCommand
        }

        docker.build(imageTag)
      }

      stage('Docker push image') {

        reason='docker image push'
        pipeline.imagePush(cloudProject: cloudProject, imageTag: imageTag)
      }

      stage('Test') {

        reason='test'
        if (config.runTests){

    	    docker.image(nodeImage).inside(nodeTestEnv) {
    	        sh "npm run ci-test"
    	    }
    	    echo "npm run ci-test finished. currentBuild.result=${currentBuild.result}"

        	//junit allowEmptyResults: true, healthScaleFactor: 10.0, keepLongStdio: true, testResults: 'test.xml'
        	step([$class: 'JUnitResultArchiver', allowEmptyResults: true, healthScaleFactor: 10.0, keepLongStdio: true, testResults: 'test.xml'])
    	    echo "junit finished. currentBuild.result=${currentBuild.result}"

        	step([$class: 'CloverPublisher',cloverReportDir: cloverReportDir, cloverReportFileName: cloverReportFileName, failingTarget: [conditionalCoverage: 0, methodCoverage: 0, statementCoverage: 0], healthyTarget: [conditionalCoverage: 80, methodCoverage: 70, statementCoverage: 80], unhealthyTarget: [conditionalCoverage: 0, methodCoverage: 0, statementCoverage: 0]])
        	echo "CloverPublisher finished. currentBuild.result=${currentBuild.result}"

        	//publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'clover-report', reportFiles: 'index.html', reportName: 'Coverage HTML Report', reportTitles: ''])
        	//publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: false, reportDir: 'clover', reportFiles: 'index.html', reportName: 'Coverage HTML Report 2', reportTitles: ''])

        	if (currentBuild.result == 'UNSTABLE') {
        	    throw new RuntimeException("shit's fucked")
        	}
        }
        else {
    	    echo 'tests skipped'
        }
      }


      stage('Lint') {

        reason='lint'
        if (config.runLint){
	        docker.image(nodeImage).inside(nodeTestEnv) {
	            sh "npm run ci-lint"
	        }

	        checkStyle(pattern, usePreviousBuildAsReference)
        }
  	    else {
  	        echo 'skipped'
  	    }
      }

      stage('Deploy') {

        if(currentBuild.result != 'UNSTABLE') reason='deploy'
        if (dryRun == false) {
          pipeline.kubeCreateNamespace(cloudProject: cloudProject, namespace: namespace)

          def folders = ["services", "deployment"]
          for (folder in folders){
              def path = config.envFolder + "/" + folder
              Map values = ['APP_NAME': appName,
                            'PROJECT_NAME': projectName,
                            'APP_ROLE': appRole,
                            'APP_TIER': appTier,
                            'ENV_NAME': (branch.equalsIgnoreCase('master') ? 'production' : branch),
                            'IMAGE_URL': imageTag
                            ]

              pipeline.replaceInTemplates(folder: path, toReplace: values)
              pipeline.kubeDeploy(cloudProject: cloudProject, namespace: namespace, yamlFolder: path)
          }
        }
      }

      stage('K8s Info')

        pipeline.printKubeInfo(config)
      }

      catch (err) {
          currentBuild.result = "FAILURE"
          println(err.toString());
          println(err.getMessage());
          println(err.getStackTrace());
          throw err
      }
      finally {
          pipeline.notifySlack(channel: slackChannel, reason: reason)
      }
  }

}
