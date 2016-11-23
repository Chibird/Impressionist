package com.chibird.impressionist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int _alpha = 250;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Circle;
    private float _minBrushRadius = 5;
    private int radius = 25;
    private VelocityTracker mVelocityTracker = null;

    // PaintPoint class from Jon Froehlich
    private class PaintPoint {
        private Paint _paint = new Paint();
        private PointF _point;
        private float _brushRadius;

        public PaintPoint(float x, float y, float brushRadius, Paint paintSrc){
            // Copy the fields from paintSrc into this paint
            _paint.set(paintSrc);
            _point = new PointF(x, y);
            _brushRadius = brushRadius;
        }

        public Paint getPaint(){
            return _paint;
        }

        public float getX(){
            return _point.x;
        }

        public float getY(){
            return _point.y;
        }

        public float getBrushRadius(){
            return _brushRadius;
        }
    }

    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    public Bitmap getBitmap(){
        return _offScreenBitmap;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        if(_offScreenCanvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
        }
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);

    }

    /**
     * Sets blocks of base colors before you start painting to help you fill in the colors without
     * whitespace.
     */
    public void setBaseColors() {
        Rect bitmapRect = getBitmapPositionInsideImageView(_imageView);
        BitmapDrawable bitmapD = (BitmapDrawable)_imageView.getDrawable();
        if (bitmapD != null) {
            Bitmap bitmap = bitmapD.getBitmap();
            float[] f = new float[9];
            _imageView.getImageMatrix().getValues(f);

            // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
            final float scaleX = f[Matrix.MSCALE_X];
            final float scaleY = f[Matrix.MSCALE_Y];
            for (int i = bitmapRect.left; i < bitmapRect.left+bitmapRect.width(); i += 75) {
                for (int j = bitmapRect.top; j < bitmapRect.top+bitmapRect.height(); j += 75) {
                    int color = bitmap.getPixel(i,j);
                    Paint currPaint = new Paint();
                    currPaint.setColor(color);

                    currPaint.setAlpha(175);
                    float brushRadius = 75;
                    _offScreenCanvas.drawRect(i,j, i+brushRadius, j+brushRadius, currPaint);

                }
            }
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){

        //Basically, the way this works is to listen for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location

        float x = motionEvent.getX();
        float y = motionEvent.getY();
        float brushRadius = radius;

        BitmapDrawable bitmapD = (BitmapDrawable)_imageView.getDrawable();
        if (bitmapD != null) {
            Bitmap bitmap = bitmapD.getBitmap();

            int index = motionEvent.getActionIndex();
            int pointerId = motionEvent.getPointerId(index);
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if(mVelocityTracker == null) {
                        // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                        mVelocityTracker = VelocityTracker.obtain();
                    }
                    else {
                        // Reset the velocity tracker back to its initial state.
                        mVelocityTracker.clear();
                    }
                    // Add a user's movement to the tracker.
                    mVelocityTracker.addMovement(motionEvent);
                    break;
                case MotionEvent.ACTION_MOVE:
                    mVelocityTracker.addMovement(motionEvent);
                    // When you want to determine the velocity, call
                    // computeCurrentVelocity(). Then call getXVelocity()
                    // and getYVelocity() to retrieve the velocity for each pointer ID.
                    mVelocityTracker.computeCurrentVelocity(1000);
                    float xVel = VelocityTrackerCompat.getXVelocity(mVelocityTracker,
                            pointerId);
                    float yVel = VelocityTrackerCompat.getYVelocity(mVelocityTracker,
                            pointerId);

                    // Check if touch inside bitmap rectangle
                    Rect bitmapRect = getBitmapPositionInsideImageView(_imageView);
                    if (bitmapRect.contains((int)x,(int)y)){
                        float[] f = new float[9];
                        _imageView.getImageMatrix().getValues(f);

                        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
                        final float scaleX = f[Matrix.MSCALE_X];
                        final float scaleY = f[Matrix.MSCALE_Y];

                        int color = bitmap.getPixel((int) ((x - bitmapRect.left)/scaleX), (int) ((y - bitmapRect.top)/scaleY));
                        Paint currPaint = new Paint();
                        currPaint.setColor(color);
                        currPaint.setAlpha(120);
                        int historySize = motionEvent.getHistorySize();
                        for (int i = 0; i < historySize; i++) {
                            // Random numbers for brushes
                            Random r1 = new Random();
                            int spl1 = r1.nextInt(30);
                            Random r2 = new Random();
                            int spl2 = r2.nextInt(30);
                            Random r3 = new Random();
                            int spl3 = r3.nextInt(50);
                            Random r4 = new Random();
                            int spl4 = r4.nextInt(60);
                            float touchX = motionEvent.getHistoricalX(i);
                            float touchY = motionEvent.getHistoricalY(i);

                            // Draw to the offscreen bitmap for historical x,y points
                            // Insert one line of code here
                            switch(_brushType) {
                                case Circle:
                                    currPaint.setAlpha(175);
                                    _offScreenCanvas.drawCircle(touchX, touchY, brushRadius, currPaint);
                                    break;
                                case Square:
                                    currPaint.setAlpha(150);
                                    brushRadius = 10 + (xVel + yVel)/100;
                                    _offScreenCanvas.drawRect(touchX - brushRadius, touchY - brushRadius, touchX + brushRadius, touchY + brushRadius, currPaint);
                                case Line:
                                    break;
                                case CircleSplatter:
                                    _offScreenCanvas.drawCircle(touchX, touchY, brushRadius, currPaint);
                                    _offScreenCanvas.drawCircle(touchX+spl1, touchY+spl1, 20, currPaint);
                                    _offScreenCanvas.drawCircle(Math.max(touchX-spl2,0), Math.max(touchY-spl2,0), 10, currPaint);
                                    _offScreenCanvas.drawCircle(Math.max(touchX-spl3,0), touchY+spl3, 10-spl4/7, currPaint);
                                    _offScreenCanvas.drawCircle(touchX+spl4, Math.max(touchY-spl4,0), 10-spl4/7, currPaint);
                                    break;
                                case LineSplatter:
                                    brushRadius = 35;
                                    _offScreenCanvas.drawCircle(touchX, touchY, brushRadius, currPaint);
                                    _offScreenCanvas.drawCircle(touchX+spl1, Math.max(touchY-spl2,0), brushRadius-10, currPaint);
                                    _offScreenCanvas.drawCircle(Math.max(touchX-spl1,0), touchY+spl2, brushRadius-8, currPaint);
                                    break;
                            }
                        }

                        // Random numbers for brushes
                        Random r1 = new Random();
                        int spl1 = r1.nextInt(30);
                        Random r2 = new Random();
                        int spl2 = r2.nextInt(30);
                        Random r3 = new Random();
                        int spl3 = r3.nextInt(50);
                        Random r4 = new Random();
                        int spl4 = r4.nextInt(60);
                        switch(_brushType) {
                            case Circle:
                                currPaint.setAlpha(175);
                                _offScreenCanvas.drawCircle(x, y, brushRadius, currPaint);
                                break;
                            case Square:
                                currPaint.setAlpha(150);
                                brushRadius = 10 + (xVel + yVel)/100;
                                _offScreenCanvas.drawRect(x - brushRadius, y -brushRadius, x + brushRadius, y + brushRadius, currPaint);                            case Line:
                                break;
                            case CircleSplatter:
                                _offScreenCanvas.drawCircle(x, y, brushRadius, currPaint);
                                _offScreenCanvas.drawCircle(x+spl1, y+spl1, 20, currPaint);
                                _offScreenCanvas.drawCircle(Math.max(x-spl2,0), Math.max(y-spl2,0), 10, currPaint);
                                _offScreenCanvas.drawCircle(Math.max(x-spl3,0), y+spl3, 10-spl4/7, currPaint);
                                _offScreenCanvas.drawCircle(x+spl4, Math.max(y-spl4,0), 10-spl4/7, currPaint);
                                break;
                            case LineSplatter:
                                brushRadius = 35;
                                _offScreenCanvas.drawCircle(x, y, brushRadius+spl1, currPaint);
                                _offScreenCanvas.drawCircle(x+spl1, Math.max(y-spl2,0), brushRadius-10, currPaint);
                                _offScreenCanvas.drawCircle(Math.max(x-spl1,0), y+spl2, brushRadius-8, currPaint);
                                break;
                        }

                        invalidate();
                    }

                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
        }
        return true;
    }




    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();

        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

