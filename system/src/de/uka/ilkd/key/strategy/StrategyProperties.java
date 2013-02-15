// This file is part of KeY - Integrated Deductive Software Design 
//
// Copyright (C) 2001-2011 Universitaet Karlsruhe (TH), Germany 
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
// Copyright (C) 2011-2013 Karlsruhe Institute of Technology, Germany 
//                         Technical University Darmstadt, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General 
// Public License. See LICENSE.TXT for details.
// 


package de.uka.ilkd.key.strategy;

import java.util.Map;
import java.util.Properties;
import java.util.Set;


public final class StrategyProperties extends Properties {
    
    /**
     * 
     */
    private static final long serialVersionUID = 8651946636880456925L;
    public final static String STOPMODE_OPTIONS_KEY = "STOPMODE_OPTIONS_KEY";
    public final static String STOPMODE_DEFAULT = "STOPMODE_DEFAULT";
    public final static String STOPMODE_NONCLOSE = "STOPMODE_NONCLOSE";
    
    public final static String RETREAT_MODE_OPTIONS_KEY = "RETREAT_MODE_OPTIONS_KEY";
    public final static String RETREAT_MODE_NONE = "RETREAT_MODE_NONE";
    public final static String RETREAT_MODE_RETREAT = "RETREAT_MODE_RETREAT";

    public final static String SPLITTING_OPTIONS_KEY = "SPLITTING_OPTIONS_KEY";
    public final static String SPLITTING_NORMAL = "SPLITTING_NORMAL";
    public final static String SPLITTING_OFF = "SPLITTING_OFF";
    public final static String SPLITTING_DELAYED = "SPLITTING_DELAYED";

    public final static String LOOP_OPTIONS_KEY = "LOOP_OPTIONS_KEY";
    public final static String LOOP_EXPAND = "LOOP_EXPAND";
    public final static String LOOP_EXPAND_BOUNDED = "LOOP_EXPAND_BOUNDED"; //Used for test generation chrisg
    public final static String LOOP_INVARIANT = "LOOP_INVARIANT";
    public final static String LOOP_NONE = "LOOP_NONE";
    
    public final static String BLOCK_OPTIONS_KEY = "BLOCK_OPTIONS_KEY";
    public final static String BLOCK_CONTRACT = "BLOCK_CONTRACT";
    public final static String BLOCK_EXPAND = "BLOCK_EXPAND";
    public final static String BLOCK_NONE = "BLOCK_NONE";
    
    public final static String METHOD_OPTIONS_KEY = "METHOD_OPTIONS_KEY";
    public final static String METHOD_EXPAND = "METHOD_EXPAND";
    public final static String METHOD_CONTRACT = "METHOD_CONTRACT";
    public final static String METHOD_NONE = "METHOD_NONE";
    
    public final static String DEP_OPTIONS_KEY = "DEP_OPTIONS_KEY";
    public final static String DEP_ON = "DEP_ON";
    public final static String DEP_OFF = "DEP_OFF";

    public final static String QUERY_OPTIONS_KEY = "QUERY_NEW_OPTIONS_KEY";
    public final static String QUERY_ON = "QUERY_ON";
    public final static String QUERY_RESTRICTED = "QUERY_RESTRICTED";
    public final static String QUERY_OFF = "QUERY_OFF";

    public final static String QUERYAXIOM_OPTIONS_KEY = "QUERYAXIOM_OPTIONS_KEY";
    public final static String QUERYAXIOM_ON  = "QUERYAXIOM_ON";
    public final static String QUERYAXIOM_OFF = "QUERYAXIOM_OFF";
    
    public final static String NON_LIN_ARITH_OPTIONS_KEY = "NON_LIN_ARITH_OPTIONS_KEY";
    public final static String NON_LIN_ARITH_NONE = "NON_LIN_ARITH_NONE";
    public final static String NON_LIN_ARITH_DEF_OPS = "NON_LIN_ARITH_DEF_OPS";
    public final static String NON_LIN_ARITH_COMPLETION = "NON_LIN_ARITH_COMPLETION";

    public final static String QUANTIFIERS_OPTIONS_KEY = "QUANTIFIERS_OPTIONS_KEY";
    public final static String QUANTIFIERS_NONE = "QUANTIFIERS_NONE";
    public final static String QUANTIFIERS_NON_SPLITTING = "QUANTIFIERS_NON_SPLITTING";
    public final static String QUANTIFIERS_NON_SPLITTING_WITH_PROGS = "QUANTIFIERS_NON_SPLITTING_WITH_PROGS";
    public final static String QUANTIFIERS_INSTANTIATE = "QUANTIFIERS_INSTANTIATE";

