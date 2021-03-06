package org.dbzl.schedule;

import java.util.Arrays;

public enum Division {
    NORTH_KAI("NK", 3, 7, 11),
    EAST_KAI("EK", 4, 8, 12),
    WEST_KAI("WK", 5, 9, 13),
    SOUTH_KAI("SK", 6, 10, 14);

    private final String abbreviatedName;
    private final int [] divisionalWeeks;

    Division(String name, int ...  divisionalWeeks){
        this.abbreviatedName = name;
        this.divisionalWeeks = divisionalWeeks;
    }

    public int calculateDivisionalWeek(int divisionsAlreadyPlayed){
        return divisionalWeeks[divisionsAlreadyPlayed];
    }

    public boolean isDivisionalWeek(int week){
        return Arrays.stream(divisionalWeeks).anyMatch(Integer.valueOf(week)::equals);
    }

    public static Division isDivisionalWeekFor(int week){
        for(Division kai : Division.values()){
            if(Arrays.stream(kai.getDivisionalWeeks()).anyMatch(Integer.valueOf(week)::equals)){
                return kai;
            }
        }
        return null;
    }

    public int[] getDivisionalWeeks(){
        return divisionalWeeks;
    }

    public String getAbbreviatedName() {
        return abbreviatedName;
    }
}
