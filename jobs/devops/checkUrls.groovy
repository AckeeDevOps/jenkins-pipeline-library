@Library('ackee-shared-libs')
import cloudflare.Zone

node('monitoring') {
    
    def errors=0
    
    stage('check urls') {
     def z = new Zone();
     String s = z.getZones("status=active&per_page=50")
     sh("set +x ; echo '${s}' | jq  '.result[].name' | tr -d '\"' >domains.txt")
     sh('set +x; while read domain; do echo $domain ; curl -I "https://www.$domain" 2>/dev/null | grep "HTTP\\|Location" || echo "fail" ; echo "------" ;done < domains.txt; echo done')
    }
     
    color='good'
    //if(empty.toInteger() >0) color='warning'
    if(errors.toInteger() >0) color='danger'
    slackSend channel: 'monitoring', color: color, message: "Url check finished. Check <$env.JOB_URL/lastBuild/console|log>."
    
}