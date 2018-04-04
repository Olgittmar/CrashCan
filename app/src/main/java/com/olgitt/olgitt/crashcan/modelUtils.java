package com.olgitt.olgitt.crashcan;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.mokiat.data.front.parser.IOBJParser;
import com.mokiat.data.front.parser.OBJModel;
import com.mokiat.data.front.parser.OBJParser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.MessageFormat;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by olgitt on 2018-03-06.
 */

public final class modelUtils {

    private modelUtils() {
        throw new IllegalStateException("No instances.");
    }

    public static OBJModel LoadModel( final String filename, final Context context) {
        // make use of final to protect your context
        // do stuff w/ context and height here        Log.d("debug", "in LoadModel");
        OBJModel model = new OBJModel();
        AssetManager assetManager = context.getAssets();
        InputStream in = null;

        try {
            in = assetManager.open(filename);
            if (in != null) {
                Log.d("debug", "" + in.available());
                // Create an OBJParser and parse the resource
                final IOBJParser parser = new OBJParser();
                model = parser.parse(in);

                Log.d("debug", "In LoadModel, this is inside the try statement");
                // Use the model representation to get some basic info
                Log.d("debug", MessageFormat.format(
                        "OBJ model has {0} vertices, {1} normals, {2} texture coordinates, and {3} objects.",
                        model.getVertices().size(),
                        model.getNormals().size(),
                        model.getTexCoords().size(),
                        model.getObjects().size()));
            }
        } catch (java.io.FileNotFoundException e){
            Log.e("error", "FileNotFound:" + e.getMessage());

        } catch (java.io.IOException e){
            Log.e("error","IOException" + e.getMessage());
        }
        return model;
    }

    public static String loadShaderString(String filename, Context context){

        String str = new String();

        try {
             InputStream shaderStream = context.getAssets().open(filename);
             str = IOUtils.toString(shaderStream, UTF_8);
            } catch (IOException e){
            Log.e("debug", e.getMessage());
        }
        return str;
    }

}
