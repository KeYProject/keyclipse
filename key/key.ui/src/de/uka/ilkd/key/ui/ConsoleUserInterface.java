// This file is part of KeY - Integrated Deductive Software Design
//
// Copyright (C) 2001-2011 Universitaet Karlsruhe (TH), Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
// Copyright (C) 2011-2014 Karlsruhe Institute of Technology, Germany
//                         Technical University Darmstadt, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General
// Public License. See LICENSE.TXT for details.
//

package de.uka.ilkd.key.ui;

import java.io.File;

import org.key_project.utils.collection.ImmutableList;
import org.key_project.utils.collection.ImmutableSLList;

import de.uka.ilkd.key.core.KeYMediator;
import de.uka.ilkd.key.gui.ApplyTacletDialogModel;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.macros.ProofMacro;
import de.uka.ilkd.key.proof.ApplyStrategy;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.TaskFinishedInfo;
import de.uka.ilkd.key.proof.init.InitConfig;
import de.uka.ilkd.key.proof.init.ProblemInitializer;
import de.uka.ilkd.key.proof.init.Profile;
import de.uka.ilkd.key.proof.init.ProofOblInput;
import de.uka.ilkd.key.proof.io.ProblemLoader;
import de.uka.ilkd.key.proof.mgt.ProofEnvironmentEvent;
import de.uka.ilkd.key.util.removegenerics.Main;

/**
 * Implementation of {@link UserInterface} used by command line interface of
 * KeY.
 */
public class ConsoleUserInterface extends AbstractMediatorUserInterface {
   private static final int PROGRESS_BAR_STEPS = 50;
   private static final String PROGRESS_MARK = ">";


   // Substitute for TaskTree (GUI) to facilitate side proofs in console mode
   ImmutableList<Proof> proofStack = ImmutableSLList.<Proof>nil();

   final byte verbosity;
   final KeYMediator mediator;

   // for a progress bar
   int progressMax = 0;

    
    // flag to indicate that a file should merely be loaded not proved. (for
    // "reload" testing)
    private final boolean loadOnly;
    
    
    /**
     * Current key problem file that is attempted to be proven.
     */
    private File keyProblemFile = null;
    
    /**
     * We want to record whether there was a proof that could not be proven.
     * {@link Main} calls System.exit() after all files have been loaded with
     * {@link #loadProblem(java.io.File)}. Program return value depends on
     * whether there has been a proof attempt that was not successful.
     */
    public boolean allProofsSuccessful = true;
    
    public ConsoleUserInterface(byte verbosity, boolean loadOnly) {
        this.verbosity = verbosity;
        this.mediator  = new KeYMediator(this);
        this.loadOnly = loadOnly;
    }

    public ConsoleUserInterface(boolean verbose, boolean loadOnly) {
        this(verbose? Verbosity.DEBUG: Verbosity.NORMAL, loadOnly);
    }

   private void printResults(final int openGoals,
                                  TaskFinishedInfo info,
                                  final Object result2) {
       if (verbosity >= Verbosity.HIGH) {
           System.out.println("]"); // end progress bar
       }
       if (verbosity > Verbosity.SILENT) {
           System.out.println("[ DONE  ... rule application ]");
           if (verbosity >= Verbosity.HIGH) {
               System.out.println("\n== Proof "+ (openGoals > 0 ? "open": "closed")+ " ==");
               final Proof.Statistics stat = info.getProof().statistics();
               System.out.println("Proof steps: "+stat.nodes);
               System.out.println("Branches: "+stat.branches);
               System.out.println("Automode Time: "+stat.autoModeTime+"ms");
               System.out.println("Time per step: "+stat.timePerStep+"ms");
           }
           System.out.println("Number of goals remaining open: " + openGoals);
           System.out.flush();
       }
       // this seems to be a good place to free some memory
       Runtime.getRuntime().gc();

       /*
        * It is assumed that this part of the code is never reached, unless a 
        * value has been assigned to keyProblemFile in method loadProblem(File).
        */
       assert keyProblemFile != null : "Unexcpected null pointer. Trying to"
               + " save a proof but no corresponding key problem file is "
               + "available.";
       allProofsSuccessful &= BatchMode.saveProof(result2, info.getProof(), keyProblemFile);
       /*
        * We "delete" the value of keyProblemFile at this point by assigning
        * null to it. That way we prevent KeY from saving another proof (that
        * belongs to another key problem file) for a key problem file whose
        * execution cycle has already been finished (and whose proof has
        * already been saved). It is assumed that a new value has been assigned
        * beforehand in method loadProblem(File), if this part of the code is
        * reached again.
        */
       keyProblemFile = null;
   }

    @Override
   public void taskFinished(TaskFinishedInfo info) {
       progressMax = 0; // reset progress bar marker
       final Proof proof = info.getProof();
       if (proof==null) {
           if (verbosity > Verbosity.SILENT) {
               System.out.println("Proof loading failed");
               final Object error = info.getResult();
               if (error instanceof Throwable) {
                   ((Throwable) error).printStackTrace();
               }
           }
           System.exit(1);
       }
       final int openGoals = proof.openGoals().size();
       final Object result2 = info.getResult();
       if (info.getSource() instanceof ApplyStrategy ||
           info.getSource() instanceof ProofMacro) {
           if (!isAtLeastOneMacroRunning()) {
               printResults(openGoals, info, result2);
           } else if (!macroChosen()) {
               finish(proof);
           }
       } else if (info.getSource() instanceof ProblemLoader) {
           if (verbosity > Verbosity.SILENT) System.out.println("[ DONE ... loading ]");
           if (result2 != null) {
               if (verbosity > Verbosity.SILENT) System.out.println(result2);
               if (verbosity >= Verbosity.HIGH && result2 instanceof Throwable) {
                   ((Throwable) result2).printStackTrace();
               }
               System.exit(-1);
           }
           if(loadOnly ||  openGoals==0) {
               if (verbosity > Verbosity.SILENT)
                   System.out.println("Number of open goals after loading: " +
                           openGoals);
               System.exit(0);
           }
           if (macroChosen()) {
               applyMacro();
           } else {
               finish(proof);
           }
       }
   }

