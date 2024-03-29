/*
 * Created on Dec 8, 2009
 *
 */
package com.wolfram.alpha.impl;

import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAWarning;
import com.wolfram.alpha.visitor.Visitable;
import com.wolfram.alpha.visitor.Visitor;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.Serializable;
import java.util.ArrayList;

// Warnings are not well defined in structure, bit they all have a text attribute. The other attributes are made
// available as an array of string pairs: {{"word", "Foo"}, {"suggestion", "Bar"}}.

public class WAWarningImpl implements WAWarning, Visitable, Serializable {

    static final WAWarningImpl[] EMPTY_ARRAY = new WAWarningImpl[0];
    private static final String[][] NO_ATTRIBUTES = new String[0][2];
    private static final long serialVersionUID = 2599384508960192266L;
    private String type;
    private String text;
    private String[][] attributes = NO_ATTRIBUTES;

    
    WAWarningImpl(Element thisElement) throws WAException {
        
        type = thisElement.getNodeName();
        text = thisElement.getAttribute("text");
        ArrayList attrPairs = new ArrayList();
        NamedNodeMap attrs = thisElement.getAttributes();
        int numAttrs = attrs.getLength();
        for (int i = 0; i < numAttrs; i++) {
            Node attr = attrs.item(i);
            String attrName = attr.getNodeName();
            if (!attrName.equals("text")) {
                String attrValue = attr.getNodeValue();
                attrPairs.add(new String[]{attrName, attrValue});
            }
        }
        if (attrPairs.size() > 0)
            attributes = (String[][]) attrPairs.toArray(new String[attrPairs.size()][2]);
    }
    
    
    public String[][] getAttributes() {
        return attributes;
    }


    public String getText() {
        return text;
    }


    public String getType() {
        return type;
    }
    

    ///////////////////////////  Visitor interface  ////////////////////////////
    
    public void accept(Visitor v) {
        v.visit(this);
    }


}
