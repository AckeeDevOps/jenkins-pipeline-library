import cloudflare.DNS

def call(String name, String domain) {

        def d = new DNS();
        d.getRecord("name=${name}")
        d.createCNAMErecord(baseURL, domain)

}
