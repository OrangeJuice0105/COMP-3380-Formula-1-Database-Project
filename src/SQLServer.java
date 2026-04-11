import java.sql.SQLException;
import java.util.Scanner;

public class SQLServer {

    // Connect to your database.
    // Replace server name, username, and password with your credentials
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Enter password: ");
            String password = scanner.nextLine().trim();

            try (F1Database db = new F1Database(username, password)) {
                boolean exit = false;

                System.out.println("\nEnter an option to run a query.");
                System.out.println("Enter 'h' for help.");
                System.out.println("Enter 'q' to quit.");

                do {
                    System.out.print("\nOption: ");
                    String option = scanner.nextLine().trim();

                    if (option.equalsIgnoreCase("q")) {
                        System.out.println("Program terminated.");
                        exit = true;
                    }  else if (option.equalsIgnoreCase("h")) {
                        printHelp();
                    } else {
                        switch (option) {
                            case "1" -> db.driversPerformance(scanner);
                            case "2" -> db.constructorsWithMostDNF(scanner);
                            case "3" -> db.hatTrackList();
                            case "4" -> db.grandSlime();
                            case "5" -> db.lapDegradation();
                            case "6" -> db.mostChampionshipPointsInASeason();
                            case "7" -> db.mostConsecutiveDriverWins();
                            case "8" -> db.driversWithMostDNF();
                            case "9" -> db.mostWinsButNotChampionsSeasons();
                            case "10" -> {
                                System.out.print("Enter constructor name: ");
                                String constructorName = scanner.nextLine().trim();
                                db.racesForSingleConstructorThatNoDriversAreClassified(constructorName);
                            }
                            case "11" -> db.racesWhereAllDriversAreClassified();
                            case "12" -> db.winsWithoutPole();
                            case "13" -> db.worstPoleToWinRatio();
                            case "14" -> db.fastestAveragePitStopTeamBySeason(scanner);
                            case "15" -> db.constructorTeammateHeadToHead(scanner);
                            case "16" -> db.listDrivers(scanner);
                            case "17" -> db.listConstructors(scanner);
                            case "18" -> db.listCircuits(scanner);
                            case "19" -> {
                                System.out.print("Are you sure you want to delete all data? (y/N): ");
                                String deleteConfirm = scanner.nextLine().trim();
                                if (deleteConfirm.equalsIgnoreCase("y")) {
                                    db.deleteAllData();
                                } else {
                                    System.out.println("Delete cancelled.");
                                }
                            }
                            case "20" -> {
                                System.out.print("Are you sure you want to repopulate the database? (y/N): ");
                                String loadConfirm = scanner.nextLine().trim();
                                if (loadConfirm.equalsIgnoreCase("y")) {
                                    db.repopulateDatabase();
                                } else {
                                    System.out.println("Repopulate cancelled.");
                                }
                            }
                            default -> System.out.println("Invalid option. Enter 'h' for help.");
                        }
                    }
                } while (!exit);
            } catch (SQLException e) {
                System.err.println("Failed to connect to database. Exiting...");
            }
        }
    }
    

    public static void printHelp() {
        System.out.println("\nAvailable commands:");
        System.out.println(" h   -> show help");
        System.out.println(" q   -> quit");

        System.out.println("\nQuery commands:");
        System.out.println(" 1   -> Driver performance by season");
        System.out.println(" 2   -> Constructors with most DNFs by season");
        System.out.println(" 3   -> Hat-trick list");
        System.out.println(" 4   -> Grand Slam list");
        System.out.println(" 5   -> Lap degradation");
        System.out.println(" 6   -> Most championship points in a season");
        System.out.println(" 7   -> Most consecutive driver wins");
        System.out.println(" 8   -> Drivers with most DNFs per season");
        System.out.println(" 9   -> Most wins but not champions");
        System.out.println(" 10  -> Races for a single constructor where no drivers are classified");
        System.out.println(" 11  -> Races where all drivers are classified");
        System.out.println(" 12  -> Wins without pole");
        System.out.println(" 13  -> Worst pole-to-win ratio");
        System.out.println(" 14  -> Fastest average pit stop team by season");
        System.out.println(" 15  -> Constructor teammate head-to-head");
        System.out.println(" 16  -> List drivers");
        System.out.println(" 17  -> List constructors");
        System.out.println(" 18  -> List circuits");
        System.out.println(" 19  -> Delete all data");
        System.out.println(" 20  -> Repopulate database");
        System.out.println();
    }
}


