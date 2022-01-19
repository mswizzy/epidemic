// Simulator.java
/* Program that will eventually develop into an epidemic simulator
 * author Douglas W. Jones, Maria Hall
 * version Apr. 11, 2021 Lifted from Epidemic.java
 */

import java.util.PriorityQueue;

/** Framework for discrete event simulation
 */
class Simulator {
    private Simulator() {} // prevent construction of instances!  Don't call!

    /** Functional interface for scheduling actions to be done later
     *  Users will generally never mention Action or trigger because
     *  this is used to support lambda expressions passed to schedule().
     */
    public static interface Action {
        void trigger( double time );
    }

    private static class Event {
        public final double time; // when will this event occur
        public final Action act;  // what to do then
        public Event( double t, Action a ) {
            time = t;
            act = a;
        }
    }

    private static final PriorityQueue<Event> eventSet = new PriorityQueue<>(
            ( Event e1, Event e2 )-> Double.compare( e1.time, e2.time )
    );

    /** Schedule an event to occur at a future time
     *  @param t, the time of the event
     *  @param a, what to do for that event
     *  example:
     *  <pre>
     *    Simulator.schedule( now+later, (double t)-> whatToDo( then, stuff ) );
     *  </pre>
     */
    public static void schedule( double t, Action a ) {
        eventSet.add( new Event( t, a ) );
    }

    /** Run the simulation
     *  Before running the simulation, schedule the initial events
     *  all of the simulation occurs as side effects of scheduled events
     */
    public static void run() {
        while (!eventSet.isEmpty()) {
            Event e = eventSet.remove();
            e.act.trigger( e.time );
        }
    }
}