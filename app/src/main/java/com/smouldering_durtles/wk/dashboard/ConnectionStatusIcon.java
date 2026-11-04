package com.smouldering_durtles.wk.dashboard;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Keeps the standard overflow glyph and adds a small, independent red status dot. */
public final class ConnectionStatusIcon extends Drawable {
    private final Drawable icon;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public ConnectionStatusIcon(Drawable icon) {
        this.icon = icon.mutate();
        paint.setColor(Color.rgb(211, 47, 47));
    }
    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        icon.setBounds(bounds);
        icon.draw(canvas);
        float radius = bounds.width() / 7f;
        canvas.drawCircle(bounds.right - radius, bounds.top + radius, radius, paint);
    }
    @Override public int getIntrinsicWidth() { return icon.getIntrinsicWidth(); }
    @Override public int getIntrinsicHeight() { return icon.getIntrinsicHeight(); }
    @Override public void setAlpha(int alpha) { icon.setAlpha(alpha); paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { icon.setColorFilter(filter); }
    @Override public void setTint(int color) { icon.setTint(color); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
