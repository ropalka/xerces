/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2000,2001 The Apache Software Foundation.  
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xerces" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, International
 * Business Machines, Inc., http://www.apache.org.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package org.apache.xerces.impl.dtd;

import java.lang.Integer;
import java.util.Hashtable;

import org.apache.xerces.impl.dtd.XMLAttributeDecl;
import org.apache.xerces.impl.dtd.XMLNotationDecl;
import org.apache.xerces.impl.dtd.XMLEntityDecl;
import org.apache.xerces.impl.dtd.XMLSimpleType;
import org.apache.xerces.impl.dtd.models.CMNode;
import org.apache.xerces.impl.dtd.models.CMAny;
import org.apache.xerces.impl.dtd.models.CMLeaf;
import org.apache.xerces.impl.dtd.models.CMUniOp;
import org.apache.xerces.impl.dtd.models.CMBinOp;
import org.apache.xerces.impl.dtd.models.DFAContentModel;
import org.apache.xerces.impl.dtd.models.MixedContentModel;
import org.apache.xerces.impl.dtd.models.SimpleContentModel;
import org.apache.xerces.impl.validation.EntityState;
import org.apache.xerces.impl.dv.dtd.DatatypeValidator;
import org.apache.xerces.impl.dtd.models.ContentModelValidator;
import org.apache.xerces.util.SymbolTable;

import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.grammars.Grammar;

/**
 * A generic grammar for use in validating XML documents. The Grammar
 * object stores the validation information in a compiled form. Specific
 * subclasses extend this class and "populate" the grammar by compiling 
 * the specific syntax (DTD, Schema, etc) into the data structures used
 * by this object.
 * <p>
 * <strong>Note:</strong> The AbstractDTDGrammar object is not useful as a generic 
 * grammar access or query object. In other words, you cannot round-trip 
 * specific grammar syntaxes with the compiled grammar information in 
 * the AbstractDTDGrammar object. You <em>can</em> create equivalent validation
 * rules in your choice of grammar syntax but there is no guarantee that
 * the input and output will be the same.
 *
 * Renamed from Grammar to AbstractDTDGrammar by neilg, 01/17/02, to
 * reflect the fact that this is anything but a general-purpose grammar.
 * REVISIT :  shouldn't this class and DTDGrammar be combined?
 *
 * @author Jeffrey Rodriguez, IBM
 * @author Eric Ye, IBM
 * @author Andy Clark, IBM
 *
 * @version $Id$
 */
public abstract class AbstractDTDGrammar implements EntityState, Grammar {

    //
    // Constants
    //

    /** Top level scope (-1). */
    public static final int TOP_LEVEL_SCOPE = -1;

    // private

    /** Chunk shift (8). */
    private static final int CHUNK_SHIFT = 8; // 2^8 = 256

    /** Chunk size (1 << CHUNK_SHIFT). */
    private static final int CHUNK_SIZE = 1 << CHUNK_SHIFT;

    /** Chunk mask (CHUNK_SIZE - 1). */
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;

    /** Initial chunk count (). */
    private static final int INITIAL_CHUNK_COUNT = (1 << (10 - CHUNK_SHIFT)); // 2^10 = 1k

    /** List flag (0x80). */
    private static final short LIST_FLAG = 0x80;

    /** List mask (~LIST_FLAG). */
    private static final short LIST_MASK = ~LIST_FLAG;

    //
    // Data
    //

    /** Symbol table. */
    private SymbolTable fSymbolTable;

    /** Target namespace of grammar. */
    private String fTargetNamespace;

    // element declarations

    /** Number of element declarations. */
    private int fElementDeclCount = 0;

    /** Element declaration name. */
    private QName fElementDeclName[][] = new QName[INITIAL_CHUNK_COUNT][];

    /** 
     * Element declaration type. 
     * @see XMLElementDecl
     */
    private short fElementDeclType[][] = new short[INITIAL_CHUNK_COUNT][];

    /** 
     * Element declaration default value. This value is used when
     * the element is of simple type.
     */
    private String fElementDeclDefaultValue[][] = new String[INITIAL_CHUNK_COUNT][];

    /** 
     * Element declaration default type. This value is used when
     * the element is of simple type.
     */
    private short   fElementDeclDefaultType[][] = new short[INITIAL_CHUNK_COUNT][];

    /** 
     * Element declaration datatype validator. This value is used when
     * the element is of simple type. 
     */
    private DatatypeValidator fElementDeclDatatypeValidator[][] = new DatatypeValidator[INITIAL_CHUNK_COUNT][];

    /** 
     * Element declaration content spec index. This index value is used
     * to refer to the content spec information tables.
     */
    private int fElementDeclContentSpecIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    /** 
     * Element declaration content model validator. This validator is
     * constructed from the content spec nodes.
     */
    private ContentModelValidator fElementDeclContentModelValidator[][] = new ContentModelValidator[INITIAL_CHUNK_COUNT][];

    /** First attribute declaration of an element declaration. */
    private int fElementDeclFirstAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    /** Last attribute declaration of an element declaration. */
    private int fElementDeclLastAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    // attribute declarations

    /** Number of attribute declarations. */
    private int fAttributeDeclCount = 0 ;

    /** Attribute declaration name. */
    private QName fAttributeDeclName[][] = new QName[INITIAL_CHUNK_COUNT][];

    /** 
     * Attribute declaration type.
     * @see XMLAttributeDecl
     */
    private short fAttributeDeclType[][] = new short[INITIAL_CHUNK_COUNT][];

    /** Attribute declaratoin enumeration values. */
    private String[] fAttributeDeclEnumeration[][] = new String[INITIAL_CHUNK_COUNT][][];
    private short fAttributeDeclDefaultType[][] = new short[INITIAL_CHUNK_COUNT][];
    private DatatypeValidator fAttributeDeclDatatypeValidator[][] = new DatatypeValidator[INITIAL_CHUNK_COUNT][];
    private String fAttributeDeclDefaultValue[][] = new String[INITIAL_CHUNK_COUNT][];
    private String fAttributeDeclNonNormalizedDefaultValue[][] = new String[INITIAL_CHUNK_COUNT][];
    private int fAttributeDeclNextAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    // content specs

    // here saves the content spec binary trees for element decls, 
    // each element with a content model will hold a pointer which is 
    // the index of the head node of the content spec tree. 

    private int fContentSpecCount = 0;
    private short fContentSpecType[][] = new short[INITIAL_CHUNK_COUNT][];
    private Object fContentSpecValue[][] = new Object[INITIAL_CHUNK_COUNT][];
    private Object fContentSpecOtherValue[][] = new Object[INITIAL_CHUNK_COUNT][];

    // entities

    private int fEntityCount = 0;
    private String fEntityName[][] = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fEntityValue = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fEntityPublicId = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fEntitySystemId = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fEntityBaseSystemId = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fEntityNotation = new String[INITIAL_CHUNK_COUNT][];
    private byte[][] fEntityIsPE = new byte[INITIAL_CHUNK_COUNT][];
    private byte[][] fEntityInExternal = new byte[INITIAL_CHUNK_COUNT][];

    // notations

    private int fNotationCount = 0;
    private String fNotationName[][] = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fNotationPublicId = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fNotationSystemId = new String[INITIAL_CHUNK_COUNT][];
    private String[][] fNotationBaseSystemId = new String[INITIAL_CHUNK_COUNT][];

    // other information

    /** Scope mapping table. */
    private TupleHashtable fScopeMapping = new TupleHashtable();

    // temporary variables

    /** Temporary qualified name. */
    private QName fQName1 = new QName();
    
    /** Temporary qualified name. */
    private QName fQName2 = new QName();

    /** Temporary Attribute decl. */
    protected XMLAttributeDecl fAttributeDecl = new XMLAttributeDecl();

    // for buildSyntaxTree method

    private int fLeafCount = 0;
    private int fEpsilonIndex = -1;
    
    //
    // Constructors
    //

