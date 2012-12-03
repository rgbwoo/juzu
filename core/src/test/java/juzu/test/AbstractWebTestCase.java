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

package juzu.test;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import juzu.impl.common.QN;
import juzu.impl.fs.Visitor;
import juzu.impl.fs.spi.ReadWriteFileSystem;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@RunWith(Arquillian.class)
public abstract class AbstractWebTestCase extends AbstractTestCase {

  /** . */
  private static QN applicationName;

  /** . */
  private static boolean asDefault;

  /** . */
  private static CompilerAssert<File, File> compiler;

  /**
   * Returns the currently deployed application name.
   *
   * @return the application name
   */
  public static QN getApplicationName() {
    return applicationName;
  }

  /**
   * Returns true if the currently application name should be the defaulted.
   *
   * @return the as default value
   */
  public static boolean asDefault() {
    return asDefault;
  }

  /**
   * Returns the compiler for the currently deployed web application.
   *
   * @return the compiler
   */
  public static CompilerAssert<File, File> getCompiler() {
    return compiler;
  }

  public static WebArchive createDeployment(boolean asDefault, String... applicationNames) {
    return createDeployment(asDefault, false, applicationNames);
  }

  public static WebArchive createDeployment(boolean asDefault, boolean incremental, String... applicationNames) {

    //
    QN[] applicationQNs = new QN[applicationNames.length];
    QN packageQN = null;
    for (int i = 0;i < applicationNames.length;i++) {
      QN applicationQN = QN.parse(applicationNames[i]);
      applicationQNs[i] = applicationQN;
      packageQN = packageQN == null ? applicationQN : packageQN.getPrefix(applicationQN);
    }

    // Compile classes
    CompilerAssert<File, File> compiler = compiler(incremental, packageQN);
    compiler.assertCompile();

    //
    ReadWriteFileSystem<File> classOutput = compiler.getClassOutput();

    // Create war
    final WebArchive war = ShrinkWrap.create(WebArchive.class, "juzu.war");

    // Add output to war
    try {
      classOutput.traverse(new Visitor.Default<File>() {

        /** . */
//        LinkedList<String> path = new LinkedList<String>();
        StringBuffer path = new StringBuffer();

        @Override
        public void enterDir(File dir, String name) throws IOException {
          if (name.isEmpty()) {
            path.append("classes/");
          } else {
            path.append(name).append('/');
          }
        }

        @Override
        public void leaveDir(File dir, String name) throws IOException {
          if (name.isEmpty()) {
            path.setLength(0);
          } else {
            path.setLength(path.length() - name.length() - 1);
          }
        }

        @Override
        public void file(File file, String name) throws IOException {
          path.append(name);
          String target = path.toString();
          path.setLength(path.length() - name.length());
          war.addAsWebInfResource(new ByteArrayAsset(new FileInputStream(file)), target);
        }
      });
    }
    catch (IOException e) {
      throw failure(e);
    }

    // Set static state that we may need later
    AbstractWebTestCase.applicationName = applicationQNs.length > 0 ? applicationQNs[0] : null;
    AbstractWebTestCase.asDefault = asDefault;
    AbstractWebTestCase.compiler = compiler;

    //
    return war;
  }

  @ArquillianResource
  protected URL deploymentURL;

  /**
   * Returns the portlet URL for standalone portlet unit test.
   *
   * @return the base portlet URL
   */
  public URL getPortletURL() {
    try {
      return deploymentURL.toURI().resolve("embed/JuzuPortlet").toURL();
    }
    catch (Exception e) {
      throw failure(e);
    }
  }

  public void assertInternalError() {
    WebClient client = new WebClient();
    try {
      Page page = client.getPage(deploymentURL + "/juzu");
      throw failure("Was expecting an internal error instead of page " + page.toString());
    }
    catch (FailingHttpStatusCodeException e) {
      assertEquals(500, e.getStatusCode());
    }
    catch (IOException e) {
      throw failure("Was not expecting io exception", e);
    }
  }

  public UserAgent assertInitialPage() {
    return new UserAgent(applicationURL());
  }

  public URL applicationURL() {
    return applicationURL("");
  }

  public URL applicationURL(String path) {
    try {
      return deploymentURL.toURI().resolve(getApplicationName().getLastName() + path).toURL();
    }
    catch (Exception e) {
      throw failure("Could not build application url " + path, e);
    }
  }
}
