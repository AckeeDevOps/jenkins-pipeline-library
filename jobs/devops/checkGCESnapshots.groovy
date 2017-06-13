node('linux-docker') {
    
    def cloudProjects = getAllCloudprojects()
    def today
    def errors
    sh 'rm *.txt || echo nothing to delete'
     
    stage('list all snapshots') {
        
         for (i=0;i<cloudProjects.length;i++) {
           def project = cloudProjects[i]
           sh "gcloud --configuration ${project} compute snapshots list --regexp='^backup.*' || echo 'no snapshots found'"
           sh "gcloud compute disks list >>alldisks.txt || echo 'no disks found'"
         }
     
     }
     
     stage('list backup snapshots') {
          sh 'echo "" > list.txt '
          for (volume in volumes) {
               sh "echo listing ${volume}" 
               sh "gcloud compute snapshots list --regexp='^backup-.*${volume}.*' 2>>listerr.txt >>list.txt"
          }
            
      
     }
    archiveArtifacts '*.txt'
    color='good'
    def err = sh(script: 'wc -l <listerr.txt', returnStdout: true).trim()
    
    //if(empty.toInteger() >0) color='warning'
    if(err.toInteger() >0) color='danger'
    
    slackSend channel: 'monitoring', color: color, message: "GCE Snapshots check finished. <$env.JOB_URL/lastSuccessfulBuild/artifact/alldisks.txt|All disks>. Found ${err} errors. Check the <$env.JOB_URL/lastSuccessfulBuild/artifact/list.txt|output>."
    
}
