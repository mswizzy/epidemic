// Schedule.java
/* Program that will eventually develop into an epidemic simulator
 * author Douglas W. Jones, Maria Hall
 * version Apr. 11, 2021 Lifted from Epidemic.java
 */

/** Tuple of start and end times used for scheduling people's visits to places
 */
class Schedule {
    // instance variables
    public final double startTime; // times are in seconds
    public final double duration;  // duration of visit
    public final double probability; //probability schedule followed

    private static final MyRandom rand = MyRandom.stream();

    /** construct a new Schedule
     *  @param in -- the input stream
     *  @param context -- the context for error messages
     *  Syntax: (0.0-0.0 0.0)
     *  Meaning: (start-end) times given in hours from midnight
     *  The last float is an optional probability of the schedule
     *  Default probability = 1.0
     *  The begin paren must just have been scanned from the input stream
     */
    public Schedule( MyScanner in, MyScanner.Message context ) {

        // get start time of schedule
        final double st = in.getNextFloat(
                23.98F, ()-> context.myString() + "(: not followed by start time"
        );
        in.getNextLiteral(
                MyScanner.dash, ()-> context.myString() + "(" + st
                        + ": not followed by -"
        );
        // get end time of schedule
        final double et = in.getNextFloat(
                23.99F, ()-> context.myString() + "(f" + st
                        + "-: not followed by end time"
        );

        //default probability is 1.0
        final double prob = in.getNextFloat(1.0, ()-> context.myString()
                + "(" + st + "-" + et +
                " not followed by probability. Defaulted to 1.0");

        in.getNextLiteral(
                MyScanner.endParen,
                ()-> context.myString() + "(" + st + "-" + et +
                        prob + ": not followed by )"
        );

        // check sanity constraints on start and end times
        if (st >= 24.00F) {
            Error.warn(
                    context.myString() + "(" + st + "-" + et
                            + "): start time is tomorrow"
            );
        }
        Check.nonNeg( st, 0.0F,
                ()-> context.myString() + "(" + st + "-" + et
                        + "): start time is yesterday"
        );
        if (st >= et) {
            Error.warn(
                    context.myString() + "(" + st + "-" + et
                            + "): times out of order"
            );
        }

        //check sanity constraints on probability
        if (prob < 0.0) {
            Error.warn(context.myString() + "(" + st + "-" + et
                    + prob + "): negative probability");
        }
        if (prob > 1.0) {
            Error.warn(context.myString() + "(" + st + "-" + et
                    + prob + "): probability greater than 1");
        }


        startTime = st * Time.hour;
        duration = (et * Time.hour) - startTime;
        probability = prob;
    }

    /** compare two schedules to see if they overlap
     *  @return true if they overlap, false otherwise
     */
    public boolean overlap( Schedule s ) {
        if (s == null) return false;
        double thisEnd = this.startTime + this.duration;
        if (this.startTime <= s.startTime) {
            if (s.startTime <= (this.startTime + this.duration)) return true;
        }
        double sEnd = s.startTime + s.duration;
        if (s.startTime <= this.startTime) {
            if (this.startTime <= (s.startTime + s.duration)) return true;
        }
        return false;
    }

    /** determines whether the schedule will be followed
     * @return true if follow schedule, false if ignore
     */
    public boolean follow() {
        return rand.nextFloat() <= probability;
    }

    /** commit a person to following a schedule regarding a place
     *  @param person
     *  @param place
     *  this starts the logical process of making a person follow this schedule
     */
    public void apply( Person person, Place place ) {
        //will the schedule be followed
        if (follow()) {
            Simulator.schedule(startTime, (double t) -> go(t, person, place));
        }
    }

    /** keep a person on schedule
     *  @param person
     *  @param place
     *  this continues a logical process of moving a person on this schedule
     */
    private void go( double time, Person person, Place place ) {
        double tomorrow = time + Time.day;

        // first, ensure that we keep following this schedule
        Simulator.schedule( tomorrow, (double t)-> go( t, person, place ) );

        // second, make the person go there
        person.travelTo( time, place );

        // third, make sure we get home
        Simulator.schedule( time + duration, (double t)-> person.goHome( t ) );
    }

    /** convert a Schedule back to textual form
     *  @return the schedule as a string
     *  Syntax: (0.0-0.0)
     *  Meaning: (start-end) times given in hours from midnight
     */
    public String toString() {
        return "(" + startTime/Time.hour
                + "-" + (startTime + duration) / Time.hour + ")";
    }
}