// Error.java
/* Program that will eventually develop into an epidemic simulator
 * author Douglas W. Jones, Maria Hall
 * version Apr. 11, 2021 Lifted from Epidemic.java
 */



/** Error reporting framework
 *  All error messages go to System.err (aka stderr, the standard error stream).
 *  Currently, this only supports fatal error reporting.
 *  Later it would be nice to have a way to report non-fatal errors.
 */
class Error {
    private static int warningCount = 0;

    /** Report a fatal error
     *  @param msg -- error message to be output
     *  This never returns, the program terminates reporting failure.
     */
    public static void fatal( String msg ) {
        System.err.println( "Epidemic: " + msg );
        System.exit( 1 );  // abnormal termination
    }

    /** Non-fatal warning
     *  @param msg -- the warning message
     *  keeps a running count of warnings
     */
    public static void warn( String msg ) {
        System.err.println( "Warning: " + msg );
        warningCount = warningCount + 1;
    }

    /** Error exit if any warnings
     */
    public static void exitIfWarnings( String msg ) {
        if (warningCount > 0) fatal( msg );
    }
}