# JavaWebProxyServer

Keys:
* Web cache replacement
* Web cache consistency

How it works:
* Start TCP port and listen to the port
* Accept incoming HTTP request
* Log request
* Spawn a thread to process the request

Log contains:
* Time when proxy server received request
* Duration (from the receiving time to when the proxy server finishes sending the last byte of the response)
* Cache hit or miss
* Cache replacement status
* Cache consistency status
* Client address
* URL requested
* Status code of the request

Supported features:
* Get
* Conditional Get
* Persistent connection
* Cache replacement algorithm
* Cache consistency algorithm
* Setting for the proxy server's cache limitation (Ex: maximum number of web pages the proxy server can cache in the cache)
* Setting for the proxy server’s cache replacement algorithm from the command line
* Setting for the proxy server’s listening port from the command
* Cache report: total http request, total cache hits, total cache missed, cache hit rate.

Default settings:
* Port: 9999
* Threshold: 20000
* Max-webpages: 20
