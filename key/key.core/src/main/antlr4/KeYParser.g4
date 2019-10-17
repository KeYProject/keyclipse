// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2011 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License.
// See LICENSE.TXT for details.
//
// This file is part of KeY - Integrated Deductive Software Design
// Copyright (C) 2001-2009 Universitaet Karlsruhe, Germany
//                         Universitaet Koblenz-Landau, Germany
//                         Chalmers University of Technology, Sweden
//
// The KeY system is protected by the GNU General Public License.
// See LICENSE.TXT for details.
//
//

parser grammar KeYParser;

@members {
private SyntaxErrorReporter errorReporter = new SyntaxErrorReporter();
public SyntaxErrorReporter getErrorReporter() { return errorReporter;}
}

options { tokenVocab=KeYLexer; } // use tokens from STLexer.g4


file: (decls | problem | proof) EOF;

decls
:
    (one_include_statement)*
    (options_choice)?
    ( option_decls | sort_decls | prog_var_decls
    | schema_var_decls | pred_decls | func_decls
    | transform_decls  | ruleset_decls) *
;

one_include_statement
:
    (INCLUDE | (INCLUDELDTS ))
    one_include (COMMA one_include)* SEMI
;

one_include 
:
    (absfile=IDENT | relfile=string_literal)
;

options_choice
:
  (WITHOPTIONS activated_choice (COMMA activated_choice)* SEMI)
;

activated_choice
:
    cat=IDENT COLON choice_=IDENT
;

option_decls
:
    OPTIONSDECL LBRACE (choice SEMI)* RBRACE
;

choice
:
  category=IDENT  (COLON LBRACE choice_option (COMMA choice_option)* RBRACE)?
;

choice_option:
  choice_=IDENT
;

/* TODO: Why is the result of one_sort_decl stored in the local variables?
 * It does not seem to be employed at all ?! (MU)
 */
sort_decls

: SORTS LBRACE
       ( multipleSorts = one_sort_decl )*
  RBRACE
;

one_sort_decl
:
      GENERIC  sortIds=simple_ident_comma_list
        (ONEOF sortOneOf = oneof_sorts)?
        ( EXTENDS sortExt = extends_sorts )? SEMI                                        #one_sort_decl_generic
    | PROXY  sortIds = simple_ident_comma_list (EXTENDS sortExt=extends_sorts)? SEMI     #one_sort_decl_proxy
    | ABSTRACT?
      firstSort=simple_ident_dots
      (EXTENDS sortExt=extends_sorts | COMMA sortIds=simple_ident_comma_list)?  SEMI     #one_sort_decl_default
;

simple_ident_dots
:
  id = simple_ident
    (DOT
 	(id = simple_ident | num=NUM_LITERAL )
 	)*
 ;

extends_sorts
:
    any_sortId_check (COMMA any_sortId_check)*
;

oneof_sorts

    : LBRACE
        s = sortId_check
        (
            COMMA s = sortId_check
        ) *
      RBRACE 
    ;

keyjavatype
:
    type = simple_ident_dots (EMPTYBRACKETS)*
;

prog_var_decls
:
        
        PROGRAMVARIABLES
        LBRACE
        (
            kjt = keyjavatype
            var_names = simple_ident_comma_list            
            SEMI
        )*
        RBRACE
    ;

//this rule produces a StringLiteral
string_literal: id=STRING_LITERAL;

//this rule produces a String
string_value: STRING_LITERAL;

simple_ident
   :
     id=IDENT 
   ;

simple_ident_comma_list
   :
   id = simple_ident 
   (COMMA id = simple_ident )*
 ;


schema_var_decls :
        SCHEMAVARIABLES LBRACE  
  	  ( one_schema_var_decl )*
        RBRACE 
    ;

