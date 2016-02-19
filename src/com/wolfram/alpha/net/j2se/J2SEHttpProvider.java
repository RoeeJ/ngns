/*
 * Created on Dec 5, 2009
 *
 */
package com.wolfram.alpha.net.j2se;

import com.wolfram.alpha.net.HttpProvider;
import com.wolfram.alpha.net.impl.HttpTransaction;

import java.net.URL;



public class J2SEHttpProvider implements HttpProvider {

    
    private String userAgent = "Wolfram|Alpha Java Binding 1.1";


    public HttpTransaction createHttpTransaction(URL url) {
        return new J2SEHttpTransaction(url, userAgent);
    }


    public void setUserAgent(String agent) {
        this.userAgent = agent;
    }

}
