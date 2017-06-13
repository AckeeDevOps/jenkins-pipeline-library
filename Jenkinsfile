library ("ackee-shared-libs@${env.BRANCH_NAME}")

node(){

 stage('checkout scm') {
    checkout scm
 }

 stage('seed jobs') {
 
    jobDsl targets: 'jobs/**/seed.groovy'
 }

 stage('test') {
 
    // test Google Compute Engine - Storage
    createDisk('dev-cluster','test-delete-me')
    deleteDisk('dev-cluster','test-delete-me')
    
    // test Google Container Engine
    
    // test Cloudflare
    
    // test other things
 }
 
 stage('lint') {
    //TODO: add lint
 }
}
