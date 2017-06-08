package com.bec.scratchcard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * 刮刮卡View
 * Created by ZGP on 2017/6/7.
 */

public class ScratchCardView extends View {

    private Bitmap mBitmap, mBackBitmap;
    private Paint mPaint, mBackPint;
    private Path mPath;
    private int mLastX;
    private int mLastY;
    private boolean isComplete;
    private Xfermode mXfermode;
    /**
     * 绘制线条的Paint,即用户手指绘制Path
     */
    private Paint mOutterPaint;
    private Rect mTextBound;
    /**
     * 内存中创建的Canvas
     */
    private Canvas mCanvas;
    private String mText = "一等奖";
    private CountThread mCountThread;

    public ScratchCardView(Context context) {
        super(context);
        init();
    }

    public ScratchCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        mCountThread = new CountThread();
        mXfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
        mPaint = new Paint();
        //路径记录滑动屏幕的路径。
        mPath = new Path();


        mTextBound = new Rect();

        mBackBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.wallpaper);

        //设置刮奖区样式
        mBackPint = new Paint();
        mBackPint.setColor(Color.RED);
        mBackPint.setStrokeWidth(40);
        mBackPint.setStyle(Paint.Style.FILL);
        mBackPint.setTextScaleX(5f);
        mBackPint.setColor(Color.RED);
        mBackPint.setTextSize(30);
        mBackPint.getTextBounds(mText, 0, mText.length(), mTextBound);


        // 设置画笔
        mOutterPaint = new Paint();
        mOutterPaint.setColor(Color.RED);
        mOutterPaint.setAntiAlias(true);
        mOutterPaint.setDither(true);
        mOutterPaint.setStyle(Paint.Style.STROKE);
        mOutterPaint.setStrokeJoin(Paint.Join.ROUND); // 圆角
        mOutterPaint.setStrokeCap(Paint.Cap.ROUND); // 圆角
        // 设置画笔宽度
        mOutterPaint.setStrokeWidth(20);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 初始化bitmap
        if (mBitmap == null) {
            int width = getMeasuredWidth();
            int height = getMeasuredHeight();
            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mCanvas.drawColor(Color.parseColor("#c0c0c0"));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        canvas.drawText(mText, getWidth() / 2 - mTextBound.width() / 2,
                getHeight() / 2 + mTextBound.height() / 2, mBackPint);
        if (!isComplete) {
            mOutterPaint.setXfermode(mXfermode);
            mCanvas.drawPath(mPath, mOutterPaint);
            canvas.drawBitmap(mBitmap, 0, 0, null);
        }

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mCountThread != null && mCountThread.isAlive()) {
            isComplete = true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getX();
        int y = (int) event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastX = x;
                mLastY = y;
                mPath.moveTo(mLastX, mLastY);
                break;
            case MotionEvent.ACTION_MOVE:
                int dx = Math.abs(x - mLastX);
                int dy = Math.abs(y - mLastY);
                if (dx > 3 || dy > 3) {
                    mPath.lineTo(x, y);
                }
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_UP:
//                new Thread(mRunnable).start();
                if (!mCountThread.isAlive()&&!isComplete) {
                    mCountThread.start();
                }
                break;
        }
        invalidate();
        return true;
    }

    class CountThread extends Thread {
        private int[] mPixels;

        @Override
        public void run() {
            while (!isComplete) {
                int w = getWidth();
                int h = getHeight();

                float wipeArea = 0;
                float totalArea = w * h;

                Bitmap bitmap = mBitmap;

                mPixels = new int[w * h];

                /**
                 * 拿到所有的像素信息
                 */
                bitmap.getPixels(mPixels, 0, w, 0, 0, w, h);

                /**
                 * 遍历统计擦除的区域
                 */
                for (int i = 0; i < w; i++) {
                    for (int j = 0; j < h; j++) {
                        int index = i + j * w;
                        if (mPixels[index] == 0) {
                            wipeArea++;
                        }
                    }
                }

                /**
                 * 根据所占百分比，进行一些操作
                 */
                if (wipeArea > 0 && totalArea > 0) {
                    int percent = (int) (wipeArea * 100 / totalArea);
                    if (percent > 70) {
                        isComplete = true;
                        postInvalidate();
                    }
                }
            }
        }
    }

}
