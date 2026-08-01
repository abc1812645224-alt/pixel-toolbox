package com.example.pixeltoolbox.ui.custom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/* compiled from: DashboardManager.java */
/* loaded from: classes5.dex */
class RingChartView extends View {
    private Paint paint;
    private int percent;
    private RectF rect;
    private Paint textPaint;

    public RingChartView(Context context) {
        super(context);
        this.percent = 0;
        this.paint = new Paint(1);
        this.paint.setStyle(Paint.Style.STROKE);
        this.paint.setStrokeWidth(16.0f);
        this.paint.setStrokeCap(Paint.Cap.ROUND);
        this.textPaint = new Paint(1);
        this.textPaint.setColor(Color.parseColor("#4CAF50"));
        this.textPaint.setTextSize(36.0f);
        this.textPaint.setTextAlign(Paint.Align.CENTER);
        this.textPaint.setFakeBoldText(true);
        this.rect = new RectF();
    }

    public void setPercent(int i) {
        this.percent = i;
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        this.rect.set(16.0f, 16.0f, width - 16, height - 16);
        this.paint.setColor(Color.parseColor("#E0E0E0"));
        canvas.drawArc(this.rect, 0.0f, 360.0f, false, this.paint);
        this.paint.setColor(Color.parseColor("#4CAF50"));
        canvas.drawArc(this.rect, -90.0f, (this.percent / 100.0f) * 360.0f, false, this.paint);
        Paint.FontMetrics fontMetrics = this.textPaint.getFontMetrics();
        canvas.drawText(this.percent + "%", width / 2.0f, ((height - fontMetrics.ascent) - fontMetrics.descent) / 2.0f, this.textPaint);
    }
}
