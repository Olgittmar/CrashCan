/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.olgitt.olgitt.crashcan;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;



public class LookActivity extends Activity {

    private MyGLSurfaceView mSurfaceView;

    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_look);

        //init surfaceView
        mSurfaceView = findViewById(R.id.myGLSurfaceView);

        mSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}



