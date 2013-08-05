package de.uka.ilkd.key.taclettranslation.lemma;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import de.uka.ilkd.key.collection.DefaultImmutableSet;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.proof.ProofAggregate;
import de.uka.ilkd.key.proof.init.IPersistablePO;
import de.uka.ilkd.key.proof.init.InitConfig;
import de.uka.ilkd.key.proof.init.ProblemInitializer;
import de.uka.ilkd.key.proof.init.Profile;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.proof.init.ProofOblInput;
import de.uka.ilkd.key.rule.Taclet;
import de.uka.ilkd.key.taclettranslation.lemma.TacletSoundnessPOLoader.LoaderListener;
import de.uka.ilkd.key.taclettranslation.lemma.TacletSoundnessPOLoader.TacletFilter;
import de.uka.ilkd.key.taclettranslation.lemma.TacletSoundnessPOLoader.TacletInfo;

/**
 * The Class TacletProofObligationInput is a special purpose proof obligations
 * for taclet proofs.
 * 
 * A proof for a KeY system-taclet can thus be reloaded and checked against the
 * current environment.
 * 
 * @author mattias ulbrich
 */
public class TacletProofObligationInput implements ProofOblInput, IPersistablePO {

    private String tacletName;
    private ProofAggregate proofObligation;
    private final InitConfig initConfig;

    // The following may all possibly be null
    private String definitionFile;
    private String tacletFile;
    private String[] axiomFiles;

    /**
     * This filter is used to filter out precisely that taclet which has the
     * required name.
     */
    private TacletFilter filter = new TacletFilter() {
        @Override 
        public ImmutableSet<Taclet> filter(List<TacletInfo> taclets) {
            Name name = new Name(tacletName);
            for (TacletInfo tacletInfo : taclets) {
                if(tacletInfo.getTaclet().name().equals(name)) {
                    return DefaultImmutableSet.<Taclet>nil().add(tacletInfo.getTaclet());
                }
            }
            return DefaultImmutableSet.nil();
        }
    };

    /**
     * This listener communicates with the PO loader.
     */
    private LoaderListener listener = new LoaderListener() {

        @Override 
        public void stopped(Throwable exception) {
            System.err.println("Exception while loading proof obligation for taclet:");
            exception.printStackTrace();
        }

        @Override 
        public void stopped(ProofAggregate p, ImmutableSet<Taclet> taclets, boolean addAsAxioms) {
            System.out.println(p);
            proofObligation = p;
        }

        @Override 
        public void started() {
        }

        @Override 
        public void resetStatus(Object sender) {
        }

        @Override 
        public void reportStatus(Object sender, String string) {
        }

        @Override 
        public void progressStarted(Object sender) {
        }
    };

    /**
     * Instantiates a new taclet proof obligation input object.
     * 
     * @param tacletName
     *            the name of the taclet which is to be created
     * @param initConfig
     *            the initconfig under which the PO is to be examined
     */
    public TacletProofObligationInput(String tacletName, InitConfig initConfig) {
        this.tacletName = tacletName;
        this.initConfig = initConfig;
    }

    /*
     * Fill in only the necessary info.
     */
    @Override 
    public void fillSaveProperties(Properties properties) throws IOException {
        properties.setProperty(IPersistablePO.PROPERTY_CLASS, getClass().getCanonicalName());
        properties.setProperty(IPersistablePO.PROPERTY_NAME, name());

        if (tacletFile != null) {
            properties.setProperty("tacletFile", tacletFile);
        }
        if (definitionFile != null) {
            properties.setProperty("definitionFile", definitionFile.toString());
        }
        if (axiomFiles != null) {
            for (int i = 0; i < axiomFiles.length; i++) {
                String name = "axiomFile" + (i == 0 ? "" : (i + 1));
                properties.setProperty(name, axiomFiles[i]);
            }
        }
    }

    @Override 
    public String name() {
        return tacletName;
    }

    /*
     * use the TacletLoader and TacletSoundlessPOLoader to generate the PO.
     */
    @Override 
    public void readProblem() throws ProofInputException {
        TacletLoader loader = null;
        if (tacletFile == null) {
            // prove a KeY taclet
            loader = new TacletLoader.KeYsTacletsLoader(null, null, getProfile());
        } else {

            final ProblemInitializer problemInitializer =
                    new ProblemInitializer(getProfile());
            loader = new TacletLoader.TacletFromFileLoader(null, null, problemInitializer,
                    new File(definitionFile), new File(tacletFile), 
                    fileCollection(axiomFiles), initConfig.getProofEnv());
        }

        TacletSoundnessPOLoader poloader =
                new TacletSoundnessPOLoader(listener, filter, true, loader);

        poloader.startSynchronously();
    }

    public Profile getProfile() {
        return initConfig.getProfile();
    }

    private static Collection<File> fileCollection(String[] strings) {
        ArrayList<File> result = new ArrayList<File>();
        for (int i = 0; i < strings.length; i++) {
            result.add(new File(strings[i]));
        }
        return result;
    }

    /*
     * just deliver the precalculated PO
     */
    @Override 
    public ProofAggregate getPO() throws ProofInputException {
        assert proofObligation != null : 
            "readProblem should have been called first";
        return proofObligation;
    }

    @Override public boolean implies(ProofOblInput po) {
        return this == po;
    }

    public static LoadedPOContainer loadFrom(InitConfig initConfig, Properties properties) {
        String tacletName = properties.getProperty(PROPERTY_NAME);
        TacletProofObligationInput proofOblInput =
                new TacletProofObligationInput(tacletName, initConfig);
        proofOblInput.setLoadInfo(properties);
        return new LoadedPOContainer(proofOblInput);
    }

    private void setLoadInfo(Properties properties) {
        this.tacletFile = properties.getProperty("tacletFile");
        this.definitionFile = properties.getProperty("definitionFile");
        List<String> axioms = new ArrayList<String>();
        String name = "axiomFile";
        String axFile = properties.getProperty(name);
        while (axFile != null) {
            axioms.add(axFile);
            name = "axiomFile" + (axioms.size() + 1);
            axFile = properties.getProperty(name);
        }
        this.axiomFiles = (String[]) axioms.toArray(new String[axioms.size()]);
    }

    public void setLoadInfo(File tacletFile, File definitionFile,
            Collection<File> axiomFiles) {
        this.tacletFile = tacletFile.toString();
        this.definitionFile = definitionFile.toString();
        this.axiomFiles = new String[axiomFiles.size()];

        int i = 0;
        for (File file : axiomFiles) {
            this.axiomFiles[i] = file.toString();
            i++;
        }
    }
}
