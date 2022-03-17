package org.dbzl.writer;


import org.dbzl.domain.Match;
import org.dbzl.domain.Team;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

/**
 * Writes out schedule to text file.
 */
public class TextWriter {

    public void writeToFile(Map<Integer, List<Match>> seasonSchedule, List<Team> allTeams, String filePath){
    System.out.println("Writing schedule to plain text file.");
        StringBuilder builder = new StringBuilder();
        allTeams.forEach(team -> builder.append(team.printSchedule()));

        seasonSchedule.forEach((week, matches) -> {
            builder.append("Week ").append(week).append('\n');
            matches.forEach(match -> builder.append(match.getMatchDescription()).append(", "));
            builder.append('\n');
        });
        try{
            Files.writeString(Paths.get(filePath), builder.toString(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);

        } catch(Exception e){
            System.out.println("unable to write to file: " + e.getLocalizedMessage());
        }
    }
}
