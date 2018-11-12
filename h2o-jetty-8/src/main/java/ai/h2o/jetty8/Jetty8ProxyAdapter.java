package ai.h2o.jetty8;

import ai.h2o.jetty8.proxy.TransparentProxyServlet;
import ai.h2o.webserver.iface.Credentials;
import ai.h2o.webserver.iface.H2OHttpServer;
import ai.h2o.webserver.iface.H2OProxy;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

class Jetty8ProxyAdapter implements H2OProxy {
  private final Jetty8Helper helper;
  private final H2OHttpServer h2oHttpServer;
  private final Credentials credentials;
  private final String proxyTo;
  private int port;

  private Jetty8ProxyAdapter(Jetty8Helper helper, H2OHttpServer h2oHttpServer, Credentials credentials, String proxyTo) {
    this.helper = helper;
    this.h2oHttpServer = h2oHttpServer;
    this.credentials = credentials;
    this.proxyTo = proxyTo;
  }

  static H2OProxy create(final H2OHttpServer h2oHttpServer, final Credentials credentials, final String proxyTo) {
    final Jetty8Helper helper = new Jetty8Helper(h2oHttpServer);
    return new Jetty8ProxyAdapter(helper, h2oHttpServer, credentials, proxyTo);
  }

  @Override
  public int getPort() { // todo move this to proxy starter side
    return port;
  }

  @Override
  public void start(String ip, int port) throws Exception {
    final Server jettyServer = helper.createJettyServer(ip, port);
    final HandlerWrapper handlerWrapper = helper.authWrapper(jettyServer);
    final ServletContextHandler context = helper.createServletContextHandler();
    registerHandlers(handlerWrapper, context, credentials, proxyTo);
    this.port = port;
    jettyServer.start();
  }

  private void registerHandlers(HandlerWrapper handlerWrapper, ServletContextHandler context, Credentials credentials, String proxyTo) {
    // setup authenticating proxy servlet (each request is forwarded with BASIC AUTH)
    final ServletHolder proxyServlet = new ServletHolder(TransparentProxyServlet.class);
    proxyServlet.setInitParameter("ProxyTo", proxyTo);
    proxyServlet.setInitParameter("Prefix", "/");
    proxyServlet.setInitParameter("BasicAuth", credentials.toBasicAuth());
    context.addServlet(proxyServlet, "/*");
    // authHandlers assume the user is already authenticated
    final HandlerCollection authHandlers = new HandlerCollection();
    authHandlers.setHandlers(new Handler[]{
        helper.authenticationHandler(),
        context,
    });
    // handles requests of login form and delegates the rest to the authHandlers
    final ProxyLoginHandler loginHandler = new ProxyLoginHandler();
    loginHandler.setHandler(authHandlers);
    // login handler is the root handler
    handlerWrapper.setHandler(loginHandler);
  }

  private class ProxyLoginHandler extends HandlerWrapper {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      final boolean handled = h2oHttpServer.proxyLoginHandler(target, request, response);
      if (handled) {
        baseRequest.setHandled(true);
      } else {
        super.handle(target, baseRequest, request, response);
      }
    }
  }


}
