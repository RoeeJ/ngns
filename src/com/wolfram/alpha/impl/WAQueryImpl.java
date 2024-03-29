/*
 * Created on Nov 8, 2009
 *
 */
package com.wolfram.alpha.impl;

import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryParameters;

import java.io.Serializable;
import java.util.List;


public class WAQueryImpl extends WAQueryParametersImpl implements WAQuery, Serializable {

    
    private static final long serialVersionUID = -1282976731786573517L;


    public WAQueryImpl(WAQueryParameters params) {
        super(params);
    }
    
    
    public WAQuery copy() {
        return new WAQueryImpl(this);
    }

    
    // Creates the URL representation of this query, not including server, path, and appid param. Result starts with &.
    public String toString() {
        
        StringBuffer s = new StringBuffer(600);

        List params = getParameters();
        Object[] paramsArr = params.toArray();
        for (int i = 0; i < paramsArr.length; i++) {
            String[] param = (String[]) paramsArr[i];
            s.append("&");
            s.append(param[0]);
            s.append("=");
            s.append(param[1]);
        }
        
        if (signature != null) {
            s.append("&sig=");
            s.append(signature);
        }
        
        return s.toString();        
    }

}
