/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package juzu.bridge.vertx;

import juzu.Method;
import juzu.Response;
import juzu.asset.AssetLocation;
import juzu.impl.bridge.Bridge;
import juzu.impl.bridge.spi.servlet.ServletScopedContext;
import juzu.impl.bridge.spi.web.WebBridge;
import juzu.impl.bridge.spi.web.WebRequestContext;
import juzu.impl.common.Lexers;
import juzu.impl.common.Logger;
import juzu.impl.bridge.spi.ScopedContext;
import juzu.impl.common.Tools;
import juzu.impl.inject.Scoped;
import juzu.io.Stream;
import juzu.request.ApplicationContext;
import juzu.request.ClientContext;
import juzu.request.HttpContext;
import juzu.request.RequestParameter;
import juzu.request.UserContext;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpCookie;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class VertxWebBridge extends WebBridge implements HttpContext {

  /** . */
  private static final ApplicationContext APPLICATION_CONTEXT = new ApplicationContext() {
    public ResourceBundle resolveBundle(Locale locale) {
      return null;
    }
  };

  /** . */
  private final Application application;

  /** . */
  private ScopedContext requestScope;

  /** . */
  private final Bridge bridge;

  final VertxRequestContext ctx;

  public VertxWebBridge(Bridge bridge, VertxRequestContext ctx, Application application) {


    //
    this.application = application;
    this.requestScope = null;
    this.bridge = bridge;
    this.ctx = ctx;

  }

  void handle(juzu.impl.bridge.spi.web.Handler handler) {
    try {
      handler.handle(this);
    }
    catch (Throwable throwable) {
      throwable.printStackTrace();
      try {
        ctx.send(Response.status(500));
      }
      catch (IOException e) {
        // Handle me somehow better
        e.printStackTrace();
      }
    }
  }

  @Override
  public WebRequestContext getRequestContext() {
    return ctx;
  }

  public void renderRequestURL(Appendable appendable) throws IOException {
    appendable.append("http://localhost:8080");
  }

  public ScopedContext getRequestScope(boolean create) {
    if (requestScope == null && create) {
      requestScope = new ServletScopedContext(ctx.log);
    }
    return requestScope;
  }

  public ScopedContext getFlashScope(boolean create) {
    return ctx.getCookieScopeContext(CookieScopeContext.FLASH, create);
  }

  public ScopedContext getSessionScope(boolean create) {
    return ctx.getCookieScopeContext(CookieScopeContext.SESSION, create);
  }

  public void purgeSession() {
    throw new UnsupportedOperationException("todo");
  }

  public HttpContext getHttpContext() {
    return this;
  }

  public ClientContext getClientContext() {
    throw new UnsupportedOperationException("todo");
  }

  public ApplicationContext getApplicationContext() {
    return APPLICATION_CONTEXT;
  }


  @Override
  public UserContext getUserContext() {
    return VertxUserContext.INSTANCE;
  }

  // HttpContext implementation ****************************************************************************************

  public Method getMethod() {
    return ctx.method;
  }

  public javax.servlet.http.Cookie[] getCookies() {
    throw new UnsupportedOperationException();
  }

  public String getScheme() {
    return "http";
  }

  public int getServerPort() {
    return application.port;
  }

  public String getServerName() {
    throw new UnsupportedOperationException();
  }

  public String getContextPath() {
    return "/";
  }
}