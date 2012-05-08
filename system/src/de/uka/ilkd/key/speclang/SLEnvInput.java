// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.speclang;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.swing.*;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.collection.ImmutableSet;
import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.gui.configuration.GeneralSettings;
import de.uka.ilkd.key.gui.configuration.ProofSettings;
import de.uka.ilkd.key.java.JavaInfo;
import de.uka.ilkd.key.java.JavaReduxFileCollection;
import de.uka.ilkd.key.java.ProgramElement;
import de.uka.ilkd.key.java.Recoder2KeY;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.declaration.ClassDeclaration;
import de.uka.ilkd.key.java.declaration.InterfaceDeclaration;
import de.uka.ilkd.key.java.declaration.TypeDeclaration;
import de.uka.ilkd.key.java.statement.LoopStatement;
import de.uka.ilkd.key.java.visitor.JavaASTCollector;
import de.uka.ilkd.key.logic.op.ProgramMethod;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.proof.io.AbstractEnvInput;
import de.uka.ilkd.key.proof.io.KeYFile;
import de.uka.ilkd.key.proof.io.RuleSource;
import de.uka.ilkd.key.proof.mgt.SpecificationRepository;
import de.uka.ilkd.key.speclang.jml.JMLSpecExtractor;
import de.uka.ilkd.key.util.KeYResourceManager;


/** 
 * EnvInput for standalone specification language front ends.
 */
public final class SLEnvInput extends AbstractEnvInput {
    
            
    //-------------------------------------------------------------------------
    //constructors
    //-------------------------------------------------------------------------
    
    public SLEnvInput(String javaPath,
  	      	      List<File> classPath,
  	      	      File bootClassPath) {
	super(getLanguage() + " specifications", 
	      javaPath, 
	      classPath, 
	      bootClassPath);
    }
    
    
    public SLEnvInput(String javaPath) {
	this(javaPath, null, null);
    }    
    

    
    //-------------------------------------------------------------------------
    //internal methods
    //-------------------------------------------------------------------------
    
    private static String getLanguage() {
        GeneralSettings gs 
            = ProofSettings.DEFAULT_SETTINGS.getGeneralSettings();
        if(gs.useJML() && gs.useOCL()) {
            return "JML/OCL";
        } else if(gs.useJML()) {
            return "JML";
        } else if(gs.useOCL()) {
            return "OCL";
        } else {
            return "no";
        }
    }    
    
