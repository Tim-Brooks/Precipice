package net.uncontended.beehive;

/**
 * Created by timbrooks on 1/10/15.
 */
public class RejectedActionException extends RuntimeException {

    public final RejectionReason reason;

    public RejectedActionException(RejectionReason reason) {
        this.reason = reason;
    }

}
