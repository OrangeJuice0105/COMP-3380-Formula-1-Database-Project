import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class F1Database implements AutoCloseable {

    private static final int WINDOW_SIZE = 10;

    private Connection connection;

    private static final String SOURCE_DIR = "sql/";

    private static final String[] SQL_FILES = {
        "results.sql",
        "status.sql",
        "circuits.sql",
        "constructors.sql",
        "drivers.sql",
        "seasons.sql",
        "races.sql",
        "lap_times.sql",
        "pit_stops.sql",
        "qualifying.sql",
        "driver_standings.sql",
        "constructor_standings.sql",
        "constructor_results.sql",
        "sprint_results.sql"
    };

    public F1Database(String username, String password) throws SQLException {
        String connectionUrl = 
                "jdbc:sqlserver://uranium.cs.umanitoba.ca:1433;"
            + "database=cs338019;"
            + "user=" + username +";"
            + "password="+ password +";"
            + "encrypt=false;"
            + "trustServerCertificate=false;"
            + "loginTimeout=30;"; 
        connection = DriverManager.getConnection(connectionUrl);
    }

    public void driversPerformance(Scanner scanner) {
        List<Integer> years = new ArrayList<>();

        String yearsSql = """
                SELECT DISTINCT r.year
                FROM races r
                ORDER BY r.year;
                """;

        String performanceSql = """
                SELECT
                    r.year,
                    d.driverId,
                    d.forename,
                    d.surname,
                    COUNT(*) AS races,
                    SUM(CASE WHEN res.positionOrder = 1 THEN 1 ELSE 0 END) AS wins,
                    SUM(CASE WHEN res.positionOrder <= 3 THEN 1 ELSE 0 END) AS podiums,
                    SUM(res.points) AS total_points,
                    ROUND(AVG(CAST(res.positionOrder AS FLOAT)), 2) AS avg_finish
                FROM results res
                JOIN drivers d ON d.driverId = res.driverId
                JOIN races r ON r.raceId = res.raceId
                WHERE r.year = ?
                GROUP BY r.year, d.driverId, d.forename, d.surname
                ORDER BY total_points DESC, wins DESC, podiums DESC, d.driverId;
                """;

        try (PreparedStatement yearsStmt = connection.prepareStatement(yearsSql);
            ResultSet yearsRs = yearsStmt.executeQuery()) {

            while (yearsRs.next()) {
                years.add(yearsRs.getInt("year"));
            }

            if (years.isEmpty()) {
                System.out.println("No seasons found.");
            } else {
                int index = 0;
                boolean exit = false;

                while (!exit) {
                    int selectedYear = years.get(index);

                    try (PreparedStatement performanceStmt = connection.prepareStatement(performanceSql)) {
                        performanceStmt.setInt(1, selectedYear);

                        try (ResultSet resultSet = performanceStmt.executeQuery()) {
                            System.out.println("\nMost Successful Drivers By Season");
                            System.out.println("Year: " + selectedYear);

                            System.out.printf(
                                    "%-10s %-12s %-18s %-8s %-6s %-8s %-14s %-10s%n",
                                    "driverId", "forename", "surname",
                                    "races", "wins", "podiums", "total_points", "avg_finish"
                            );
                            System.out.println("----------------------------------------------------------------------------------------------");

                            boolean hasRows = false;

                            while (resultSet.next()) {
                                hasRows = true;
                                System.out.printf(
                                        "%-10d %-12s %-18s %-8d %-6d %-8d %-14.1f %-10.2f%n",
                                        resultSet.getInt("driverId"),
                                        resultSet.getString("forename"),
                                        resultSet.getString("surname"),
                                        resultSet.getInt("races"),
                                        resultSet.getInt("wins"),
                                        resultSet.getInt("podiums"),
                                        resultSet.getDouble("total_points"),
                                        resultSet.getDouble("avg_finish")
                                );
                            }

                            if (!hasRows) {
                                System.out.println("No driver performance data for this year.");
                            }
                        }
                    }

                    System.out.print("\n[p] previous year | [n] next year | [q] quit | [year] go to year: ");
                    String input = scanner.nextLine().trim().toLowerCase();

                    switch (input) {
                        case "n" -> {
                            if (index < years.size() - 1) {
                                index++;
                            } else {
                                System.out.println("Already at the latest year.");
                            }
                        }
                        case "p" -> {
                            if (index > 0) {
                                index--;
                            } else {
                                System.out.println("Already at the earliest year.");
                            }
                        }
                        case "q" -> exit = true;
                        default -> {
                            try {
                                int requestedYear = Integer.parseInt(input);
                                int yearIndex = years.indexOf(requestedYear);

                                if (yearIndex >= 0) {
                                    index = yearIndex;
                                } else {
                                    System.out.printf("Year %d not found.\n", requestedYear);
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid command.");
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    public void constructorsWithMostDNF(Scanner scanner){
        String sql = """
                        with setRetirement as (
                            select
                                r.year,
                                c.constructorID,
                                c.name,
                                sum(case when rs.positionText = 'R' then 1 else 0 end) as countRetirement
                            from results rs 
                            join races r ON rs.raceId = r.raceId
                            join constructors c on c.constructorId = rs.constructorId
                            group by r.year, c.constructorID, c.name
                        )
                        
                        select  sr.year,
                                sr.constructorId,
                                sr.name, 
                                sr.countRetirement  
                        from setRetirement sr 
                            where(sr.countRetirement = (select max(r2.countRetirement) 
                                                    from setRetirement r2
                                                    where r2.year = sr.year))
                            order by sr.year, sr.countRetirement desc, sr.name desc
                    """;
        try( PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            System.out.println("Constructors With Most DNFs By Season");

            System.out.printf("%-25s %-6s %-10s %-10s\n",
                    "name", "year", "id", "count");
            System.out.println("------------------------------------------------------");
            while (resultSet.next()) {
                System.out.printf("%-25s %-6d %-10d %-10d\n",
                    resultSet.getString("name"),
                    resultSet.getInt("year"),
                    resultSet.getInt("constructorID"),
                    resultSet.getInt("countRetirement")
                );
            }
        }
        catch(SQLException e){
            e.printStackTrace(System.out);
        }
    }

    public void hatTrickList(){
        try {
            String sql ="""
                            WITH race_winners AS (
                                    SELECT
                                        raceId,
                                        driverId
                                    FROM results
                                    WHERE positionOrder = 1
                                ),
                                pole_positions AS (
                                    SELECT
                                        raceId,
                                        driverId
                                    FROM qualifying
                                    WHERE position = 1
                                ),
                                fastest_laps AS (
                                    SELECT
                                        raceId,
                                        driverId
                                    FROM results
                                    WHERE rank = 1   -- fastest lap
                                ),
                                lap_leaders AS (
                                    SELECT
                                        raceId,
                                        driverId,
                                        COUNT(*) AS laps_led
                                    FROM lap_times
                                    GROUP BY raceId, driverId
                                ),
                                race_total_laps AS (
                                    SELECT
                                        raceId,
                                        MAX(lap) AS total_laps
                                    FROM lap_times
                                    GROUP BY raceId
                                ),
                                all_laps_led AS (
                                    SELECT
                                        l.raceId,
                                        l.driverId
                                    FROM lap_leaders l
                                    JOIN race_total_laps t
                                        ON l.raceId = t.raceId
                                    WHERE l.laps_led = t.total_laps
                                )
                                SELECT
                                    r.year,
                                    r.name AS race_name,
                                    CONCAT(d.forename,' ',d.surname) AS driver_name
                                FROM race_winners w
                                JOIN pole_positions p
                                    ON w.raceId = p.raceId AND w.driverId = p.driverId
                                JOIN fastest_laps f
                                    ON w.raceId = f.raceId AND w.driverId = f.driverId
                                JOIN all_laps_led a
                                    ON w.raceId = a.raceId AND w.driverId = a.driverId
                                JOIN races r
                                    ON w.raceId = r.raceId
                                JOIN drivers d
                                    ON w.driverId = d.driverId
                                ORDER BY r.year, r.round;
                        """;
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            System.out.println("Drivers who achieved pole position, fastest lap and won the race");

            System.out.printf("%-6s %-35s %-25s\n", 
                    "year", "race_name", "driver_name");
            System.out.println("------------------------------------------------------");
            while (resultSet.next()) {
                System.out.printf("%-6d %-35s %-25s\n",
                    resultSet.getInt("year"),
                    resultSet.getString("race_name"),
                    resultSet.getString("driver_name")
                );
            }
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
    }

    public void grandSlam(){
        try {
            String sql ="""
                           WITH race_winners AS (
                                    SELECT
                                        raceId,
                                        driverId
                                    FROM results
                                    WHERE positionOrder = 1
                                ),
                                pole_positions AS (
                                    SELECT
                                        raceId,
                                        driverId
                                    FROM qualifying
                                    WHERE position = 1
                                ),
                                fastest_laps AS (
                                    SELECT
                                        raceId,
                                        driverId
                                    FROM results
                                    WHERE rank = 1   -- fastest lap
                                ),
                                lap_leaders AS (
                                    SELECT
                                        raceId,
                                        driverId,
                                        COUNT(*) AS laps_led
                                    FROM lap_times
                                    WHERE position = 1
                                    GROUP BY raceId, driverId
                                ),
                                race_total_laps AS (
                                    SELECT
                                        raceId,
                                        MAX(lap) AS total_laps
                                    FROM lap_times
                                    GROUP BY raceId
                                ),
                                all_laps_led AS (
                                    SELECT
                                        l.raceId,
                                        l.driverId
                                    FROM lap_leaders l
                                    JOIN race_total_laps t
                                        ON l.raceId = t.raceId
                                    WHERE l.laps_led = t.total_laps
                                )
                                SELECT
                                    r.year,
                                    r.name AS race_name,
                                    CONCAT(d.forename,' ',d.surname) AS driver_name
                                FROM race_winners w
                                JOIN pole_positions p
                                    ON w.raceId = p.raceId AND w.driverId = p.driverId
                                JOIN fastest_laps f
                                    ON w.raceId = f.raceId AND w.driverId = f.driverId
                                JOIN all_laps_led a
                                    ON w.raceId = a.raceId AND w.driverId = a.driverId
                                JOIN races r
                                    ON w.raceId = r.raceId
                                JOIN drivers d
                                    ON w.driverId = d.driverId
                                ORDER BY r.year, r.round;
                        """;
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
            System.out.println("Grand Slam");
            
          
            System.out.printf("%-6s %-35s %-25s\n",
                    "year", "race_name", "driver_name");
            System.out.println("------------------------------------------------------");
            while (resultSet.next()) {
                System.out.printf("%-6d %-35s %-25s\n",
                    resultSet.getInt("year"),
                    resultSet.getString("race_name"),
                    resultSet.getString("driver_name")
                );
            }
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
    }
    
    public void lapDegradation(Scanner scanner) {
        List<Integer> years = new ArrayList<>();

        String yearsSql = """
                SELECT DISTINCT year
                FROM races
                ORDER BY year;
                """;

        String baseCte = """
                WITH ordered_stops AS (
                    SELECT
                        raceId,
                        driverId,
                        stop,
                        lap AS stop_lap,
                        LEAD(lap) OVER (
                            PARTITION BY raceId, driverId
                            ORDER BY lap
                        ) AS next_stop_lap
                    FROM pit_stops
                ),
                stints AS (
                    SELECT
                        raceId,
                        driverId,
                        stop,
                        stop_lap + 1 AS stint_start_lap,
                        COALESCE(next_stop_lap - 1, 9999) AS stint_end_lap
                    FROM ordered_stops
                ),
                stint_laps AS (
                    SELECT
                        s.raceId,
                        s.driverId,
                        s.stop,
                        MIN(lt.lap) AS first_lap,
                        MAX(lt.lap) AS last_lap
                    FROM stints s
                    JOIN lap_times lt
                        ON lt.raceId = s.raceId
                        AND lt.driverId = s.driverId
                        AND lt.lap BETWEEN s.stint_start_lap AND s.stint_end_lap
                    GROUP BY s.raceId, s.driverId, s.stop
                ),
                stint_times AS (
                    SELECT
                        sl.raceId,
                        sl.driverId,
                        sl.stop,
                        first_lt.milliseconds AS first_lap_ms,
                        last_lt.milliseconds AS last_lap_ms,
                        last_lt.milliseconds - first_lt.milliseconds AS degradation_ms
                    FROM stint_laps sl
                    JOIN lap_times first_lt
                        ON first_lt.raceId = sl.raceId
                        AND first_lt.driverId = sl.driverId
                        AND first_lt.lap = sl.first_lap
                    JOIN lap_times last_lt
                        ON last_lt.raceId = sl.raceId
                        AND last_lt.driverId = sl.driverId
                        AND last_lt.lap = sl.last_lap
                )
                """;

        String allRacesSql = baseCte + """
                SELECT
                    r.year,
                    r.name,
                    d.forename,
                    d.surname,
                    ROUND(AVG(st.degradation_ms * 1.0), 2) AS avg_stint_degradation_ms
                FROM stint_times st
                JOIN races r ON st.raceId = r.raceId
                JOIN drivers d ON d.driverId = st.driverId
                WHERE r.year = ?
                GROUP BY r.year, r.name, d.forename, d.surname
                ORDER BY r.name, avg_stint_degradation_ms DESC, d.surname, d.forename;
                """;

        String specificRaceSql = baseCte + """
                SELECT
                    r.year,
                    r.name,
                    d.forename,
                    d.surname,
                    ROUND(AVG(st.degradation_ms * 1.0), 2) AS avg_stint_degradation_ms
                FROM stint_times st
                JOIN races r ON st.raceId = r.raceId
                JOIN drivers d ON d.driverId = st.driverId
                WHERE r.year = ?
                AND r.name = ?
                GROUP BY r.year, r.name, d.forename, d.surname
                ORDER BY avg_stint_degradation_ms DESC, d.surname, d.forename;
                """;

        try (PreparedStatement yearsStmt = connection.prepareStatement(yearsSql);
            ResultSet yearsRs = yearsStmt.executeQuery()) {

            while (yearsRs.next()) {
                years.add(yearsRs.getInt("year"));
            }

            if (years.isEmpty()) {
                System.out.println("No seasons found.");
            } else {
                int index = 0;
                boolean exit = false;
                boolean showAllRaces = true;
                String selectedRace = null;

                while (!exit) {
                    int selectedYear = years.get(index);
                    String sqlToUse = showAllRaces ? allRacesSql : specificRaceSql;

                    try (PreparedStatement statement = connection.prepareStatement(sqlToUse)) {
                        statement.setInt(1, selectedYear);
                        if (!showAllRaces) {
                            statement.setString(2, selectedRace);
                        }

                        try (ResultSet resultSet = statement.executeQuery()) {
                            System.out.println("\nLap Time Degradation Between Pit Stops");
                            System.out.println("Year: " + selectedYear);
                            System.out.println("View: " + (showAllRaces ? "All races" : selectedRace));

                            System.out.printf("%-6s %-30s %-15s %-15s %-20s%n",
                                    "year", "race_name", "forename", "surname", "avg_degradation(ms)");
                            System.out.println("---------------------------------------------------------------------------------------------");

                            boolean hasRows = false;

                            while (resultSet.next()) {
                                hasRows = true;
                                System.out.printf("%-6d %-30s %-15s %-15s %-20.2f%n",
                                        resultSet.getInt("year"),
                                        resultSet.getString("name"),
                                        resultSet.getString("forename"),
                                        resultSet.getString("surname"),
                                        resultSet.getDouble("avg_stint_degradation_ms")
                                );
                            }

                            if (!hasRows) {
                                System.out.println("No lap degradation data found.");
                            }
                        }
                    }

                    System.out.println("\nCommands:");
                    System.out.println("  n  -> next year");
                    System.out.println("  p  -> previous year");
                    System.out.println("  a  -> view all races in this year");
                    System.out.println("  r  -> choose a specific race in this year");
                    System.out.println("  q  -> quit");
                    System.out.print("Or enter a year directly: ");

                    String input = scanner.nextLine().trim();

                    switch (input.toLowerCase()) {
                        case "n" -> {
                            if (index < years.size() - 1) {
                                index++;
                            } else {
                                System.out.println("Already at the latest year.");
                            }
                        }
                        case "p" -> {
                            if (index > 0) {
                                index--;
                            } else {
                                System.out.println("Already at the earliest year.");
                            }
                        }
                        case "a" -> {
                            showAllRaces = true;
                            selectedRace = null;
                        }
                        case "r" -> {
                            System.out.print("Enter exact race name: ");
                            String raceInput = scanner.nextLine().trim();
                            if (!raceInput.isEmpty()) {
                                showAllRaces = false;
                                selectedRace = raceInput;
                            } else {
                                System.out.println("Race name cannot be empty.");
                            }
                        }
                        case "q" -> exit = true;
                        default -> {
                            try {
                                int requestedYear = Integer.parseInt(input);
                                int yearIndex = Collections.binarySearch(years, requestedYear);

                                if (yearIndex >= 0) {
                                    index = yearIndex;
                                } else {
                                    System.out.println("Year " + requestedYear + " not found.");
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid command.");
                            }
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    public void mostChampionshipPointsInASeason(){
        try {
            String sql ="""
                WITH final_standings AS (
                    SELECT
                        r.year,
                        ds.driverId,
                        ds.points,
                        ds.position
                    FROM driver_standings ds
                    JOIN races r
                        ON ds.raceId = r.raceId
                    WHERE ds.raceId IN (
                        SELECT MAX(r2.raceId)
                        FROM races r2
                        GROUP BY r2.year
                    )
                )
                SELECT TOP 10
                    f.year,
                    CONCAT(d.forename,' ',d.surname) AS driver_name,
                    f.points
                FROM final_standings f
                JOIN drivers d ON f.driverId = d.driverId
                ORDER BY f.points DESC, f.year DESC;                    
                        """;
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
           
            System.out.println("Top 10 Most Championship Points in a Season");
            

            System.out.printf("%-6s %-25s %-10s\n",
                    "year", "driver_name", "points");
            System.out.println("------------------------------------------------------");
            while (resultSet.next()) {
                System.out.printf("%-6d %-25s %-10d\n",
                    resultSet.getInt("year"),
                    resultSet.getString("driver_name"),
                    resultSet.getInt("points")
                );
            }
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
    }

    public void mostConsecutiveDriverWins(){
        try {
            String sql ="""
                           WITH driver_span AS (
                                SELECT
                                    driverId,
                                    MIN(raceId) AS first_race_id,
                                    MAX(raceId) AS last_race_id
                                FROM results
                                GROUP BY driverId
                            ),
                            driver_race_calendar AS (
                                SELECT
                                    ds.driverId,
                                    r.raceId,
                                    r.year,
                                    r.round
                                FROM driver_span ds
                                JOIN races r
                                ON r.raceId BETWEEN ds.first_race_id AND ds.last_race_id
                            ),
                            driver_race_results AS (
                                SELECT
                                    c.driverId,
                                    c.raceId,
                                    c.year,
                                    c.round,
                                    CASE
                                        WHEN res.positionOrder = 1 THEN 1
                                        ELSE 0
                                    END AS is_win
                                FROM driver_race_calendar c
                                LEFT JOIN results res
                                ON res.driverId = c.driverId
                                AND res.raceId = c.raceId
                            ),
                            win_groups AS (
                                SELECT
                                    driverId,
                                    raceId,
                                    year,
                                    round,
                                    is_win,
                                    SUM(CASE WHEN is_win = 0 THEN 1 ELSE 0 END) OVER (
                                        PARTITION BY driverId
                                        ORDER BY year, round
                                        ROWS UNBOUNDED PRECEDING
                                    ) AS grp
                                FROM driver_race_results
                            ),
                            win_streaks AS (
                                SELECT
                                    driverId,
                                    grp,
                                    COUNT(*) AS consecutive_wins,
                                    MIN(raceId) AS start_race_id,
                                    MAX(raceId) AS end_race_id
                                FROM win_groups
                                WHERE is_win = 1
                                GROUP BY driverId, grp
                            )
                            SELECT TOP 10
                                CONCAT(d.forename,' ',d.surname) AS driver_name,
                                ws.consecutive_wins,
                                rs.year AS start_year,
                                rs.name AS start_race,
                                re.year AS end_year,
                                re.name AS end_race
                            FROM win_streaks ws
                            JOIN drivers d
                            ON d.driverId = ws.driverId
                            JOIN races rs
                            ON rs.raceId = ws.start_race_id
                            JOIN races re
                            ON re.raceId = ws.end_race_id
                            ORDER BY ws.consecutive_wins DESC, end_year DESC, driver_name;     
                        """;
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
           
            System.out.println("Top 10 Longest Consecutive Win Streaks");
            
            System.out.printf("%-25s %-10s %-12s %-26s %-10s %-25s\n",
        "driver_name", "wins", "start_year", "start_race", "end_year", "end_race");
            System.out.println("------------------------------------------------------");
            while (resultSet.next()) {
                System.out.printf("%-25s %-10d %-12d %-26s %-10d %-25s\n",
                    resultSet.getString("driver_name"),
                    resultSet.getInt("consecutive_wins"),
                    resultSet.getInt("start_year"),
                    resultSet.getString("start_race"),
                    resultSet.getInt("end_year"),
                    resultSet.getString("end_race")
                );
}
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
    }

    public void driversWithMostDNF(){
        try {
            String sql ="""
                        WITH dnf_per_season AS (
                            SELECT
                                r.year,
                                res.driverId,
                                COUNT(*) AS dnfs
                            FROM results res
                            JOIN races r ON r.raceId = res.raceId
                            JOIN status s ON s.statusId = res.statusId
                            WHERE NOT (
                                s.status LIKE 'Finished'
                                OR s.status LIKE '+%Lap%'
                            )
                            GROUP BY r.year, res.driverId
                        ),
                        max_dnf_per_season AS (
                            SELECT
                                year,
                                MAX(dnfs) AS max_dnfs
                            FROM dnf_per_season
                            GROUP BY year
                        )
                        SELECT
                            dps.year,
                            CONCAT(d.forename, ' ', d.surname) AS driver_name,
                            dps.dnfs
                        FROM dnf_per_season dps
                        JOIN max_dnf_per_season m
                            ON dps.year = m.year AND dps.dnfs = m.max_dnfs
                        JOIN drivers d
                            ON d.driverId = dps.driverId
                        ORDER BY dps.year;                               
                        """;
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
           
            System.out.println("Drivers With Most DNFs Per Season");
            
            System.out.printf("%-6s %-25s %-10s\n",
                    "year", "driver_name", "dnfs");
            System.out.println("------------------------------------------------------");
            while (resultSet.next()) {
                System.out.printf("%-6d %-25s %-10d\n",
                    resultSet.getInt("year"),
                    resultSet.getString("driver_name"),
                    resultSet.getInt("dnfs")
                );
            }
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
    }

    public void mostWinsButNotChampionsSeasons(){
        try {
            String sql ="""
                        WITH wins_per_season AS (
                            SELECT 
                                r.year,
                                res.driverId,
                                COUNT(*) AS wins
                            FROM results res
                            JOIN races r ON res.raceId = r.raceId
                            WHERE res.position = 1
                            GROUP BY r.year, res.driverId
                        ),
                        max_wins_per_season AS (
                            SELECT 
                                year,
                                MAX(wins) AS max_wins
                            FROM wins_per_season
                            GROUP BY year
                        ),
                        season_champions AS (
                            SELECT DISTINCT
                                r.year,
                                ds.driverId
                            FROM driver_standings ds
                            JOIN races r ON ds.raceId = r.raceId
                            WHERE ds.position = 1
                        )
                        SELECT 
                            w.year,
                            CONCAT(d.forename,' ',d.surname) AS driver_name,
                            w.wins
                        FROM wins_per_season w
                        JOIN max_wins_per_season m
                            ON w.year = m.year AND w.wins = m.max_wins
                        LEFT JOIN season_champions c
                            ON w.year = c.year AND w.driverId = c.driverId
                        JOIN drivers d
                            ON w.driverId = d.driverId
                        WHERE c.driverId IS NULL
                        ORDER BY w.year;                                                   
                        """;
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery();
           
            System.out.println("Drivers With Most Wins But Not Champions (By Season)");

            System.out.printf("%-6s %-25s %-10s\n",
                    "year", "driver_name", "wins");

            System.out.println("------------------------------------------------------");

            while (resultSet.next()) {
                System.out.printf("%-6d %-25s %-10d\n",
                    resultSet.getInt("year"),
                    resultSet.getString("driver_name"),
                    resultSet.getInt("wins")
                );
            }
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
    }

    public void racesForSingleConstructorThatNoDriversAreClassified(String name){
        try {
            String sql = """
                        SELECT
                            r.year,
                            r.name AS race_name
                        FROM results re
                        JOIN races r
                            ON re.raceId = r.raceId
                        JOIN constructors c
                            ON re.constructorId = c.constructorId
                        JOIN status s
                            ON re.statusId = s.statusId
                        WHERE c.name = ?
                        GROUP BY r.raceId, r.year, r.name, c.name
                        HAVING SUM(
                            CASE
                                WHEN s.status = 'Finished'
                                OR s.status LIKE '+% Lap%'
                                THEN 1
                                ELSE 0
                            END
                        ) = 0
                        ORDER BY r.year;                   
                        """;
            PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, name);
			ResultSet resultSet = statement.executeQuery();

            System.out.println("Races Where No Drivers From Constructor Finished");

            System.out.printf("%-6s %-30s\n",
                    "year", "race_name");

            System.out.println("------------------------------------------------------------------");

            while (resultSet.next()) {
                System.out.printf("%-6d %-30s\n",
                    resultSet.getInt("year"),
                    resultSet.getString("race_name")
                );
            }
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
    }

    public void racesWhereAllDriversAreClassified(){
        try {
            String sql = """
                        WITH winner_laps AS (
                            SELECT
                                raceId,
                                laps AS winner_laps
                            FROM results
                            WHERE positionOrder = 1
                        )
                        SELECT
                            r.year,
                            r.name,
                            r.date
                        FROM races r
                        JOIN results re
                            ON r.raceId = re.raceId
                        JOIN winner_laps w
                            ON r.raceId = w.raceId
                        GROUP BY r.raceId, r.year, r.name, r.date, r.round, w.winner_laps
                        HAVING MIN(
                            CASE
                                WHEN re.laps >= 0.9 * w.winner_laps THEN 1
                                ELSE 0
                            END
                        ) = 1
                        ORDER BY r.year, r.round;                                          
                        """;
            PreparedStatement statement = connection.prepareStatement(sql);
			ResultSet resultSet = statement.executeQuery();

            System.out.println("Races Where All Drivers Are Classified");

            // header
            System.out.printf("%-6s %-35s %-12s\n",
                    "year", "race_name", "date");

            // separator
            System.out.println("----------------------------------------------------------------");

            while (resultSet.next()) {
                System.out.printf("%-6d %-35s %-12s\n",
                    resultSet.getInt("year"),
                    resultSet.getString("name"),
                    resultSet.getString("date")
                );
            }           
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
    }

    public void winsWithoutPole(){
        try {
            String sql = """
                        SELECT TOP 10
                            CONCAT(d.forename,' ',d.surname) AS driver_name,
                            COUNT(*) AS wins_without_pole
                        FROM results r
                        JOIN qualifying q
                            ON r.raceId = q.raceId
                        AND r.driverId = q.driverId
                        JOIN drivers d
                            ON r.driverId = d.driverId
                        WHERE r.positionOrder = 1        -- race winner
                        AND q.position <> 1            -- did NOT start from pole
                        GROUP BY d.forename, d.surname, r.driverId
                        HAVING COUNT(*) > 0
                        ORDER BY wins_without_pole DESC, driver_name;                                        
                        """;
            PreparedStatement statement = connection.prepareStatement(sql);
			ResultSet resultSet = statement.executeQuery();

            System.out.println("Top 10 Wins Without Starting From Pole");

            System.out.printf("%-25s %-10s\n",
                    "driver_name", "wins_no_pole");

            System.out.println("---------------------------------------------");

            while (resultSet.next()) {
                System.out.printf("%-25s %-10d\n",
                    resultSet.getString("driver_name"),
                    resultSet.getInt("wins_without_pole")
                );
            }
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
    }

    public void worstPoleToWinRatio(){
        try {
            String sql = """
                        WITH poles AS (
                            SELECT
                                q.driverId,
                                COUNT(*) AS total_poles
                            FROM qualifying q
                            WHERE q.position = 1
                            GROUP BY q.driverId
                        ),
                        wins_from_pole AS (
                            SELECT
                                q.driverId,
                                COUNT(*) AS wins_from_pole
                            FROM qualifying q
                            JOIN results r
                                ON q.raceId = r.raceId
                            AND q.driverId = r.driverId
                            WHERE q.position = 1
                            AND r.positionOrder = 1
                            GROUP BY q.driverId
                        )
                        SELECT TOP 10
                            CONCAT(d.forename,' ',d.surname) AS driver_name,
                            p.total_poles,
                            COALESCE(w.wins_from_pole, 0) AS wins_from_pole,
                            ROUND(
                                COALESCE(w.wins_from_pole, 0) * 1.0 / p.total_poles,
                                3
                            ) AS pole_to_win_ratio
                        FROM poles p
                        LEFT JOIN wins_from_pole w
                            ON p.driverId = w.driverId
                        JOIN drivers d
                            ON d.driverId = p.driverId
                        WHERE p.total_poles >= 5   -- avoid tiny samples
                        ORDER BY pole_to_win_ratio ASC, p.total_poles DESC;                                                               
                        """;
            PreparedStatement statement = connection.prepareStatement(sql);
			ResultSet resultSet = statement.executeQuery();

            System.out.println("Worst Pole-to-Win Conversion Ratio");

            System.out.printf("%-25s %-12s %-15s %-15s\n",
                    "driver_name", "total_poles", "wins_from_pole", "ratio");

            System.out.println("---------------------------------------------------------------------");

            while (resultSet.next()) {
                System.out.printf("%-25s %-12d %-15d %-15.3f\n",
                    resultSet.getString("driver_name"),
                    resultSet.getInt("total_poles"),
                    resultSet.getInt("wins_from_pole"),
                    resultSet.getDouble("pole_to_win_ratio")
                );
            }
        } catch(SQLException e){
            e.printStackTrace(System.out);
        }
    }

    public void fastestAveragePitStopTeamBySeason(Scanner scanner) {
        final int windowSize = 10;
        int offset = 0;
        boolean exit = false;
        while (!exit) {
            String sql = """
                        SELECT
                            r.year,
                            c.constructorId,
                            c.name,
                            COUNT(*) AS total_stops,
                            ROUND(AVG(ps.milliseconds * 1.0), 2) AS avg_pit_ms
                        FROM pit_stops ps
                        JOIN results res
                            ON ps.raceId = res.raceId
                        AND ps.driverId = res.driverId
                        JOIN constructors c ON c.constructorId = res.constructorId
                        JOIN races r ON r.raceId = ps.raceId
                        GROUP BY r.year, c.constructorId, c.name
                        HAVING COUNT(*) >= 5
                        ORDER BY r.year, avg_pit_ms ASC
                        OFFSET ? ROWS FETCH NEXT ? ROWS ONLY;
                        """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, offset);
                statement.setInt(2, windowSize);

                ResultSet resultSet = statement.executeQuery();

                System.out.println("Fastest Average Pit Stop Team by Season");
                System.out.printf("Showing %d - %d%n", offset + 1, offset + windowSize);

                // header
                System.out.printf("%-6s %-10s %-25s %-12s %-15s\n",
                    "year", "id", "constructor", "stops", "avg_pit_ms");

                // separator
                System.out.println("-------------------------------------------------------------------------------");

                boolean hasRows = false;

                while (resultSet.next()) {
                    hasRows = true;
                    System.out.printf("%-6d %-10d %-25s %-12d %-15.2f\n",
                        resultSet.getInt("year"),
                        resultSet.getInt("constructorId"),
                        resultSet.getString("name"),
                        resultSet.getInt("total_stops"),
                        resultSet.getDouble("avg_pit_ms")
                    );
                }

                if (!hasRows && offset > 0) {
                    // went too far → go back
                    offset -= windowSize;
                    System.out.println("No more records.");
                } else {
                    // controls
                    System.out.print("\n[n] next | [p] previous | [q] quit: ");
                    String input = scanner.nextLine().trim().toLowerCase();

                    switch (input) {
                        case "n" -> offset += windowSize;
                        case "p" -> offset = Math.max(0, offset - windowSize);
                        case "q" -> exit = true;
                        default -> System.out.println("Invalid command.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace(System.out);
                exit = true;
            }
        }
    }

    public void constructorTeammateHeadToHead(Scanner scanner) {
        List<Integer> years = new ArrayList<>();
        boolean exit = false;

        String yearsSql = """
                SELECT DISTINCT year
                FROM races
                ORDER BY year;
                """;

        String sql = """
                SELECT
                    r.year,
                    c.name AS constructor_name,
                    d1.forename + ' ' + d1.surname AS driver_1,
                    d2.forename + ' ' + d2.surname AS driver_2,
                    SUM(CASE WHEN res1.positionOrder < res2.positionOrder THEN 1 ELSE 0 END) AS driver_1_ahead,
                    SUM(CASE WHEN res2.positionOrder < res1.positionOrder THEN 1 ELSE 0 END) AS driver_2_ahead
                FROM results res1
                JOIN results res2
                    ON res1.raceId = res2.raceId
                    AND res1.constructorId = res2.constructorId
                    AND res1.driverId < res2.driverId
                JOIN drivers d1 ON d1.driverId = res1.driverId
                JOIN drivers d2 ON d2.driverId = res2.driverId
                JOIN constructors c ON c.constructorId = res1.constructorId
                JOIN races r ON r.raceId = res1.raceId
                WHERE res1.positionOrder IS NOT NULL
                AND res2.positionOrder IS NOT NULL
                AND r.year = ?
                GROUP BY r.year, c.name, d1.forename, d1.surname, d2.forename, d2.surname
                ORDER BY c.name, driver_1, driver_2;
                """;

        try (
                PreparedStatement yearsStmt = connection.prepareStatement(yearsSql);
                ResultSet yearsRs = yearsStmt.executeQuery()
        ) {
            while (yearsRs.next()) {
                years.add(yearsRs.getInt("year"));
            }

            int index = 0;

            if (years.isEmpty()) {
                System.out.println("No seasons found.");
                exit = true;
            }

            while (!exit) {

                int selectedYear = years.get(index);

                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, selectedYear);

                    try (ResultSet resultSet = statement.executeQuery()) {

                        System.out.println("\nConstructor Teammate Head-to-Head");
                        System.out.println("Year: " + selectedYear);

                        System.out.printf("%-6s %-25s %-25s %-25s %-15s %-15s%n",
                                "year", "constructor", "driver_1", "driver_2",
                                "driver_1_ahead", "driver_2_ahead");

                        System.out.println("------------------------------------------------------------------------------------------------------------------------");

                        boolean hasRows = false;

                        while (resultSet.next()) {
                            hasRows = true;
                            System.out.printf("%-6d %-25s %-25s %-25s %-15d %-15d%n",
                                    resultSet.getInt("year"),
                                    resultSet.getString("constructor_name"),
                                    resultSet.getString("driver_1"),
                                    resultSet.getString("driver_2"),
                                    resultSet.getInt("driver_1_ahead"),
                                    resultSet.getInt("driver_2_ahead")
                            );
                        }

                        if (!hasRows) {
                            System.out.println("No teammate data for this year.");
                        }
                    }
                }

                System.out.print("\n[p] previous year | [n] next year | [q] quit | [year] go to year: ");
                String input = scanner.nextLine().trim().toLowerCase();

                switch (input) {
                    case "n" -> {
                        if (index < years.size() - 1) {
                            index++;
                        } else {
                            System.out.println("Already at the latest year.");
                        }
                    }
                    case "p" -> {
                        if (index > 0) {
                            index--;
                        } else {
                            System.out.println("Already at the earliest year.");
                        }
                    }
                    case "q" -> exit = true;
                    default -> {
                        try {
                            int requestedYear = Integer.parseInt(input);
                            int yearIndex = years.indexOf(requestedYear);

                            if (yearIndex >= 0) {
                                index = yearIndex;
                            } else {
                                System.out.printf("Year %d not found.\n", requestedYear);
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid command.");
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    public void listDrivers(Scanner scanner) {
        int offset = 0;
        boolean exit = false;
        while (!exit) {
            String sql = """
                        SELECT
                            driverId,
                            forename,
                            surname
                        FROM drivers
                        ORDER BY driverId
                        OFFSET ? ROWS FETCH NEXT ? ROWS ONLY;
                        """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, offset);
                statement.setInt(2, WINDOW_SIZE);

                ResultSet resultSet = statement.executeQuery();

                System.out.println("\nDrivers List");
                System.out.printf("Showing %d - %d%n", offset + 1, offset + WINDOW_SIZE);

                // header
                System.out.printf("%-10s %-15s %-15s\n",
                        "id", "forename", "surname");

                System.out.println("------------------------------------------");

                boolean hasRows = false;

                while (resultSet.next()) {
                    hasRows = true;
                    System.out.printf("%-10d %-15s %-15s\n",
                            resultSet.getInt("driverId"),
                            resultSet.getString("forename"),
                            resultSet.getString("surname")
                    );
                }

                if (!hasRows && offset > 0) {
                    // went too far → go back
                    offset -= WINDOW_SIZE;
                    System.out.println("No more records.");
                } else {
                    // controls
                    System.out.print("\n[n] next | [p] previous | [q] quit: ");
                    String input = scanner.nextLine().trim().toLowerCase();

                    switch (input) {
                        case "n" -> offset += WINDOW_SIZE;
                        case "p" -> offset = Math.max(0, offset - WINDOW_SIZE);
                        case "q" -> exit = true;
                        default -> System.out.println("Invalid command.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace(System.out);
                exit = true;
            }
        }
    }

    public void listConstructors(Scanner scanner) {
        int offset = 0;
        boolean exit = false;
        while (!exit) {
            String sql = """
                        SELECT
                            constructorId,
                            name
                        FROM constructors
                        ORDER BY constructorId
                        OFFSET ? ROWS FETCH NEXT ? ROWS ONLY;
                        """;

            try (PreparedStatement statement = connection.prepareStatement(sql)){
                statement.setInt(1, offset);
                statement.setInt(2, WINDOW_SIZE);

                ResultSet resultSet = statement.executeQuery();

                System.out.println("Constructors List");
                System.out.printf("Showing %d - %d%n", offset + 1, offset + WINDOW_SIZE);
                // header
                System.out.printf("%-10s %-25s\n",
                        "id", "name");

                System.out.println("------------------------------------------");

                boolean hasRows = false;

                while (resultSet.next()) {
                    hasRows = true;
                    System.out.printf("%-10d %-25s\n",
                            resultSet.getInt("constructorId"),
                            resultSet.getString("name")
                    );
                }

                if (!hasRows && offset > 0) {
                    // went too far → go back
                    offset -= WINDOW_SIZE;
                    System.out.println("No more records.");
                } else {
                    // controls
                    System.out.print("\n[n] next | [p] previous | [q] quit: ");
                    String input = scanner.nextLine().trim().toLowerCase();

                    switch (input) {
                        case "n" -> offset += WINDOW_SIZE;
                        case "p" -> offset = Math.max(0, offset - WINDOW_SIZE);
                        case "q" -> exit = true;
                        default -> System.out.println("Invalid command.");
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace(System.out);
            }
        }
    }

    public void listCircuits(Scanner scanner) {
        int offset = 0;
        boolean exit = false;
        while (!exit) {
            String sql = """
                        SELECT
                            circuitId,
                            name,
                            location,
                            country
                        FROM circuits
                        ORDER BY circuitId
                        OFFSET ? ROWS FETCH NEXT ? ROWS ONLY;
                        """;

            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, offset);
                statement.setInt(2, WINDOW_SIZE);

                ResultSet resultSet = statement.executeQuery();

                System.out.println("Circuits List");
                System.out.printf("Showing %d - %d%n", offset + 1, offset + WINDOW_SIZE);

                
                System.out.printf("%-6s %-30s %-20s %-20s\n",
                        "id", "name", "location", "country");

                System.out.println("--------------------------------------------------------------------------------");

                boolean hasRows = false;

                while (resultSet.next()) {
                    hasRows = true;
                    System.out.printf("%-6d %-30s %-20s %-20s\n",
                        resultSet.getInt("circuitId"),
                        resultSet.getString("name"),
                        resultSet.getString("location"),
                        resultSet.getString("country")
                    );
                }

                if (!hasRows && offset > 0) {
                    // went too far → go back
                    offset -= WINDOW_SIZE;
                    System.out.println("No more records.");
                } else {
                    // controls
                    System.out.print("\n[n] next | [p] previous | [q] quit: ");
                    String input = scanner.nextLine().trim().toLowerCase();

                    switch (input) {
                        case "n" -> offset += WINDOW_SIZE;
                        case "p" -> offset = Math.max(0, offset - WINDOW_SIZE);
                        case "q" -> exit = true;
                        default -> System.out.println("Invalid command.");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace(System.out);
                exit = true;
            }
        }
    }

    
    public void deleteAllData() {
        String[] deleteOrder = {
            "sprint_results",
            "constructor_results",
            "constructor_standings",
            "driver_standings",
            "qualifying",
            "pit_stops",
            "lap_times",
            "results",
            "races",
            "seasons",
            "drivers",
            "constructors",
            "circuits",
            "status"
        };

        try (Statement stmt = connection.createStatement()) {
            System.out.println("Deleting all data...");
            for (String table : deleteOrder) {
                stmt.executeUpdate("DELETE FROM " + table);
            }
            System.out.println("All data deleted.");
        } catch (SQLException e) {
            e.printStackTrace(System.out);
        }
    }

    private void runInsertStatementsFromFile(String sourceDir, String filePath) throws IOException, SQLException {
        List<String> lines = Files.readAllLines(Paths.get(sourceDir, filePath));
        StringBuilder statementBuilder = new StringBuilder();
        int batchCount = 0;

        try (Statement stmt = connection.createStatement()) {
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                statementBuilder.append(line).append(" ");

                if (trimmed.endsWith(";")) {
                    String sql = statementBuilder.toString().trim();
                    statementBuilder.setLength(0);

                    String upper = sql.toUpperCase();
                    if (upper.startsWith("INSERT INTO")) {
                        stmt.addBatch(sql);
                        batchCount++;
                    }

                    if (batchCount >= 500) {
                        stmt.executeBatch();
                        stmt.clearBatch();
                        batchCount = 0;
                    }
                }
            }

            if (batchCount > 0) {
                stmt.executeBatch();
                stmt.clearBatch();
            }
        }
    }

    public void repopulateDatabase() {

        

        boolean oldAutoCommit = true;

        try {
            oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            System.out.println("Repopulating database...");
            for (String file : SQL_FILES) {
                runInsertStatementsFromFile(SOURCE_DIR, file);
                System.out.println("Loaded: " + Paths.get(SOURCE_DIR, file).toAbsolutePath());
            }

            connection.commit();
            System.out.println("Database repopulated.");
        } catch (IOException | SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace(System.out);
            }
            e.printStackTrace(System.out);
        } finally {
            try {
                connection.setAutoCommit(oldAutoCommit);
            } catch (SQLException e) {
                e.printStackTrace(System.out);
            }
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}