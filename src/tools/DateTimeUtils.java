package tools;

import java.util.Date;

/**
 * Created by cipher on 3/10/16.
 */
public class DateTimeUtils {
    private static DateTimeUtils ourInstance = new DateTimeUtils();

    public static DateTimeUtils getInstance() {
        return ourInstance;
    }

    private DateTimeUtils() {
    }
    public Pair<Long, String> getTimeDifference(Date date1, Date date2) {

        //milliseconds
        long different = date1.getTime() - date2.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;
        StringBuilder sb = new StringBuilder();
        if(elapsedDays > 0) sb.append(elapsedDays).append(" days").append(elapsedHours > 0 ? ",":"").append(" ");
        if(elapsedHours > 0) sb.append(elapsedHours).append(" hours, ").append(elapsedMinutes > 0 ? ",":"").append(" ");
        if(elapsedMinutes > 0) sb.append(elapsedMinutes).append(" minutes, ").append(elapsedMinutes > 0 ? ",":"").append(" ");
        if(elapsedSeconds > 0) sb.append(elapsedSeconds).append(" seconds");
        return new Pair<>(different,sb.toString());
    }
}
