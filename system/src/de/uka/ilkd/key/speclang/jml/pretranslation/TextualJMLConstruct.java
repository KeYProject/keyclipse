// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License. 
// See LICENSE.TXT for details.
//
//

package de.uka.ilkd.key.speclang.jml.pretranslation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.uka.ilkd.key.collection.ImmutableList;
import de.uka.ilkd.key.ldt.HeapLDT;
import de.uka.ilkd.key.logic.Name;
import de.uka.ilkd.key.speclang.PositionedString;


/**
 * Objects of this type represent the various JML specification constructs
 * in textual, unprocessed form.
 */
public abstract class TextualJMLConstruct {
    
    protected final ImmutableList<String> mods;
    
    /** A user-provided identifier to keep an overview over large specification collections */
    protected String name;
    
    
    public TextualJMLConstruct(ImmutableList<String> mods) {
        assert mods != null;
        this.mods = mods;
    }
    
    public TextualJMLConstruct(ImmutableList<String> mods, String name){
        this(mods);
        this.name = name;
    }
    
    
    public final ImmutableList<String> getMods() {
        return mods;
    }

    protected void addGeneric(Map<String, ImmutableList<PositionedString>> item, PositionedString ps) {
        String t = ps.text;
        if(!t.startsWith("<")) {
           ImmutableList<PositionedString> l = item.get(HeapLDT.BASE_HEAP_NAME.toString());
           l = l.append(ps);
           item.put(HeapLDT.BASE_HEAP_NAME.toString(), l);
           return; 
        }
        List<String> hs = new ArrayList<String>();
        while(t.startsWith("<")) {
          for(Name heapName : HeapLDT.VALID_HEAP_NAMES) {
            final String hName = heapName.toString();
            String h = "<" + hName + ">";
            if(t.startsWith(h)) {
              hs.add(hName);
              t = t.substring(h.length());
            }
          }
        }
        ps = new PositionedString(t, ps.fileName, ps.pos);
        for(String h : hs) {
           ImmutableList<PositionedString> l = item.get(h);
           l = l.append(ps);
           item.put(h, l); 
        }
    }


}
