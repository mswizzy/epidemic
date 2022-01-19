// Person.java
/* Program that will eventually develop into an epidemic simulator
 * author Douglas W. Jones, Maria Hall
 * version Apr. 11, 2021 Lifted from Epidemic.java
 */

import java.util.LinkedList;

/** People are the central actors in the simulation
 *  @see Role for the roles people play
 *  @see Place for the places people visit
 */
class Person {

    private static enum DiseaseStates {
        uninfected,
        latent,
        asymptomatic,
        symptomatic,
        bedridden,
        recovered,
        dead // this must be the last state so that
        // DiseaseStates.dead.ordinal()+1 is the number of disease states
    }

    // population broken down by disease state
    private static int[] popByState = new int[ DiseaseStates.dead.ordinal()+1 ];

    // timing characteristics of disease state
    private static InfectionRule latent;
    private static InfectionRule asymptomatic;
    private static InfectionRule symptomatic;
    private static InfectionRule bedridden;

    public static void setDiseaseParameters(
            InfectionRule l, InfectionRule a, InfectionRule s, InfectionRule b
    ) {
        latent = l;
        asymptomatic = a;
        symptomatic = s;
        bedridden = b;
    }

    // linkage from person to place involves a schedule
    private class PlaceSchedule {
        public Place place;
        public Schedule schedule;
        public PlaceSchedule( Place p, Schedule s ) {
            place = p;
            schedule = s;
        }
    }

    // instance variables created from model description
    private final Role role;      // role of this person
    private Place home;           // this person's home place, set by emplace
    private final LinkedList<PlaceSchedule> places = new LinkedList<>();

    // instance variables that change as simulation progresses
    private DiseaseStates diseaseState = DiseaseStates.uninfected;
    private Place location;	       // initialized by emplace
    private double infectMeTime = 0.0; // time I will get infected
    // for the above, the default 0.0 allows for infection at startup

    // static variables used for all people
    private static LinkedList<Person> allPeople = new LinkedList<Person>();
    private static MyRandom rand = MyRandom.stream;

    /** Construct a new person to perform some role
     *  @param r -- the role
     *  This constructor deliberately defers putting people in any places
     */
    public Person( Role r ) {
        role = r;

        allPeople.add( this ); // include this person in the list of all

        popByState[ diseaseState.ordinal() ]++;  // include it in the statistics
    };

    // methods used during model construction, at time 0.0

    /** Associate this person to a particular place
     *  @param p -- the place
     *  @param s -- the associated schedule
     */
    public void emplace( Place p, Schedule s ) {
        if (s != null) {
            places.add( new PlaceSchedule( p, s ) );
            s.apply( this, p ); // commit to following schedule s for place p
        } else {
            assert home == null: "Role guarantees only one home place";
            home = p;
            location = home;

            location.arrive( 0.0, this ); // tell location about new occupant
        }
    }

    // state query

    /** Is this person contagious?
     *  @returns true if they are
     */
    public boolean isContagious() {
        return
                (diseaseState.compareTo( DiseaseStates.asymptomatic ) >= 0)
                        && (diseaseState.compareTo( DiseaseStates.bedridden ) <= 0);
    }

    // simulation of behavior

    /** Schedule the time at which a person will be infected
     *  @param time -- the current time
     *  @param meanDelay -- the delay until infection
     */
    public void scheduleInfect( double time, double meanDelay ) {
        if (diseaseState == DiseaseStates.uninfected) { // irrelevant if not
            double delay = rand.nextExponential( meanDelay );
            infectMeTime = time + delay;
            Simulator.schedule( infectMeTime, (double t)-> infect( t ) );
        }
    }

    /** Infect this person
     *  @param now -- the time of infection
     *  This may be called on a person in any infection state and makes the
     *  person latent.
     *  This is a schedulable event service routine
     */
    public void infect( double now ) {
        if (   (diseaseState == DiseaseStates.uninfected) // no reinfection
                && (infectMeTime == now)                      // if not rescheduled
        ) {
            final double duration = latent.duration();

            // update statistics
            popByState[ diseaseState.ordinal() ]--;
            diseaseState = DiseaseStates.latent;
            popByState[ diseaseState.ordinal() ]++;

            if (latent.recover()) {
                Simulator.schedule( now + duration, (double t)-> recover( t ) );
            } else {
                Simulator.schedule(
                        now + duration, (double t)-> beContagious( t )
                );
            }
        }
    }

    /** This person becomes contagious and asymptomatic
     *  @param time -- the time of this state change
     *  This may be called on a person in with a latent infection and makes the
     *  person asymptomatic.
     *  This is a schedulable event service routine
     */
    public void beContagious( double time ) {
        assert diseaseState == DiseaseStates.latent : "not latent";
        final double duration = asymptomatic.duration();

        // update statistics
        popByState[ diseaseState.ordinal() ]--;
        diseaseState = DiseaseStates.asymptomatic;
        popByState[ diseaseState.ordinal() ]++;

        // tell place that I'm sick
        if (location != null) location.contagious( time, +1 );

        if (asymptomatic.recover()) {
            Simulator.schedule( time + duration, (double t)-> recover( t ) );
        } else {
            Simulator.schedule( time + duration, (double t)-> feelSick( t ) );
        }
    }