    /** Default constructor. */
    protected AbstractDTDGrammar(SymbolTable symbolTable) {
        fSymbolTable = symbolTable;
    } // <init>(SymbolTable)

    // Grammar methods
    public String getGrammarType() {
        return Grammar.XML_DTD;
    } // getGrammarType():  String

    //
    // Public methods
    //

    /** Returns true if this grammar is namespace aware. */
    public abstract boolean isNamespaceAware();

    /** Returns the symbol table. */
    public SymbolTable getSymbolTable() {
        return fSymbolTable;
    } // getSymbolTable():SymbolTable

    /** Returns this grammar's target namespace. */
    public String getTargetNamespace() {
        return fTargetNamespace;
    } // getTargetNamespace():String

    // REVISIT: Make this getElementDeclCount/getElementDeclAt. -Ac

    /**
     * Returns the index of the first element declaration. This index
     * is then used to query more information about the element declaration.
     *
     * @see #getNextElementDeclIndex
     * @see #getElementDecl
     */
    public int getFirstElementDeclIndex() {
        return fElementDeclCount > 0 ? fElementDeclCount : -1;
    } // getFirstElementDeclIndex():int

    /**
     * Returns the next index of the element declaration following the
     * specified element declaration.
     * 
     * @param elementDeclIndex The element declaration index.
     */
    public int getNextElementDeclIndex(int elementDeclIndex) {
        return elementDeclIndex < fElementDeclCount - 1 
             ? elementDeclIndex + 1 : -1;
    } // getNextElementDeclIndex(int):int

    /**
     * getElementDeclIndex
     * 
     * @param elementDeclName 
     * @param scope 
     * 
     * @return index of the elementDeclName in scope
     */
    public int getElementDeclIndex(String elementDeclName, int scope) {
        int mapping = fScopeMapping.get(scope, elementDeclName, null);
        //System.out.println("getElementDeclIndex("+elementDeclName+','+scope+") -> "+mapping);
        return mapping;
    } // getElementDeclIndex(String,int):int
   
    /**
     * getElementDeclIndex
     * 
     * @param elementDeclQName 
     * @param scope 
     * 
     * @return  index of elementDeclQName in scope
     */
    public int getElementDeclIndex(QName elementDeclQName, int scope) {
        int mapping = fScopeMapping.get(scope, elementDeclQName.localpart, elementDeclQName.uri);
        //System.out.println("getElementDeclIndex("+elementDeclQName+','+scope+") -> "+mapping);
        return mapping;
    } // getElementDeclIndex(QName,int):int

    /**
     * getElementDecl
     * 
     * @param elementDeclIndex 
     * @param elementDecl The values of this structure are set by this call.
     * 
     * @return True if find the element, False otherwise. 
     */
    public boolean getElementDecl(int elementDeclIndex, 
                                  XMLElementDecl elementDecl) {

        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
            return false;
        }

        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        elementDecl.name.setValues(fElementDeclName[chunk][index]);

        if (fElementDeclType[chunk][index] == -1) {
            elementDecl.type                    = -1;
            elementDecl.simpleType.list = false;
        } else {
            elementDecl.type            = (short) (fElementDeclType[chunk][index] & LIST_MASK);
            elementDecl.simpleType.list = (fElementDeclType[chunk][index] & LIST_FLAG) != 0;
        }

        /* Validators are null until we add that code */
        if (elementDecl.type == XMLElementDecl.TYPE_CHILDREN || elementDecl.type == XMLElementDecl.TYPE_MIXED) {
            elementDecl.contentModelValidator = getElementContentModelValidator(elementDeclIndex);
        }
              
        elementDecl.simpleType.datatypeValidator = fElementDeclDatatypeValidator[chunk][index];      
        elementDecl.simpleType.defaultType       = fElementDeclDefaultType[chunk][index];
        elementDecl.simpleType.defaultValue      = fElementDeclDefaultValue[chunk][index];