one_schema_var_decl
 :
   (MODALOPERATOR one_schema_modal_op_decl SEMI)
 |
  (
   (
    PROGRAM
    
    ( schema_modifiers ) ?
    id = simple_ident ( LBRACKET nameString = simple_ident EQUALS parameter = simple_ident_dots RBRACKET )? 
    ids = simple_ident_comma_list
  | FORMULA
    
    ( schema_modifiers ) ?
    
    ids = simple_ident_comma_list
  | TERMLABEL
    
    
    ( schema_modifiers ) ?
    ids = simple_ident_comma_list
  | UPDATE
    
    ( schema_modifiers ) ?
    
    ids = simple_ident_comma_list
  | SKOLEMFORMULA
    
    
    ( schema_modifiers ) ?
    
    ids = simple_ident_comma_list
  | (    TERM
         
         ( schema_modifiers ) ?
      | ( (VARIABLES | VARIABLE)
         
         
         ( schema_modifiers ) ?)
      | (SKOLEMTERM
         
         
         ( schema_modifiers ) ?)
    )
    s = any_sortId_check
    ids = simple_ident_comma_list
  ) SEMI
   
 )

 ;

schema_modifiers
    :
        LBRACKET
        opts = simple_ident_comma_list
        RBRACKET
       
    ;

one_schema_modal_op_decl
:
    (LPAREN sort = any_sortId_check RPAREN)?
    LBRACE ids = simple_ident_comma_list RBRACE id = simple_ident
;

pred_decl
    :
        pred_name = funcpred_name

        (
	    whereToBind = where_to_bind
	)?
     argSorts = arg_sorts
        SEMI
    ;

pred_decls
    :
        PREDICATES
        LBRACE
        (
            pred_decl
        ) *
        RBRACE
    ;


location_ident
    :
        id = simple_ident
   
    ;



func_decl

    :
        (
            UNIQUE 
        )?

        retSort = any_sortId_check

        func_name = funcpred_name

	(
	    whereToBind = where_to_bind
	)?
        argSorts = arg_sorts
        SEMI
    ;

func_decls
    :
        FUNCTIONS
        LBRACE
        (
            func_decl
        ) *
        RBRACE
    ;


// like arg_sorts but admits also the keyword "\formula"
arg_sorts_or_formula
:
    ( LPAREN
      arg_sorts_or_formula_helper
      (COMMA arg_sorts_or_formula_helper)*
      RPAREN
    )?
;

arg_sorts_or_formula_helper
:
    sortId_check | FORMULA
;

transform_decl
    :
        (
          retSort = any_sortId_check
        | FORMULA 
        )

        trans_name = funcpred_name
        argSorts = arg_sorts_or_formula
        SEMI
    ;

transform_decls
    :
        TRANSFORMERS
        LBRACE
        (
            transform_decl
        ) *
        RBRACE
    ;

arrayopid

    :
        EMPTYBRACKETS
        LPAREN
        componentType = keyjavatype
        RPAREN
    ;

arg_sorts

    :
        (
            LPAREN
            s = sortId_check 
            (
                COMMA s = sortId_check 
            ) *
            RPAREN
        ) ?
        
    ;

where_to_bind

    :
        LBRACE
        b+=(TRUE | FALSE)
        (COMMA b+=(TRUE | FALSE) )*
        RBRACE
        
   ;

ruleset_decls
    :
        HEURISTICSDECL
        LBRACE
        (id = simple_ident SEMI)*
        RBRACE
    ;

sortId
:
    s = sortId_check
;

// Non-generic sorts, array sorts allowed
sortId_check 

    :
        p = sortId_check_help
        s = array_decls
    ;

// Generic and non-generic sorts, array sorts allowed
any_sortId_check 

    :
        p = any_sortId_check_help
        s = array_decls
    ;


// Non-generic sorts
sortId_check_help 

    :
        result = any_sortId_check_help
    ;


// Generic and non-generic sorts
any_sortId_check_help 
    :
        name = simple_sort_name
     
    ;


array_decls

    :
     (EMPTYBRACKETS )*
    ;

id_declaration
    :
        id=IDENT
        ( COLON s = sortId_check ) ?
        
    ;

funcpred_name
:
    sort_name DOUBLECOLON name = simple_ident
  | simple_ident
;


// no array sorts
simple_sort_name
:
  id=simple_ident_dots
