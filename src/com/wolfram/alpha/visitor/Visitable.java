/*
 * Created on Dec 8, 2009
 *
 */
package com.wolfram.alpha.visitor;

import java.io.Serializable;


public interface Visitable extends Serializable {

    Visitable[] EMPTY_VISITABLE_ARRAY = new Visitable[0];

    void accept(Visitor v);
}
