node {
    
    def report = 'report.log'
    sh('echo ""> report.log')

   stage('Print all non-attached disks') {
       def cloudProjects = getAllCloudprojects()
       
       for (i=0;i<cloudProjects.length;i++) {
         def project = cloudProjects[i]
         String output = printNonattachedDisks(project)
         sh("echo '${output}' >> ${report}")
       }
       
       // delete empty lines in the report file
       sh("sed -i '/^\$/d' ${report}")
   }
   stage('Archive log') {
      archiveArtifacts '*.log'
   }
   
   stage('Send notification') {
     color='good'
     def num = sh(script: "wc -l < ${report}", returnStdout: true).trim()
     if(num.toInteger() >0) color='danger'
   
     slackSend channel: 'monitoring', color: color, message: "GCE Disks check finished. ${num} unattached disks found. See the <$env.JOB_URL/lastSuccessfulBuild/artifact/${report}|output>."
   }
}
