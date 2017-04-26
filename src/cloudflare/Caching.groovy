// src/cloudflare/Caching.groovy
package cloudflare;
def setDevelopmentMode(String credId, String value) {
    
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'cloudflare-api',
                    usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD'],
                    string(credentialsId: credId, variable: 'zone'),]) {
                        
    
    sh """#!/bin/bash
    set -x
    curl -X PATCH "https://api.cloudflare.com/client/v4/zones/$zone/settings/development_mode" \
    -H "Content-Type:application/json" \
    -H "X-Auth-Key:$PASSWORD" \
    -H "X-Auth-Email:$USERNAME" \
    --data '{"value":"${value}"}'
    """
    }
}
def purgeAll(String credId) {
    
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'cloudflare-api',
                    usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD'],
                    string(credentialsId: credId, variable: 'zone'),]) {
                        
    
    sh """#!/bin/bash
    set -x
    curl -X DELETE "https://api.cloudflare.com/client/v4/zones/$zone/purge_cache" \
    -H "Content-Type:application/json" \
    -H "X-Auth-Key:$PASSWORD" \
    -H "X-Auth-Email:$USERNAME" \
    --data '{"purge_everything":true}'
    """
    }
}
