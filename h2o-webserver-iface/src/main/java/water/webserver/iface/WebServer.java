package water.webserver.iface;

/**
 * All the functionality that we need to call on an existing instance of HTTP server (servlet container).
 */
public interface WebServer {

  void start(String ip, int port) throws Exception;

  void stop() throws Exception;

}
