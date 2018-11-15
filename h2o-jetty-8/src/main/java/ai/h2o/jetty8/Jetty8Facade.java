package ai.h2o.jetty8;

import ai.h2o.webserver.iface.Credentials;
import ai.h2o.webserver.iface.H2OHttpView;
import ai.h2o.webserver.iface.HttpServerFacade;
import ai.h2o.webserver.iface.ProxyServer;
import ai.h2o.webserver.iface.WebServer;

public class Jetty8Facade implements HttpServerFacade {
  @Override
  public WebServer createWebServer(H2OHttpView h2oHttpView) {
    return Jetty8ServerAdapter.create(h2oHttpView);
  }

  @Override
  public ProxyServer createProxyServer(H2OHttpView h2oHttpView, Credentials credentials, String proxyTo) {
    return Jetty8ProxyServerAdapter.create(h2oHttpView, credentials, proxyTo);
  }
}