    private KeYJavaType[] sortKJTs(KeYJavaType[] kjts) {
        
        Arrays.sort(kjts, new Comparator<KeYJavaType> () {
            public int compare(KeYJavaType o1, KeYJavaType o2) {
        	assert o1.getFullName() != null : "type without name: " + o1;
        	assert o2.getFullName() != null : "type without name: " + o2;
                return o1.getFullName().compareTo(o2.getFullName());
            }
        });
        
        return kjts;
    }
    
    
    private void showWarningDialog(ImmutableSet<PositionedString> warnings) {
        if(!MainWindow.visible) {
            return;
        }
                
        final JDialog dialog = new JDialog(MainWindow.getInstance(), 
                                           getLanguage() + " warning", 
                                           true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        Container pane = dialog.getContentPane();
        pane.setLayout(new BorderLayout());
        
        //top label
        JLabel label = new JLabel("The following non-fatal "
                                  + "problems occurred when translating your " 
                                  + getLanguage() + " specifications:");
        label.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
        pane.add(label, BorderLayout.NORTH);
          
        //scrollable warning list
        JScrollPane scrollpane = new JScrollPane();
        scrollpane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JList list = new JList(warnings.toArray(new PositionedString[warnings.size()]));
        list.setBorder(BorderFactory.createLoweredBevelBorder());
        scrollpane.setViewportView(list);
        pane.add(scrollpane, BorderLayout.CENTER);
    
        //ok button
        final JButton button = new JButton("OK");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.setVisible(false);
            }
        });
        Dimension buttonDim = new Dimension(100, 27);
        button.setPreferredSize(buttonDim);
        button.setMinimumSize(buttonDim);
        JPanel panel = new JPanel();
        panel.add(button);
        pane.add(panel, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(button);
        
        button.registerKeyboardAction(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    if(event.getActionCommand().equals("ESC")) {
                        button.doClick();
                    }
                }
            },
            "ESC",
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        
        dialog.setSize(700, 300);
        dialog.setLocationRelativeTo(MainWindow.getInstance());
        dialog.setVisible(true);
        dialog.dispose();
    }
    
    
    private void createDLLibrarySpecsHelper(Set<KeYJavaType> allKJTs,
	                                    String path) 
    		throws ProofInputException {
        for(KeYJavaType kjt : allKJTs) {
            if(kjt.getJavaType() instanceof TypeDeclaration
               && ((TypeDeclaration)kjt.getJavaType()).isLibraryClass()) {
                final String filePath
                	= path + "/" + kjt.getFullName().replace(".", "/") 
                	       + ".key";
                RuleSource rs = null;
                
                //external or internal path?
                if(filePath.startsWith("/")) {
                    File file = new File(filePath);
                    if(file.exists()) {
                	rs = RuleSource.initRuleFile(file);
                    }
                } else {
                    URL url = KeYResourceManager.getManager().getResourceFile(
                				Recoder2KeY.class, 
                				filePath);
                    if(url != null) {
                	rs = RuleSource.initRuleFile(url);
                    }
                }
                
                //rule source found? -> read
                if(rs != null) {
                    final KeYFile keyFile = new KeYFile(path, rs, null);
                    keyFile.setInitConfig(initConfig);
                    keyFile.read();
                }
            }
        }	
    }
    
    
    /** 
     * For all library classes C, look for file C.key in same 
     * directory; if found, read specifications from this file.
     */
    private void createDLLibrarySpecs() throws ProofInputException {
        final Set<KeYJavaType> allKJTs 
		= initConfig.getServices().getJavaInfo().getAllKeYJavaTypes();			
	
	//either boot class path or JavaRedux
	if(bootClassPath != null) {
	    createDLLibrarySpecsHelper(allKJTs, 
		                       bootClassPath.getAbsolutePath());	    
	} else {
            String path = JavaReduxFileCollection.JAVA_SRC_DIR;
            if (!initConfig.getProfile().getInternalClassDirectory().isEmpty()) { 
        	path += "/" + initConfig.getProfile().getInternalClassDirectory();
            }
            createDLLibrarySpecsHelper(allKJTs, path);
	}
        
        //if applicable: class path
        if(classPath != null) {
            for(File file : classPath) {
        	createDLLibrarySpecsHelper(allKJTs, file.getAbsolutePath());
            }
        }
    }
    
    
    private void createSpecs(SpecExtractor specExtractor) 
            throws ProofInputException {
        final JavaInfo javaInfo 
            = initConfig.getServices().getJavaInfo();
        final SpecificationRepository specRepos 
            = initConfig.getServices().getSpecificationRepository();
        
        //read DL library specs before any other specs
        createDLLibrarySpecs();        
       
        //sort types alphabetically (necessary for deterministic names)
        final Set<KeYJavaType> allKeYJavaTypes = javaInfo.getAllKeYJavaTypes();
        final KeYJavaType[] kjts = 
            sortKJTs(allKeYJavaTypes.toArray(new KeYJavaType[allKeYJavaTypes.size()]));
        
        //create specifications for all types
        for(KeYJavaType kjt : kjts) {
            if(!(kjt.getJavaType() instanceof ClassDeclaration 
        	  || kjt.getJavaType() instanceof InterfaceDeclaration)) {
        	continue;
            }
            
            //class invariants, represents clauses, ...
            final ImmutableSet<SpecificationElement> classSpecs 
            	= specExtractor.extractClassSpecs(kjt);
            specRepos.addSpecs(classSpecs);
            
            // check whether a static invariant is present
            boolean staticInvPresent = false;
            for (SpecificationElement s: classSpecs){
                if (s instanceof ClassInvariant && ((ClassInvariant)s).isStatic()) {
                    staticInvPresent = true;
                    break;
                }
            }
            
            //contracts, loop invariants
            final ImmutableList<ProgramMethod> pms 
                = javaInfo.getAllProgramMethodsLocallyDeclared(kjt);
            for(ProgramMethod pm : pms) {
                //contracts
        	final ImmutableSet<SpecificationElement> methodSpecs
        	    = specExtractor.extractMethodSpecs(pm,staticInvPresent);
        	specRepos.addSpecs(methodSpecs);
                
                //loop invariants
                final JavaASTCollector collector 
                    = new JavaASTCollector(pm.getBody(), LoopStatement.class);
                collector.start();
                for(ProgramElement loop : collector.getNodes()) {
                    LoopInvariant inv = specExtractor.extractLoopInvariant(
                	    			pm, 
                        			(LoopStatement) loop);
                    if(inv != null) {
                        specRepos.setLoopInvariant(inv);
                    }
                }
            }
            
            //constructor contracts
            final ImmutableList<ProgramMethod> constructors 
            	= javaInfo.getConstructors(kjt);
            for(ProgramMethod constructor : constructors) {
        	assert constructor.isConstructor();
        	final ImmutableSet<SpecificationElement> constructorSpecs 
			= specExtractor.extractMethodSpecs(constructor);
        	specRepos.addSpecs(constructorSpecs);
            }
        }

        //add initially clauses to constructor contracts
        specRepos.createContractsFromInitiallyClauses();
        
        //show warnings to user
        ImmutableSet<PositionedString> warnings = specExtractor.getWarnings();
        if(warnings != null && warnings.size() > 0) {
            showWarningDialog(warnings);
        }
    }

    
    
    //-------------------------------------------------------------------------
    //public interface
    //-------------------------------------------------------------------------

    @Override
    public void read() throws ProofInputException {
        if(initConfig == null) {
            throw new IllegalStateException("InitConfig not set.");
        }
            
        final GeneralSettings gs 
            = ProofSettings.DEFAULT_SETTINGS.getGeneralSettings();
        if(gs.useJML()) {
            createSpecs(new JMLSpecExtractor(initConfig.getServices()));
        }
    }
}
