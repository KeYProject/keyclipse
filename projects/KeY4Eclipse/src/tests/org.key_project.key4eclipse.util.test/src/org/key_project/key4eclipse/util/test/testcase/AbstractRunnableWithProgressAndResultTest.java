package org.key_project.key4eclipse.util.test.testcase;

import java.lang.reflect.InvocationTargetException;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;
import org.key_project.key4eclipse.util.java.thread.AbstractRunnableWithProgressAndResult;
import org.key_project.key4eclipse.util.java.thread.IRunnableWithProgressAndResult;

/**
 * Tests for {@link AbstractRunnableWithProgressAndResult}.
 * @author Martin Hentschel
 */
public class AbstractRunnableWithProgressAndResultTest extends TestCase {
    /**
     * Tests {@link AbstractRunnableWithProgressAndResult#getResult()}.
     */
    @Test
    public void testGetResult() throws InvocationTargetException, InterruptedException {
       IRunnableWithProgressAndResult<String> run = new AbstractRunnableWithProgressAndResult<String>() {
          @Override
          public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
              setResult("The result.");
          }
       };
       assertNull(run.getResult());
       run.run(new NullProgressMonitor());
       assertEquals("The result.", run.getResult());
    }
}