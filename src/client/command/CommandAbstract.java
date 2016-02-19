/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client.command;

/**
 *
 * @author Menno
 */
public abstract class CommandAbstract {
    
    protected static String joinStringFrom(String arr[], int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            builder.append(arr[i]);
            if (i != arr.length - 1) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }
    
    protected static int getNoticeType(String typestring) {
        
        switch (typestring) {
            case "n":
                return 0;
            case "p":
                return 1;
            case "l":
                return 2;
            case "nv":
                return 5;
            case "v":
                return 5;
            case "b":
                return 6;
        }
        
        return -1;
    }
}
