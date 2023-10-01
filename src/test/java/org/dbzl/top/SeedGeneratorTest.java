package org.dbzl.top;

public class SeedGeneratorTest {


    private final String teams = """
            Alternative Z Fighters
            Happy Tuesday!
            Spice Boys
            Kung Fu Generation
            Family Tree
            Mean Green Demon Kings
            Chad Blades
            The Old and Alone
            WDFA
            Drunken Sailors
            HP Goes Boom
            Demonology
            Balanced Breakfast
            Pepeg
            Bad Boys
            Public Enemy
            """;


    // format: team 1,team 2, team that won, win margin (health bars)
    private final String matches = """
            1,Public Enemy,Kung Fu Generation,1,3
            1,Balanced Breakfast,Demonology,1,4
            1,HP Goes Boom,Spice Boys,1,1
            1,WDFA,The Old and Alone,1,5
            1,Alternative Z Fighters,Bad Boys,2,2
            1,Mean Green Demon Kings,Drunken Sailors,1,2
            1,Family Tree,Happy Tuesday!,2,6
            1,Pepeg,Chad Blades,2,5
            2,Kung Fu Generation,Drunken Sailors,2,1
            2,Pepeg,The Old and Alone,1,2
            2,Family Tree,Spice Boys,1,6
            2,Alternative Z Fighters,Demonology,2,5
            2,WDFA,Chad Blades,1,3
            2,HP Goes Boom,Happy Tuesday!,1,1
            2,Balanced Breakfast,Bad Boys,2,8
            2,Public Enemy,Mean Green Demon Kings,1,2
            3,Chad Blades,Drunken Sailors,2,4
            3,Happy Tuesday!,Demonology,2,6
            3,Mean Green Demon Kings,Pepeg,2,1
            3,Balanced Breakfast,Family Tree,1,4
            3,Spice Boys,Alternative Z Fighters,1,1
            3,The Old and Alone,Kung Fu Generation,2,8
            3,HP Goes Boom,Bad Boys,2,3
            3,Public Enemy, WDFA,1,6
            4,Happy Tuesday!,Spice Boys,2,4
            4,Kung Fu Generation,Family Tree,2,5
            4,Mean Green Demon Kings,Chad Blades,1,2
            4,The Old and Alone,Alternative Z Fighters,1,4
            4,WDFA,Drunken Sailors,1,1
            4,HP Goes Boom,Demonology,2,7
            4,Balanced Breakfast,Pepeg,1,9
            4,Bad Boys,Public Enemy,1,1
            """;
}
