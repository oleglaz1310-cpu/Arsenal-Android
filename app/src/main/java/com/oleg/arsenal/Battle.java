package com.oleg.arsenal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

/** New prototype rules, not reverse engineered Arsenal mechanics. */
public final class Battle {
    public static final int W = 24, H = 16;
    public final ArrayList<Unit> units = new ArrayList<>();
    public int selected = 0;
    public boolean paused;
    public String message = "Выбери танк, затем укажи клетку";
    public static final class Unit {
        public float x, y;
        public final ArrayDeque<Integer> route = new ArrayDeque<>();
        Unit(int x, int y) { this.x = x; this.y = y; }
    }
    public Battle() { reset(); }
    public void reset() {
        units.clear(); units.add(new Unit(3, 6)); units.add(new Unit(4, 8)); units.add(new Unit(3, 10));
        selected = 0; paused = false; message = "Выбери танк, затем укажи клетку";
    }
    public boolean land(int x, int y) {
        return x >= 0 && y >= 0 && x < W && y < H && !(x >= 11 && x <= 13 && y != 7 && y != 8);
    }
    public void tap(int x, int y) {
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (Math.round(u.x) == x && Math.round(u.y) == y) { selected = i; message = "Танк " + (i + 1) + " выбран"; return; }
        }
        Unit u = units.get(selected);
        // Replan from the next cell center, so a mid-move order cannot cut a water corner.
        int start = u.route.isEmpty() ? Math.round(u.y) * W + Math.round(u.x) : u.route.peek();
        ArrayDeque<Integer> path = path(start, x, y);
        if (path == null) { message = "Здесь нет сухопутного пути"; return; }
        u.route.clear(); u.route.add(start); u.route.addAll(path); message = "Танк " + (selected + 1) + ": движение";
    }
    public ArrayDeque<Integer> path(int start, int x, int y) {
        if (!land(x, y)) return null;
        int end = y * W + x;
        int[] prev = new int[W * H]; Arrays.fill(prev, -1); prev[start] = start;
        ArrayDeque<Integer> queue = new ArrayDeque<>(); queue.add(start);
        int[] dx = {1, -1, 0, 0}, dy = {0, 0, 1, -1};
        while (!queue.isEmpty() && prev[end] == -1) {
            int p = queue.remove();
            for (int d = 0; d < 4; d++) {
                int nx = p % W + dx[d], ny = p / W + dy[d], n = ny * W + nx;
                if (land(nx, ny) && prev[n] == -1) { prev[n] = p; queue.add(n); }
            }
        }
        if (prev[end] == -1) return null;
        ArrayDeque<Integer> result = new ArrayDeque<>();
        for (int p = end; p != start; p = prev[p]) result.addFirst(p);
        return result;
    }
    public void tick(float dt) {
        if (paused) return;
        for (Unit u : units) {
            float remaining = Math.min(dt, .1f) * 3;
            while (!u.route.isEmpty() && remaining > 0) {
                int p = u.route.peek(); float dx = p % W - u.x, dy = p / W - u.y;
                float distance = (float)Math.hypot(dx, dy);
                if (distance <= remaining) { u.x = p % W; u.y = p / W; u.route.remove(); remaining -= distance; }
                else { u.x += dx / distance * remaining; u.y += dy / distance * remaining; remaining = 0; }
            }
        }
    }
}
