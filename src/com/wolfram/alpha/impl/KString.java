package com.wolfram.alpha.impl;

import java.util.Vector;

public class KString {

    public static final String[] split(final String data, final char splitChar) {
        return split(data, splitChar, true);
    }

    public static final String[] split(final String data, final char splitChar, final boolean allowEmpty) {
        Vector v = new Vector();

        int indexStart = 0;
        int indexEnd = data.indexOf(splitChar);
        if (indexEnd != -1) {
            while (indexEnd != -1) {
                String s = data.substring(indexStart, indexEnd);
                if (allowEmpty || s.length() > 0) {
                    v.addElement(s);
                }
                indexStart = indexEnd + 1;
                indexEnd = data.indexOf(splitChar, indexStart);
            }

            if (indexStart != data.length()) {
                // Add the rest of the string
                String s = data.substring(indexStart);
                if (allowEmpty || s.length() > 0) {
                    v.addElement(s);
                }
            }
        } else {
            if (allowEmpty || data.length() > 0) {
                v.addElement(data);
            }
        }

        String[] result = new String[v.size()];
        v.copyInto(result);
        return result;
    }
}
