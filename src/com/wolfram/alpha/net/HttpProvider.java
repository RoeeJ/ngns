/*
 * Created on Dec 5, 2009
 *
 */
package com.wolfram.alpha.net;


import com.wolfram.alpha.net.impl.HttpTransaction;

import java.net.URL;


// IF thids never gets anything more than createTransaction(), it should probably be a class, HttpTransactionFactory.

public interface HttpProvider {

    HttpTransaction createHttpTransaction(URL url);
    
    // TODO: Don't like this. If style is to create one provider and use it always, then having a setter
    // can change state of all uses of this provider in other threads. Better to have a factory that
    // creates providers with certain params (like useragent), and these are not singletons. Thus if you
    // want to change useragent in a session, or have multiple different types of transactions, you just
    // create different providers. MAYBE this is OK, Have setters for all params here.
    void setUserAgent(String agent);

}
