package org.dbzl.writer;

import org.dbzl.domain.Match;
import org.dbzl.domain.Team;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

public class MarkdownWriter {


    public void printScheduleByWeek(Map<Integer, List<Match>> seasonSchedule, String filePath){
        System.out.println("Writing schedule to plain text file.");
        StringBuilder builder = new StringBuilder();

        seasonSchedule.forEach((week, matches) -> {
            builder.append("### Week ").append(week).append('\n');
            builder.append(getScheduleWeekHeader());
            matches.forEach(match -> builder.append(getScheduleMarkdown(match, matches.indexOf(match)+1)));
            builder.append('\n');

        });
        try{
            Files.write(Paths.get(filePath), builder.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND, StandardOpenOption.CREATE);

        } catch(Exception e){
            System.out.println("unable to write to file: " + e.getLocalizedMessage());
        }
    }

    public void printScheduleByTeam(List<Team> allTeams, String filePath){
        System.out.println("Printing team schedule for site.");
        StringBuilder builder = new StringBuilder();

        allTeams.forEach(team -> {
            builder.append("### ").append(team.getName()).append("\n\n");
            builder.append(getScheduleHeader());
            builder.append(getScheduleMarkdown(team));
            builder.append('\n');
        });

        try{
            Files.write(Paths.get(filePath), builder.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND, StandardOpenOption.CREATE);

        } catch(Exception e){
            System.out.println("unable to write to file: " + e.getLocalizedMessage());
        }
    }

    public String getScheduleHeader(){
        return """
                |Match          |  Home Team            | Away Team        | 
                | :-------------| :---------------------| :----------------| 
                """;
    }

    public String getScheduleWeekHeader(){
        return """
                |Match          |  Home Team            | Away Team        | Winner           | 
                | :-------------| :---------------------| :----------------| :----------------| 
                """;
    }


    public String getScheduleMarkdown(Team team){
        StringBuilder builder = new StringBuilder(200);
        for (Match match : team.getSchedule()) {
            builder.append("|").append(match.getWeek());
            if(match.isDivisionalMatch()){
                builder.append(" (div)");
            }
            builder.append("| ").append(match.getHomeTeam().getName()).append(" | ").append(match.getAwayTeam().getName()).append(" |").append('\n');
        }
        builder.append("Home game count: ").append(team.getHomeGamesCount()).append('\n');
        builder.append('\n');
        return builder.toString();
    }


    public String getScheduleMarkdown(Match match, int week){
        StringBuilder builder = new StringBuilder(200);
        builder.append("|").append(week);
        if(match.isDivisionalMatch()){
            builder.append(" (div)");
        }
        builder.append("| ").append(match.getHomeTeam().getName()).append(" | ").append(match.getAwayTeam().getName()).append(" |").append(" |").append('\n');

        return builder.toString();
    }
}
