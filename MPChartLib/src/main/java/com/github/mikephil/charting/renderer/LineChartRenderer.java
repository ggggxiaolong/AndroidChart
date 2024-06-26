package com.github.mikephil.charting.renderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.github.mikephil.charting.animation.ChartAnimator;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.MultiFiledData;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.MPPointD;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Transformer;
import com.github.mikephil.charting.utils.Utils;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class LineChartRenderer extends LineRadarRenderer {

    protected LineDataProvider mChart;

    /**
     * paint for the inner circle of the value indicators
     */
    protected Paint mCirclePaintInner;

    /**
     * Bitmap object used for drawing the paths (otherwise they are too long if
     * rendered directly on the canvas)
     */
    protected WeakReference<Bitmap> mDrawBitmap;

    /**
     * on this canvas, the paths are rendered, it is initialized with the
     * pathBitmap
     */
    protected Canvas mBitmapCanvas;

    /**
     * the bitmap configuration to be used
     */
    protected Bitmap.Config mBitmapConfig = Bitmap.Config.ARGB_8888;

    protected Path cubicPath = new Path();
    protected Path cubicFillPath = new Path();

    protected Path cubicMultiFillPath = new Path();

    protected float[] mGetPositionBuffer = new float[2];

    public LineChartRenderer(LineDataProvider chart, ChartAnimator animator,
                             ViewPortHandler viewPortHandler) {
        super(animator, viewPortHandler);
        mChart = chart;

        mCirclePaintInner = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaintInner.setStyle(Paint.Style.FILL);
        mCirclePaintInner.setColor(Color.WHITE);
    }

    @Override
    public void initBuffers() {
    }

    @Override
    public void drawData(Canvas c) {

        int width = (int) mViewPortHandler.getChartWidth();
        int height = (int) mViewPortHandler.getChartHeight();

        Bitmap drawBitmap = mDrawBitmap == null ? null : mDrawBitmap.get();

        if (drawBitmap == null
                || (drawBitmap.getWidth() != width)
                || (drawBitmap.getHeight() != height)) {

            if (width > 0 && height > 0) {
                drawBitmap = Bitmap.createBitmap(width, height, mBitmapConfig);
                mDrawBitmap = new WeakReference<>(drawBitmap);
                mBitmapCanvas = new Canvas(drawBitmap);
            } else
                return;
        }

        drawBitmap.eraseColor(Color.TRANSPARENT);

        LineData lineData = mChart.getLineData();

        for (ILineDataSet set : lineData.getDataSets()) {

            if (set.isVisible())
                drawDataSet(c, set);
        }

        c.drawBitmap(drawBitmap, 0, 0, mRenderPaint);
    }

    protected void drawDataSet(Canvas c, ILineDataSet dataSet) {

        if (dataSet.getEntryCount() < 1)
            return;

        mRenderPaint.setStrokeWidth(dataSet.getLineWidth());
        mRenderPaint.setPathEffect(dataSet.getDashPathEffect());

        switch (dataSet.getMode()) {
            default:
            case LINEAR:
            case STEPPED:
                drawLinear(c, dataSet);
                break;

            case CUBIC_BEZIER:
                drawCubicBezier(dataSet);
                break;

            case HORIZONTAL_BEZIER:
                drawHorizontalBezier(dataSet);
                break;
        }

        mRenderPaint.setPathEffect(null);
    }

    protected void drawHorizontalBezier(ILineDataSet dataSet) {

        float phaseY = mAnimator.getPhaseY();

        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        mXBounds.set(mChart, dataSet);

        cubicPath.reset();

        if (mXBounds.range >= 1) {

            Entry prev = dataSet.getEntryForIndex(mXBounds.min);
            Entry cur = prev;

            // let the spline start
            cubicPath.moveTo(cur.getX(), cur.getY() * phaseY);

            for (int j = mXBounds.min + 1; j <= mXBounds.range + mXBounds.min; j++) {

                prev = cur;
                cur = dataSet.getEntryForIndex(j);

                final float cpx = (prev.getX())
                        + (cur.getX() - prev.getX()) / 2.0f;

                cubicPath.cubicTo(
                        cpx, prev.getY() * phaseY,
                        cpx, cur.getY() * phaseY,
                        cur.getX(), cur.getY() * phaseY);
            }
        }

        List<MultiFiledData> multiFiledDataList = dataSet.getMultiFiledDataList();
        if (dataSet.isDrawMultiFilledEnable() && multiFiledDataList != null) {
            applyValueTextStyle(dataSet);
            Iterator<MultiFiledData> var18 = multiFiledDataList.iterator();
            label54:
            while(true) {
                MultiFiledData data;
                do {
                    do {
                        do {
                            if (!var18.hasNext()) {
                                break label54;
                            }
                            data = (MultiFiledData)var18.next();
                        } while(data.range <= 1);
                    } while(data.endIndex <= this.mXBounds.min);
                } while(data.startIndex >= this.mXBounds.max);
                this.cubicMultiFillPath.reset();
                Entry firstPoint = dataSet.getEntryForIndex(data.startIndex);
                Entry endPoint = dataSet.getEntryForIndex(data.endIndex);
                Entry textPoint = firstPoint;
                float k = (endPoint.getY() - firstPoint.getY()) / (endPoint.getX() - firstPoint.getX());
                float b = firstPoint.getY() - firstPoint.getX() * k;
//                Log.i("multiFill", String.format("first: %s, second: %s, k: %f, b: %f", firstPoint.toString(), endPoint.toString(), k, b));
                int minIndex = Math.max(this.mXBounds.min, data.startIndex);
                int maxIndex = Math.min(this.mXBounds.max, data.endIndex);
                Entry prev = dataSet.getEntryForIndex(minIndex);
                Entry cur = prev;
                this.cubicMultiFillPath.moveTo(prev.getX(), prev.getY() * phaseY);

                for(int j = minIndex + 1; j <= maxIndex; ++j) {
                    prev = cur;
                    cur = dataSet.getEntryForIndex(j);
                    if (cur.getY() > textPoint.getY()){
                        textPoint = cur;
                    }
                    float cpx = prev.getX() + (cur.getX() - prev.getX()) / 2.0F;
                    this.cubicMultiFillPath.cubicTo(cpx, prev.getY() * phaseY, cpx, cur.getY() * phaseY, cur.getX(), cur.getY() * phaseY);
                }

                float x;
                if (this.mXBounds.max < data.endIndex) {
                    x = endPoint.getX();
                    this.cubicMultiFillPath.lineTo(x, (x * k + b) * phaseY);
                }

                if (this.mXBounds.min > data.startIndex) {
                    cur = dataSet.getEntryForIndex(this.mXBounds.min - 1);
                    x = cur.getX();
                    Log.i("multiFill", String.format("min: %d, startIndex: %d, x: %f,getY :%f, y: %f, phaseY: %f", this.mXBounds.min, data.startIndex, x, cur.getY(), (x * k + b) * phaseY, phaseY));
                    this.cubicMultiFillPath.lineTo(x, (x * k + b) * phaseY);
                }

                this.cubicMultiFillPath.close();
                this.drawCubicMultiFill(this.mBitmapCanvas, this.cubicMultiFillPath, trans, data, phaseY, textPoint);
            }
        } else if (dataSet.isDrawFilledEnabled()) {
            cubicFillPath.reset();
            cubicFillPath.addPath(cubicPath);
            // create a new path, this is bad for performance
            drawCubicFill(mBitmapCanvas, dataSet, cubicFillPath, trans, mXBounds);
        }

        mRenderPaint.setColor(dataSet.getColor());

        mRenderPaint.setStyle(Paint.Style.STROKE);

        trans.pathValueToPixel(cubicPath);

        mBitmapCanvas.drawPath(cubicPath, mRenderPaint);

        mRenderPaint.setPathEffect(null);
    }

    protected void drawCubicBezier(ILineDataSet dataSet) {

        float phaseY = mAnimator.getPhaseY();

        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        mXBounds.set(mChart, dataSet);

        float intensity = dataSet.getCubicIntensity();

        cubicPath.reset();
        float prevDx;
        Entry firstPoint;
        Entry endPoint;
        Entry textPoint;
        if (mXBounds.range >= 1) {

            prevDx = 0f;
            float prevDy = 0f;
            float curDx = 0f;
            float curDy = 0f;

            // Take an extra point from the left, and an extra from the right.
            // That's because we need 4 points for a cubic bezier (cubic=4), otherwise we get lines moving and doing weird stuff on the edges of the chart.
            // So in the starting `prev` and `cur`, go -2, -1
            // And in the `lastIndex`, add +1

            final int firstIndex = mXBounds.min + 1;
            final int lastIndex = mXBounds.min + mXBounds.range;

            Entry prevPrev;
            Entry prev = dataSet.getEntryForIndex(Math.max(firstIndex - 2, 0));
            Entry cur = dataSet.getEntryForIndex(Math.max(firstIndex - 1, 0));
            Entry next = cur;
            int nextIndex = -1;

            if (cur == null) return;

            // let the spline start
            cubicPath.moveTo(cur.getX(), cur.getY() * phaseY);

            for (int j = mXBounds.min + 1; j <= mXBounds.range + mXBounds.min; j++) {

                prevPrev = prev;
                prev = cur;
                cur = nextIndex == j ? next : dataSet.getEntryForIndex(j);

                nextIndex = j + 1 < dataSet.getEntryCount() ? j + 1 : j;
                next = dataSet.getEntryForIndex(nextIndex);

                prevDx = (cur.getX() - prevPrev.getX()) * intensity;
                prevDy = (cur.getY() - prevPrev.getY()) * intensity;
                curDx = (next.getX() - prev.getX()) * intensity;
                curDy = (next.getY() - prev.getY()) * intensity;

                cubicPath.cubicTo(prev.getX() + prevDx, (prev.getY() + prevDy) * phaseY,
                        cur.getX() - curDx,
                        (cur.getY() - curDy) * phaseY, cur.getX(), cur.getY() * phaseY);
            }
        }

        List<MultiFiledData> multiFiledDataList = dataSet.getMultiFiledDataList();
        if (dataSet.isDrawMultiFilledEnable() && multiFiledDataList != null) {
            applyValueTextStyle(dataSet);
            Iterator<MultiFiledData> var25 = multiFiledDataList.iterator();

            label82:
            while(true) {
                MultiFiledData data;
                do {
                    do {
                        do {
                            if (!var25.hasNext()) {
                                break label82;
                            }

                            data = (MultiFiledData)var25.next();
                        } while(data.range <= 1);
                    } while(data.endIndex <= this.mXBounds.min);
                } while(data.startIndex >= this.mXBounds.max);

                this.cubicMultiFillPath.reset();
                prevDx = 0.0F;
                float prevDy = 0.0F;
                float curDx = 0.0F;
                float curDy = 0.0F;
                firstPoint = dataSet.getEntryForIndex(data.startIndex);
                textPoint = firstPoint;
                endPoint = dataSet.getEntryForIndex(data.endIndex);
                float k = (endPoint.getY() - firstPoint.getY()) / (endPoint.getX() - firstPoint.getX());
                float b = firstPoint.getY() - firstPoint.getX() * k;
//                Log.i("multiFill", String.format("first: %s, second: %s, k: %f, b: %f", firstPoint.toString(), endPoint.toString(), k, b));
                int minIndex = Math.max(this.mXBounds.min, data.startIndex);
                int maxIndex = Math.min(this.mXBounds.max, data.endIndex);
                Entry prev = dataSet.getEntryForIndex(Math.max(minIndex - 1, 0));
                Entry cur = dataSet.getEntryForIndex(Math.max(minIndex, 0));
                Entry next = cur;
                int nextIndex = -1;
                if (cur == null) {
                    return;
                }

                this.cubicMultiFillPath.moveTo(cur.getX(), cur.getY() * phaseY);

                for(int j = minIndex + 1; j <= maxIndex; ++j) {
                    Entry prevPrev = prev;
                    prev = cur;
                    cur = nextIndex == j ? next : dataSet.getEntryForIndex(j);
                    if (cur.getY() > textPoint.getY()){
                        textPoint = cur;
                    }
                    nextIndex = j + 1 < dataSet.getEntryCount() ? j + 1 : j;
                    next = dataSet.getEntryForIndex(nextIndex);
                    prevDx = (cur.getX() - prevPrev.getX()) * intensity;
                    prevDy = (cur.getY() - prevPrev.getY()) * intensity;
                    curDx = (next.getX() - prev.getX()) * intensity;
                    curDy = (next.getY() - prev.getY()) * intensity;
                    this.cubicMultiFillPath.cubicTo(prev.getX() + prevDx, (prev.getY() + prevDy) * phaseY, cur.getX() - curDx, (cur.getY() - curDy) * phaseY, cur.getX(), cur.getY() * phaseY);
                }

                float x;
                if (this.mXBounds.max < data.endIndex) {
                    x = endPoint.getX();
                    this.cubicMultiFillPath.lineTo(x, (x * k + b) * phaseY);
                }

                if (this.mXBounds.min > data.startIndex) {
                    cur = dataSet.getEntryForIndex(this.mXBounds.min - 1);
                    x = cur.getX();
//                    Log.i("multiFill", String.format("min: %d, startIndex: %d, x: %f,getY :%f, y: %f, phaseY: %f", this.mXBounds.min, data.startIndex, x, cur.getY(), (x * k + b) * phaseY, phaseY));
                    this.cubicMultiFillPath.lineTo(x, (x * k + b) * phaseY);
                }

                this.cubicMultiFillPath.close();
                this.drawCubicMultiFill(this.mBitmapCanvas, this.cubicMultiFillPath, trans, data, phaseY, textPoint);
            }
        } else if (dataSet.isDrawFilledEnabled()) {

            cubicFillPath.reset();
            cubicFillPath.addPath(cubicPath);

            drawCubicFill(mBitmapCanvas, dataSet, cubicFillPath, trans, mXBounds);
        }

        mRenderPaint.setColor(dataSet.getColor());

        mRenderPaint.setStyle(Paint.Style.STROKE);

        trans.pathValueToPixel(cubicPath);

        mBitmapCanvas.drawPath(cubicPath, mRenderPaint);

        mRenderPaint.setPathEffect(null);
    }

    protected void drawCubicFill(Canvas c, ILineDataSet dataSet, Path spline, Transformer trans, XBounds bounds) {

        float fillMin = dataSet.getFillFormatter()
                .getFillLinePosition(dataSet, mChart);

        spline.lineTo(dataSet.getEntryForIndex(bounds.min + bounds.range).getX(), fillMin);
        spline.lineTo(dataSet.getEntryForIndex(bounds.min).getX(), fillMin);
        spline.close();

        trans.pathValueToPixel(spline);

        final Drawable drawable = dataSet.getFillDrawable();
        if (drawable != null) {

            drawFilledPath(c, spline, drawable);
        } else {

            drawFilledPath(c, spline, dataSet.getFillColor(), dataSet.getFillAlpha());
        }
    }

    private float[] mLineBuffer = new float[4];

    protected void drawCubicMultiFill(Canvas c, Path spline, Transformer trans, MultiFiledData filedData, float phaseY, Entry textPoint) {
        trans.pathValueToPixel(spline);
        Drawable drawable = filedData.fillDrawable;
        if (drawable != null) {
            this.drawFilledPath(c, spline, drawable);
        } else {
            this.drawFilledPath(c, spline, filedData.fillColor, filedData.fillAlpha);
        }
        mGetPositionBuffer[1] = textPoint.getY() * phaseY;
        mGetPositionBuffer[0] = textPoint.getX();
        trans.pointValuesToPixel(mGetPositionBuffer);
        this.drawText(this.mBitmapCanvas, filedData.name, mGetPositionBuffer[0] + filedData.nameOffsite.x, mGetPositionBuffer[1] - filedData.nameOffsite.y, filedData.fillColor);
    }

    /**
     * Draws a normal line.
     *
     * @param c
     * @param dataSet
     */
    protected void drawLinear(Canvas c, ILineDataSet dataSet) {

        int entryCount = dataSet.getEntryCount();

        final boolean isDrawSteppedEnabled = dataSet.isDrawSteppedEnabled();
        final int pointsPerEntryPair = isDrawSteppedEnabled ? 4 : 2;

        Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

        float phaseY = mAnimator.getPhaseY();

        mRenderPaint.setStyle(Paint.Style.STROKE);

        Canvas canvas = null;

        // if the data-set is dashed, draw on bitmap-canvas
        if (dataSet.isDashedLineEnabled()) {
            canvas = mBitmapCanvas;
        } else {
            canvas = c;
        }

        mXBounds.set(mChart, dataSet);

        List<MultiFiledData> multiFiledDataList = dataSet.getMultiFiledDataList();
        Entry endPoint;
        float k;
        float b;
        if (dataSet.isDrawMultiFilledEnable() && multiFiledDataList != null) {
            applyValueTextStyle(dataSet);
            Iterator<MultiFiledData> var10 = multiFiledDataList.iterator();
            label172:
            while(true) {
                MultiFiledData data;
                do {
                    do {
                        do {
                            if (!var10.hasNext()) {
                                break label172;
                            }

                            data = var10.next();
                        } while(data.range <= 1);
                    } while(data.endIndex <= this.mXBounds.min);
                } while(data.startIndex >= this.mXBounds.max);

                this.cubicMultiFillPath.reset();
                Entry firstPoint = dataSet.getEntryForIndex(data.startIndex);
                Entry textPoint = firstPoint;
                endPoint = dataSet.getEntryForIndex(data.endIndex);
                if (firstPoint == null || endPoint == null) {
                    return;
                }

                k = (endPoint.getY() - firstPoint.getY()) / (endPoint.getX() - firstPoint.getX());
                b = firstPoint.getY() - firstPoint.getX() * k;
//                Log.i("multiFill", String.format("first: %s, second: %s, k: %f, b: %f", firstPoint.toString(), endPoint.toString(), k, b));
                int minIndex = Math.max(this.mXBounds.min, data.startIndex);
                int maxIndex = Math.min(this.mXBounds.max, data.endIndex);
                Entry cur = dataSet.getEntryForIndex(minIndex);
                if (cur == null) {
                    return;
                }

                Entry prev;
                if (isDrawSteppedEnabled) {
                    prev = dataSet.getEntryForIndex(minIndex + 1);
                    this.cubicMultiFillPath.moveTo(prev.getX(), cur.getY() * phaseY);
                } else {
                    this.cubicMultiFillPath.moveTo(cur.getX(), cur.getY() * phaseY);
                }

                for(int j = minIndex + 1; j <= maxIndex; ++j) {
                    prev = cur;
                    cur = dataSet.getEntryForIndex(j);
                    if (isDrawSteppedEnabled) {
                        this.cubicMultiFillPath.lineTo(cur.getX(), prev.getY() * phaseY);
                    }
                    if (cur.getY() > textPoint.getY()){
                        textPoint = cur;
                    }
                    this.cubicMultiFillPath.lineTo(cur.getX(), cur.getY() * phaseY);
                }

                float x;
                if (this.mXBounds.max < data.endIndex) {
                    x = endPoint.getX();
                    this.cubicMultiFillPath.lineTo(x, (x * k + b) * phaseY);
                }

                if (this.mXBounds.min > data.startIndex) {
                    cur = dataSet.getEntryForIndex(this.mXBounds.min - 1);
                    x = cur.getX();
//                    Log.i("multiFill", String.format("min: %d, startIndex: %d, x: %f,getY :%f, y: %f, phaseY: %f", this.mXBounds.min, data.startIndex, x, cur.getY(), (x * k + b) * phaseY, phaseY));
                    if (isDrawSteppedEnabled) {
                        prev = dataSet.getEntryForIndex(this.mXBounds.min);
                        this.cubicMultiFillPath.lineTo(prev.getX(), (x * k + b) * phaseY);
                        this.cubicMultiFillPath.lineTo(prev.getX(), prev.getY() * phaseY);
                    } else {
                        this.cubicMultiFillPath.lineTo(x, (x * k + b) * phaseY);
                    }
                }

                this.cubicMultiFillPath.close();
                this.drawCubicMultiFill(this.mBitmapCanvas, this.cubicMultiFillPath, trans, data, phaseY, textPoint);
            }
        } else if (dataSet.isDrawFilledEnabled() && entryCount > 0) {
            drawLinearFill(c, dataSet, trans, mXBounds);
        }

        // more than 1 color
        if (dataSet.getColors().size() > 1) {

            int numberOfFloats = pointsPerEntryPair * 2;

            if (mLineBuffer.length <= numberOfFloats)
                mLineBuffer = new float[numberOfFloats * 2];

            int max = mXBounds.min + mXBounds.range;

            for (int j = mXBounds.min; j < max; j++) {

                Entry e = dataSet.getEntryForIndex(j);
                if (e == null) continue;

                mLineBuffer[0] = e.getX();
                mLineBuffer[1] = e.getY() * phaseY;

                if (j < mXBounds.max) {

                    e = dataSet.getEntryForIndex(j + 1);

                    if (e == null) break;

                    if (isDrawSteppedEnabled) {
                        mLineBuffer[2] = e.getX();
                        mLineBuffer[3] = mLineBuffer[1];
                        mLineBuffer[4] = mLineBuffer[2];
                        mLineBuffer[5] = mLineBuffer[3];
                        mLineBuffer[6] = e.getX();
                        mLineBuffer[7] = e.getY() * phaseY;
                    } else {
                        mLineBuffer[2] = e.getX();
                        mLineBuffer[3] = e.getY() * phaseY;
                    }

                } else {
                    mLineBuffer[2] = mLineBuffer[0];
                    mLineBuffer[3] = mLineBuffer[1];
                }

                // Determine the start and end coordinates of the line, and make sure they differ.
                float firstCoordinateX = mLineBuffer[0];
                float firstCoordinateY = mLineBuffer[1];
                float lastCoordinateX = mLineBuffer[numberOfFloats - 2];
                float lastCoordinateY = mLineBuffer[numberOfFloats - 1];

                if (firstCoordinateX == lastCoordinateX &&
                        firstCoordinateY == lastCoordinateY)
                    continue;

                trans.pointValuesToPixel(mLineBuffer);

                if (!mViewPortHandler.isInBoundsRight(firstCoordinateX))
                    break;

                // make sure the lines don't do shitty things outside
                // bounds
                if (!mViewPortHandler.isInBoundsLeft(lastCoordinateX) ||
                        !mViewPortHandler.isInBoundsTop(Math.max(firstCoordinateY, lastCoordinateY)) ||
                        !mViewPortHandler.isInBoundsBottom(Math.min(firstCoordinateY, lastCoordinateY)))
                    continue;

                // get the color that is set for this line-segment
                mRenderPaint.setColor(dataSet.getColor(j));

                canvas.drawLines(mLineBuffer, 0, pointsPerEntryPair * 2, mRenderPaint);
            }

        } else { // only one color per dataset

            if (mLineBuffer.length < Math.max((entryCount) * pointsPerEntryPair, pointsPerEntryPair) * 2)
                mLineBuffer = new float[Math.max((entryCount) * pointsPerEntryPair, pointsPerEntryPair) * 4];

            Entry e1, e2;

            e1 = dataSet.getEntryForIndex(mXBounds.min);

            if (e1 != null) {

                int j = 0;
                for (int x = mXBounds.min; x <= mXBounds.range + mXBounds.min; x++) {

                    e1 = dataSet.getEntryForIndex(x == 0 ? 0 : (x - 1));
                    e2 = dataSet.getEntryForIndex(x);

                    if (e1 == null || e2 == null) continue;

                    mLineBuffer[j++] = e1.getX();
                    mLineBuffer[j++] = e1.getY() * phaseY;

                    if (isDrawSteppedEnabled) {
                        mLineBuffer[j++] = e2.getX();
                        mLineBuffer[j++] = e1.getY() * phaseY;
                        mLineBuffer[j++] = e2.getX();
                        mLineBuffer[j++] = e1.getY() * phaseY;
                    }

                    mLineBuffer[j++] = e2.getX();
                    mLineBuffer[j++] = e2.getY() * phaseY;
                }

                if (j > 0) {
                    trans.pointValuesToPixel(mLineBuffer);

                    final int size = Math.max((mXBounds.range + 1) * pointsPerEntryPair, pointsPerEntryPair) * 2;

                    mRenderPaint.setColor(dataSet.getColor());

                    canvas.drawLines(mLineBuffer, 0, size, mRenderPaint);
                }
            }
        }

        mRenderPaint.setPathEffect(null);
    }

    protected Path mGenerateFilledPathBuffer = new Path();

    /**
     * Draws a filled linear path on the canvas.
     *
     * @param c
     * @param dataSet
     * @param trans
     * @param bounds
     */
    protected void drawLinearFill(Canvas c, ILineDataSet dataSet, Transformer trans, XBounds bounds) {

        final Path filled = mGenerateFilledPathBuffer;

        final int startingIndex = bounds.min;
        final int endingIndex = bounds.range + bounds.min;
        final int indexInterval = 128;

        int currentStartIndex = 0;
        int currentEndIndex = indexInterval;
        int iterations = 0;

        // Doing this iteratively in order to avoid OutOfMemory errors that can happen on large bounds sets.
        do {
            currentStartIndex = startingIndex + (iterations * indexInterval);
            currentEndIndex = currentStartIndex + indexInterval;
            currentEndIndex = Math.min(currentEndIndex, endingIndex);

            if (currentStartIndex <= currentEndIndex) {
                final Drawable drawable = dataSet.getFillDrawable();

                int startIndex = currentStartIndex;
                int endIndex = currentEndIndex;

                // Add a little extra to the path for drawables, larger data sets were showing space between adjacent drawables
                if (drawable != null) {

                    startIndex = Math.max(0, currentStartIndex - 1);
                    endIndex = Math.min(endingIndex, currentEndIndex + 1);
                }

                generateFilledPath(dataSet, startIndex, endIndex, filled);

                trans.pathValueToPixel(filled);

                if (drawable != null) {

                    drawFilledPath(c, filled, drawable);
                } else {

                    drawFilledPath(c, filled, dataSet.getFillColor(), dataSet.getFillAlpha());
                }
            }

            iterations++;

        } while (currentStartIndex <= currentEndIndex);

    }

    /**
     * Generates a path that is used for filled drawing.
     *
     * @param dataSet    The dataset from which to read the entries.
     * @param startIndex The index from which to start reading the dataset
     * @param endIndex   The index from which to stop reading the dataset
     * @param outputPath The path object that will be assigned the chart data.
     * @return
     */
    private void generateFilledPath(final ILineDataSet dataSet, final int startIndex, final int endIndex, final Path outputPath) {

        final float fillMin = dataSet.getFillFormatter().getFillLinePosition(dataSet, mChart);
        final float phaseY = mAnimator.getPhaseY();
        final boolean isDrawSteppedEnabled = dataSet.getMode() == LineDataSet.Mode.STEPPED;

        final Path filled = outputPath;
        filled.reset();

        final Entry entry = dataSet.getEntryForIndex(startIndex);

        filled.moveTo(entry.getX(), fillMin);
        filled.lineTo(entry.getX(), entry.getY() * phaseY);

        // create a new path
        Entry currentEntry = null;
        Entry previousEntry = entry;
        for (int x = startIndex + 1; x <= endIndex; x++) {

            currentEntry = dataSet.getEntryForIndex(x);

            if (currentEntry != null) {
                if (isDrawSteppedEnabled) {
                    filled.lineTo(currentEntry.getX(), previousEntry.getY() * phaseY);
                }

                filled.lineTo(currentEntry.getX(), currentEntry.getY() * phaseY);

                previousEntry = currentEntry;
            }
        }

        // close up
        if (currentEntry != null) {
            filled.lineTo(currentEntry.getX(), fillMin);
        }

        filled.close();
    }

    @Override
    public void drawValues(Canvas c) {

        if (isDrawingValuesAllowed(mChart)) {

            List<ILineDataSet> dataSets = mChart.getLineData().getDataSets();

            for (int i = 0; i < dataSets.size(); i++) {

                ILineDataSet dataSet = dataSets.get(i);
                if (dataSet.getEntryCount() == 0) {
                    continue;
                }
                if (!shouldDrawValues(dataSet) || dataSet.getEntryCount() < 1) {
                    continue;
                }

                // apply the text-styling defined by the DataSet
                applyValueTextStyle(dataSet);

                Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

                // make sure the values do not interfear with the circles
                int valOffset = (int) (dataSet.getCircleRadius() * 1.75f);

                if (!dataSet.isDrawCirclesEnabled())
                    valOffset = valOffset / 2;

                mXBounds.set(mChart, dataSet);

                float[] positions = trans.generateTransformedValuesLine(dataSet, mAnimator.getPhaseX(), mAnimator
                        .getPhaseY(), mXBounds.min, mXBounds.max);

                MPPointF iconsOffset = MPPointF.getInstance(dataSet.getIconsOffset());
                iconsOffset.x = Utils.convertDpToPixel(iconsOffset.x);
                iconsOffset.y = Utils.convertDpToPixel(iconsOffset.y);

                for (int j = 0; j < positions.length; j += 2) {

                    float x = positions[j];
                    float y = positions[j + 1];

                    if (!mViewPortHandler.isInBoundsRight(x))
                        break;

                    if (!mViewPortHandler.isInBoundsLeft(x) || !mViewPortHandler.isInBoundsY(y))
                        continue;

                    Entry entry = dataSet.getEntryForIndex(j / 2 + mXBounds.min);

                    if (entry != null) {
                        if (dataSet.isDrawValuesEnabled()) {
                            drawValue(c, dataSet.getValueFormatter(), entry.getY(), entry, i, x,
                                    y - valOffset, dataSet.getValueTextColor(j / 2));
                        }

                        if (entry.getIcon() != null && dataSet.isDrawIconsEnabled()) {

                            Drawable icon = entry.getIcon();

                            Utils.drawImage(
                                    c,
                                    icon,
                                    (int)(x + iconsOffset.x),
                                    (int)(y + iconsOffset.y),
                                    icon.getIntrinsicWidth(),
                                    icon.getIntrinsicHeight());
                        }
                    } else  {
                        Log.i("Draw value", "(entry == null");
                    }
                }

                MPPointF.recycleInstance(iconsOffset);
            }
        }
    }

    @Override
    public void drawExtras(Canvas c) {
        drawCircles(c);
    }

    /**
     * cache for the circle bitmaps of all datasets
     */
    private final HashMap<IDataSet, DataSetImageCache> mImageCaches = new HashMap<>();

    /**
     * buffer for drawing the circles
     */
    private final float[] mCirclesBuffer = new float[2];

    protected void drawCircles(Canvas c) {

        mRenderPaint.setStyle(Paint.Style.FILL);

        float phaseY = mAnimator.getPhaseY();

        mCirclesBuffer[0] = 0;
        mCirclesBuffer[1] = 0;

        List<ILineDataSet> dataSets = mChart.getLineData().getDataSets();

        for (int i = 0; i < dataSets.size(); i++) {

            ILineDataSet dataSet = dataSets.get(i);

            if (!dataSet.isVisible() || !dataSet.isDrawCirclesEnabled() ||
                    dataSet.getEntryCount() == 0)
                continue;

            mCirclePaintInner.setColor(dataSet.getCircleHoleColor());

            Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

            mXBounds.set(mChart, dataSet);

            float circleRadius = dataSet.getCircleRadius();
            float circleHoleRadius = dataSet.getCircleHoleRadius();
            boolean drawCircleHole = dataSet.isDrawCircleHoleEnabled() &&
                    circleHoleRadius < circleRadius &&
                    circleHoleRadius > 0.f;
            boolean drawTransparentCircleHole = drawCircleHole &&
                    dataSet.getCircleHoleColor() == ColorTemplate.COLOR_NONE;

            DataSetImageCache imageCache;

            if (mImageCaches.containsKey(dataSet)) {
                imageCache = mImageCaches.get(dataSet);
            } else {
                imageCache = new DataSetImageCache();
                mImageCaches.put(dataSet, imageCache);
            }

            boolean changeRequired = imageCache.init(dataSet);

            // only fill the cache with new bitmaps if a change is required
            if (changeRequired) {
                imageCache.fill(dataSet, drawCircleHole, drawTransparentCircleHole);
            }

            int boundsRangeCount = mXBounds.range + mXBounds.min;

            for (int j = mXBounds.min; j <= boundsRangeCount; j++) {

                Entry e = dataSet.getEntryForIndex(j);

                if (e == null) break;

                mCirclesBuffer[0] = e.getX();
                mCirclesBuffer[1] = e.getY() * phaseY;

                trans.pointValuesToPixel(mCirclesBuffer);

                if (!mViewPortHandler.isInBoundsRight(mCirclesBuffer[0]))
                    break;

                if (!mViewPortHandler.isInBoundsLeft(mCirclesBuffer[0]) ||
                        !mViewPortHandler.isInBoundsY(mCirclesBuffer[1]))
                    continue;

                Bitmap circleBitmap = imageCache.getBitmap(j);

                if (circleBitmap != null) {
                    c.drawBitmap(circleBitmap, mCirclesBuffer[0] - circleRadius, mCirclesBuffer[1] - circleRadius, null);
                }
            }
        }
    }

    @Override
    public void drawHighlighted(Canvas c, Highlight[] indices) {

        LineData lineData = mChart.getLineData();

        for (Highlight high : indices) {

            ILineDataSet set = lineData.getDataSetByIndex(high.getDataSetIndex());

            if (set == null || !set.isHighlightEnabled())
                continue;

            Entry e = set.getEntryForXValue(high.getX(), high.getY());

            if (!isInBoundsX(e, set))
                continue;

            MPPointD pix = mChart.getTransformer(set.getAxisDependency()).getPixelForValues(e.getX(), e.getY() * mAnimator
                    .getPhaseY());

            high.setDraw((float) pix.x, (float) pix.y);

            // draw the lines
            drawHighlightLines(c, (float) pix.x, (float) pix.y, set);
        }
    }

    /**
     * Sets the Bitmap.Config to be used by this renderer.
     * Default: Bitmap.Config.ARGB_8888
     * Use Bitmap.Config.ARGB_4444 to consume less memory.
     *
     * @param config
     */
    public void setBitmapConfig(Bitmap.Config config) {
        mBitmapConfig = config;
        releaseBitmap();
    }

    /**
     * Returns the Bitmap.Config that is used by this renderer.
     *
     * @return
     */
    public Bitmap.Config getBitmapConfig() {
        return mBitmapConfig;
    }

    /**
     * Releases the drawing bitmap. This should be called when {LineChart#onDetachedFromWindow()}.
     */
    public void releaseBitmap() {
        if (mBitmapCanvas != null) {
            mBitmapCanvas.setBitmap(null);
            mBitmapCanvas = null;
        }
        if (mDrawBitmap != null) {
            Bitmap drawBitmap = mDrawBitmap.get();
            if (drawBitmap != null) {
                drawBitmap.recycle();
            }
            mDrawBitmap.clear();
            mDrawBitmap = null;
        }
    }

    private class DataSetImageCache {

        private final Path mCirclePathBuffer = new Path();

        private Bitmap[] circleBitmaps;

        /**
         * Sets up the cache, returns true if a change of cache was required.
         *
         * @param set
         * @return
         */
        protected boolean init(ILineDataSet set) {

            int size = set.getCircleColorCount();
            boolean changeRequired = false;

            if (circleBitmaps == null) {
                circleBitmaps = new Bitmap[size];
                changeRequired = true;
            } else if (circleBitmaps.length != size) {
                circleBitmaps = new Bitmap[size];
                changeRequired = true;
            }

            return changeRequired;
        }

        /**
         * Fills the cache with bitmaps for the given dataset.
         *
         * @param set
         * @param drawCircleHole
         * @param drawTransparentCircleHole
         */
        protected void fill(ILineDataSet set, boolean drawCircleHole, boolean drawTransparentCircleHole) {

            int colorCount = set.getCircleColorCount();
            float circleRadius = set.getCircleRadius();
            float circleHoleRadius = set.getCircleHoleRadius();

            for (int i = 0; i < colorCount; i++) {

                Bitmap.Config conf = Bitmap.Config.ARGB_4444;
                Bitmap circleBitmap = Bitmap.createBitmap((int) (circleRadius * 2.1), (int) (circleRadius * 2.1), conf);

                Canvas canvas = new Canvas(circleBitmap);
                circleBitmaps[i] = circleBitmap;
                mRenderPaint.setColor(set.getCircleColor(i));

                if (drawTransparentCircleHole) {
                    // Begin path for circle with hole
                    mCirclePathBuffer.reset();

                    mCirclePathBuffer.addCircle(
                            circleRadius,
                            circleRadius,
                            circleRadius,
                            Path.Direction.CW);

                    // Cut hole in path
                    mCirclePathBuffer.addCircle(
                            circleRadius,
                            circleRadius,
                            circleHoleRadius,
                            Path.Direction.CCW);

                    // Fill in-between
                    canvas.drawPath(mCirclePathBuffer, mRenderPaint);
                } else {

                    canvas.drawCircle(
                            circleRadius,
                            circleRadius,
                            circleRadius,
                            mRenderPaint);

                    if (drawCircleHole) {
                        canvas.drawCircle(
                                circleRadius,
                                circleRadius,
                                circleHoleRadius,
                                mCirclePaintInner);
                    }
                }
            }
        }

        /**
         * Returns the cached Bitmap at the given index.
         *
         * @param index
         * @return
         */
        protected Bitmap getBitmap(int index) {
            return circleBitmaps[index % circleBitmaps.length];
        }
    }
}
