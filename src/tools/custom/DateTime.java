/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.custom;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
/**
 *
 * @author Menno
 */
public class DateTime {
   
    public static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
}
