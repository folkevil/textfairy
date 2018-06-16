/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.googlecode.leptonica.android;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Image input and output methods.
 *
 * @author alanv@google.com (Alan Viverette)
 */
public class ReadFile {

    private static final String LOG_TAG = ReadFile.class.getSimpleName();

    /**
     * Creates a 32bpp Pix object from encoded data. Supported formats are BMP
     * and JPEG.
     *
     * @param encodedData JPEG or BMP encoded byte data.
     * @return a 32bpp Pix object
     */
    public static Pix readMem(byte[] encodedData, Context ctx) {
        if (encodedData == null) {
            return null;
        }
        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

        final Bitmap bmp = BitmapFactory.decodeByteArray(encodedData, 0, encodedData.length, opts);
        final Pix pix = readBitmap(bmp);
        bmp.recycle();
        return pix;

    }

    /**
     * Creates an 8bpp Pix object from raw 8bpp grayscale pixels.
     *
     * @param pixelData 8bpp grayscale pixel data.
     * @param width     The width of the input image.
     * @param height    The height of the input image.
     * @return an 8bpp Pix object
     */
    public static Pix readBytes8(byte[] pixelData, int width, int height) {
        if (pixelData == null)
            throw new IllegalArgumentException("Byte array must be non-null");
        if (width <= 0)
            throw new IllegalArgumentException("Image width must be greater than 0");
        if (height <= 0)
            throw new IllegalArgumentException("Image height must be greater than 0");
        if (pixelData.length < width * height)
            throw new IllegalArgumentException("Array length does not match dimensions");

        long nativePix = nativeReadBytes8(pixelData, width, height);

        if (nativePix == 0)
            throw new RuntimeException("Failed to read pix from memory");

        return new Pix(nativePix);
    }

    /**
     * Replaces the bytes in an 8bpp Pix object with raw grayscale 8bpp pixels.
     * Width and height be identical to the input Pix.
     *
     * @param pixs      The Pix whose bytes will be replaced.
     * @param pixelData 8bpp grayscale pixel data.
     * @param width     The width of the input image.
     * @param height    The height of the input image.
     * @return an 8bpp Pix object
     */
    public static boolean replaceBytes8(Pix pixs, byte[] pixelData, int width, int height) {
        if (pixs == null)
            throw new IllegalArgumentException("Source pix must be non-null");
        if (pixelData == null)
            throw new IllegalArgumentException("Byte array must be non-null");
        if (width <= 0)
            throw new IllegalArgumentException("Image width must be greater than 0");
        if (height <= 0)
            throw new IllegalArgumentException("Image height must be greater than 0");
        if (pixelData.length < width * height)
            throw new IllegalArgumentException("Array length does not match dimensions");
        if (pixs.getWidth() != width)
            throw new IllegalArgumentException("Source pix width does not match image width");
        if (pixs.getHeight() != height)
            throw new IllegalArgumentException("Source pix width does not match image width");

        return nativeReplaceBytes8(pixs.mNativePix, pixelData, width, height);
    }

    public static Pix readFile(File file) {
        return readFile(null, file);
    }


    /**
     * Creates a Pix object from encoded file data. Supported formats are BMP
     * and JPEG.
     *
     * @param file The JPEG or BMP-encoded file to read in as a Pix.
     * @return a Pix object
     */
    public static Pix readFile(Context context, File file) {
        if (file == null) {
            Log.w(LOG_TAG, "File must be non-null");
            return null;
        }
        if (!file.exists()) {
            Log.w(LOG_TAG, "File does not exist");
            return null;
        }
        if (!file.canRead()) {
            Log.w(LOG_TAG, "Cannot read file");
            return null;
        }

        return loadWithPicasso(context, file);
    }

    public static Pix loadWithPicasso(Context context, File file) {
        try {
            final Bitmap bmp = Picasso.with(context).load(file).memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).get();
            if (bmp != null) {
                final Pix pix = readBitmap(bmp);
                bmp.recycle();
                return pix;
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    public static Pix loadWithPicasso(Context context, Uri uri) {
        try {
            final Bitmap bmp = Picasso.with(context).load(uri).memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE).get();
            if (bmp != null) {
                final Pix pix = readBitmap(bmp);
                bmp.recycle();
                return pix;
            }
        } catch (IOException ignored) {
        }
        return null;
    }


    /**
     * Creates a Pix object from Bitmap data. Currently supports only
     * ARGB_8888-formatted bitmaps.
     *
     * @param bmp The Bitmap object to convert to a Pix.
     * @return a Pix object
     */
    public static Pix readBitmap(Bitmap bmp) {
        if (bmp == null) {
            Log.w(LOG_TAG, "Bitmap must be non-null");
            return null;
        }
        if (bmp.getConfig() != Bitmap.Config.ARGB_8888) {
            Log.w(LOG_TAG, "Bitmap config must be ARGB_8888");
            return null;
        }


        long nativePix = nativeReadBitmap(bmp);

        if (nativePix == 0) {
            Log.w(LOG_TAG, "Failed to read pix from bitmap");
            return null;
        }

        return new Pix(nativePix);
    }

    // ***************
    // * NATIVE CODE *
    // ***************

    private static native long nativeReadMem(byte[] data, int size);

    private static native long nativeReadBytes8(byte[] data, int w, int h);

    private static native boolean nativeReplaceBytes8(long nativePix, byte[] data, int w, int h);

    private static native long nativeReadFiles(String dirname, String prefix);

    private static native long nativeReadFile(String filename);

    private static native long nativeReadBitmap(Bitmap bitmap);
}
