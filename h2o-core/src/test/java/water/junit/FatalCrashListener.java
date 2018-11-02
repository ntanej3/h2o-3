package water.junit;

import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

@Ignore("Support for tests, but no actual tests here")
public class FatalCrashListener extends RunListener {

  private static final File BUILD_DIR = new File("build");
  private PrintWriter pw;

  public FatalCrashListener() throws FileNotFoundException {
    pw = new PrintWriter(new File(BUILD_DIR, "adhoc-output.txt"));
    pw.println("Hello from " + getClass());
  }

  @Override
  public void testStarted(Description description) throws Exception {
    pw.printf(":STARTED:: %s:%s = '%s'%n",
        description.getClassName(),
        description.getMethodName(),
        description.getDisplayName()
    );
    pw.flush();
  }

  @Override
  public void testFinished(Description description) throws Exception {
    pw.printf(":FINISHED:: %s:%s = '%s'%n",
        description.getClassName(),
        description.getMethodName(),
        description.getDisplayName()
    );
    pw.flush();
  }

  @Override
  public void testFailure(Failure failure) {
    final Throwable exception = failure.getException();

    pw.printf("::: %s : '%s' = '%s'%n",
        failure.getTestHeader(),
        failure.getDescription(),
        failure.getMessage()
    );
    pw.flush();

    // ignore recoverable exceptions
    if (exception instanceof AssertionError) return;

    // any other causes JVM exit
    exception.printStackTrace();
    System.exit(-1);
  }
}
