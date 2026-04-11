### Submission details

In this zipped folder consists of the folder for SQLs to repopulate the database, two Java source files for the server, 1 JDBC Driver JAR File to connect to the database and a Makefile to run

### Transfer to Aviary

Make sure to transfer the unzip the file and transfer the entire folder to Aviary by running the following commands:

```bash
scp -r [unzipped-folder] [path-to-aviary]
```

### Running the server:

Simply run `make run` to compile and run the server, and `make clean` once everything is done.

#### Commands:

There are 20 queries that you can run within the program, as denoted below:

```
1   -> Driver performance by season
2   -> Constructors with most DNFs by season
3   -> Hat-trick list
4   -> Grand Slam list
5   -> Lap degradation
6   -> Most championship points in a season
7   -> Most consecutive driver wins
8   -> Drivers with most DNFs per season
9   -> Most wins but not champions
10  -> Races for a single constructor where no drivers are classified
11  -> Races where all drivers are classified
12  -> Wins without pole
13  -> Worst pole-to-win ratio
14  -> Fastest average pit stop team by season
15  -> Constructor teammate head-to-head
16  -> List drivers
17  -> List constructors
18  -> List circuits
19  -> Delete all data
20  -> Repopulate database
```

Some queries might require you to manually input to display additional information. An example of this is the 15th query - Constructor teammate head-to-head:

`[p] previous year | [n] next year | [q] quit | [year] go to year:`

In this case, you can either press the above options, or type out a numerical year to display information for that year (for instance, type `2024` to display information for the 2024 season).

Other query that require complex inputs is Lap degradation:

```
Commands:
  n  -> next year
  p  -> previous year
  a  -> view all races in this year
  r  -> choose a specific race in this year
  q  -> quit
Or enter a year directly:
```

Here, you can choose `a` to view all specific races, or `r` to display the result of single race for all seasons as follows:

`Enter exact race name: `

Here, type the full race name to see the result (i.e., `British Grand Prix` for all British Grand Prixs hosted).

Otherwise, as with the former input, type a numeric year to display information for that year (for instance, type `2024` to display information for the 2024 season).