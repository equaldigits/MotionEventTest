package com.example.clevo.motioneventtest;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

abstract class VersionedGestureDetector {
    private static final String TAG = "VersionedGestureDetector";

    OnGestureListener mListener;

    public static VersionedGestureDetector newInstance(Context context,
                                                       OnGestureListener listener) {
        final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
        VersionedGestureDetector detector = null;
        if (sdkVersion < Build.VERSION_CODES.ECLAIR) {
            detector = new CupcakeDetector();
        } else if (sdkVersion < Build.VERSION_CODES.FROYO) {
            detector = new EclairDetector();
        } else {
            detector = new FroyoDetector(context);
        }

        Log.d(TAG, "Created new " + detector.getClass());
        detector.mListener = listener;

        return detector;
    }

    public abstract boolean onTouchEvent(MotionEvent ev);

    public interface OnGestureListener {
        public void onDrag(float dx, float dy);
        public void onScale(float scaleFactor);
    }

    private static class CupcakeDetector extends VersionedGestureDetector {
        float mLastTouchX;
        float mLastTouchY;

        float getActiveX(MotionEvent ev) {
            return ev.getX();
        }

        float getActiveY(MotionEvent ev) {
            return ev.getY();
        }

        boolean shouldDrag() {
            return true;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    mLastTouchX = getActiveX(ev);
                    mLastTouchY = getActiveY(ev);
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    final float x = getActiveX(ev);
                    final float y = getActiveY(ev);

                    if (shouldDrag()) {
                        mListener.onDrag(x - mLastTouchX, y - mLastTouchY);
                    }

                    mLastTouchX = x;
                    mLastTouchY = y;
                    break;
                }
            }
            return true;
        }
    }

    private static class EclairDetector extends CupcakeDetector {
        private static final int INVALID_POINTER_ID = -1;
        private int mActivePointerId = INVALID_POINTER_ID;
        private int mActivePointerIndex = 0;

        @Override
        float getActiveX(MotionEvent ev) {
            return ev.getX(mActivePointerIndex);
        }

        @Override
        float getActiveY(MotionEvent ev) {
            return ev.getY(mActivePointerIndex);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            final int action = ev.getAction();
            switch (action & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    mActivePointerId = ev.getPointerId(0);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    mActivePointerId = INVALID_POINTER_ID;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                            >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                    final int pointerId = ev.getPointerId(pointerIndex);
                    if (pointerId == mActivePointerId) {
                        // This was our active pointer going up. Choose a new
                        // active pointer and adjust accordingly.
                        final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                        mActivePointerId = ev.getPointerId(newPointerIndex);
                        mLastTouchX = ev.getX(newPointerIndex);
                        mLastTouchY = ev.getY(newPointerIndex);
                    }
                    break;
            }

            mActivePointerIndex = ev.findPointerIndex(mActivePointerId);
            return super.onTouchEvent(ev);
        }
    }

    private static class FroyoDetector extends EclairDetector {
        private ScaleGestureDetector mDetector;

        public FroyoDetector(Context context) {
            mDetector = new ScaleGestureDetector(context,
                    new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                        @Override public boolean onScale(ScaleGestureDetector detector) {
                            mListener.onScale(detector.getScaleFactor());
                            return true;
                        }
                    });
        }

        @Override
        boolean shouldDrag() {
            return !mDetector.isInProgress();
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            mDetector.onTouchEvent(ev);
            return super.onTouchEvent(ev);
        }
    }
}

class TouchExampleView extends View {
    private Drawable mIcon;
    private float mPosX;
    private float mPosY;

    private VersionedGestureDetector mDetector;
    private float mScaleFactor = 1.f;

    public TouchExampleView(Context context) {
        this(context, null, 0);
    }

    public TouchExampleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchExampleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_launcher_background, null);
        mIcon.setBounds(0, 0, mIcon.getIntrinsicWidth(), mIcon.getIntrinsicHeight());

        mDetector = VersionedGestureDetector.newInstance(context, new GestureCallback());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mDetector.onTouchEvent(ev);
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(mPosX, mPosY);
        canvas.scale(mScaleFactor, mScaleFactor);
        mIcon.draw(canvas);
        canvas.restore();
    }

    private class GestureCallback implements VersionedGestureDetector.OnGestureListener {
        public void onDrag(float dx, float dy) {
            mPosX += dx;
            mPosY += dy;
            invalidate();
        }

        public void onScale(float scaleFactor) {
            mScaleFactor *= scaleFactor;

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

            invalidate();
        }
    }
}


public class MainActivity extends AppCompatActivity{


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TouchExampleView view = new TouchExampleView(this);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(view);
    }
}