;


sort_name
:
  simple_sort_name
  (brackets=EMPTYBRACKETS)*
;

/**
 * In the special but important case of Taclets, we don't yet know
 * whether we are going to have a term or a formula, and it is hard
 * to decide a priori what we are looking at.  So the `term'
 * non-terminal will recognize a term or a formula.  The `formula'
 * reads a formula/term and throws an error if it wasn't a formula.
 * This gives a rather late error message. */

formula
:
  term
;

term
:
    result=elementary_update_term
    (PARALLEL a=elementary_update_term)*
;

termEOF
:
    result=term EOF
;

elementary_update_term
:
  result=equivalence_term
  (ASSIGN a=equivalence_term)?
;


equivalence_term
:   a=implication_term (EQV a1=implication_term)*
;

implication_term
:   
  a=disjunction_term (IMP a1=implication_term)?
;

disjunction_term
:   
  a=conjunction_term (OR a1=conjunction_term)*
;

conjunction_term
:   
  a=term60 (AND a1=term60)*
;

term60
:
      unary_formula
  |   equality_term
;

unary_formula
:
    NOT term60
  |	quantifierterm
  | modality_dl_term
;

equality_term
:
  a=logicTermReEntry
  ((EQUALS | NOT_EQUALS)
	  a1 = logicTermReEntry)?
;
 

relation_op
:
  (
    LESS         
 |  LESSEQUAL    
 |  GREATER      
 |  GREATEREQUAL 
 ) 
;

weak_arith_op
:
 (PLUS |  MINUS)
;

strong_arith_op
:
 (
    STAR         
 |  SLASH        
 |  PERCENT      
 ) 
;

// term80
logicTermReEntry
:
   a = weak_arith_op_term (op=relation_op a1=weak_arith_op_term)?
;



weak_arith_op_term
:
   a = strong_arith_op_term (op = weak_arith_op a1=strong_arith_op_term )*
;


strong_arith_op_term
:
   a = term110 (op = strong_arith_op a1=term110 )*
;


/**
 * helps to better distinguish if formulas are allowed or only term are
 * accepted
 * WATCHOUT: Woj: the check for Sort.FORMULA had to be removed to allow
 * infix operators and the whole bunch of grammar rules above.
 */
term110
:
    braces_term |  accessterm
;

staticAttributeOrQueryReference
:
  id=IDENT
  (EMPTYBRACKETS )*
;

static_attribute_suffix
:
  attributeName = staticAttributeOrQueryReference
;


attribute_or_query_suffix
:
    DOT ( STAR 
        | ( memberName=attrid
            (result=query_suffix )?
          ) 
        )
;
 
attrid
:
// the o.f@(packagename.Classname) syntax has been dropped.
// instead, one can write o.(packagename.Classname::f)
    id = simple_ident
  | LPAREN clss = sort_name DOUBLECOLON id2 = simple_ident RPAREN
;

query_suffix 
:
    args = argument_list
;
 

//term120
accessterm
:
    MINUS result = term110
  | LPAREN s = any_sortId_check RPAREN result=term110
  | ( // look for package1.package2.Class.query(
        static_query
      | // look for package1.package2.Class.attr
        static_attribute_suffix
      | atom
    )
    ( accessterm_bracket_suffix
    | attribute_or_query_suffix
    )*
    // at most one heap selection suffix
    ( heap_selection_suffix )? // resets globalSelectNestingDepth to zero
;


heap_selection_suffix
    :
    AT heap=accessterm

    ;

accessterm_bracket_suffix
:
    heap_update_suffix
  | seq_get_suffix
  | array_access_suffix
;

seq_get_suffix
:
  LBRACKET logicTermReEntry RBRACKET
;

static_query
:
    queryRef=staticAttributeOrQueryReference
    args=argument_list
;

heap_update_suffix 
    : // TODO find the right kind of non-terminal for "o.f" and "a"
      // and do not resign to parsing an arbitrary term
    LBRACKET
    ( target=equivalence_term ASSIGN val=equivalence_term
    | id=simple_ident args=argument_list
    )
    RBRACKET
