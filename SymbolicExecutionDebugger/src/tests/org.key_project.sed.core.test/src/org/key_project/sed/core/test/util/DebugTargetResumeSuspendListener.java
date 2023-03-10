/*******************************************************************************
 * Copyright (c) 2014 Karlsruhe Institute of Technology, Germany
 *                    Technical University Darmstadt, Germany
 *                    Chalmers University of Technology, Sweden
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Technical University Darmstadt - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.key_project.sed.core.test.util;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.key_project.util.java.ObjectUtil;

/**
 * This {@link IDebugEventSetListener} can be used to execute some code
 * which cause a suspend/resume event (e.g. resume, step into, step over or step return)
 * and to block the test thread with {@link SWTBot} functionality until
 * both events are detected.
 * @author Martin Hentschel
 */
public class DebugTargetResumeSuspendListener implements IDebugEventSetListener {
   /**
    * The {@link IDebugTarget} to wait for resume/suspend events.
    */
   private final IDebugTarget target;
   
   /**
    * Indicates if events from children like an {@link IThread} are also accepted.
    */
   private final boolean acceptChildEvents;
   
   /**
    * Indicates that the resume event was detected.
    */
   private boolean resumeDetected;
   
   /**
    * Indicates that the suspend event was detected.
    */
   private boolean suspendDetected;

   /**
    * Constructor.
    * @param target The {@link IDebugTarget} to wait for resume/suspend events.
    * @param acceptChildEvents Indicates if events from children like an {@link IThread} are also accepted.
    */
   public DebugTargetResumeSuspendListener(IDebugTarget target, 
                                           boolean acceptChildEvents) {
      this.target = target;
      this.acceptChildEvents = acceptChildEvents;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void handleDebugEvents(DebugEvent[] events) {
      for (DebugEvent event : events) {
         Object eventSource = event.getSource();
         if (acceptChildEvents && eventSource instanceof IDebugElement) {
            eventSource = ((IDebugElement) eventSource).getDebugTarget();
         }
         if (ObjectUtil.equals(target, eventSource)) {
            if (event.getKind() == DebugEvent.SUSPEND) {
               suspendDetected = true;
            }
            else if (event.getKind() == DebugEvent.RESUME) {
               resumeDetected = true;
            }
         }
      }
   }
   
   /**
    * Starts listening for events.
    */
   public void start() {
      resumeDetected = false;
      suspendDetected = false;
      DebugPlugin.getDefault().addDebugEventListener(this);
   }
   
   /**
    * Stops listening for events.
    */
   public void stop() {
      DebugPlugin.getDefault().removeDebugEventListener(this);
   }

   /**
    * Waits until resume/suspend events are detected.
    * @param bot The {@link SWTBot} to use.
    */
   public void waitUntilResumeSuspendCompleted(SWTBot bot) {
      bot.waitUntil(new ICondition() {
         @Override
         public boolean test() throws Exception {
            return resumeDetected && suspendDetected && target.canResume();
         }
         
         @Override
         public void init(SWTBot bot) {
         }
         
         @Override
         public String getFailureMessage() {
            return "Suspend, resume events not detected.";
         }
      });
   }
   
   /**
    * Runs the {@link Runnable} which cause suspend/resume events and waits
    * until both events are detected.
    * @param bot The {@link SWTBot} to use.
    * @param target The {@link IDebugTarget} to work with.
    * @param acceptChildEvents Indicates if events from children like an {@link IThread} are also accepted.
    * @param run The {@link Runnable} to execute which cause suspend/resume events.
    */
   public static void run(SWTBot bot, IDebugTarget target, boolean acceptChildEvents, Runnable run) {
      DebugTargetResumeSuspendListener listener = new DebugTargetResumeSuspendListener(target, acceptChildEvents);
      try {
         listener.start();
         if (run != null) {
            run.run();
         }
         listener.waitUntilResumeSuspendCompleted(bot);
      }
      finally {
         listener.stop();
      }
   }
}