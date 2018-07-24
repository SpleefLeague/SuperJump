/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.spleefleague.parkour.variable;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author NickM13
 */
public class SimpleDate {
    
    public int year, month, day;
    private int intDate;
    static private TimeZone timeZone = TimeZone.getTimeZone("PST");

    public SimpleDate() {
        Calendar cal = Calendar.getInstance(timeZone);
        this.year = cal.get(Calendar.YEAR);
        this.month = cal.get(Calendar.MONTH);
        this.day = cal.get(Calendar.DATE);
        this.intDate = cal.get(Calendar.YEAR) * 365 + cal.get(Calendar.DAY_OF_YEAR);
    }

    public SimpleDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance(timeZone);
        this.year = year;
        this.month = month;
        this.day = day;
        cal.set(year, month, day);
        this.intDate = cal.get(Calendar.YEAR) * 365 + cal.get(Calendar.DAY_OF_YEAR);
    }

    public boolean equals(int year, int month, int day) {
        return (year == this.year
                && month == this.month
                && day == this.day);
    }

    public boolean equals(SimpleDate date) {
        return (date.intDate == this.intDate);
    }

    public boolean isToday() {
        Calendar cal = Calendar.getInstance(timeZone);
        return (cal.get(Calendar.YEAR) == this.year
                && cal.get(Calendar.MONTH) == this.month
                && cal.get(Calendar.DATE) == this.day);
    }
    
    public int getSecondsLeft() {
        return (59 - Calendar.getInstance().get(Calendar.SECOND));
    }
    
    public int getMinutesLeft() {
        return (59 - Calendar.getInstance().get(Calendar.MINUTE));
    }
    
    public int getHoursLeft() {
        return (23 - Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
    }
    
    public int asInt() {
        return intDate;
    }
    
    static public TimeZone getTimeZone() {
        return timeZone;
    }
    
    static public long getNextDayMillis() {
        Calendar cal = Calendar.getInstance(timeZone);
        return(TimeUnit.DAYS.toMillis(1) 
                - (TimeUnit.HOURS.toMillis(cal.get(Calendar.HOUR_OF_DAY))
                + TimeUnit.MINUTES.toMillis(cal.get(Calendar.MINUTE))
                + TimeUnit.SECONDS.toMillis(cal.get(Calendar.SECOND))));
    }
}