;
 

array_access_suffix 
:
  LBRACKET
	( STAR
  | indexTerm=logicTermReEntry (DOTRANGE rangeTo=logicTermReEntry)?
  )
  RBRACKET
;

/*
// This would require repeated 
accesstermlist  :
     (t=accessterm  ( COMMA t=accessterm )* )? ;
*/

boolean_constant: FALSE | TRUE;

atom
:
    ( specialTerm
    | funcpredvarterm
    | LPAREN term RPAREN
    | boolean_constant
    | ifThenElseTerm
    | ifExThenElseTerm
    | string_literal
    )
    (LGUILLEMETS labels = label RGUILLEMETS)?
;
 

label
:
   l=single_label  (COMMA l=single_label )*
;

single_label
:
  (name=IDENT
  | star=STAR  )

  (LPAREN
    (string_value
      (COMMA string_value )*
    )?
    RPAREN
  )?
;


abbreviation
:
  sc = simple_ident
;


ifThenElseTerm
:
  IF LPAREN condF = term RPAREN
  THEN LPAREN thenT = term RPAREN
  ELSE LPAREN elseT = term RPAREN
;
 


ifExThenElseTerm


    :
        IFEX exVars = bound_variables
        LPAREN condF = term RPAREN
        THEN LPAREN thenT = term RPAREN
        ELSE LPAREN elseT = term RPAREN
 ;
 


argument
:
   // WATCHOUT Woj: can (should) this be unified to term60?
  term | term60
;


quantifierterm
:
  (   FORALL  | EXISTS  )
  bound_variables term60
;

/*
 * A term that is surrounded by braces: 
 */
braces_term
:
    substitutionterm
  | locset_term
  | updateterm
;

locset_term
:
    LBRACE
        ( l = location_term 
        ( COMMA l = location_term  )* )?
    RBRACE
;

location_term
    :
    LPAREN obj=equivalence_term COMMA field=equivalence_term RPAREN
            
    ;

substitutionterm


:
   LBRACE SUBST
     v = one_bound_variable SEMI
     
     a1=logicTermReEntry
     
   RBRACE
   ( a2 = term110 | unary_formula )
;
 


updateterm
:
  LBRACE u=term RBRACE
  ( term110 | unary_formula )
;
 

bound_variables
:
      var = one_bound_variable 
      (
          COMMA var = one_bound_variable 
      )*
      SEMI
;

one_bound_variable
:
   one_logic_bound_variable_nosort
 | one_schema_bound_variable
 | one_logic_bound_variable
;

one_schema_bound_variable
:
   id = simple_ident 
;

one_logic_bound_variable
:
  s=sortId id=simple_ident 
;

one_logic_bound_variable_nosort
:
  id=simple_ident 
;

modality_dl_term
   :
   modality = MODALITY
   // CAREFUL here, op can be null during guessing stage (use lazy &&)
   a1=term60
     // This here will accept both (1) \modality...\endmodality post and
     // (2) \modality...\endmodality(post)
     // so that it is consistent with pretty printer that prints (1).
     // A term "(post)" seems to be parsed as "post" anyway
;
 


argument_list


    :
        LPAREN
        (p1 = argument 

            (COMMA p2 = argument  )* )?

        RPAREN
        

    ;

number:
  (MINUS )?
  ( NUM_LITERAL | HEX_LITERAL | BIN_LITERAL)
;

char_literal:
    CHAR_LITERAL;

funcpredvarterm
:
     char_literal
    | number
    | AT a = abbreviation
    | varfuncid = funcpred_name
      ((
         LBRACE
         boundVars = bound_variables
         RBRACE
       )?
        args = argument_list
      )?
        //args==null indicates no argument list
        //args.size()==0 indicates open-close-parens ()
;
 

specialTerm
:
       result = metaTerm
;
 

arith_op
:
    PERCENT 
  | STAR 
  | MINUS 
  | SLASH 
  | PLUS 
;


varId
:
  id=IDENT
;

varIds
:
  ids=simple_ident_comma_list
