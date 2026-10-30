package com.oleg.arsenal;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private final Battle battle = new Battle();
    private Field field;
    private TextView status;
    private boolean foreground;
    private long last;
    private final Runnable frame = new Runnable() {
        public void run() {
            if (!foreground) return;
            long now = System.nanoTime();
            battle.tick((now - last) / 1_000_000_000f); last = now;
            status.setText(battle.message + "\n" + battle.fuelStatus()); field.invalidate(); field.postDelayed(this, 16);
        }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (saved != null) {
            for (int i = 0; i < battle.units.size(); i++) {
                battle.units.get(i).x = saved.getFloat("x" + i, battle.units.get(i).x);
                battle.units.get(i).y = saved.getFloat("y" + i, battle.units.get(i).y);
                battle.units.get(i).fuel = Math.max(0, Math.min(battle.units.get(i).capacity,
                    saved.getFloat("fuel" + i, battle.units.get(i).capacity)));
                int[] route = saved.getIntArray("route" + i);
                if (route != null) for (int p : route) battle.units.get(i).route.add(p);
            }
            battle.selected = Math.max(0, Math.min(battle.units.size() - 1, saved.getInt("selected"))); battle.paused = saved.getBoolean("paused");
        }
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(17, 25, 29));
        // Let system bars reserve safe space, including on edge-to-edge Android versions.
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()); return insets;
        });
        TextView title = new TextView(this); title.setText("АРСЕНАЛ  /  ПРОТОТИП 0.2");
        title.setTextColor(Color.rgb(222, 198, 137)); title.setTextSize(20); title.setPadding(16, 8, 16, 8); root.addView(title);
        field = new Field(); root.addView(field, new LinearLayout.LayoutParams(-1, 0, 1));
        status = new TextView(this); status.setTextColor(Color.WHITE); status.setTextSize(16); status.setPadding(16, 8, 16, 8); root.addView(status);
        TextView hint = new TextView(this); hint.setText("Жёлтая техника — бензовоз. Подведи его к танку для заправки.");
        hint.setTextColor(0xFFCCD4D2); hint.setTextSize(12); hint.setPadding(16, 0, 16, 4); root.addView(hint);
        LinearLayout bar = new LinearLayout(this);
        Button pause = new Button(this); pause.setText(battle.paused ? "Продолжить" : "Пауза");
        pause.setOnClickListener(v -> { battle.paused = !battle.paused; pause.setText(battle.paused ? "Продолжить" : "Пауза"); });
        Button reset = new Button(this); reset.setText("Заново"); reset.setOnClickListener(v -> { battle.reset(); pause.setText("Пауза"); });
        bar.addView(pause, new LinearLayout.LayoutParams(0, -2, 1)); bar.addView(reset, new LinearLayout.LayoutParams(0, -2, 1)); root.addView(bar);
        setContentView(root); root.requestApplyInsets();
    }
    @Override protected void onResume() { super.onResume(); foreground = true; last = System.nanoTime(); field.post(frame); }
    @Override protected void onPause() { foreground = false; field.removeCallbacks(frame); super.onPause(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("selected", battle.selected); out.putBoolean("paused", battle.paused);
        for (int i = 0; i < battle.units.size(); i++) {
            Battle.Unit u = battle.units.get(i); out.putFloat("x" + i, u.x); out.putFloat("y" + i, u.y);
            out.putFloat("fuel" + i, u.fuel);
            int[] route = new int[u.route.size()]; int j = 0; for (int p : u.route) route[j++] = p;
            out.putIntArray("route" + i, route);
        }
    }
    private final class Field extends View {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        float cell, left, top;
        Field() { super(MainActivity.this); setContentDescription("Поле боя: выбери танк и клетку назначения"); }
        void rect(Canvas c, float x, float y, float w, float h, int color) {
            paint.setColor(color); c.drawRect(left+x*cell, top+y*cell, left+(x+w)*cell, top+(y+h)*cell, paint);
        }
        @Override protected void onDraw(Canvas c) {
            cell = Math.min(getWidth() / (float)Battle.W, getHeight() / (float)Battle.H);
            left = (getWidth() - Battle.W*cell)/2; top = (getHeight() - Battle.H*cell)/2;
            for (int y=0; y<Battle.H; y++) for (int x=0; x<Battle.W; x++) {
                int color = battle.land(x,y) ? Color.rgb(72+(x*7+y*11)%14, 89+(x+y)%12, 52) : Color.rgb(39, 88, 111);
                if (x>=11 && x<=13 && (y==7 || y==8)) color=Color.rgb(155, 137, 100);
                rect(c,x,y,1,1,color);
            }
            Battle.Unit active = battle.units.get(battle.selected);
            for (int p : active.route) { paint.setColor(0x88FFE29A); c.drawCircle(left+(p%Battle.W+.5f)*cell,top+(p/Battle.W+.5f)*cell,cell*.09f,paint); }
            for (int i=0; i<battle.units.size(); i++) {
                Battle.Unit u=battle.units.get(i);
                if (i==battle.selected) {
                    paint.setColor(0xFFFFDB80); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2);
                    c.drawCircle(left+(u.x+.5f)*cell, top+(u.y+.5f)*cell,cell*.48f,paint); paint.setStyle(Paint.Style.FILL);
                }
                if (u.type == Battle.FUEL_TRUCK) {
                    rect(c,u.x+.12f,u.y+.21f,.76f,.58f,0xFF444431);
                    rect(c,u.x+.2f,u.y+.23f,.47f,.48f,0xFFE2C764);
                    rect(c,u.x+.7f,u.y+.3f,.17f,.31f,0xFFE8E9DD);
                } else {
                    rect(c,u.x+.14f,u.y+.18f,.72f,.64f,0xFF202B24);
                    rect(c,u.x+.24f,u.y+.12f,.5f,.76f,0xFFA8B979);
                    rect(c,u.x+.34f,u.y+.32f,.65f,.15f,0xFFD5D5A2);
                }
                float fx = left + (u.x + .12f) * cell, fy = top + (u.y + .91f) * cell;
                paint.setColor(0xFF1C2424);
                c.drawRect(fx, fy, fx + .76f * cell, fy + .09f * cell, paint);
                paint.setColor(u.fuel < u.capacity * .25f ? 0xFFF16C4D : 0xFF8BD18B);
                c.drawRect(fx, fy, fx + .76f * cell * (u.fuel / u.capacity),
                    fy + .09f * cell, paint);
            }
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction()==MotionEvent.ACTION_UP) {
                if (cell>0) {
                    int x=(int)Math.floor((e.getX()-left)/cell), y=(int)Math.floor((e.getY()-top)/cell);
                    if (x>=0 && y>=0 && x<Battle.W && y<Battle.H) battle.tap(x,y);
                }
                performClick();
            }
            return true;
        }
        @Override public boolean performClick() { super.performClick(); return true; }
    }
}