    /** This person is contagious and starts feeling sick
     *  @param time -- the time of this state change
     *  This may be called on a person in with an asymptomatic infection and
     *  makes the person symptomatic.
     *  This is a schedulable event service routine
     */
    public void feelSick( double time ) {
        assert diseaseState == DiseaseStates.asymptomatic: "not asymptomatic";
        final double duration = symptomatic.duration();

        // update statistics
        popByState[ diseaseState.ordinal() ]--;
        diseaseState = DiseaseStates.symptomatic;
        popByState[ diseaseState.ordinal() ]++;

        if (symptomatic.recover()) {
            Simulator.schedule( time + duration, (double t)-> recover( t ) );
        } else {
            Simulator.schedule( time + duration, (double t)-> goToBed( t ) );
        }
    }

    /** This person is contagious and feels so bad they go to bed
     *  @param time -- the time of this state change
     *  This may be called on a person in with an symptomatic infection and
     *  makes the person bedridden.
     *  This is a schedulable event service routine
     */
    public void goToBed( double time ) {
        assert diseaseState == DiseaseStates.symptomatic: "not symptomatic";
        final double duration = bedridden.duration();

        // update statistics
        popByState[ diseaseState.ordinal() ]--;
        diseaseState = DiseaseStates.bedridden;
        popByState[ diseaseState.ordinal() ]++;

        if (symptomatic.recover()) {
            Simulator.schedule( time + duration, (double t)-> recover( t ) );
        } else {
            Simulator.schedule( time + duration, (double t)-> die( t ) );
        }
    }

    /** This person gets better
     *  @param time -- the time of this state change
     *  This may be called on a person in any infected disease state
     *  and leaves the person well and immune from further infection.
     *  This is a schedulable event service routine
     */
    public void recover( double time ) {
        // update statistics
        popByState[ diseaseState.ordinal() ]--;
        diseaseState = DiseaseStates.recovered;
        popByState[ diseaseState.ordinal() ]++;

        if (location != null) location.contagious( time, -1 );
    }

    /** This person dies
     *  @param time -- the time of this state change
     *  This may be called on a bedridden person and
     *  makes the person die.
     *  This is a schedulable event service routine
     */
    public void die( double time ) {
        assert diseaseState == DiseaseStates.bedridden: "not bedridden";
        // update statistics
        popByState[ diseaseState.ordinal() ]--;
        diseaseState = DiseaseStates.dead;
        popByState[ diseaseState.ordinal() ]++;

        if (location != null) {
            location.depart( time, this );
        }

        // no new event is scheduled.
    }

    /** Tell this person to go home at this time
     *  @param time
     *  This is a schedulable event service routine.
     */
    public void goHome( double time ) {
        travelTo( time, home );
    }

    /** Tell this person to go somewhere
     *  @param time
     *  @param place to go
     *  This is a schedulable event service routine.
     *  Note that bedridden people never leave home.
     */
    public void travelTo( double time, Place place ) {
        if ((diseaseState != DiseaseStates.bedridden) || (place == home)) {
            location.depart( time, this );
            location = place;
            location.arrive( time, this );
        }
    }

    // reporting tools

     /**
     * Reports in CSV format
     * @param headline whether or not to include a headline
     */
    public static void startReporting (boolean headline) {
        if (headline) {
            System.out.print("time");
            for (DiseaseStates s: DiseaseStates.values()) {
                System.out.print(",");
                System.out.print(s.name());
            }
            System.out.println();
        }
        //schedule first report
        Simulator.schedule(0.0, (double t)-> Person.report(t));
    }
    
    /** Report population statistics at the given time
     *  @param time
     *  Intended to be scheduled as an event at time zero, initiates a
     *  sequence of daily reporting events.
     *  Each report is a CSV line giving the time and the population
     *  for each disease state.
     */
    public static void report( double time ) {
        System.out.print( Double.toString( time/Time.day ) );
        for (int i = 0; i <= DiseaseStates.dead.ordinal(); i++ ) {
            System.out.print( "," );
            System.out.print( Integer.toString( popByState[i] ) );
        }
        System.out.println();

        // schedule the next report
        Simulator.schedule( time + 24*Time.hour,
                (double t)-> Person.report( t )
        );
    }

    /** Print out the entire population
     *  This is needed only in the early stages of debugging
     *  and obviously useless for large populations.
     */
    public static void printAll() {
        for (Person p: allPeople) {
            // line 1: person id and role
            System.out.print( p.toString() );
            System.out.print( " " );
            System.out.println( p.role.name );

            // line 2 the home
            System.out.print( " " ); // indent following lines
            System.out.print( p.home.kind.name );
            System.out.print( " " );
            System.out.print( p.home.toString() );
            System.out.println();
            // lines 3 and up: each place and its schedule
            for (PlaceSchedule ps: p.places ) {
                System.out.print( " " ); // indent following lines
                System.out.print( ps.place.kind.name );
                System.out.print( " " );
                System.out.print( ps.place.toString() );
                assert ps.schedule != null: "guaranteed by PlaceKind";
                System.out.print( ps.schedule.toString() );
                System.out.println();
            }
        }
    }
}
