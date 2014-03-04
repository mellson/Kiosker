package dk.itu.kiosker.utils;

import java.util.Calendar;
import java.util.Date;

public class Time {
    private int hours;
    private int minutes;

    public Time(Object time) {
        String timeString = time.toString();
        String[] splitTimeString = timeString.trim().replace(";", ".").replace(":", ".").replace(",", ".").split("\\.");
        if (splitTimeString.length > 0) {
            hours = Integer.parseInt(splitTimeString[0]);
            hours = hours < 0 ? 0 : hours;
            hours = hours > 23 ? 23 : hours;
        } else
            hours = 0;
        if (splitTimeString.length > 1) {
            minutes = Integer.parseInt(splitTimeString[1]);
            minutes = minutes < 0 ? 0 : minutes;
            minutes = minutes > 59 ? 59 : minutes;
        } else
            minutes = 0;
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    @Override
    public String toString() {
        String hourString = hours < 10 ? "0" + hours : "" + hours;
        String minutesString = minutes < 10 ? "0" + minutes : "" + minutes;
        return hourString + "." + minutesString;
    }

    public int secondsUntil() {
        return secondsUntil(hours, minutes);
    }

    public static int secondsUntil(int hours, int minutes) {
        Date now = new Date();
        Calendar cal = getDateFromTime(hours, minutes);
        Long milliseconds = cal.getTimeInMillis() - now.getTime();
        int res = milliseconds.intValue() / 1000;
        int numberOfSecondsInDay = 24 * 60 * 60;
        if (res < 0)
            return numberOfSecondsInDay + res;
        else
            return res;
    }

    private static Calendar getDateFromTime(int hours, int minutes) {
        Date now = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, hours);
        cal.set(Calendar.MINUTE, minutes);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    public static Boolean isNowBetweenTheseTimes(Time t1, Time t2) {
        Date now = new Date();
        Calendar cal1 = getDateFromTime(t1.hours, t1.minutes);
        Calendar cal2 = getDateFromTime(t2.hours, t2.minutes);
        Boolean betweenTimes = cal1.getTimeInMillis() <= now.getTime();
        betweenTimes &= cal2.getTimeInMillis() >= now.getTime();
        return betweenTimes;
    }
}