;

triggers
:
   TRIGGER
     LBRACE id = simple_ident
     	
       RBRACE
     t=term (AVOID avoidCond=term 
      (COMMA avoidCond=term )*)? SEMI
   
;

taclet
    :
      (LEMMA )?
      name=IDENT (choices_=option_list)?
      LBRACE
      ( form=formula
      |
        ( SCHEMAVAR one_schema_var_decl ) *
        ( ASSUMES LPAREN ifSeq=seq RPAREN ) ?
        ( FIND LPAREN find=termorseq RPAREN
            (   SAMEUPDATELEVEL
              | INSEQUENTSTATE
              | ANTECEDENTPOLARITY
              | SUCCEDENTPOLARITY
            )*
        )?

        ( VARCOND LPAREN varexplist RPAREN ) ?
        goalspecs
        modifiers
        
    )
    RBRACE
    ;

tacletgen
:
  /* (LEMMA )? */
  name=IDENT (choices_=option_list)?
  LBRACE
  (VARCOND LPAREN varexplist RPAREN)?
  goalspecs
  modifiers
  (TEMPLATE)
  RBRACE
;


modifiers
:
        ( rs = rulesets 
        | NONINTERACTIVE 
        | DISPLAYNAME dname = string_value
        | HELPTEXT htext = string_value
        | triggers
        ) *
    ;

seq
:
  ant=semisequent SEQARROW suc=semisequent
;

seqEOF: seq EOF;

termorseq
:
      head=term ( COMMA s=seq | SEQARROW ss=semisequent ) ?
    | SEQARROW ss=semisequent
;

semisequent
:
    /* empty */
  | head=term ( COMMA ss=semisequent) ?
;

varexplist : varexp ( COMMA varexp ) * ;

varexp

:
  ( varcond_applyUpdateOnRigid
    | varcond_dropEffectlessElementaries
    | varcond_dropEffectlessStores
    | varcond_enum_const
    | varcond_free
    | varcond_hassort
    | varcond_fieldtype
    | varcond_equalUnique
    | varcond_new
    | varcond_newlabel
    | varcond_observer
    | varcond_different
    | varcond_metadisjoint
    | varcond_simplifyIfThenElseUpdate
    | varcond_differentFields
    | varcond_sameObserver
  )
  |
  ( (NOT_  )?
    (   varcond_abstractOrInterface
	    | varcond_array
        | varcond_array_length
        | varcond_enumtype
        | varcond_freeLabelIn
        | varcond_localvariable
        | varcond_thisreference
        | varcond_reference
        | varcond_referencearray
        | varcond_static
        | varcond_staticmethod
        | varcond_mayexpandmethod
        | varcond_final
        | varcond_typecheck
        | varcond_constant
        | varcond_label
        | varcond_static_field
        | varcond_subFormulas
        | varcond_containsAssignment
      )
  )
;

varcond_sameObserver
:
  SAME_OBSERVER LPAREN t1=varId COMMA t2=varId RPAREN
  
  ;


varcond_applyUpdateOnRigid 
:
   APPLY_UPDATE_ON_RIGID LPAREN u=varId COMMA x=varId COMMA x2=varId RPAREN
   
;

varcond_dropEffectlessElementaries
:
   DROP_EFFECTLESS_ELEMENTARIES LPAREN u=varId COMMA x=varId COMMA result=varId RPAREN
   
;

varcond_dropEffectlessStores
:
   DROP_EFFECTLESS_STORES LPAREN h=varId COMMA o=varId COMMA f=varId COMMA x=varId COMMA result=varId RPAREN
   
;

varcond_differentFields 
:
   DIFFERENTFIELDS
   LPAREN
     x = varId COMMA y = varId
   RPAREN
   
;


varcond_simplifyIfThenElseUpdate
:
   SIMPLIFY_IF_THEN_ELSE_UPDATE LPAREN phi=varId COMMA u1=varId COMMA u2=varId COMMA commonFormula=varId COMMA result=varId RPAREN
   
;