    @Override
    public void taskStarted(String message, int size) {
        progressMax = size;
        if (verbosity >= Verbosity.HIGH) {
            if (ApplyStrategy.PROCESSING_STRATEGY.equals(message)) {
                System.out.print(message+" ["); // start progress bar
            } else {
                System.out.println(message);
            }
        }
    }

    @Override
    public void loadProblem(File file) {
        /*
         * Current file is stored in a private field.
         * It will be used in method printResults() to determine file names,
         * in which proofs will be written.
         */
        keyProblemFile = file;
        getProblemLoader(file, null, null, mediator).runSynchronously();
    }

    @Override
    public void proofRegistered(ProofEnvironmentEvent event) {
        mediator.setProof(event.getProofList().getFirstProof());
        proofStack = proofStack.prepend(event.getProofList().getFirstProof());
    }
    
    void finish(Proof proof) {
       // setInteractive(false) has to be called because the ruleAppIndex
       // has to be notified that we work in auto mode (CS)
       mediator.setInteractive(false);
       startAndWaitForAutoMode(proof);
       if (verbosity >= Verbosity.HIGH) { // WARNING: Is never executed since application terminates via System.exit() before.
           System.out.println(proof.statistics());
       }
   }

    @Override
    final public void progressStarted(Object sender) {
        // TODO Implement ProblemInitializerListener.progressStarted
        if(verbosity >= Verbosity.DEBUG) {
            System.out.println("ConsoleUserInterface.progressStarted(" + sender + ")");
        }
    }

    @Override
    final public void progressStopped(Object sender) {
        if(verbosity >= Verbosity.DEBUG) {
            System.out.println("ConsoleUserInterface.progressStopped(" + sender + ")");
        }
    }

    @Override
    final public void reportException(Object sender, ProofOblInput input, Exception e) {
        // TODO Implement ProblemInitializerListener.reportException
        if(verbosity >= Verbosity.DEBUG) {
            System.out.println("ConsoleUserInterface.reportException(" + sender + "," + input + "," + e + ")");
            e.printStackTrace();
        }
    }

    @Override
    final public void reportStatus(Object sender, String status, int progress) {
        // TODO Implement ProblemInitializerListener.reportStatus
        if(verbosity >= Verbosity.DEBUG) {
            System.out.println("ConsoleUserInterface.reportStatus(" + sender + "," + status + "," + progress + ")");
        }
    }

    @Override
    final public void reportStatus(Object sender, String status) {
        // TODO Implement ProblemInitializerListener.reportStatus
        if(verbosity >= Verbosity.DEBUG) {
            System.out.println("ConsoleUserInterface.reportStatus(" + sender + "," + status + ")");
        }
    }

    @Override
    final public void resetStatus(Object sender) {
        // TODO Implement ProblemInitializerListener.resetStatus
        if(verbosity >= Verbosity.DEBUG) {
            System.out.println("ConsoleUserInterface.resetStatus(" + sender + ")");
        }
    }

    @Override
    final public void taskProgress(int position) {
        if (verbosity >= Verbosity.HIGH && progressMax > 0) {
            if ((position*PROGRESS_BAR_STEPS) % progressMax == 0) {
                System.out.print(PROGRESS_MARK);
            }
        }
    }

    @Override
    final public void setMaximum(int maximum) {
        // TODO Implement ProgressMonitor.setMaximum
        if(verbosity >= Verbosity.DEBUG) {
            System.out.println("ConsoleUserInterface.setMaximum(" + maximum + ")");
        }
    }

    @Override
    final public void setProgress(int progress) {
        // TODO Implement ProgressMonitor.setProgress
        if(verbosity >= Verbosity.DEBUG) {
            System.out.println("ConsoleUserInterface.setProgress(" + progress + ")");
        }
    }

    @Override
    public void completeAndApplyTacletMatch(ApplyTacletDialogModel[] models, Goal goal) {
        if(verbosity >= Verbosity.DEBUG) {
         System.out.println("Taclet match completion not supported by console.");
        }
    }

   @Override
   final public void openExamples() {
       System.out.println("Open Examples not suported by console UI.");
   }

   @Override
   final public ProblemInitializer createProblemInitializer(Profile profile) {
      ProblemInitializer pi = new ProblemInitializer(this,
            new Services(profile),
            this);
      return pi;
   }

    /**
     * {@inheritDoc}
     */
    @Override
    final public void removeProof(Proof proof) {
        if (proof != null) {
            if (!proofStack.isEmpty()) {
                Proof p = proofStack.head();
                proofStack = proofStack.removeAll(p);
                assert p.name().equals(proof.name());
                mediator.setProof(proofStack.head());
            } else {
                // proofStack might be empty, though proof != null. This can
                // happen for symbolic execution tests, if proofCreated was not
                // called by the test setup.
            }
            proof.dispose();
        }
    }

    @Override
    final public boolean selectProofObligation(InitConfig initConfig) {
        if(verbosity >= Verbosity.DEBUG) {
            System.out.println("Proof Obligation selection not supported by console.");
        }
        return false;
    }
    
   /**
    * {@inheritDoc}
    */
   @Override
   public KeYMediator getMediator() {
      return mediator;
   }
}
