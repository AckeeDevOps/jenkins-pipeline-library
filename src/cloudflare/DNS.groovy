// src/cloudflare/DNS.groovy
package cloudflare;
def createCNAMErecord(String name, String domain) {
    
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'cloudflare-api',
                    usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD'],
                    string(credentialsId: credId, variable: 'zone'),]) {
                        
    
    sh """#!/bin/bash
    curl -X POST "https://api.cloudflare.com/client/v4/$zone/c67af1c878dfb84ee76e7b776517cd2e/dns_records" \
    -H "Content-Type:application/json" \
    -H "X-Auth-Key:$PASSWORD" \
    -H "X-Auth-Email:$USERNAME" \
    --data '{"type":"CNAME","name":"${name}","content":"${domain}","ttl":1,"proxied":true}'
    """
    }
}
def getRecord(String filter) {
    
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'cloudflare-api',
                    usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD'],
                    string(credentialsId: credId, variable: 'zone'),]) {
                        
    
    sh """#!/bin/bash
    curl -X GET "https://api.cloudflare.com/client/v4/$zone/c67af1c878dfb84ee76e7b776517cd2e/dns_records?$filter" \
    -H "Content-Type:application/json" \
    -H "X-Auth-Key:$PASSWORD" \
    -H "X-Auth-Email:$USERNAME"
    """
    }
}
