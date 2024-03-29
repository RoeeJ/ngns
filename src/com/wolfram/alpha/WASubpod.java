/*
 * Created on Dec 9, 2009
 *
 */
package com.wolfram.alpha;

import com.wolfram.alpha.visitor.Visitable;


public interface WASubpod extends Visitable {

    String getTitle();
    Visitable[] getContents();
    
    void acquireImage();
    
    Object getUserData();

    void setUserData(Object obj);

}