type_resolver
:
      (s = any_sortId_check)
    | (TYPEOF LPAREN y = varId RPAREN)
    | (CONTAINERTYPE LPAREN y = varId RPAREN)
;

varcond_new 
:
   NEW LPAREN x=varId COMMA
      (
          TYPEOF LPAREN y=varId RPAREN 
      |
         DEPENDINGON LPAREN y=varId RPAREN 
      | kjt=keyjavatype 
      )
   RPAREN

;

varcond_newlabel 
:
  NEWLABEL LPAREN x=varId RPAREN 
;
varcond_typecheck 

:
   (  SAME
    | ISSUBTYPE
    | STRICT ISSUBTYPE
    | DISJOINTMODULONULL
   )
   LPAREN fst = type_resolver COMMA snd = type_resolver RPAREN
;


varcond_free 
:
   NOTFREEIN LPAREN x=varId COMMA ys=varIds RPAREN 
;


varcond_hassort 

:
   HASSORT
   LPAREN
   (x=varId | ELEMSORT LPAREN x=varId RPAREN )
   COMMA
   s=any_sortId_check
   RPAREN
;

varcond_fieldtype 
:
    FIELDTYPE
    LPAREN
    x=varId
    COMMA
    s=any_sortId_check
    RPAREN
;

varcond_containsAssignment
:
   CONTAINS_ASSIGNMENT LPAREN x=varId RPAREN
   
;

varcond_enumtype 
:
   ISENUMTYPE LPAREN tr = type_resolver RPAREN
      
;


varcond_reference 

:
   ISREFERENCE (LBRACKET  id=simple_ident RBRACKET)?
   LPAREN
        tr = type_resolver
   RPAREN
;

varcond_thisreference 
:
   ISTHISREFERENCE
   LPAREN
     x = varId
   RPAREN
   
;


varcond_staticmethod 
:
   STATICMETHODREFERENCE LPAREN x=varId COMMA y=varId COMMA z=varId RPAREN 
;

varcond_mayexpandmethod 
:
   MAXEXPANDMETHOD LPAREN x=varId COMMA y=varId
   ( COMMA z=varId RPAREN 
   | RPAREN 
   )
;

varcond_referencearray 
:
   ISREFERENCEARRAY LPAREN x=varId RPAREN 
;

varcond_array 
:
   ISARRAY LPAREN x=varId RPAREN 
;

varcond_array_length 
:
   ISARRAYLENGTH LPAREN x=varId RPAREN 
;


varcond_abstractOrInterface 
:
   IS_ABSTRACT_OR_INTERFACE LPAREN tr=type_resolver RPAREN 
;

varcond_enum_const 
:
   ENUM_CONST LPAREN x=varId RPAREN 
;

varcond_final 
:
   FINAL LPAREN x=varId RPAREN 
;

varcond_static 
:
   STATIC LPAREN x=varId RPAREN 
;

varcond_localvariable 
:
   ISLOCALVARIABLE
	LPAREN x=varId RPAREN 
;


varcond_observer 
:
   ISOBSERVER
	LPAREN obs=varId COMMA heap=varId  RPAREN 
;


varcond_different 
:
   DIFFERENT
	LPAREN var1=varId COMMA var2=varId RPAREN 
;


varcond_metadisjoint 
:
   METADISJOINT
	LPAREN var1=varId COMMA var2=varId RPAREN 
;



varcond_equalUnique 
:
   EQUAL_UNIQUE
	LPAREN t=varId COMMA t2=varId COMMA phi=varId RPAREN 
;


varcond_freeLabelIn 
:

 FREELABELIN
    LPAREN l=varId COMMA statement=varId RPAREN 
;

varcond_constant 
:
   ISCONSTANT LPAREN x=varId RPAREN
;

varcond_label 
:
   HASLABEL
        LPAREN l=varId COMMA name=simple_ident RPAREN 
;

varcond_static_field 
:
   ISSTATICFIELD
        LPAREN field=varId RPAREN 
;

varcond_subFormulas 
:
   HASSUBFORMULAS
        LPAREN x=varId RPAREN 