    public final static String VBT_PHASE = "VBT_PHASE"; //Used for verification-based testing
    public final static String VBT_SYM_EX = "VBT_SYM_EX";
    public final static String VBT_QUAN_INST = "VBT_QUAN_INST";
    public final static String VBT_MODEL_GEN = "VBT_MODEL_GEN";
    
    //chrisg
    public final static String AUTO_INDUCTION_OPTIONS_KEY          = "AUTO_INDUCTION_OPTIONS_KEY"; 
    public final static String AUTO_INDUCTION_OFF      = "AUTO_INDUCTION_OFF"; 
    public final static String AUTO_INDUCTION_RESTRICTED      = "AUTO_INDUCTION_RESTRICTED"; 
    public final static String AUTO_INDUCTION_ON       = "AUTO_INDUCTION_ON"; 
    public final static String AUTO_INDUCTION_LEMMA_ON = "AUTO_INDUCTION_LEMMA_ON";

    public final static int USER_TACLETS_NUM = 3;
    private final static String USER_TACLETS_OPTIONS_KEY_BASE = "USER_TACLETS_OPTIONS_KEY";
    public static String USER_TACLETS_OPTIONS_KEY(int i)
                             { return USER_TACLETS_OPTIONS_KEY_BASE + i; }
    public final static String USER_TACLETS_OFF = "USER_TACLETS_OFF";
    public final static String USER_TACLETS_LOW = "USER_TACLETS_LOW";
    public final static String USER_TACLETS_HIGH = "USER_TACLETS_HIGH";

    //String identities.
    private static final String[] stringPool = {
    	STOPMODE_OPTIONS_KEY, STOPMODE_DEFAULT, STOPMODE_NONCLOSE,
    	RETREAT_MODE_OPTIONS_KEY, RETREAT_MODE_NONE, RETREAT_MODE_RETREAT,
    	SPLITTING_OPTIONS_KEY, SPLITTING_NORMAL, SPLITTING_OFF, SPLITTING_DELAYED,
    	LOOP_OPTIONS_KEY, LOOP_EXPAND, LOOP_EXPAND_BOUNDED, LOOP_INVARIANT, LOOP_NONE,
    	BLOCK_OPTIONS_KEY, BLOCK_CONTRACT, BLOCK_EXPAND, BLOCK_NONE,
    	METHOD_OPTIONS_KEY, METHOD_EXPAND, METHOD_CONTRACT, METHOD_NONE,
    	DEP_OPTIONS_KEY, DEP_ON, DEP_OFF,
    	QUERY_OPTIONS_KEY, QUERY_ON, QUERY_RESTRICTED, QUERY_OFF,
    	QUERYAXIOM_OPTIONS_KEY, QUERYAXIOM_ON, QUERYAXIOM_OFF,
    	NON_LIN_ARITH_OPTIONS_KEY, NON_LIN_ARITH_NONE, NON_LIN_ARITH_DEF_OPS, NON_LIN_ARITH_COMPLETION,
    	QUANTIFIERS_OPTIONS_KEY, QUANTIFIERS_NONE, QUANTIFIERS_NON_SPLITTING, QUANTIFIERS_NON_SPLITTING_WITH_PROGS, QUANTIFIERS_INSTANTIATE,
    	VBT_PHASE, VBT_SYM_EX, VBT_QUAN_INST, VBT_MODEL_GEN,
    	AUTO_INDUCTION_OPTIONS_KEY, AUTO_INDUCTION_OFF, AUTO_INDUCTION_RESTRICTED, AUTO_INDUCTION_ON,  AUTO_INDUCTION_LEMMA_ON,
    	USER_TACLETS_OPTIONS_KEY_BASE, USER_TACLETS_OFF, USER_TACLETS_LOW, USER_TACLETS_HIGH, 
    	USER_TACLETS_OPTIONS_KEY(1), USER_TACLETS_OPTIONS_KEY(2), USER_TACLETS_OPTIONS_KEY(3)};
    
   
    private static final Properties defaultMap = new Properties();
    
