/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999,2000 The Apache Software Foundation.  All rights 
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

package org.apache.xerces.impl.dv.dtd;

import java.util.Hashtable;

/**
 * @version $Id$
 */
public interface DatatypeValidator {
    //
    // Data
    //
    public static final int FACET_LENGTH       = 1;
    public static final int FACET_MINLENGTH    = 1<<1;
    public static final int FACET_MAXLENGTH    = 1<<2;
    public static final int FACET_PATTERN      = 1<<3; 
    public static final int FACET_ENUMERATION  = 1<<4;
    public static final int FACET_WHITESPACE   = 1<<5;
    public static final int FACET_MAXINCLUSIVE = 1<<6;
    public static final int FACET_MAXEXCLUSIVE = 1<<7;
    public static final int FACET_MININCLUSIVE = 1<<8;
    public static final int FACET_MINEXCLUSIVE = 1<<9;
    public static final int FACET_PRECISSION   = 1<<10;
    public static final int FACET_SCALE        = 1<<11;
    public static final int FACET_ENCODING     = 1<<12;
    public static final int FACET_DURATION     = 1<<13;
    public static final int FACET_PERIOD       = 1<<14;

    public static final short WHITESPACE_NONE     =0;
    public static final short WHITESPACE_PRESERVE =1;
    public static final short WHITESPACE_REPLACE  =2;
    public static final short WHITESPACE_COLLAPSE =3;

    //
    // Methods
    //

    /**
     * getFacets
     * 
     * @return a hashtable of the facet suported by the validator
     */
    public Hashtable getFacets();

    /**
     * validate
     * 
     * @param data 
     * @param state 
     */
    public void validate(String data, Object state)
        throws InvalidDatatypeValueException;

    /**
     * compare
     * 
     * @param value1 
     * @param value2 
     * 
     * @return 0 if value1 and value2 are equal, a value less than 0 if value1 is less than value2, 
     * a value greater than 0 if value1 is greater than value2
     */
    public int compare(String value1, String value2)
        throws InvalidDatatypeValueException;

} // interface DatatypeValidator
