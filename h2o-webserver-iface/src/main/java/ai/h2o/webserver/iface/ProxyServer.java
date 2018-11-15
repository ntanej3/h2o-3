package ai.h2o.webserver.iface;

/**
 * All the functionality that we need to call on an existing instance of HTTP proxy.
 */
public interface ProxyServer {

  void start(String ip, int port) throws Exception;

}