        return true;

    } // getElementDecl(int,XMLElementDecl):boolean

    // REVISIT: Make this getAttributeDeclCount/getAttributeDeclAt. -Ac

    /**
     * getFirstAttributeDeclIndex
     * 
     * @param elementDeclIndex 
     * 
     * @return index of the first attribute for element declaration elementDeclIndex
     */
    public int getFirstAttributeDeclIndex(int elementDeclIndex) {
        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        return  fElementDeclFirstAttributeDeclIndex[chunk][index];
    } // getFirstAttributeDeclIndex

    /**
     * getNextAttributeDeclIndex
     * 
     * @param attributeDeclIndex 
     * 
     * @return index of the next attribute of the attribute at attributeDeclIndex
     */
    public int getNextAttributeDeclIndex(int attributeDeclIndex) {
        int chunk = attributeDeclIndex >> CHUNK_SHIFT;
        int index = attributeDeclIndex &  CHUNK_MASK;

        return fAttributeDeclNextAttributeDeclIndex[chunk][index];
    } // getNextAttributeDeclIndex

    /**
     * getAttributeDeclIndex
     * 
     * @param elementDeclIndex 
     * @param attributeDeclName 
     * 
     * @return index of the attribue named attributeDeclName for the element with index elementDeclIndex
     */
    public int getAttributeDeclIndex(int elementDeclIndex, String attributeDeclName) {
        // REVISIT: [Q] How is this supposed to be overridden efficiently by 
        //          a subclass if all of the data structures are private? -Ac
        return -1; // should be overide by sub classes
    }

    /**
     * getAttributeDecl
     * 
     * @param attributeDeclIndex 
     * @param attributeDecl The values of this structure are set by this call.
     * 
     * @return true if getAttributeDecl was able to fill in the value of attributeDecl
     */
    public boolean getAttributeDecl(int attributeDeclIndex, XMLAttributeDecl attributeDecl) {
        if (attributeDeclIndex < 0 || attributeDeclIndex >= fAttributeDeclCount) {
            return false;
        }
        int chunk = attributeDeclIndex >> CHUNK_SHIFT;
        int index = attributeDeclIndex & CHUNK_MASK;

        attributeDecl.name.setValues(fAttributeDeclName[chunk][index]);

        short attributeType;
        boolean isList;

        if (fAttributeDeclType[chunk][index] == -1) {

            attributeType = -1;
            isList = false;
        } else {
            attributeType = (short) (fAttributeDeclType[chunk][index] & LIST_MASK);
            isList = (fAttributeDeclType[chunk][index] & LIST_FLAG) != 0;
        }
        attributeDecl.simpleType.setValues(attributeType,fAttributeDeclName[chunk][index].localpart,
                                           fAttributeDeclEnumeration[chunk][index],
                                           isList, fAttributeDeclDefaultType[chunk][index],
                                           fAttributeDeclDefaultValue[chunk][index], 
                                           fAttributeDeclNonNormalizedDefaultValue[chunk][index], 
                                           fAttributeDeclDatatypeValidator[chunk][index]);
        return true;

    } // getAttributeDecl


    /**
     * Returns whether the given attribute is of type CDATA or not
     *
     * @param elName The element name.
     * @param atName The attribute name.
     *
     * @return true if the attribute is of type CDATA
     */
    public boolean isCDATAAttribute(QName elName, QName atName) {
        int elDeclIdx = getElementDeclIndex(elName, -1);
        int atDeclIdx = getAttributeDeclIndex(elDeclIdx, atName.rawname);
        if (getAttributeDecl(elDeclIdx, fAttributeDecl)
            && fAttributeDecl.simpleType.type != XMLSimpleType.TYPE_CDATA){
            return false;
        }
        return true;
    }


    /**
     * getFirstEntityDeclIndex
     * 
     * @return index of the first EntityDecl
     */
    public int getFirstEntityDeclIndex() {
        throw new RuntimeException("implement Grammar#getFirstEntityDeclIndex():int");
    } // getFirstEntityDeclIndex

    /**
     * getNextEntityDeclIndex
     * 
     * @param elementDeclIndex 
     * 
     * @return index of the next EntityDecl
     */
    public int getNextEntityDeclIndex(int elementDeclIndex) {
        throw new RuntimeException("implement Grammar#getNextEntityDeclIndex(int):int");
    } // getNextEntityDeclIndex

    /**
     * getEntityDeclIndex
     * 
     * @param entityDeclName 
     * 
     * @return the index of the EntityDecl
     */
    public int getEntityDeclIndex(String entityDeclName) {
        if (entityDeclName == null) {
            return -1;
        }
        for (int i=0; i<fEntityCount; i++) {
            int chunk = i >> CHUNK_SHIFT;
            int index = i & CHUNK_MASK;
            if ( fEntityName[chunk][index] == entityDeclName || entityDeclName.equals(fEntityName[chunk][index]) ) {
                return i;
            }
        }
    
        return -1;
    } // getEntityDeclIndex

    /**
     * getEntityDecl
     * 
     * @param entityDeclIndex 
     * @param entityDecl 
     * 
     * @return true if getEntityDecl was able to fill entityDecl with the contents of the entity
     * with index entityDeclIndex
     */
    public boolean getEntityDecl(int entityDeclIndex, XMLEntityDecl entityDecl) {
        if (entityDeclIndex < 0 || entityDeclIndex >= fEntityCount) {
            return false;
        }
        int chunk = entityDeclIndex >> CHUNK_SHIFT;
        int index = entityDeclIndex & CHUNK_MASK;

        entityDecl.setValues(fEntityName[chunk][index],
                             fEntityPublicId[chunk][index],
                             fEntitySystemId[chunk][index],
                             fEntityBaseSystemId[chunk][index],
                             fEntityNotation[chunk][index],
                             fEntityValue[chunk][index],
                             fEntityIsPE[chunk][index] == 0 ? false : true ,
                             fEntityInExternal[chunk][index] == 0 ? false : true );

        return true;
    } // getEntityDecl

    /**
     * getFirstNotationDeclIndex
     * 
     * @return the index of the first notation declaration in the grammar
     */
    public int getFirstNotationDeclIndex() {
        throw new RuntimeException("implement Grammar#getFirstNotationDeclIndex():int");
    } // getFirstNotationDeclIndex

    /**
     * getNextNotationDeclIndex
     * 
     * @param elementDeclIndex 
     * 
     * @return index of the next notation declaration in the grammar
     */
    public int getNextNotationDeclIndex(int elementDeclIndex) {
        throw new RuntimeException("implement Grammar#getNextNotationDeclIndex(int):int");
    } // getNextNotationDeclIndex

    /**
     * getNotationDeclIndex
     * 
     * @param notationDeclName 
     * 
     * @return the index if found a notation with the name, otherwise -1.
     */
    public int getNotationDeclIndex(String notationDeclName) {
        if (notationDeclName == null) {
            return -1;
        }
        for (int i=0; i<fNotationCount; i++) {
            int chunk = i >> CHUNK_SHIFT;
            int index = i & CHUNK_MASK;
            if ( fNotationName[chunk][index] == notationDeclName || notationDeclName.equals(fNotationName[chunk][index]) ) {
                return i;
            }
        }

        return -1;
    } // getNotationDeclIndex

    /**
     * getNotationDecl
     * 
     * @param notationDeclIndex 
     * @param notationDecl 
     * 
     * @return return true of getNotationDecl can fill notationDecl with information about 
     * the notation at notationDeclIndex.
     */
    public boolean getNotationDecl(int notationDeclIndex, XMLNotationDecl notationDecl) {
        if (notationDeclIndex < 0 || notationDeclIndex >= fNotationCount) {
            return false;
        }
        int chunk = notationDeclIndex >> CHUNK_SHIFT;
        int index = notationDeclIndex & CHUNK_MASK;

        notationDecl.setValues(fNotationName[chunk][index], 
                               fNotationPublicId[chunk][index],
                               fNotationSystemId[chunk][index],
			       fNotationBaseSystemId[chunk][index]);

        return true;

    } // getNotationDecl

    /**
     * getContentSpec
     * 
     * @param contentSpecIndex 
     * @param contentSpec
     * 
     * @return true if find the requested contentSpec node, false otherwise
     */
    public boolean getContentSpec(int contentSpecIndex, XMLContentSpec contentSpec) {
        if (contentSpecIndex < 0 || contentSpecIndex >= fContentSpecCount )
            return false;

        int chunk = contentSpecIndex >> CHUNK_SHIFT;
        int index = contentSpecIndex & CHUNK_MASK;

        contentSpec.type       = fContentSpecType[chunk][index];
        contentSpec.value      = fContentSpecValue[chunk][index];
        contentSpec.otherValue = fContentSpecOtherValue[chunk][index];
        return true;
    }

    /**
     * getContentSpecAsString
     *
     * @param elementDeclIndex
     *
     * @ return String
     */
    public String getContentSpecAsString(int elementDeclIndex){

        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
            return null;
        }

        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        int contentSpecIndex = fElementDeclContentSpecIndex[chunk][index];

        // lookup content spec node
        XMLContentSpec contentSpec = new XMLContentSpec();

        if (getContentSpec(contentSpecIndex, contentSpec)) {

            // build string
            StringBuffer str = new StringBuffer();
            int    parentContentSpecType = contentSpec.type & 0x0f;
            int    nextContentSpec;
            switch (parentContentSpecType) {
                case XMLContentSpec.CONTENTSPECNODE_LEAF: {
                    str.append('(');
                    if (contentSpec.value == null && contentSpec.otherValue == null) {
                        str.append("#PCDATA");
                    }
                    else {
                        str.append(contentSpec.value);
                    }
                    str.append(')');
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE: {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    nextContentSpec = contentSpec.type;

                    if (nextContentSpec == XMLContentSpec.CONTENTSPECNODE_LEAF) {
                        str.append('(');
                        str.append(contentSpec.value);
                        str.append(')');
                    } else if( nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE  ||
                        nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE  ||
                        nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ) {
                        str.append('(' );
                        appendContentSpec(contentSpec, str, 
                                          true, parentContentSpecType );
                        str.append(')');
                    } else {
                        appendContentSpec(contentSpec, str, 
                                          true, parentContentSpecType );
                    }
                    str.append('?');
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE: {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    nextContentSpec = contentSpec.type;

                    if ( nextContentSpec == XMLContentSpec.CONTENTSPECNODE_LEAF) {
                        str.append('(');
                        if (contentSpec.value == null && contentSpec.otherValue == null) {
                            str.append("#PCDATA");
                        }
                        else if (contentSpec.otherValue != null) {
                            str.append("##any:uri="+contentSpec.otherValue);
                        }
                        else if (contentSpec.value == null) {
                            str.append("##any");
                        }
                        else {
                            appendContentSpec(contentSpec, str, 
                                              true, parentContentSpecType );
                        }
                        str.append(')');
                    } else if( nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE  ||
                        nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE  ||
                        nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ) {
                        str.append('(' );
                        appendContentSpec(contentSpec, str, 
                                          true, parentContentSpecType );
                        str.append(')');
                    } else {
                        appendContentSpec(contentSpec, str, 
                                          true, parentContentSpecType );
                    }
                    str.append('*');
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE: {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    nextContentSpec = contentSpec.type;

                    if ( nextContentSpec == XMLContentSpec.CONTENTSPECNODE_LEAF) {
                        str.append('(');
                        if (contentSpec.value == null && contentSpec.otherValue == null) {
                            str.append("#PCDATA");
                        }
                        else if (contentSpec.otherValue != null) {
                            str.append("##any:uri="+contentSpec.otherValue);
                        }
                        else if (contentSpec.value == null) {
                            str.append("##any");
                        }
                        else {
                            str.append(contentSpec.value);
                        }
                        str.append(')');
                    } else if( nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE  ||
                        nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE  ||
                        nextContentSpec == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ) {
                        str.append('(' );
                        appendContentSpec(contentSpec, str, 
                                          true, parentContentSpecType );
                        str.append(')');
                    } else {
                        appendContentSpec(contentSpec, str,
                                          true, parentContentSpecType);
                    }
                    str.append('+');
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_CHOICE:
                case XMLContentSpec.CONTENTSPECNODE_SEQ: {
                    appendContentSpec(contentSpec, str, 
                                      true, parentContentSpecType );
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_ANY: {
                    str.append("##any");
                    if (contentSpec.otherValue != null) {
                        str.append(":uri=");
                        str.append(contentSpec.otherValue);
                    }
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_ANY_OTHER: {
                    str.append("##other:uri=");
                    str.append(contentSpec.otherValue);
                    break;
                }
                case XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL: {
                    str.append("##local");
                    break;
                }
                default: {
                    str.append("???");
                }

            } // switch type

            // return string
            return str.toString();
        }

        // not found
        return null;

    } // getContentSpecAsString(int):String

    // debugging

    public void printElements(  ) {
        int elementDeclIndex = 0;
        XMLElementDecl elementDecl = new XMLElementDecl();
        while (getElementDecl(elementDeclIndex++, elementDecl)) {

            System.out.println("element decl: "+elementDecl.name+
                               ", "+ elementDecl.name.rawname  );

            //                   ", "+ elementDecl.contentModelValidator.toString());
        }
    }

    public void printAttributes(int elementDeclIndex) {
        int attributeDeclIndex = getFirstAttributeDeclIndex(elementDeclIndex);
        System.out.print(elementDeclIndex);
        System.out.print(" [");
        while (attributeDeclIndex != -1) {
            System.out.print(' ');
            System.out.print(attributeDeclIndex);
            printAttribute(attributeDeclIndex);
            attributeDeclIndex = getNextAttributeDeclIndex(attributeDeclIndex);
            if (attributeDeclIndex != -1) {
                System.out.print(",");
            }
        }
        System.out.println(" ]");
    }

    //
    // Protected methods
    //

    /**
     * getElementContentModelValidator
     * 
     * @param elementDeclIndex 
     * 
     * @return its ContentModelValidator if any.
     */
    protected ContentModelValidator getElementContentModelValidator(int elementDeclIndex) {

        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex & CHUNK_MASK;

        ContentModelValidator contentModel    =  fElementDeclContentModelValidator[chunk][index];

        // If we have one, just return that. Otherwise, gotta create one
        if (contentModel != null) {
            return contentModel;
        }

        int contentType = fElementDeclType[chunk][index];
        if (contentType == XMLElementDecl.TYPE_SIMPLE) {
            return null;
        }

        // Get the type of content this element has
        int contentSpecIndex = fElementDeclContentSpecIndex[chunk][index]; 

        /***
        if ( contentSpecIndex == -1 )
            return null;
        /***/

        XMLContentSpec  contentSpec = new XMLContentSpec();
        getContentSpec( contentSpecIndex, contentSpec );

        // And create the content model according to the spec type
        if ( contentType == XMLElementDecl.TYPE_MIXED ) {
            //
            //  Just create a mixel content model object. This type of
            //  content model is optimized for mixed content validation.
            //
            ChildrenList children = new ChildrenList();
            contentSpecTree(contentSpecIndex, contentSpec, children);
            contentModel = new MixedContentModel(children.qname,
                                                 children.type,
                                                 0, children.length, 
                                                 false, isDTD());
        } else if (contentType == XMLElementDecl.TYPE_CHILDREN) {
            //  This method will create an optimal model for the complexity
            //  of the element's defined model. If its simple, it will create
            //  a SimpleContentModel object. If its a simple list, it will
            //  create a SimpleListContentModel object. If its complex, it
            //  will create a DFAContentModel object.
            //
            contentModel = createChildModel(contentSpecIndex);
        } else {
            throw new RuntimeException("Unknown content type for a element decl "
                                     + "in getElementContentModelValidator() in AbstractDTDGrammar class");
        }

        // Add the new model to the content model for this element
        fElementDeclContentModelValidator[chunk][index] = contentModel;

        return contentModel;

    } // getElementContentModelValidator(int):ContentModelValidator

   protected int createElementDecl() {
      int chunk = fElementDeclCount >> CHUNK_SHIFT;
      int index = fElementDeclCount & CHUNK_MASK;
      ensureElementDeclCapacity(chunk);
      fElementDeclName[chunk][index]                    = new QName(); 
      fElementDeclType[chunk][index]                    = -1;  
      fElementDeclDatatypeValidator[chunk][index]       = null;
      fElementDeclContentModelValidator[chunk][index]   = null;
      fElementDeclFirstAttributeDeclIndex[chunk][index] = -1;
      fElementDeclLastAttributeDeclIndex[chunk][index]  = -1;
      fElementDeclDefaultValue[chunk][index]            = null;
      fElementDeclDefaultType[chunk][index]             = -1;
      return fElementDeclCount++;
   }

   protected void setElementDecl(int elementDeclIndex, XMLElementDecl elementDecl) {
      if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
         return;
      }
      int     chunk       = elementDeclIndex >> CHUNK_SHIFT;
      int     index       = elementDeclIndex &  CHUNK_MASK;

      int     scope       = elementDecl.scope;


      fElementDeclName[chunk][index].setValues(elementDecl.name);
      fElementDeclType[chunk][index]                  = elementDecl.type; 
      fElementDeclDatatypeValidator[chunk][index]     =
                        elementDecl.simpleType.datatypeValidator;
      fElementDeclDefaultType[chunk][index] = elementDecl.simpleType.defaultType;
      fElementDeclDefaultValue[chunk][index] = elementDecl.simpleType.defaultValue;

      fElementDeclContentModelValidator[chunk][index] = elementDecl.contentModelValidator;
         

      if (elementDecl.simpleType.list  == true ) {
         fElementDeclType[chunk][index] |= LIST_FLAG;
      }

      if (isDTD()) {
          fScopeMapping.put(scope, elementDecl.name.rawname, null, elementDeclIndex);
      }
      else {
          fScopeMapping.put( scope, elementDecl.name.localpart, 
                                             elementDecl.name.uri, elementDeclIndex);
      }
   }




   protected void putElementNameMapping(QName name, int scope,
                                        int elementDeclIndex) {
   }

   protected void setFirstAttributeDeclIndex(int elementDeclIndex, int newFirstAttrIndex){

      if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
         return;
      }

      int chunk = elementDeclIndex >> CHUNK_SHIFT;
      int index = elementDeclIndex &  CHUNK_MASK;

      fElementDeclFirstAttributeDeclIndex[chunk][index] = newFirstAttrIndex;
   }
   
   protected void setContentSpecIndex(int elementDeclIndex, int contentSpecIndex){

      if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
         return;
      }

      int chunk = elementDeclIndex >> CHUNK_SHIFT;
      int index = elementDeclIndex &  CHUNK_MASK;

      fElementDeclContentSpecIndex[chunk][index] = contentSpecIndex;
   }


   protected int createAttributeDecl() {
      int chunk = fAttributeDeclCount >> CHUNK_SHIFT;
      int index = fAttributeDeclCount & CHUNK_MASK;

      ensureAttributeDeclCapacity(chunk);
      fAttributeDeclName[chunk][index]                    = new QName();
      fAttributeDeclType[chunk][index]                    = -1;
      fAttributeDeclDatatypeValidator[chunk][index]       = null;
      fAttributeDeclEnumeration[chunk][index]             = null;
      fAttributeDeclDefaultType[chunk][index]             = XMLSimpleType.DEFAULT_TYPE_IMPLIED;
      fAttributeDeclDefaultValue[chunk][index]            = null;
      fAttributeDeclNonNormalizedDefaultValue[chunk][index]            = null;
      fAttributeDeclNextAttributeDeclIndex[chunk][index]  = -1;
      return fAttributeDeclCount++;
   }


   protected void setAttributeDecl(int elementDeclIndex, int attributeDeclIndex,
                                   XMLAttributeDecl attributeDecl) {
      int attrChunk = attributeDeclIndex >> CHUNK_SHIFT;
      int attrIndex = attributeDeclIndex &  CHUNK_MASK; 
      fAttributeDeclName[attrChunk][attrIndex].setValues(attributeDecl.name);
      fAttributeDeclType[attrChunk][attrIndex]  =  attributeDecl.simpleType.type;

      if (attributeDecl.simpleType.list) {
         fAttributeDeclType[attrChunk][attrIndex] |= LIST_FLAG;
      }
      fAttributeDeclEnumeration[attrChunk][attrIndex]  =  attributeDecl.simpleType.enumeration;
      fAttributeDeclDefaultType[attrChunk][attrIndex]  =  attributeDecl.simpleType.defaultType;
      fAttributeDeclDatatypeValidator[attrChunk][attrIndex] =  attributeDecl.simpleType.datatypeValidator;

      fAttributeDeclDefaultValue[attrChunk][attrIndex] = attributeDecl.simpleType.defaultValue;
      fAttributeDeclNonNormalizedDefaultValue[attrChunk][attrIndex] = attributeDecl.simpleType.nonNormalizedDefaultValue;

      int elemChunk     = elementDeclIndex >> CHUNK_SHIFT;
      int elemIndex     = elementDeclIndex &  CHUNK_MASK;
      int index         = fElementDeclFirstAttributeDeclIndex[elemChunk][elemIndex];
      while (index != -1) {
         if (index == attributeDeclIndex) {
            break;
         }
         attrChunk = index >> CHUNK_SHIFT;
         attrIndex = index & CHUNK_MASK;
         index = fAttributeDeclNextAttributeDeclIndex[attrChunk][attrIndex];
      }
      if (index == -1) {
         if (fElementDeclFirstAttributeDeclIndex[elemChunk][elemIndex] == -1) {
            fElementDeclFirstAttributeDeclIndex[elemChunk][elemIndex] = attributeDeclIndex;
         } else {
            index = fElementDeclLastAttributeDeclIndex[elemChunk][elemIndex];
            attrChunk = index >> CHUNK_SHIFT;
            attrIndex = index & CHUNK_MASK;
            fAttributeDeclNextAttributeDeclIndex[attrChunk][attrIndex] = attributeDeclIndex;
         }
         fElementDeclLastAttributeDeclIndex[elemChunk][elemIndex] = attributeDeclIndex;
      }
   }

   protected int createContentSpec() {
      int chunk = fContentSpecCount >> CHUNK_SHIFT;
      int index = fContentSpecCount & CHUNK_MASK;

      ensureContentSpecCapacity(chunk);
      fContentSpecType[chunk][index]       = -1;
      fContentSpecValue[chunk][index]      = null;
      fContentSpecOtherValue[chunk][index] = null;

      return fContentSpecCount++;
   }

   protected void setContentSpec(int contentSpecIndex, XMLContentSpec contentSpec) {
      int   chunk = contentSpecIndex >> CHUNK_SHIFT;
      int   index = contentSpecIndex & CHUNK_MASK;

      fContentSpecType[chunk][index]       = contentSpec.type;
      fContentSpecValue[chunk][index]      = contentSpec.value;
      fContentSpecOtherValue[chunk][index] = contentSpec.otherValue;
   }


   protected int createEntityDecl() {
       int chunk = fEntityCount >> CHUNK_SHIFT;
       int index = fEntityCount & CHUNK_MASK;

      ensureEntityDeclCapacity(chunk);
      fEntityIsPE[chunk][index] = 0;
      fEntityInExternal[chunk][index] = 0;

      return fEntityCount++;
   }

   protected void setEntityDecl(int entityDeclIndex, XMLEntityDecl entityDecl) {
       int chunk = entityDeclIndex >> CHUNK_SHIFT;
       int index = entityDeclIndex & CHUNK_MASK;

       fEntityName[chunk][index] = entityDecl.name;
       fEntityValue[chunk][index] = entityDecl.value;
       fEntityPublicId[chunk][index] = entityDecl.publicId;
       fEntitySystemId[chunk][index] = entityDecl.systemId;
       fEntityBaseSystemId[chunk][index] = entityDecl.baseSystemId;
       fEntityNotation[chunk][index] = entityDecl.notation;
       fEntityIsPE[chunk][index] = entityDecl.isPE ? (byte)1 : (byte)0;
       fEntityInExternal[chunk][index] = entityDecl.inExternal ? (byte)1 : (byte)0;
   }
   

   protected int createNotationDecl() {
       int chunk = fNotationCount >> CHUNK_SHIFT;
       int index = fNotationCount & CHUNK_MASK;

       ensureNotationDeclCapacity(chunk);

       return fNotationCount++;
   }

   protected void setNotationDecl(int notationDeclIndex, XMLNotationDecl notationDecl) {
       int chunk = notationDeclIndex >> CHUNK_SHIFT;
       int index = notationDeclIndex & CHUNK_MASK;

       fNotationName[chunk][index] = notationDecl.name;
       fNotationPublicId[chunk][index] = notationDecl.publicId;
       fNotationSystemId[chunk][index] = notationDecl.systemId;
       fNotationBaseSystemId[chunk][index] = notationDecl.baseSystemId;
   }

   protected void setTargetNamespace( String targetNamespace ){
      fTargetNamespace = targetNamespace;
   }

   // subclass shoudl overwrite this method to return the right value.
   protected boolean isDTD() {
      return true;
   }

    //
    // Private methods
    //

    private void appendContentSpec(XMLContentSpec contentSpec, 
                                   StringBuffer str, boolean parens,
                                   int parentContentSpecType ) {

        int thisContentSpec = contentSpec.type & 0x0f;
        switch (thisContentSpec) {
            case XMLContentSpec.CONTENTSPECNODE_LEAF: {
                if (contentSpec.value == null && contentSpec.otherValue == null) {
                    str.append("#PCDATA");
                }
                else if (contentSpec.value == null && contentSpec.otherValue != null) {
                    str.append("##any:uri="+contentSpec.otherValue);
                }
                else if (contentSpec.value == null) {
                    str.append("##any");
                }
                else {
                    str.append(contentSpec.value);
                }
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE: {
                if (parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE  ||
                    parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE ||
                    parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ) {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    str.append('(');
                    appendContentSpec(contentSpec, str, true, thisContentSpec );
                    str.append(')');
                } 
                else {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    appendContentSpec( contentSpec, str, true, thisContentSpec );
                }
                str.append('?');
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE: {
                if (parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE ||
                    parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE ||
                    parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ) {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    str.append('(');
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                    str.append(')' );
                } 
                else {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                }
                str.append('*');
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE: {
                if (parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE   ||
                    parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE  ||
                    parentContentSpecType == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ) {

                    str.append('(');
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                    str.append(')' );
                }
                else {
                    getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                    appendContentSpec(contentSpec, str, true, thisContentSpec);
                }
                str.append('+');
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_CHOICE:
            case XMLContentSpec.CONTENTSPECNODE_SEQ: {
                if (parens) {
                    str.append('(');
                }
                int type = contentSpec.type;
                int otherValue = ((int[])contentSpec.otherValue)[0];
                getContentSpec(((int[])contentSpec.value)[0], contentSpec);
                appendContentSpec(contentSpec, str, contentSpec.type != type, thisContentSpec);
                if (type == XMLContentSpec.CONTENTSPECNODE_CHOICE) {
                    str.append('|');
                }
                else {
                    str.append(',');
                }
                getContentSpec(otherValue, contentSpec);
                appendContentSpec(contentSpec, str, true, thisContentSpec);
                if (parens) {
                    str.append(')');
                }
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_ANY: {
                str.append("##any");
                if (contentSpec.otherValue != null) {
                    str.append(":uri=");
                    str.append(contentSpec.otherValue);
                }
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_ANY_OTHER: {
                str.append("##other:uri=");
                str.append(contentSpec.otherValue);
                break;
            }
            case XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL: {
                str.append("##local");
                break;
            }
            default: {
                str.append("???");
                break;
            }

        } // switch type

    } // appendContentSpec(XMLContentSpec.Provider,StringPool,XMLContentSpec,StringBuffer,boolean)

    //
    // Private methods
    //

    // debugging

    private void printAttribute(int attributeDeclIndex) {

        XMLAttributeDecl attributeDecl = new XMLAttributeDecl();
        if (getAttributeDecl(attributeDeclIndex, attributeDecl)) {
            System.out.print(" { ");
            System.out.print(attributeDecl.name.localpart);
            System.out.print(" }");
        }

    } // printAttribute(int)

    // content models

    /**
     * When the element has a 'CHILDREN' model, this method is called to
     * create the content model object. It looks for some special case simple
     * models and creates SimpleContentModel objects for those. For the rest
     * it creates the standard DFA style model.
     */
    private ContentModelValidator createChildModel(int contentSpecIndex) {
        
        //
        //  Get the content spec node for the element we are working on.
        //  This will tell us what kind of node it is, which tells us what
        //  kind of model we will try to create.
        //
        XMLContentSpec contentSpec = new XMLContentSpec();
        getContentSpec(contentSpecIndex, contentSpec);

        if ((contentSpec.type & 0x0f ) == XMLContentSpec.CONTENTSPECNODE_ANY ||
            (contentSpec.type & 0x0f ) == XMLContentSpec.CONTENTSPECNODE_ANY_OTHER ||
            (contentSpec.type & 0x0f ) == XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL) {
            // let fall through to build a DFAContentModel
        }

        else if (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_LEAF) {
            //
            //  Check that the left value is not -1, since any content model
            //  with PCDATA should be MIXED, so we should not have gotten here.
            //
            if (contentSpec.value == null && contentSpec.otherValue == null)
                throw new RuntimeException("ImplementationMessages.VAL_NPCD");

            //
            //  Its a single leaf, so its an 'a' type of content model, i.e.
            //  just one instance of one element. That one is definitely a
            //  simple content model.
            //

            fQName1.setValues(null, (String)contentSpec.value, 
                              (String)contentSpec.value, (String)contentSpec.otherValue);
            return new SimpleContentModel(contentSpec.type, fQName1, null, isDTD());
        } else if ((contentSpec.type == XMLContentSpec.CONTENTSPECNODE_CHOICE)
                    ||  (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_SEQ)) {
            //
            //  Lets see if both of the children are leafs. If so, then it
            //  it has to be a simple content model
            //
            XMLContentSpec contentSpecLeft  = new XMLContentSpec();
            XMLContentSpec contentSpecRight = new XMLContentSpec();

            getContentSpec( ((int[])contentSpec.value)[0], contentSpecLeft);
            getContentSpec( ((int[])contentSpec.otherValue)[0], contentSpecRight);

            if ((contentSpecLeft.type == XMLContentSpec.CONTENTSPECNODE_LEAF)
                 &&  (contentSpecRight.type == XMLContentSpec.CONTENTSPECNODE_LEAF)) {
                //
                //  Its a simple choice or sequence, so we can do a simple
                //  content model for it.
                //
                fQName1.setValues(null, (String)contentSpecLeft.value, 
                                  (String)contentSpecLeft.value, (String)contentSpecLeft.otherValue);
                fQName2.setValues(null, (String)contentSpecRight.value, 
                                  (String)contentSpecRight.value, (String)contentSpecRight.otherValue);
                return new SimpleContentModel(contentSpec.type, fQName1, fQName2, isDTD());
            }
        } else if ((contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE)
                    ||  (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE)
                    ||  (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE)) {
            //
            //  Its a repetition, so see if its one child is a leaf. If so
            //  its a repetition of a single element, so we can do a simple
            //  content model for that.
            //
            XMLContentSpec contentSpecLeft = new XMLContentSpec();
            getContentSpec(((int[])contentSpec.value)[0], contentSpecLeft);
    
            if (contentSpecLeft.type == XMLContentSpec.CONTENTSPECNODE_LEAF) {
                //
                //  It is, so we can create a simple content model here that
                //  will check for this repetition. We pass -1 for the unused
                //  right node.
                //
                fQName1.setValues(null, (String)contentSpecLeft.value, 
                                  (String)contentSpecLeft.value, (String)contentSpecLeft.otherValue);
                return new SimpleContentModel(contentSpec.type, fQName1, null, isDTD());
            }
        } else {
            throw new RuntimeException("ImplementationMessages.VAL_CST");
        }

        //
        //  Its not a simple content model, so here we have to create a DFA
        //  for this element. So we create a DFAContentModel object. He
        //  encapsulates all of the work to create the DFA.
        //

        fLeafCount = 0;
        //int leafCount = countLeaves(contentSpecIndex);
        fLeafCount = 0;
        CMNode cmn    = buildSyntaxTree(contentSpecIndex, contentSpec);

        // REVISIT: has to be fLeafCount because we convert x+ to x,x*, one more leaf
        return new DFAContentModel(  cmn, fLeafCount, isDTD(), false);

    } // createChildModel(int):ContentModelValidator

    private final CMNode buildSyntaxTree(int startNode, 
                                         XMLContentSpec contentSpec) {

        // We will build a node at this level for the new tree
        CMNode nodeRet = null;
        getContentSpec(startNode, contentSpec);
        if ((contentSpec.type & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY) {
            //nodeRet = new CMAny(contentSpec.type, -1, fLeafCount++);
            nodeRet = new CMAny(contentSpec.type, (String)contentSpec.otherValue, fLeafCount++);
        }
        else if ((contentSpec.type & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY_OTHER) {
            nodeRet = new CMAny(contentSpec.type, (String)contentSpec.otherValue, fLeafCount++);
        }
        else if ((contentSpec.type & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL) {
            nodeRet = new CMAny(contentSpec.type, null, fLeafCount++);
        }
        //
        //  If this node is a leaf, then its an easy one. We just add it
        //  to the tree.
        //
        else if (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_LEAF) {
            //
            //  Create a new leaf node, and pass it the current leaf count,
            //  which is its DFA state position. Bump the leaf count after
            //  storing it. This makes the positions zero based since we
            //  store first and then increment.
            //
            fQName1.setValues(null, (String)contentSpec.value, 
                              (String)contentSpec.value, (String)contentSpec.otherValue);
            nodeRet = new CMLeaf(fQName1, fLeafCount++);
        } 
        else {
            //
            //  Its not a leaf, so we have to recurse its left and maybe right
            //  nodes. Save both values before we recurse and trash the node.
            final int leftNode = ((int[])contentSpec.value)[0];
            final int rightNode = ((int[])contentSpec.otherValue)[0];

            if ((contentSpec.type == XMLContentSpec.CONTENTSPECNODE_CHOICE)
                ||  (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_SEQ)) {
                //
                //  Recurse on both children, and return a binary op node
                //  with the two created sub nodes as its children. The node
                //  type is the same type as the source.
                //

                nodeRet = new CMBinOp( contentSpec.type, buildSyntaxTree(leftNode, contentSpec)
                                       , buildSyntaxTree(rightNode, contentSpec));
            } 
            else if (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE) {
                nodeRet = new CMUniOp( contentSpec.type, buildSyntaxTree(leftNode, contentSpec));
            } 
            else if (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE
		       || contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE
		       || contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE) {
                nodeRet = new CMUniOp(contentSpec.type, buildSyntaxTree(leftNode, contentSpec));
            } 
            else {
		        throw new RuntimeException("ImplementationMessages.VAL_CST");
            }
        }
        // And return our new node for this level
        return nodeRet;
    }
   
    /**
     * Build a vector of valid QNames from Content Spec
     * table.
     * 
     * @param contentSpecIndex
     *               Content Spec index
     * @param vectorQName
     *               Array of QName
     * @exception RuntimeException
     */
    private void contentSpecTree(int contentSpecIndex, 
                                 XMLContentSpec contentSpec,
                                 ChildrenList children) {

        // Handle any and leaf nodes
        getContentSpec( contentSpecIndex, contentSpec);
        if ( contentSpec.type == XMLContentSpec.CONTENTSPECNODE_LEAF ||
            (contentSpec.type & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY ||
            (contentSpec.type & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY_LOCAL ||
            (contentSpec.type & 0x0f) == XMLContentSpec.CONTENTSPECNODE_ANY_OTHER) {

            // resize arrays, if needed
            if (children.length == children.qname.length) {
                QName[] newQName = new QName[children.length * 2];
                System.arraycopy(children.qname, 0, newQName, 0, children.length);
                children.qname = newQName;
                int[] newType = new int[children.length * 2];
                System.arraycopy(children.type, 0, newType, 0, children.length);
                children.type = newType;
            }

            // save values and return length
            children.qname[children.length] = new QName(null, (String)contentSpec.value, 
                                                     (String) contentSpec.value, 
                                                     (String) contentSpec.otherValue);
            children.type[children.length] = contentSpec.type;
            children.length++;
            return;
        }

        //
        //  Its not a leaf, so we have to recurse its left and maybe right
        //  nodes. Save both values before we recurse and trash the node.
        //
        final int leftNode = contentSpec.value != null 
                           ? ((int[])(contentSpec.value))[0] : -1;
        int rightNode = -1 ;
        if (contentSpec.otherValue != null ) 
            rightNode = ((int[])(contentSpec.otherValue))[0];
        else 
            return;

        if (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_CHOICE ||
            contentSpec.type == XMLContentSpec.CONTENTSPECNODE_SEQ) {
            contentSpecTree(leftNode, contentSpec, children);
            contentSpecTree(rightNode, contentSpec, children);
            return;
        }

        if (contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_ONE ||
            contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ZERO_OR_MORE ||
            contentSpec.type == XMLContentSpec.CONTENTSPECNODE_ONE_OR_MORE) {
            contentSpecTree(leftNode, contentSpec, children);
            return;
        }

        // error
        throw new RuntimeException("Invalid content spec type seen in contentSpecTree() method of Grammar class : "+contentSpec.type);

    } // contentSpecTree(int,XMLContentSpec,ChildrenList)

    // ensure capacity

    private boolean ensureElementDeclCapacity(int chunk) {
        try {
            return fElementDeclName[chunk][0] == null;
        } catch (ArrayIndexOutOfBoundsException ex) {
            fElementDeclName = resize(fElementDeclName, fElementDeclName.length * 2);
            fElementDeclType = resize(fElementDeclType, fElementDeclType.length * 2);
            fElementDeclDatatypeValidator = resize(fElementDeclDatatypeValidator, fElementDeclDatatypeValidator.length * 2);
            fElementDeclContentModelValidator = resize(fElementDeclContentModelValidator, fElementDeclContentModelValidator.length * 2);
            fElementDeclContentSpecIndex = resize(fElementDeclContentSpecIndex,fElementDeclContentSpecIndex.length * 2);
            fElementDeclFirstAttributeDeclIndex = resize(fElementDeclFirstAttributeDeclIndex, fElementDeclFirstAttributeDeclIndex.length * 2);
            fElementDeclLastAttributeDeclIndex = resize(fElementDeclLastAttributeDeclIndex, fElementDeclLastAttributeDeclIndex.length * 2);
            fElementDeclDefaultValue = resize( fElementDeclDefaultValue, fElementDeclDefaultValue.length * 2 ); 
            fElementDeclDefaultType  = resize( fElementDeclDefaultType, fElementDeclDefaultType.length *2 );
        } catch (NullPointerException ex) {
            // ignore
        }
        fElementDeclName[chunk] = new QName[CHUNK_SIZE];
        fElementDeclType[chunk] = new short[CHUNK_SIZE];
        fElementDeclDatatypeValidator[chunk] = new DatatypeValidator[CHUNK_SIZE];
        fElementDeclContentModelValidator[chunk] = new ContentModelValidator[CHUNK_SIZE];
        fElementDeclContentSpecIndex[chunk] = new int[CHUNK_SIZE];
        fElementDeclFirstAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        fElementDeclLastAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        fElementDeclDefaultValue[chunk] = new String[CHUNK_SIZE]; 
        fElementDeclDefaultType[chunk]  = new short[CHUNK_SIZE]; 
        return true;
    }

    private boolean ensureAttributeDeclCapacity(int chunk) {
        try {
            return fAttributeDeclName[chunk][0] == null;
        } catch (ArrayIndexOutOfBoundsException ex) {
            fAttributeDeclName = resize(fAttributeDeclName, fAttributeDeclName.length * 2);
            fAttributeDeclType = resize(fAttributeDeclType, fAttributeDeclType.length * 2);
            fAttributeDeclEnumeration = resize(fAttributeDeclEnumeration, fAttributeDeclEnumeration.length * 2);
            fAttributeDeclDefaultType = resize(fAttributeDeclDefaultType, fAttributeDeclDefaultType.length * 2);
            fAttributeDeclDatatypeValidator = resize(fAttributeDeclDatatypeValidator, fAttributeDeclDatatypeValidator.length * 2);
            fAttributeDeclDefaultValue = resize(fAttributeDeclDefaultValue, fAttributeDeclDefaultValue.length * 2);
            fAttributeDeclNonNormalizedDefaultValue = resize(fAttributeDeclNonNormalizedDefaultValue, fAttributeDeclNonNormalizedDefaultValue.length * 2);
            fAttributeDeclNextAttributeDeclIndex = resize(fAttributeDeclNextAttributeDeclIndex, fAttributeDeclNextAttributeDeclIndex.length * 2);
        } catch (NullPointerException ex) {
            // ignore
        }
        fAttributeDeclName[chunk] = new QName[CHUNK_SIZE];
        fAttributeDeclType[chunk] = new short[CHUNK_SIZE];
        fAttributeDeclEnumeration[chunk] = new String[CHUNK_SIZE][];
        fAttributeDeclDefaultType[chunk] = new short[CHUNK_SIZE];
        fAttributeDeclDatatypeValidator[chunk] = new DatatypeValidator[CHUNK_SIZE];
        fAttributeDeclDefaultValue[chunk] = new String[CHUNK_SIZE];
        fAttributeDeclNonNormalizedDefaultValue[chunk] = new String[CHUNK_SIZE];
        fAttributeDeclNextAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        return true;
    }
   
    private boolean ensureEntityDeclCapacity(int chunk) {
        try {
            return fEntityName[chunk][0] == null;
        } catch (ArrayIndexOutOfBoundsException ex) {
            fEntityName = resize(fEntityName, fEntityName.length * 2);
            fEntityValue = resize(fEntityValue, fEntityValue.length * 2);
            fEntityPublicId = resize(fEntityPublicId, fEntityPublicId.length * 2);
            fEntitySystemId = resize(fEntitySystemId, fEntitySystemId.length * 2);
            fEntityBaseSystemId = resize(fEntityBaseSystemId, fEntityBaseSystemId.length * 2);
            fEntityNotation = resize(fEntityNotation, fEntityNotation.length * 2);
            fEntityIsPE = resize(fEntityIsPE, fEntityIsPE.length * 2);
            fEntityInExternal = resize(fEntityInExternal, fEntityInExternal.length * 2);
        } catch (NullPointerException ex) {
            // ignore
        }
        fEntityName[chunk] = new String[CHUNK_SIZE];
        fEntityValue[chunk] = new String[CHUNK_SIZE];
        fEntityPublicId[chunk] = new String[CHUNK_SIZE];
        fEntitySystemId[chunk] = new String[CHUNK_SIZE];
        fEntityBaseSystemId[chunk] = new String[CHUNK_SIZE];
        fEntityNotation[chunk] = new String[CHUNK_SIZE];
        fEntityIsPE[chunk] = new byte[CHUNK_SIZE];
        fEntityInExternal[chunk] = new byte[CHUNK_SIZE];
        return true;
    }
      
    private boolean ensureNotationDeclCapacity(int chunk) {
        try {
            return fNotationName[chunk][0] == null;
        } catch (ArrayIndexOutOfBoundsException ex) {
            fNotationName = resize(fNotationName, fNotationName.length * 2);
            fNotationPublicId = resize(fNotationPublicId, fNotationPublicId.length * 2);
            fNotationSystemId = resize(fNotationSystemId, fNotationSystemId.length * 2);
            fNotationBaseSystemId = resize(fNotationBaseSystemId, fNotationBaseSystemId.length * 2);
        } catch (NullPointerException ex) {
            // ignore
        }
        fNotationName[chunk] = new String[CHUNK_SIZE];
        fNotationPublicId[chunk] = new String[CHUNK_SIZE];
        fNotationSystemId[chunk] = new String[CHUNK_SIZE];
        fNotationBaseSystemId[chunk] = new String[CHUNK_SIZE];
        return true;
    }

    private boolean ensureContentSpecCapacity(int chunk) {
        try {
            return fContentSpecType[chunk][0] == 0;
        } catch (ArrayIndexOutOfBoundsException ex) {
            fContentSpecType = resize(fContentSpecType, fContentSpecType.length * 2);
            fContentSpecValue = resize(fContentSpecValue, fContentSpecValue.length * 2);
            fContentSpecOtherValue = resize(fContentSpecOtherValue, fContentSpecOtherValue.length * 2);
        } catch (NullPointerException ex) {
            // ignore
        }
        fContentSpecType[chunk] = new short[CHUNK_SIZE];
        fContentSpecValue[chunk] = new Object[CHUNK_SIZE];
        fContentSpecOtherValue[chunk] = new Object[CHUNK_SIZE];
        return true;
    }

    //
    // Private static methods
    //

    // resize chunks

    private static byte[][] resize(byte array[][], int newsize) {
        byte newarray[][] = new byte[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }
   
    private static short[][] resize(short array[][], int newsize) {
        short newarray[][] = new short[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static int[][] resize(int array[][], int newsize) {
        int newarray[][] = new int[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static DatatypeValidator[][] resize(DatatypeValidator array[][], int newsize) {
        DatatypeValidator newarray[][] = new DatatypeValidator[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static ContentModelValidator[][] resize(ContentModelValidator array[][], int newsize) {
        ContentModelValidator newarray[][] = new ContentModelValidator[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static Object[][] resize(Object array[][], int newsize) {
        Object newarray[][] = new Object[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static QName[][] resize(QName array[][], int newsize) {
        QName newarray[][] = new QName[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static String[][] resize(String array[][], int newsize) {
        String newarray[][] = new String[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private static String[][][] resize(String array[][][], int newsize) {
        String newarray[][][] = new String[newsize] [][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    //
    // Classes
    //
    
    /**
     * Children list for <code>contentSpecTree</code> method.
     *
     * @author Eric Ye, IBM
     */
    private static class ChildrenList {
        
        //
        // Data
        //

        /** Length. */
        public int length = 0;

        // NOTE: The following set of data is mutually exclusive. It is
        //       written this way because Java doesn't have a native
        //       union data structure. -Ac

        /** Left and right children names. */
        public QName[] qname = new QName[2];

        /** Left and right children types. */
        public int[] type = new int[2];

    } // class ChildrenList

    //
    // Classes
    //

    /**
     * A simple Hashtable implementation that takes a tuple (int, String, 
     * String) as the key and a int as value.
     *
     * @author Eric Ye, IBM
     * @author Andy Clark, IBM
     */
    protected static final class TupleHashtable {
    
        //
        // Constants
        //
    
        /** Initial bucket size (4). */
        private static final int INITIAL_BUCKET_SIZE = 4;

        // NOTE: Changed previous hashtable size from 512 to 101 so
        //       that we get a better distribution for hashing. -Ac
        /** Hashtable size (101). */
        private static final int HASHTABLE_SIZE = 101;

        //
        // Data
        //

        private Object[][] fHashTable = new Object[HASHTABLE_SIZE][];

        //
        // Public methods
        //

        /** Associates the given value with the specified key tuple. */
        public void put(int key1, String key2, String key3, int value) {

            // REVISIT: Why +2? -Ac
            int hash = (key1+hash(key2)+hash(key3)+2) % HASHTABLE_SIZE;
            Object[] bucket = fHashTable[hash];

            if (bucket == null) {
                bucket = new Object[1 + 4*INITIAL_BUCKET_SIZE];
                bucket[0] = new int[]{1};
                bucket[1] = new int[]{key1};
                bucket[2] = key2;
                bucket[3] = key3;
                bucket[4] = new int[]{value};
                fHashTable[hash] = bucket;
            } else {
                int count = ((int[])bucket[0])[0];
                int offset = 1 + 4*count;
                if (offset == bucket.length) {
                    int newSize = count + INITIAL_BUCKET_SIZE;
                    Object[] newBucket = new Object[1 + 4*newSize];
                    System.arraycopy(bucket, 0, newBucket, 0, offset);
                    bucket = newBucket;
                    fHashTable[hash] = bucket;
                }
                boolean found = false;
                int j=1;
                for (int i=0; i<count; i++){
                    if ( ((int[])bucket[j])[0] == key1 && (String)bucket[j+1] == key2
                         && (String)bucket[j+2] == key3 ) {
                        ((int[])bucket[j+3])[0] = value;
                        found = true;
                        break;
                    }
                    j += 4;
                }
                if (! found) {
                    bucket[offset++] = new int[]{key1};
                    bucket[offset++] = key2;
                    bucket[offset++] = key3;
                    bucket[offset]= new int[]{value};
                    ((int[])bucket[0])[0] = ++count;
                }

            }
            //System.out.println("put("+key1+','+key2+','+key3+" -> "+value+')');
            //System.out.println("get("+key1+','+key2+','+key3+") -> "+get(key1,key2,key3));

        } // put(int,String,String,int)

        /** Returns the value associated with the specified key tuple. */
        public int get(int key1, String key2, String key3) {

            int hash = (key1+hash(key2)+hash(key3)+2) % HASHTABLE_SIZE;
            Object[] bucket = fHashTable[hash];

            if (bucket == null) {
                return -1;
            }
            int count = ((int[])bucket[0])[0];

            int j=1;
            for (int i=0; i<count; i++){
                if ( ((int[])bucket[j])[0] == key1 && (String)bucket[j+1] == key2
                     && (String)bucket[j+2] == key3) {
                    return ((int[])bucket[j+3])[0];
                }
                j += 4;
            }
            return -1;

        } // get(int,String,String)

        //
        // Protected methods
        //

        /** Returns a hash value for the specified symbol. */
        protected int hash(String symbol) {

            if (symbol == null) {
                return 0;
            }
            int code = 0;
            int length = symbol.length();
            for (int i = 0; i < length; i++) {
                code = code * 37 + symbol.charAt(i);
            }
            return code & 0x7FFFFFF;

        } // hash(String):int

    }  // class TupleHashtable

    //
    // EntityState methods
    //
    public boolean isEntityDeclared (String name){
        return (getEntityDeclIndex(name)!=-1)?true:false;
    }

    public boolean isEntityUnparsed (String name){
        int entityIndex = getEntityDeclIndex(name);
        if (entityIndex >-1) {
            int chunk = entityIndex >> CHUNK_SHIFT;
            int index = entityIndex & CHUNK_MASK;
            //for unparsed entity notation!=null
            return (fEntityNotation[chunk][index]!=null)?true:false;
        }
        return false; 
    }

} // class AbstractDTDGrammar