;

goalspecs :
        CLOSEGOAL
    | goalspecwithoption ( SEMI goalspecwithoption )* ;

goalspecwithoption
 :
        (( soc = option_list
                LBRACE
                goalspec
                RBRACE)
        |
            goalspec
        )
    ;

option
:
        cat=IDENT COLON choice_=IDENT
;

option_list
:
LPAREN 
  c = option 
  (COMMA c = option )*
RPAREN
;

goalspec
    :
        (name=string_value COLON)?
        (   ( rwObj = replacewith
                (addSeq=add)?
                (addRList=addrules)?
                (addpv=addprogvar)?
            )
        | ( addSeq=add (addRList=addrules)? )
        | ( addRList=addrules )
        )
        

    ;

replacewith

:
        REPLACEWITH LPAREN o=termorseq RPAREN;

add

:
        ADD LPAREN s=seq RPAREN;

addrules

:
        ADDRULES LPAREN lor=tacletlist RPAREN;

addprogvar

:
        ADDPROGVARS LPAREN pvs=pvset RPAREN;

tacletlist: taclet (COMMA taclet)*;
pvset: varId (COMMA varId)*;

rulesets:
        HEURISTICS LPAREN ruleset
        ( COMMA ruleset ) * RPAREN
;

ruleset
:
        id=IDENT
;

metaId:  id=simple_ident ;

metaTerm:
        (vf = metaId
           ( LPAREN
            t = term
            
            ( COMMA
                t = term
                
            )* RPAREN )?
            
        )
 ;
      

contracts
:
   CONTRACTS
       LBRACE 
       ( one_contract )*
       RBRACE
;

invariants

:
   INVARIANTS LPAREN selfVar=one_logic_bound_variable RPAREN
       LBRACE 
       ( one_invariant )*
       RBRACE  
;


one_contract
:
   contractName = simple_ident LBRACE

   (prog_var_decls)?
   fma = formula MODIFIES modifiesClause = term
   RBRACE SEMI
;

one_invariant
:
     invName = simple_ident LBRACE
     fma = formula
     (DISPLAYNAME displayName=string_value)?
     RBRACE SEMI
;

problem
:
        profile
        (pref = preferences)
        bootClassPath
        // the result is of no importance here (strange enough)

        stlist = classPaths
        string = javaSource

        decls

        //TODO weigl: rework into a seperate rule
        // WATCHOUT: choices is always going to be an empty set here,
      	// isn't it?
        ( contracts )*
        ( invariants )*
              (  ( RULES
                 | AXIOMS
                 )

                 ( choices = option_list )?
           (
            LBRACE
            (s=taclet SEMI)*
            RBRACE 
           )
        )*
        (  PROBLEM LBRACE a = formula RBRACE
         | CHOOSECONTRACT (chooseContract=string_value SEMI)?
         | PROOFOBLIGATION  (proofObligation=string_value SEMI)?
        )?
   ;

bootClassPath
:
  (BOOTCLASSPATH id=string_value SEMI)?
;

classPaths
:
  ( (CLASSPATH s=string_value
    (COMMA s=string_value)*
    SEMI)
  | (NODEFAULTCLASSES SEMI)
  )*
;

javaSource
:
   (JAVASOURCE result = oneJavaSource SEMI)?
;


oneJavaSource
:
  ( string_value
  | SLASH
  | COLON
  | BACKSLASH
  )+ 
;


profile:
        (PROFILE name=string_value SEMI)?
;

preferences
:
	( KEYSETTINGS
    LBRACE
      (s=string_value)?
    RBRACE
  )?
;

// delivers: <Script, start line no, start column no>
proofScript
:
  PROOFSCRIPT ps = STRING_LITERAL
;

proof: (PROOF proofBody)?;


proofBody
:
  LBRACE
      ( pseudosexpr )+
  RBRACE
;


pseudosexpr
:
  LPAREN
    (proofElementId=expreid
      (str=string_literal)?
      (pseudosexpr)*
    )?
  RPAREN
;

expreid: id=simple_ident;
