/*
 * Life.java
 * 
 * Copyright 2012 Mark Einon <mark.einon@gmail.com>
 *
 * This file is part of LivingWallpaper.
 *
 * LivingWalpaper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LivingWallpaper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LivingWallpaper. If not, see <http://www.gnu.org/licenses/>.
 */

package uk.co.einon.lifewallpaper;

import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import java.util.concurrent.locks.*;

/*
 * This animated wallpaper fills the screen with Conway's Life
 */
public class Life extends WallpaperService {

    public static final String SHARED_PREFS_NAME = "LifeSettings";

    public void onCreate() {
        super.onCreate();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public Engine onCreateEngine() {
        return new LifeEngine();
    }

    class LifeEngine extends Engine implements
            SharedPreferences.OnSharedPreferenceChangeListener {
        private final Handler mHandler = new Handler();

        private final Paint mPaint = new Paint();
        private int mWidth = -1;
        private int mHeight = -1;
        private byte[][] mGrid;
        private int mIteration = 0;
        private int mZoom = 4;
        private boolean mScalingChanged = true;
        private int mTimeout = 60;
        private int mSpeed = 35;
        private int mBackground = 0x95000000;
        private boolean mHasCells = false;
        private boolean mFgColourChange = false;
        private boolean mBgColourChange = false;
        private Lock    mGridLock = new ReentrantLock();

        private final Runnable mCycleLife = new Runnable() {
            public void run() {
                drawLife();
            }
        };
        private boolean mVisible;
        private SharedPreferences mPrefs;

        LifeEngine() {
            mPrefs = Life.this.getSharedPreferences(SHARED_PREFS_NAME, 0);

            // Create a Paint to draw the lines for our cube
            final Paint paint = mPaint;
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(false);
            paint.setDither(false);
            
            mPrefs.registerOnSharedPreferenceChangeListener(this);
            onSharedPreferenceChanged(mPrefs, null);
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs,
                String key) {
            final Paint paint = mPaint;

            mFgColourChange = false; // Reset - false for everything except 'change' 
            mBgColourChange = false;
            
            // Set the colour
            String colour = prefs.getString("life_colour", "change");

            // TODO - put these values into an xml file?
            if (colour.equals("red")) {
                paint.setColor(0xffff1111);
            }
            if (colour.equals("green")) {
                paint.setColor(0xff11ff44);
            }
            if (colour.equals("grey")) {
                paint.setColor(0xffaaaaaa);
            }
            if (colour.equals("blue")) {
                paint.setColor(0xff1111aa);
            }
            if (colour.equals("white")) {
                paint.setColor(0xffffffff);
            }
            if (colour.equals("pink")) {
                paint.setColor(0xfffc80a5);
            }
            if (colour.equals("black")) {
                paint.setColor(0xff000000);
            }
            if (colour.equals("change")) {
                paint.setColor(0xff000000);
                mFgColourChange = true;
            }

            // Set the scaling
            String zoom = prefs.getString("life_zoom", "16");
            mZoom = Integer.parseInt(zoom);
            paint.setStrokeWidth(mZoom);
            mScalingChanged = true;

            // Set the timeout
            String timeout = prefs.getString("life_timeout", "100");
            mTimeout = Integer.parseInt(timeout);
            mIteration = 0;

            // Set the speed
            String speed = prefs.getString("life_speed", "35");
            mSpeed = Integer.parseInt(speed);
            
            // Set the background
            String background = prefs.getString("background_colour", "black");

            if (background.equals("red")) {
                mBackground = 0x95880909;
            }
            if (background.equals("green")) {
                mBackground = 0x95098821;
            }
            if (background.equals("grey")) {
                mBackground = 0x95555555;
            }
            if (background.equals("blue")) {
                mBackground = 0x95090955;
            }
            if (background.equals("white")) {
                mBackground = 0x95888888;
            }
            if (background.equals("pink")) {
                mBackground = 0x95864053;
            }
            if (background.equals("black")) {
                mBackground = 0x95000000;
            }
            if (background.equals("change")) {
                mBackground = 0x95000000;
                mBgColourChange = true;
            }
            
            // Set the cell shape
            String shape = prefs.getString("cell_shape", "Circle");
            if(shape.equals("Circle")) {
                paint.setStrokeCap(Paint.Cap.ROUND);
            }
            else {
                paint.setStrokeCap(Paint.Cap.SQUARE);
            }
        }

        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            // By default we don't get touch events, so enable them.
            setTouchEventsEnabled(true);
        }

        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mCycleLife);
        }

        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                drawLife();
            } else {
                mHandler.removeCallbacks(mCycleLife);
            }
        }

        public void onSurfaceChanged(SurfaceHolder holder, int format,
                int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
        }

        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mVisible = false;
            mHandler.removeCallbacks(mCycleLife);
        }

        /*
         * Store the position of the touch event so we can use it for drawing
         * later
         */
        @Override
        public void onTouchEvent(MotionEvent event) {
            int x = (int) event.getX() / mZoom;
            int y = (int) event.getY() / mZoom;

            if ((x < 3) || (y < 3) || (x > mWidth - 1) || (y > mHeight - 1))
                return;

            byte value = 3;

            mGridLock.lock();
            mGrid[x][y] = 
            mGrid[x-1][y-1] =
            mGrid[x-1][y-2] =
            mGrid[x+1][y+1] =
            mGrid[x+1][y-1] = value;
            mGridLock.unlock();

            mIteration = 0;
            mHasCells = true;

            if (mFgColourChange || mBgColourChange) {
                float g = 0;
                float b = 0;
                
                if (mWidth > 0) 
                    g = 255 - (x*255)/mWidth;

                if (mHeight > 0)
                    b = (y*255)/mHeight;

                // to stop strange edge effects
                if (g > 255)
                    g = 255;
                
                if (b > 255) 
                    b = 255;
                
                if(mFgColourChange)
                    mPaint.setColor((((int)g << 8) & 0x0000ff00) | (((int)b) & 0x000000ff) | 0xff000000);
                
                if(mBgColourChange)
                    mBackground = (((((int)g/2) << 8) & 0x0000ff00) | (((int)b/2) & 0x000000ff) | 0x95000000);
                
            }
            super.onTouchEvent(event);
        }

        void drawLife() {
            final SurfaceHolder holder = getSurfaceHolder();

            mGridLock.lock();

            if (mScalingChanged) {
                mWidth = getDesiredMinimumWidth() / mZoom;
                mHeight = getDesiredMinimumHeight() / mZoom;
                mGrid = new byte[mWidth + 1][mHeight + 1];
                mScalingChanged = false;
            }

            Canvas c = null;
            try {
                c = holder.lockCanvas();

                if (c != null)
                    lifeCycle(c); // draw something

            } finally {
                if (c != null)
                    holder.unlockCanvasAndPost(c);
            }

            // Reschedule the next redraw
            mHandler.removeCallbacks(mCycleLife);
            if (mVisible)
                mHandler.postDelayed(mCycleLife, 1000 / mSpeed);
        }

        void lifeCycle(Canvas c) {
            int neighbours = 0;

            if (!mHasCells) {
                c.drawColor(mBackground | 0xff000000); // no alpha, to completely clear screen
                mGridLock.unlock();
                return;
            }

            if (mTimeout != 0) {
                mIteration++;

                if(mFgColourChange || mBgColourChange) {
                    float r = (mIteration*255)/mTimeout;

                    if(mFgColourChange) 
                        mPaint.setColor((mPaint.getColor() & 0xff00ffff) | ((((int)r) << 16) & 0x00ff0000));
                    
                    if(mBgColourChange)
                        mBackground = ((mBackground & 0x9500ffff) | ((((int)r/2) << 16) & 0x00ff0000));
                }
            }
                

            if (mIteration > mTimeout) {
                mIteration = 0;
                mHasCells = false;
                for (int xx = 1; xx < mWidth; xx++) {
                    for (int yy = 1; yy < mHeight; yy++) {
                        mGrid[xx][yy] = 0;
                    }
                }
                mGridLock.unlock();
                return;
            }
            // Clear screen 
            c.drawColor(mBackground);

            for (int xx = 1; xx < (mWidth - 1); xx++) {
                for (int yy = 1; yy < (mHeight - 1); yy++) {
                    // update cells
                    neighbours = mGrid[xx - 1][yy + 1] + mGrid[xx][yy + 1] + mGrid[xx + 1][yy + 1] 
                               + mGrid[xx - 1][yy]     + /* [xx][yy]  + */   mGrid[xx + 1][yy]
                               + mGrid[xx - 1][yy - 1] + mGrid[xx][yy - 1] + mGrid[xx + 1][yy - 1];

                    // Draw cells
                    if (mGrid[xx][yy] == 0) {
                        if (neighbours == 3)
                            mGrid[xx][yy] = 1; // Grow cell
                    } else {
                        c.drawPoint(xx * mZoom, yy * mZoom, mPaint);
                        if ((neighbours != 2) && (neighbours != 3))
                            mGrid[xx][yy] -= 1; // Kill cell
                    }
                }
            }
            mGridLock.unlock();
        } // LifeCycle
    } // Class LifeEngine
} // Class Life