    static {
        defaultMap.setProperty(SPLITTING_OPTIONS_KEY, SPLITTING_DELAYED);
        defaultMap.setProperty(LOOP_OPTIONS_KEY, LOOP_INVARIANT);
        defaultMap.setProperty(BLOCK_OPTIONS_KEY, BLOCK_CONTRACT);
        defaultMap.setProperty(METHOD_OPTIONS_KEY, METHOD_CONTRACT);
        defaultMap.setProperty(DEP_OPTIONS_KEY, DEP_ON);
        defaultMap.setProperty(QUERY_OPTIONS_KEY, QUERY_OFF);
        defaultMap.setProperty(QUERYAXIOM_OPTIONS_KEY, QUERYAXIOM_ON);
        defaultMap.setProperty(NON_LIN_ARITH_OPTIONS_KEY, NON_LIN_ARITH_NONE);
        defaultMap.setProperty(QUANTIFIERS_OPTIONS_KEY, QUANTIFIERS_NON_SPLITTING_WITH_PROGS);
        for (int i = 1; i <= USER_TACLETS_NUM; ++i)
            defaultMap.setProperty(USER_TACLETS_OPTIONS_KEY(i), USER_TACLETS_OFF);
        defaultMap.setProperty(STOPMODE_OPTIONS_KEY, STOPMODE_DEFAULT);
        defaultMap.setProperty(RETREAT_MODE_OPTIONS_KEY, RETREAT_MODE_NONE);
        defaultMap.setProperty(VBT_PHASE, VBT_SYM_EX);
        defaultMap.setProperty(AUTO_INDUCTION_OPTIONS_KEY, AUTO_INDUCTION_OFF); //chrisg        
    }
    
    public StrategyProperties() {
        put(SPLITTING_OPTIONS_KEY, defaultMap.get(SPLITTING_OPTIONS_KEY));                
        put(LOOP_OPTIONS_KEY, defaultMap.get(LOOP_OPTIONS_KEY));
        put(BLOCK_OPTIONS_KEY, defaultMap.get(BLOCK_OPTIONS_KEY));
        put(METHOD_OPTIONS_KEY, defaultMap.get(METHOD_OPTIONS_KEY));
        put(DEP_OPTIONS_KEY, defaultMap.get(DEP_OPTIONS_KEY));
        put(QUERY_OPTIONS_KEY, defaultMap.get(QUERY_OPTIONS_KEY));
        put(QUERYAXIOM_OPTIONS_KEY, defaultMap.get(QUERYAXIOM_OPTIONS_KEY));
        put(NON_LIN_ARITH_OPTIONS_KEY, defaultMap.get(NON_LIN_ARITH_OPTIONS_KEY));
        put(QUANTIFIERS_OPTIONS_KEY, defaultMap.get(QUANTIFIERS_OPTIONS_KEY));
        for (int i = 1; i <= USER_TACLETS_NUM; ++i)
            put(USER_TACLETS_OPTIONS_KEY(i), defaultMap.get(USER_TACLETS_OPTIONS_KEY(i)));
        put(STOPMODE_OPTIONS_KEY, defaultMap.get(STOPMODE_OPTIONS_KEY));
        put(RETREAT_MODE_OPTIONS_KEY, defaultMap.get(RETREAT_MODE_OPTIONS_KEY));
        put(VBT_PHASE, defaultMap.getProperty(VBT_PHASE));
        put(AUTO_INDUCTION_OPTIONS_KEY, defaultMap.getProperty(AUTO_INDUCTION_OPTIONS_KEY));
    }

    public static String getDefaultProperty(String key) {
        return defaultMap.getProperty(key);
    }
    
    public String getProperty(String key) {
        String val = super.getProperty(key);
        if (val!=null) return val;
        return defaultMap.getProperty(key);
    }
    
