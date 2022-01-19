# Epidemic Simulator

Simulates the spread of a disease though a community

Takes an input file containing a description of the places in a c ommunity, the roles fulfilled by the population of that community, and the nature of the disease. The output is a CSV format showing the progression of the disease spread through the community

The source files:

* Error.java	->	error reporting framework
* MyScanner.java	-> Wrapper around java.util.scanner
* Check.java	->	Utility to do sanity checks on values
* MyRandom.java	->	Extensions to Java.util.random
* Simulator.java	-> Simulation framework
* Time.java	->	Format and definitions of time and time units
* Probability.java	-> Format of probability

* InfectionRule.java	-> How do stages of the infection progress
* Schedule.java	->	How do people decide to move from place to place
* Person.java	->	How does each person behave, also population statistics
* Place.java	->	How does each place work
* PlaceKind.java	-> What kinds of places are there
* Role.java	->	What kinds of roles to people fit into

* Epidemic.java	->	the main program
