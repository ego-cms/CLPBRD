package com.ego_cms.copypaste;

/**
 * Created on 10/08/15.
 */
public final class ActivityStates {

    public static final int STATE_INITIAL = 0x0;
    public static final int STATE_CREATED = 0x1;
    public static final int STATE_STARTED = 0x3;
    public static final int STATE_RESUMED = 0x7;
    public static final int STATE_PAUSED = 0x10;
    public static final int STATE_STOPPED = 0x30;
    public static final int STATE_DESTROYED = 0x70;


    private ActivityStates() {
        /* Prevent instantiating */
    }

}