    public static StrategyProperties read(Properties p) {        
        StrategyProperties sp = new StrategyProperties();

        sp.put(SPLITTING_OPTIONS_KEY, readSingleOption(p, SPLITTING_OPTIONS_KEY));                
        sp.put(LOOP_OPTIONS_KEY, readSingleOption(p, LOOP_OPTIONS_KEY));
        sp.put(BLOCK_OPTIONS_KEY, readSingleOption(p, BLOCK_OPTIONS_KEY)); 
        sp.put(METHOD_OPTIONS_KEY, readSingleOption(p, METHOD_OPTIONS_KEY));
        sp.put(DEP_OPTIONS_KEY, readSingleOption(p,DEP_OPTIONS_KEY));
        sp.put(QUERY_OPTIONS_KEY, readSingleOption(p,QUERY_OPTIONS_KEY));
        sp.put(QUERYAXIOM_OPTIONS_KEY, readSingleOption(p,QUERYAXIOM_OPTIONS_KEY));
        sp.put(NON_LIN_ARITH_OPTIONS_KEY, readSingleOption(p,NON_LIN_ARITH_OPTIONS_KEY));
        sp.put(QUANTIFIERS_OPTIONS_KEY, readSingleOption(p,QUANTIFIERS_OPTIONS_KEY));
        for (int i = 1; i <= USER_TACLETS_NUM; ++i)
            sp.put(USER_TACLETS_OPTIONS_KEY(i), readSingleOption(p,USER_TACLETS_OPTIONS_KEY(i)));
        sp.put(STOPMODE_OPTIONS_KEY, readSingleOption(p,STOPMODE_OPTIONS_KEY));
        sp.put(RETREAT_MODE_OPTIONS_KEY, readSingleOption(p,RETREAT_MODE_OPTIONS_KEY));
        sp.put(VBT_PHASE, readSingleOption(p,VBT_PHASE));
        sp.put(AUTO_INDUCTION_OPTIONS_KEY, readSingleOption(p,AUTO_INDUCTION_OPTIONS_KEY));
        return sp;
    }

    /**
     * @param p
     */
    private static Object readSingleOption(Properties p, String key) {
        String o = (String)p.get("[StrategyProperty]"+key);
        if (o == null) o = (String)defaultMap.get(key);
        return getUniqueString(o);
    }

    public void write(Properties p) {                
        p.put("[StrategyProperty]"+SPLITTING_OPTIONS_KEY, get(SPLITTING_OPTIONS_KEY));
        p.put("[StrategyProperty]"+LOOP_OPTIONS_KEY, get(LOOP_OPTIONS_KEY));
        p.put("[StrategyProperty]"+BLOCK_OPTIONS_KEY, get(BLOCK_OPTIONS_KEY));
        p.put("[StrategyProperty]"+METHOD_OPTIONS_KEY, get(METHOD_OPTIONS_KEY));
        p.put("[StrategyProperty]"+DEP_OPTIONS_KEY, get(DEP_OPTIONS_KEY));              
        p.put("[StrategyProperty]"+QUERY_OPTIONS_KEY, get(QUERY_OPTIONS_KEY));              
        p.put("[StrategyProperty]"+QUERYAXIOM_OPTIONS_KEY, get(QUERYAXIOM_OPTIONS_KEY));              
        p.put("[StrategyProperty]"+NON_LIN_ARITH_OPTIONS_KEY, get(NON_LIN_ARITH_OPTIONS_KEY));              
        p.put("[StrategyProperty]"+QUANTIFIERS_OPTIONS_KEY, get(QUANTIFIERS_OPTIONS_KEY));              
        for (int i = 1; i <= USER_TACLETS_NUM; ++i)
            p.put("[StrategyProperty]"+USER_TACLETS_OPTIONS_KEY(i), get(USER_TACLETS_OPTIONS_KEY(i)));
        p.put("[StrategyProperty]"+STOPMODE_OPTIONS_KEY, get(STOPMODE_OPTIONS_KEY));
        p.put("[StrategyProperty]"+RETREAT_MODE_OPTIONS_KEY, get(RETREAT_MODE_OPTIONS_KEY));
        p.put("[StrategyProperty]"+VBT_PHASE, get(VBT_PHASE));
        p.put("[StrategyProperty]"+AUTO_INDUCTION_OPTIONS_KEY, get(AUTO_INDUCTION_OPTIONS_KEY));
    }

    
    public synchronized Object clone() {
        final Properties p = (Properties) super.clone();
        final StrategyProperties sp = new StrategyProperties();
        sp.putAll(p);
        return sp;        
    }
    
    
    public boolean isDefault() {
	boolean result = true;
	Set<Map.Entry<Object,Object>> defaults = defaultMap.entrySet();
	for(Map.Entry<Object,Object> def : defaults) {
	    if(!def.getValue().equals(getProperty((String)def.getKey()))) {
		result = false;
		break;
	    }
	}
	return result;
    }
    
    /** 
     * @param in A keyword from the strategy properties. It must be registered in <code>stringPool<\code>.
     * @return Returns the same string but possibly with a different but unique object identity.
     */
    private final static String getUniqueString(String in){
    	for(String id:stringPool){
    		if(id.equals(in)){
    			return id; 
    		}
    	}
    	System.err.println("The string \""+in+"\" is not registered in the string pool of StrategyProperties. Update the string pool!");
    	return null;
    }
}
