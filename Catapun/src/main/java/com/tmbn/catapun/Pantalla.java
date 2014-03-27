// CannonView.java
// Displays and controls the Cannon Game
package com.tmbn.catapun;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.SeekBar;

public class Pantalla extends SurfaceView
   implements SurfaceHolder.Callback {
    private static final String TAG = "CannonView"; // for logging errors

    private CannonThread cannonThread; // controls the game loop
    private Activity activity; // to display Game Over dialog in GUI thread
    private boolean dialogIsDisplayed = false;

    public SeekBar seekBar;


    // constants for game play
    public static final int TARGET_PIECES = 7; // sections in the target
    public static final int HIT_REWARD = 3; // seconds added on a hit

    // variables for the game loop and tracking statistics
    private boolean gameOver; // is the game over?
    private double timeLeft; // time remaining in seconds
    private int shotsFired; // shots the user has fired
    private double totalElapsedTime; // elapsed seconds

    double elapsedTimeMS;
    double timegrav; //tiempo para calcular la gravedad

    private Linea target; // start and end points of the target
    private int targetDistance; // target distance from left
    private int targetBeginning; // target distance from top
    private double pieceLength; // length of a target piece
    private int targetEnd; // target bottom's distance from top
    private int initialTargetVelocity;
    private int gravity;// initial target speed multiplier
    private float targetVelocity; // target speed multiplier

    private int lineWidth; // width of the target and blocker
    private boolean[] hitStates; // is each target piece hit?
    private int targetPiecesHit; // number of target pieces hit (out of 7)

    // variables for the cannon and cannonball
    private Point cannonball; // cannonball image's upper-left corner
    private int cannonballVelocityX; // cannonball's x velocity
    private int cannonballVelocityY; // cannonball's y velocity
    private boolean cannonballOnScreen; // whether cannonball on the screen
    private int cannonballRadius; // cannonball's radius
    private int cannonballSpeed; // cannonball's speed
    private int cannonBaseRadius; // cannon base's radius
    private int cannonLength; // cannon barrel's length
    private Point barrelEnd; // the endpoint of the cannon's barrel
    private int screenWidth;
    private int screenHeight;

    // constants and variables for managing sounds
    private static final int TARGET_SOUND_ID = 0;
    private static final int CANNON_SOUND_ID = 1;
    private static final int BLOCKER_SOUND_ID = 2;
    private SoundPool soundPool; // plays sound effects
    private SparseIntArray soundMap; // maps IDs to SoundPool

    // Paint variables used when drawing each item on the screen
    private Paint textPaint; // Paint used to draw text
    private Paint cannonballPaint; // Paint used to draw the cannonball
    private Paint cannonPaint; // Paint used to draw the cannon
    private Paint blockerPaint; // Paint used to draw the blocker
    private Paint targetPaint; // Paint used to draw the target
    private Paint backgroundPaint; // Paint used to clear the drawing area

    // public constructor
    public Pantalla(Context context, AttributeSet attrs) {
        super(context, attrs); // call superclass constructor
        activity = (Activity) context; // store reference to MainActivity

        // register SurfaceHolder.Callback listener
        getHolder().addCallback(this);

        // initialize Lines and Point representing game items
        target = new Linea(); // create the target as a Linea
        cannonball = new Point(); // create the cannonball as a Point

        // initialize hitStates as a boolean array
        hitStates = new boolean[TARGET_PIECES];

        // initialize SoundPool to play the app's three sound effects
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        // create Map of sounds and pre-load sounds
        soundMap = new SparseIntArray(3); // create new HashMap
        soundMap.put(TARGET_SOUND_ID,
                soundPool.load(context, R.raw.target_hit, 1));
        soundMap.put(CANNON_SOUND_ID,
                soundPool.load(context, R.raw.cannon_fire, 1));
        soundMap.put(BLOCKER_SOUND_ID,
                soundPool.load(context, R.raw.blocker_hit, 1));

        // construct Paints for drawing text, cannonball, cannon,
        // blocker and target; these are configured in method onSizeChanged
        textPaint = new Paint();
        cannonPaint = new Paint();
        cannonballPaint = new Paint();
        blockerPaint = new Paint();
        targetPaint = new Paint();
        backgroundPaint = new Paint();
        seekBar = (SeekBar) findViewById(R.id.seekBar);

    } // end CannonView constructor


    // called by surfaceChanged when the size of the SurfaceView changes,
    // such as when it's first added to the View hierarchy
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        screenWidth = w; // store CannonView's width
        screenHeight = h; // store CannonView's height
        cannonBaseRadius = h / 7; // cannon base radius 1/18 screen height
        cannonLength = w / 10; // cannon length 1/8 screen width

        cannonballRadius = w / 36; // cannonball radius 1/36 screen width
        cannonballSpeed = w; // cannonball speed multiplier

        gravity = w / 28;//TODO poner bien
        timegrav = 0.00;

        lineWidth = w / 20; // target and blocker 1/24 screen width


        // configure instance variables related to the target
        targetDistance = w * 7 / 8; // target 7/8 screen width from left
        targetBeginning = h / 8; // distance from top 1/8 screen height
        targetEnd = h * 7 / 8; // distance from top 7/8 screen height
        pieceLength = (targetEnd - targetBeginning) / TARGET_PIECES;
        initialTargetVelocity = -h / 4; // initial target speed multiplier
        target.start = new Point(targetDistance, targetBeginning);
        target.end = new Point(targetDistance, targetEnd);

        // endpoint of the cannon's barrel initially points horizontally
        barrelEnd = new Point(cannonLength, h);

        // configure Paint objects for drawing game elements
        textPaint.setTextSize(w / 20); // text size 1/20 of screen width
        textPaint.setAntiAlias(true); // smoothes the text
        cannonPaint.setStrokeWidth(lineWidth * 1.5f); // set line thickness
        blockerPaint.setStrokeWidth(lineWidth); // set line thickness
        targetPaint.setStrokeWidth(lineWidth); // set line thickness
        backgroundPaint.setColor(Color.WHITE); // set background color

        newGame(); // set up and start a new game
    } // end method onSizeChanged

    // reset all the screen elements and start a new game
    public void newGame() {
        //seekBar.setOnSeekBarChangeListener(new sbListener());//TODO hacer q funcione!
        // set every element of hitStates to false--restores target pieces
        for (int i = 0; i < TARGET_PIECES; i++)
            hitStates[i] = false;

        targetPiecesHit = 0; // no target pieces have been hit
        targetVelocity = initialTargetVelocity; // set initial velocity
        timeLeft = 10; // start the countdown at 10 seconds
        cannonballOnScreen = false; // the cannonball is not on the screen
        shotsFired = 0; // set the initial number of shots fired
        totalElapsedTime = 0.0; // set the time elapsed to zero

        // set the start and end Points of the blocker and target

        target.start.set(targetDistance, targetBeginning);
        target.end.set(targetDistance, targetEnd);

        if (gameOver) // starting a new game after the last game ended
        {
            gameOver = false;
            cannonThread = new CannonThread(getHolder()); // create thread
            cannonThread.start(); // start the game loop thread
        }
    } // end method newGame

    // called repeatedly by the CannonThread to update game elements
    private void updatePositions(double elapsedTimeMS) {
        double interval = elapsedTimeMS / 1000.0; // convert to seconds

        if (cannonballOnScreen) // if there is currently a shot fired
        {
            timegrav += interval;
            // update cannonball position
            cannonball.x += interval * cannonballVelocityX;

            double doblintervalo = timegrav * timegrav;
            double gravityNew = (gravity / 2) * doblintervalo;
            double velY = cannonballVelocityY * interval;
            cannonball.y += velY + gravityNew;//TODO Voy*t+(1/2)*a*t^2

            if (cannonball.x > screenWidth ||
                    cannonball.x < 0) {
                cannonballOnScreen = false; // remove cannonball from screen
                timegrav = 0.00;
            }
            // check for collisions with top and bottom walls
            else if (cannonball.y > screenHeight) {
                cannonballOnScreen = false; // remove cannonball from screen
                timegrav = 0.00;
            }
            // check for cannonball collision with target
            else if (cannonball.x + cannonballRadius > targetDistance &&
                    cannonball.x - cannonballRadius < targetDistance &&
                    cannonball.y + cannonballRadius > target.start.y &&
                    cannonball.y - cannonballRadius < target.end.y) {
                // determine target section number (0 is the top)
                int section =
                        (int) ((cannonball.y - target.start.y) / pieceLength);

                // check if the piece hasn't been hit yet
                if ((section >= 0 && section < TARGET_PIECES) &&
                        !hitStates[section]) {
                    hitStates[section] = true; // section was hit
                    cannonballOnScreen = false; // remove cannonball
                    timeLeft += HIT_REWARD; // add reward to remaining time
                    timegrav = 0.00;

                    // play target hit sound
                    soundPool.play(soundMap.get(TARGET_SOUND_ID), 1,
                            1, 1, 0, 1f);

                    // if all pieces have been hit
                    if (++targetPiecesHit == TARGET_PIECES) {
                        cannonThread.setRunning(false); // terminate thread
                        showGameOverDialog(R.string.win); // show winning dialog
                        gameOver = true;
                    }
                }
            }
        }


        // update the target's position
        double targetUpdate = interval * targetVelocity;
        target.start.y += targetUpdate;
        target.end.y += targetUpdate;


        // if the target hit the top or bottom, reverse direction
        if (target.start.y < 0 || target.end.y > screenHeight)
            targetVelocity *= -1;

        timeLeft -= interval; // subtract from time left

        // if the timer reached zero

    } // end method updatePositions

    // fires a cannonball
    public void fireCannonball(MotionEvent event) {
        if (cannonballOnScreen) // if a cannonball is already on the screen
            return; // do nothing

        double angle = alignCannon(event); // get the cannon barrel's angle

        // move the cannonball to be inside the cannon
        cannonball.x = cannonballRadius; // align x-coordinate with cannon
        cannonball.y = screenHeight; // centers ball vertically

        // get the x component of the total velocity
        cannonballVelocityX = (int) (cannonballSpeed * Math.sin(angle));

        // get the y component of the total velocity
        cannonballVelocityY = (int) (-cannonballSpeed * Math.cos(angle));

        cannonballOnScreen = true; // the cannonball is on the screen
        ++shotsFired; // increment shotsFired

        // play cannon fired sound
        soundPool.play(soundMap.get(CANNON_SOUND_ID), 1, 1, 1, 0, 1f);
    } // end method fireCannonball

    // aligns the cannon in response to a user touch
    public double alignCannon(MotionEvent event) {
        // get the location of the touch in this view
        Point touchPoint = new Point((int) event.getX(), (int) event.getY());

        // compute the touch's distance from center of the screen
        // on the y-axis
        double centerMinusY = (screenHeight - touchPoint.y);

        double angle = 0; // initialize angle to 0

        // calculate the angle the barrel makes with the horizontal
        if (centerMinusY != 0) // prevent division by 0
            angle = Math.atan((double) touchPoint.x / centerMinusY);

        // if the touch is on the lower half of the screen
        //if (touchPoint.y > screenHeight)
        //  angle += Math.PI; // adjust the angle

        // calculate the endpoint of the cannon barrel
        barrelEnd.x = (int) (cannonLength * Math.sin(angle));
        barrelEnd.y =
                (int) (-cannonLength * Math.cos(angle) + screenHeight);

        return angle; // return the computed angle
    } // end method alignCannon

    // draws the game to the given Canvas
    public void drawGameElements(Canvas canvas) {
        // clear the background
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(),
                backgroundPaint);

        // display time remaining

        canvas.drawText(getResources().getString(
                R.string.time_remaining_format, timeLeft), 30, 50, textPaint);

        // if a cannonball is currently on the screen, draw it
        if (cannonballOnScreen)
            canvas.drawCircle(cannonball.x, cannonball.y, cannonballRadius,
                    cannonballPaint);

        // draw the cannon barrel
        canvas.drawLine(0, screenHeight, barrelEnd.x, barrelEnd.y,
                cannonPaint);

        // draw the cannon base
        canvas.drawCircle(0, (int) screenHeight,
                (int) cannonBaseRadius, cannonPaint);


        Point currentPoint = new Point(); // start of current target section

        // initialize currentPoint to the starting point of the target
        currentPoint.x = target.start.x;
        currentPoint.y = target.start.y;

        // draw the target
        for (int i = 0; i < TARGET_PIECES; i++) {
            // if this target piece is not hit, draw it
            if (!hitStates[i]) {
                // alternate coloring the pieces
                if (i % 2 != 0)
                    targetPaint.setColor(Color.BLUE);
                else
                    targetPaint.setColor(Color.YELLOW);

                canvas.drawLine(currentPoint.x, currentPoint.y, target.end.x,
                        (int) (currentPoint.y + pieceLength), targetPaint);
            }

            // move currentPoint to the start of the next piece
            currentPoint.y += pieceLength;
        }
    } // end method drawGameElements

    // display an AlertDialog when the game ends
    private void showGameOverDialog(final int messageId) {
        // DialogFragment to display quiz stats and start new quiz
        final DialogFragment gameResult =
                new DialogFragment() {
                    // create an AlertDialog and return it
                    @Override
                    public Dialog onCreateDialog(Bundle bundle) {
                        // create dialog displaying String resource for messageId
                        AlertDialog.Builder builder =
                                new AlertDialog.Builder(getActivity());
                        builder.setTitle(getResources().getString(messageId));

                        // display number of shots fired and total time elapsed
                        builder.setMessage(getResources().getString(
                                R.string.results_format, shotsFired, totalElapsedTime));
                        builder.setPositiveButton(R.string.reset_game,
                                new DialogInterface.OnClickListener() {
                                    // called when "Reset Game" Button is pressed
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialogIsDisplayed = false;
                                        newGame(); // set up and start a new game
                                    }
                                } // end anonymous inner class
                        ); // end call to setPositiveButton

                        return builder.create(); // return the AlertDialog
                    } // end method onCreateDialog
                }; // end DialogFragment anonymous inner class

        // in GUI thread, use FragmentManager to display the DialogFragment
        activity.runOnUiThread(
                new Runnable() {
                    public void run() {
                        dialogIsDisplayed = true;
                        gameResult.setCancelable(false); // modal dialog
                        gameResult.show(activity.getFragmentManager(), "results");
                    }
                } // end Runnable
        ); // end call to runOnUiThread
    } // end method showGameOverDialog

    // stops the game; called by CatapunFragment's onPause method
    public void stopGame() {
        if (cannonThread != null)
            cannonThread.setRunning(false); // tell thread to terminate
    }

    // releases resources; called by CatapunFragment's onDestroy method
    public void releaseResources() {
        soundPool.release(); // release all resources used by the SoundPool
        soundPool = null;
    }

    // called when surface changes size
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format,
                               int width, int height) {
    }

    // called when surface is first created
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!dialogIsDisplayed) {
            cannonThread = new CannonThread(holder); // create thread
            cannonThread.setRunning(true); // start game running
            cannonThread.start(); // start the game loop thread
        }
    }

    // called when the surface is destroyed
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // ensure that thread terminates properly
        boolean retry = true;
        cannonThread.setRunning(false); // terminate cannonThread

        while (retry) {
            try {
                cannonThread.join(); // wait for cannonThread to finish
                retry = false;
            } catch (InterruptedException e) {
                Log.e(TAG, "Thread interrupted", e);
            }
        }
    } // end method surfaceDestroyed

    // called when the user touches the screen in this Activity
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // get int representing the type of action which caused this event
        int action = e.getAction();

        // the user user touched the screen or dragged along the screen
        if (action == MotionEvent.ACTION_DOWN ||
                action == MotionEvent.ACTION_MOVE) {
            fireCannonball(e); // fire the cannonball toward the touch point
        }

        return true;
    } // end method onTouchEvent

    // Thread subclass to control the game loop
    private class CannonThread extends Thread {
        private SurfaceHolder surfaceHolder; // for manipulating canvas
        private boolean threadIsRunning = true; // running by default

        // initializes the surface holder
        public CannonThread(SurfaceHolder holder) {
            surfaceHolder = holder;
            setName("CannonThread");
        }

        // changes running state
        public void setRunning(boolean running) {
            threadIsRunning = running;
        }

        // controls the game loop
        @Override
        public void run() {
            Canvas canvas = null; // used for drawing
            long previousFrameTime = System.currentTimeMillis();

            while (threadIsRunning) {
                try {
                    // get Canvas for exclusive drawing from this thread
                    canvas = surfaceHolder.lockCanvas(null);

                    // lock the surfaceHolder for drawing
                    synchronized (surfaceHolder) {
                        long currentTime = System.currentTimeMillis();
                        elapsedTimeMS = currentTime - previousFrameTime;
                        totalElapsedTime += elapsedTimeMS / 1000.0;
                        updatePositions(elapsedTimeMS); // update game state
                        drawGameElements(canvas); // draw using the canvas
                        previousFrameTime = currentTime; // update previous time
                    }
                } finally {
                    // display canvas's contents on the CannonView
                    // and enable other threads to use the Canvas
                    if (canvas != null)
                        surfaceHolder.unlockCanvasAndPost(canvas);
                }
            } // end while
        } // end method run
    } // end nested class CannonThread

    public void setTime(double tiempo) {
        totalElapsedTime = tiempo;
    }
} // end class CannonView
class sbListener implements SeekBar.OnSeekBarChangeListener {

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Log.d("DEBUG", "Progress is: "+progress);
    }

    public void onStartTrackingTouch(SeekBar seekBar) {}

    public void onStopTrackingTouch(SeekBar seekBar) {}


}
/*********************************************************************************
 * (C) Copyright 1992-2014 by Deitel & Associates, Inc. and * Pearson Education, *
 * Inc. All Rights Reserved. * * DISCLAIMER: The authors and publisher of this   *
 * book have used their * best efforts in preparing the book. These efforts      *
 * include the * development, research, and testing of the theories and programs *
 * * to determine their effectiveness. The authors and publisher make * no       *
 * warranty of any kind, expressed or implied, with regard to these * programs   *
 * or to the documentation contained in these books. The authors * and publisher *
 * shall not be liable in any event for incidental or * consequential damages in *
 * connection with, or arising out of, the * furnishing, performance, or use of  *
 * these programs.                                                               *
 *********************************************************************************/
