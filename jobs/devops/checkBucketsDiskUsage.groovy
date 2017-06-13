node {

   def report = 'report.log'
   sh('echo ""> report.log')

   stage('Check all cloud projects') {
   def cloudProjects = getAllCloudprojects() 
   
       for (i=0;i<cloudProjects.length;i++) {
         def project = cloudProjects[i]
         printBucketDiskUsage(project) 
       }
   }
   
   stage('Archive log') {
      archiveArtifacts '*.log'
   }
   
   slackSend channel: 'monitoring', color: 'good', message: "GCE Buckets usage check finished. See the <$env.JOB_URL/lastSuccessfulBuild/artifact/${report}|output>."
  
}