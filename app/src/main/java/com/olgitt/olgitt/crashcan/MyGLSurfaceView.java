package com.olgitt.olgitt.crashcan;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.ArrayList;

public class MyGLSurfaceView extends GLSurfaceView {

    private final myRenderer mRenderer;

    public MyGLSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);
        setEGLContextClientVersion(3);

        mRenderer = new myRenderer(this.getContext());

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(mRenderer);
        requestRender();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e){
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();
        float screenWidth = getWidth();
        float screenHeight = getHeight();
        float[] lookAtM = new float[16];


        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:

                float sceneX = (x/screenWidth)*2.0f - 1;
                float sceneY = (y/screenHeight)*(-2.0f) + 1;
                Matrix.setLookAtM(lookAtM,
                        0,
                        0,0,0,
                        sceneX, sceneY, 1,
                        0,1,0);

                //mRenderer.setLookAtM(lookAtM);
                requestRender();
        }

        return true;

    }
}
