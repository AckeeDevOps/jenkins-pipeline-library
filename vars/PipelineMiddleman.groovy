#!/usr/bin/groovy

import cloudflare.DNS

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    // now build, based on the configuration provided
    node() {
        
        def workspace = pwd()
        def projectName = config.projectName
        def branch = env.BRANCH_NAME
        def appDir = '/usr/src/app'
        def slackChannel = config.slackChannel
        env.SLACK_CHANNEL = slackChannel
    
        def buildCommand = config.buildCommand
        def buildDir = config.buildDir
        def myURL = (config.myURL ==null) ? config.baseURL : config.myURL
        
        //defaults
        def revproxyURL= config.revproxyURL ?: 'somerevproxy.domain.org'
        def bucketURL = "gs://$myURL/"
        
        //bucket
        def baseURL = config.baseURL
        
        // ftp
        def ftpHost = config.ftpHost
        def ftpUser = config.ftpUser
        def ftpPassword = config.ftpPassword
      
        def deployToFtp = (ftpHost == null) ? false : true
       
        try {
          checkoutScm()
          
          stage('Build'){
            sh("docker run -v ${workspace}:${appDir} -w ${appDir} inzinger/middleman-base:2.2 bash -c '${buildCommand}'")
           } 
           
          if (deployToFtp) {
              
            stage('Deploy to FTP') {
              sh('cd build; rm .htaccess')
              sh('cd build; find . -type f -not -path ".git" -exec curl -T {} ftp://${ftpHost}/{} --ftp-create-dirs --user ${ftpUser}:${ftpPassword} \\;')
            }
              
              
          } else {
           
              stage('Set Bucket') {
                // fail silently
                def d = new DNS();
                d.getRecord("name=${baseURL}")
                d.createCNAMErecord(baseURL, revproxyURL)
                sh("gsutil mb $bucketURL || echo error")
              }
               
              stage('Deploy to Bucket'){
                def exclude = "\\.git"
                sh("gsutil -m rsync -R -x '${exclude}' ${buildDir} ${bucketURL}")
              }
              stage('Set bucket ACL'){
                sh("gsutil -m acl ch -R -u AllUsers:R ${bucketURL}")
              }
              stage('Set bucket index and error pages'){
                sh("gsutil -m web set -m index.html ${bucketURL}")
               } 
          }  
     
      } catch (err) {
              currentBuild.result = "FAILURE"
              println(err.toString());
              println(err.getMessage());
              println(err.getStackTrace());
              throw err
      } finally {
              slackNotifyBuild()
          }
  }
}
