/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights 
 * reserved.
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

package org.apache.xerces.validators.common;

import org.apache.xerces.framework.XMLContentSpec;
import org.apache.xerces.utils.Hash2intTable;
import org.apache.xerces.utils.QName;

import org.apache.xerces.validators.datatype.DatatypeValidator;
import org.apache.xerces.validators.common.XMLContentModel;
import org.w3c.dom.Document;

/**
 * @version $Id$
 */
public class Grammar
implements XMLContentSpec.Provider {

    //
    // Constants
    //

    private static final int CHUNK_SHIFT = 8; // 2^8 = 256
    private static final int CHUNK_SIZE = (1 << CHUNK_SHIFT);
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;
    private static final int INITIAL_CHUNK_COUNT = (1 << (10 - CHUNK_SHIFT)); // 2^10 = 1k

    //
    // Data
    //

    // basic information

    private int fTargetNamespace;

    private Document fGrammarDocument;

    // element decl tables

    private int fElementDeclCount;
    private int fElementDeclNameIndex[][] = new int[INITIAL_CHUNK_COUNT][];
    private int fElementDeclType[][] = new int[INITIAL_CHUNK_COUNT][];
    private DatatypeValidator fElementDeclDatatypeValidator[][] = new DatatypeValidator[INITIAL_CHUNK_COUNT][];
    private int fElementDeclContentSpecIndex[][] = new int[INITIAL_CHUNK_COUNT][];
    private XMLContentModel fElementDeclContentModelValidator[][] = new XMLContentModel[INITIAL_CHUNK_COUNT][];
    private int fElementDeclFirstAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];
    private int fElementDeclLastAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    // content spec tables

    private int fContentSpecCount;
    private int fContentSpecType[][] = new int[INITIAL_CHUNK_COUNT][];
    private int fContentSpecValue[][] = new int[INITIAL_CHUNK_COUNT][];
    private int fContentSpecOtherValue[][] = new int[INITIAL_CHUNK_COUNT][];

    // attribute decl tables

    private int fAttributeDeclCount;
    private QName fAttributeDeclName[][] = new QName[INITIAL_CHUNK_COUNT][];
    private int   fAttributeDeclType[][] = new int[INITIAL_CHUNK_COUNT][];
    private String   fAttributeDeclDefaultType[][] = new String[INITIAL_CHUNK_COUNT][];
    private DatatypeValidator fAttributeDeclDatatypeValidator[][] = new DatatypeValidator[INITIAL_CHUNK_COUNT][];
    private String fAttributeDeclDefaultValue[][] = new String[INITIAL_CHUNK_COUNT][];
    private int fAttributeDeclNextAttributeDeclIndex[][] = new int[INITIAL_CHUNK_COUNT][];

    // scope mapping tables

    private Hash2intTable fElementNameAndScopeToElementDeclIndexMapping = new Hash2intTable();
    // TODO

    //
    // Public methods
    //

    public Document getGrammarDocument() {
        return fGrammarDocument;
    }

    public int getElementDeclIndex(int nameIndex, int scopeIndex) {//TODO
	if (nameIndex > -1 && scopeIndex >-2 ) {
	    return fElementNameAndScopeToElementDeclIndexMapping.get(nameIndex, scopeIndex);
	}
        return -1;
    }

    public boolean getElementDecl(int elementDeclIndex, XMLElementDecl elementDecl) {
        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {
            return false;
        }

        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        elementDecl.name.localpart          = fElementDeclNameIndex[chunk][index];               
        elementDecl.type                    = fElementDeclType[chunk][index];                    
        elementDecl.datatypeValidator       = fElementDeclDatatypeValidator[chunk][index];       
        elementDecl.contentSpecIndex        = fElementDeclContentSpecIndex[chunk][index];        
        elementDecl.contentModelValidator   = fElementDeclContentModelValidator[chunk][index];   

        //elementDecl.enclosingScope          = 
        elementDecl.firstAttributeDeclIndex = fElementDeclFirstAttributeDeclIndex[chunk][index];
        elementDecl.lastAttributeDeclIndex  = fElementDeclLastAttributeDeclIndex[chunk][index];

        return true;
    }

    public int getFirstAttributeDeclIndex(int elementDeclIndex) {
        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        return  fElementDeclFirstAttributeDeclIndex[chunk][index];
    }

    public int getNextAttributeDeclIndex(int attributeDeclIndex) {
        int chunk = attributeDeclIndex >> CHUNK_SHIFT;
        int index = attributeDeclIndex &  CHUNK_MASK;

        return fAttributeDeclNextAttributeDeclIndex[chunk][index];
    }

    public boolean getContentSpec(int contentSpecIndex, XMLContentSpec contentSpec) {
        if (contentSpecIndex < 0 || contentSpecIndex >= fContentSpecCount )
           return false;

        int chunk = contentSpecIndex >> CHUNK_SHIFT;
        int index = contentSpecIndex & CHUNK_MASK;

        XMLContentSpec  contentSpecNode   = new XMLContentSpec();         

        contentSpecNode.type       = fContentSpecType[chunk][index];
        contentSpecNode.value      = fContentSpecValue[chunk][index];
        contentSpecNode.otherValue = fContentSpecOtherValue[chunk][index];

        if ( contentSpecNode.type == XMLContentSpec.CONTENTSPECNODE_CHOICE ||
             contentSpecNode.type == XMLContentSpec.CONTENTSPECNODE_SEQ ) {
            if (++contentSpecIndex == CHUNK_SIZE ) {
                chunk++;
                contentSpecIndex = 0;
            }
            contentSpecNode.otherValue = fContentSpecOtherValue[chunk][contentSpecIndex];
        } else {
            contentSpecNode.otherValue = -1;
        }
        return true;
    }


    public boolean getElementContentModel(int elementDeclIndex,
                                          XMLContentModel contentModel ) {
        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount)
            return false;
        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex & CHUNK_MASK;
        contentModel =  fElementDeclContentModelValidator[chunk][index];
        return true;
    }



    public boolean getAttributeDecl(int attributeDeclIndex, XMLAttributeDecl attributeDecl) {
        return false;
    }

    //
    // Protected methods
    //

    protected void setGrammarDocument(Document grammarDocument) {
        fGrammarDocument = grammarDocument;
    }

    protected int createElementDecl() {

        int chunk = fElementDeclCount >> CHUNK_SHIFT;
        int index = fElementDeclCount & CHUNK_MASK;
        if ( ensureElementDeclCapacity(chunk) == true ) { // create an ElementDecl
            fElementDeclNameIndex[chunk][index]               = -1; 
            fElementDeclType[chunk][index]                    = -1;    
            fElementDeclDatatypeValidator[chunk][index]       = null;

            fContentSpecType[chunk][index]                    = -1;
            fContentSpecValue[chunk][index]                   = -1;
            fContentSpecOtherValue[chunk][index]              = -1;

            fElementDeclFirstAttributeDeclIndex[chunk][index] = -1;
            fElementDeclLastAttributeDeclIndex[chunk][index]  = -1;
        }
        return fElementDeclCount++;
    }

    protected void setElementDecl(int elementDeclIndex, XMLElementDecl elementDecl) {

        if (elementDeclIndex < 0 || elementDeclIndex >= fElementDeclCount) {

        }
        int chunk = elementDeclIndex >> CHUNK_SHIFT;
        int index = elementDeclIndex &  CHUNK_MASK;

        fElementDeclNameIndex[chunk][index]               = elementDecl.name.localpart;
        fElementDeclType[chunk][index]                    = elementDecl.type;
        fElementDeclDatatypeValidator[chunk][index]       = elementDecl.datatypeValidator;
        fElementDeclContentSpecIndex[chunk][index]        = elementDecl.contentSpecIndex;
        fElementDeclContentModelValidator[chunk][index]   = elementDecl.contentModelValidator;

	// add the mapping information to the 
        fElementNameAndScopeToElementDeclIndexMapping.put(elementDecl.name.localpart, elementDecl.enclosingScope, 
							  elementDeclIndex);
        //fElementDeclFirstAttributeDeclIndex[chunk][index] = 
        //fElementDeclLastAttributeDeclIndex[chunk][index]  = 
    }


    protected int createContentSpec() {
        int chunk = fContentSpecCount >> CHUNK_SHIFT;
        int index = fContentSpecCount & CHUNK_MASK;

        if ( ensureContentSpecCapacity(chunk) == true ) { // create an ContentSpec
            fContentSpecType[chunk][index]       = -1;
            fContentSpecValue[chunk][index]      = -1;
            fContentSpecOtherValue[chunk][index] = -1;
        }

        return fContentSpecCount++;
    }

    protected void setContentSpec(int contentSpecIndex, XMLContentSpec contentSpec) {
        int   chunk = contentSpecIndex >> CHUNK_SHIFT;
        int   index = contentSpecIndex & CHUNK_MASK;

        fContentSpecType[chunk][index]       = contentSpec.type;
        fContentSpecValue[chunk][index]      = contentSpec.value;
        fContentSpecOtherValue[chunk][index] = contentSpec.otherValue;
    }

    protected int createAttributeDecl() {
        int chunk = fAttributeDeclCount >> CHUNK_SHIFT;
        int index = fAttributeDeclCount & CHUNK_MASK;

        if ( ensureAttributeDeclCapacity(chunk) == true ) { // create an AttributeDecl
            fAttributeDeclName[chunk][index]                    = null;
	    fAttributeDeclType[chunk][index]                    = -1;
	    fAttributeDeclDefaultType[chunk][index]             = null; 
            fAttributeDeclDatatypeValidator[chunk][index]       = null;
            fAttributeDeclDefaultValue[chunk][index]            = null;
            fAttributeDeclNextAttributeDeclIndex[chunk][index]  = -1;
        }
        return fAttributeDeclCount++;
    }


    protected void setAttributeDecl(int elementDeclIndex, int attributeDeclIndex, XMLAttributeDecl attributeDecl) {
       int elemChunk     = elementDeclIndex >> CHUNK_SHIFT;
       int elemIndex     = elementDeclIndex &  CHUNK_MASK;

       int thisAttrChunk = attributeDeclIndex >> CHUNK_SHIFT;
       int thisAttrIndex = attributeDeclIndex &  CHUNK_MASK; 

       fAttributeDeclName[thisAttrChunk][thisAttrIndex]  =  attributeDecl.name;
       fAttributeDeclType[thisAttrChunk][thisAttrIndex]  =  attributeDecl.type;
       fAttributeDeclDefaultType[thisAttrChunk][thisAttrIndex]  =  attributeDecl.defaultType;
       fAttributeDeclDatatypeValidator[thisAttrChunk][thisAttrIndex] =  attributeDecl.datatypeValidator;
       fAttributeDeclDefaultValue[thisAttrChunk][thisAttrIndex]      =  attributeDecl.defaultValue;


       int lastAttrDeclIndex = fElementDeclLastAttributeDeclIndex[elemChunk][elemIndex];
       int lastAttrChunk     = lastAttrDeclIndex >> CHUNK_SHIFT; 
       int lastAttrIndex     = lastAttrDeclIndex &  CHUNK_MASK;

       fAttributeDeclNextAttributeDeclIndex[lastAttrChunk][lastAttrIndex]
                                                                = attributeDeclIndex;

       fElementDeclLastAttributeDeclIndex[elemChunk][elemIndex] = attributeDeclIndex;

       fAttributeDeclNextAttributeDeclIndex[thisAttrChunk][thisAttrIndex]
                                      =  -1; // we are created at the end of ElementDecl
    }

    //
    // Private methods
    //

    // ensure capacity

    private boolean ensureElementDeclCapacity(int chunk) {
        try {
            return fElementDeclNameIndex[chunk][0] == 0;
        } catch (ArrayIndexOutOfBoundsException ex) {
            fElementDeclNameIndex = resize(fElementDeclNameIndex, fElementDeclNameIndex.length * 2);
            fElementDeclType = resize(fElementDeclType, fElementDeclType.length * 2);
            fElementDeclDatatypeValidator = resize(fElementDeclDatatypeValidator, fElementDeclDatatypeValidator.length * 2);
            fElementDeclContentSpecIndex = resize(fElementDeclContentSpecIndex, fElementDeclContentSpecIndex.length * 2);
            fElementDeclContentModelValidator = resize(fElementDeclContentModelValidator, fElementDeclContentModelValidator.length * 2);
            fElementDeclFirstAttributeDeclIndex = resize(fElementDeclNameIndex, fElementDeclFirstAttributeDeclIndex.length * 2);
            fElementDeclLastAttributeDeclIndex = resize(fElementDeclLastAttributeDeclIndex, fElementDeclNameIndex.length * 2);
        } catch (NullPointerException ex) {
            // ignore
        }
        fElementDeclNameIndex[chunk] = new int[CHUNK_SIZE];
        fElementDeclType[chunk] = new int[CHUNK_SIZE];
        fElementDeclDatatypeValidator[chunk] = new DatatypeValidator[CHUNK_SIZE];
        fElementDeclContentSpecIndex[chunk] = new int[CHUNK_SIZE];
        fElementDeclContentModelValidator[chunk] = new XMLContentModel[CHUNK_SIZE];
        fElementDeclFirstAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        fElementDeclLastAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
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
        fContentSpecType[chunk] = new int[CHUNK_SIZE];
        fContentSpecValue[chunk] = new int[CHUNK_SIZE];
        fContentSpecOtherValue[chunk] = new int[CHUNK_SIZE];
        return true;
    }

    private boolean ensureAttributeDeclCapacity(int chunk) {
        try {
            return fAttributeDeclName[chunk][0] == null;
        } catch (ArrayIndexOutOfBoundsException ex) {
            fAttributeDeclName = resize(fAttributeDeclName, fAttributeDeclName.length * 2);
            fAttributeDeclType = resize(fAttributeDeclType, fAttributeDeclType.length * 2);
            fAttributeDeclDefaultType = resize(fAttributeDeclDefaultType, fAttributeDeclDefaultType.length * 2);
            fAttributeDeclDatatypeValidator = resize(fAttributeDeclDatatypeValidator, fAttributeDeclDatatypeValidator.length * 2);
            fAttributeDeclDefaultValue = resize(fAttributeDeclDefaultValue, fAttributeDeclDefaultValue.length * 2);
            fAttributeDeclNextAttributeDeclIndex = resize(fAttributeDeclNextAttributeDeclIndex, fAttributeDeclNextAttributeDeclIndex.length * 2);
        } catch (NullPointerException ex) {
            // ignore
        }
        fAttributeDeclName[chunk] = new QName[CHUNK_SIZE];
        fAttributeDeclType[chunk] = new int[CHUNK_SIZE];
        fAttributeDeclDefaultType[chunk] = new String[CHUNK_SIZE];
        fAttributeDeclDatatypeValidator[chunk] = new DatatypeValidator[CHUNK_SIZE];
        fAttributeDeclDefaultValue[chunk] = new String[CHUNK_SIZE];
        fAttributeDeclNextAttributeDeclIndex[chunk] = new int[CHUNK_SIZE];
        return true;
    }

    // resize initial chunk

    private int[][] resize(int array[][], int newsize) {
        int newarray[][] = new int[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private DatatypeValidator[][] resize(DatatypeValidator array[][], int newsize) {
        DatatypeValidator newarray[][] = new DatatypeValidator[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private XMLContentModel[][] resize(XMLContentModel array[][], int newsize) {
        XMLContentModel newarray[][] = new XMLContentModel[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private QName[][] resize(QName array[][], int newsize) {
        QName newarray[][] = new QName[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

    private String[][] resize(String array[][], int newsize) {
        String newarray[][] = new String[newsize][];
        System.arraycopy(array, 0, newarray, 0, array.length);
        return newarray;
    }

} // class Grammar
