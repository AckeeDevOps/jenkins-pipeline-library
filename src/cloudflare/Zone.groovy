// src/cloudflare/Zone.groovy
package cloudflare;
String getZones(String filter) {
    
    String ret
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'cloudflare-api',
                    usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                        
    
    ret = sh(script: """#!/bin/bash
    set -x
    curl -X GET "https://api.cloudflare.com/client/v4/zones?$filter" \
    -H "Content-Type:application/json" \
    -H "X-Auth-Key:$PASSWORD" \
    -H "X-Auth-Email:$USERNAME" \
    2>/dev/null
    """, returnStdout: true)
    
    
    }
    return ret;
}
