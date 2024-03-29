/*
 * Created on Dec 9, 2009
 *
 */
package com.wolfram.alpha.impl;

import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAInfo;
import com.wolfram.alpha.net.HttpProvider;
import com.wolfram.alpha.visitor.Visitable;
import com.wolfram.alpha.visitor.Visitor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class WAInfoImpl implements WAInfo, Serializable {

    static final WAInfoImpl[] EMPTY_ARRAY = new WAInfoImpl[0];
    private static final long serialVersionUID = 687066271144463657L;
    private String text;
    private Visitable[] contentElements = EMPTY_VISITABLE_ARRAY;

    
    WAInfoImpl(Element thisElement, HttpProvider http, File tempDir) throws WAException {
        
        text = thisElement.getAttribute("text");
        
        NodeList subElements = thisElement.getChildNodes();
        int numSubElements = subElements.getLength();
        List contentList = new ArrayList(numSubElements);
        for (int i = 0; i < numSubElements; i++) {
            Node child = subElements.item(i);
            String name = child.getNodeName();
            if ("link".equals(name)) {
                contentList.add(new WALinkImpl((Element) child));
            } else if ("img".equals(name)) {
                contentList.add(new WAImageImpl((Element) child, http, tempDir));
            } else if ("units".equals(name)) {
                contentList.add(new WAUnitsImpl((Element) child, http, tempDir));
            }
        }
        contentElements = (Visitable[]) contentList.toArray(new Visitable[contentList.size()]);
    }
    
    
    public Visitable[] getContents() {
        return contentElements;
    }

    public String getText() {
        return text;
    }

    
    ///////////////////////////  Visitor interface  ////////////////////////////
    
    public void accept(Visitor v) {
        v.visit(this);
    }

}
