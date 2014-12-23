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

import de.uka.ilkd.key.core.Main;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.io.ProofSaver;

public class BatchMode {

    private String fileName;

    // flag to indicate that a file should merely be loaded not proved. (for
    // "reload" testing)
    private final boolean loadOnly;

    public BatchMode(String fileName, boolean loadOnly) {
        this.fileName = fileName;
        this.loadOnly = loadOnly;
    }

//    public void autoRun() {
//    }


   public void finishedBatchMode (Object result,
                                  Proof proof) {

        if ( Main.getStatisticsFile() != null )
            printStatistics ( Main.getStatisticsFile(), result.toString(),
                              proof.statistics(), proof.closed() );

        if (result instanceof Throwable) {
            // Error in batchMode. Terminate with status -1.
            System.err.println("An error occurred during batch mode:");
            System.err.println(""+result);
            System.exit ( -1 );
        }

        // Save the proof before exit.

        String baseName = fileName;
        int idx = baseName.indexOf(".key");
        if (idx == -1) {
            idx = baseName.indexOf(".proof");
        }
        baseName = baseName.substring(0, idx==-1 ? baseName.length() : idx);

        File f;
        int counter = 0;
        do {

            f = new File(baseName + ".auto."+ counter +".proof");
            counter++;
        } while (f.exists());

        try {
            // a copy with running number to compare different runs
            saveProof (proof, f.getAbsolutePath() );
            // save current proof under common name as well
            saveProof (proof, baseName + ".auto.proof" );
        } catch (IOException e) {
            System.exit( 1 );
        }


        if (proof.openGoals ().size () == 0) {
            // Says that all Proofs have succeeded
            System.exit ( 0 );
        } else {
            // Says that there is at least one open Proof
            System.exit ( 1 );
        }
    }

    /**
     * used when in batch mode to write out some statistic data
     * @param file the String with the filename where to write the statistic data
     * @param result the Object encapsulating information about the result, e.g.
     *        String "Error" if an error has occurred.
     * @param statistics the proof statistics object containing several proof
     *        statistics like the number of applied rules and so on.
     * @param proofClosed information whether the proof has been closed.
     */
    private void printStatistics(String file, Object result,
                                 Proof.Statistics statistics,
                                 boolean proofClosed) {
        
        // get current memory consumption (after GC) in kB
        Runtime.getRuntime().gc();
        final long memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024;
        
        try {
            final boolean fileExists = (new File(file)).exists();
            final FileWriter statisticsFW = new FileWriter ( file, true );
            final PrintWriter statPrinter = new PrintWriter ( statisticsFW );

            if (!fileExists) {
                statPrinter.println("Name | Total rule apps | Nodes | " +
                        "Branches | Overall time | Automode time | " +
                        "Closed | Time per step | Memory");
            }

            String name = fileName;
            final int slashIndex = name.lastIndexOf ( "examples/" );
            if ( slashIndex >= 0 )
                name = name.substring ( slashIndex );

            statPrinter.print ( name + " | " );
            if ("Error".equals ( result ) )
                statPrinter.println ( "-1 | -1 | -1 | -1 | -1 | -1 | -1 | -1" );
            else
                statPrinter.println (statistics.totalRuleApps + " | " +
                                     statistics.nodes + " | " +
                                     statistics.branches + " | " +
                                     statistics.time + " | " +
                                     statistics.autoModeTime + " | " +
                                     (proofClosed ? 1 : 0) + " | " +
                                     statistics.timePerStep + " | " +
                                     memory);
            statPrinter.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    private void saveProof(Proof proof, String filename) throws IOException {
        ProofSaver saver =
        		new ProofSaver(proof, filename, Main.INTERNAL_VERSION);
        saver.save();
    }

    public boolean isLoadOnly() {
        return loadOnly;
    }


}