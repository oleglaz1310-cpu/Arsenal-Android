package com.oleg.arsenal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

/** Original-inspired prototype rules. Not the DOS game engine. */
public final class Battle {
    public static final int W = 24, H = 16;
    public static final int TANK = 0, FUEL_TRUCK = 1;
    public static final float FUEL_PER_TILE = 4f;
    public final ArrayList<Unit> units = new ArrayList<>();
    public int selected = 0;
    public boolean paused;
    public String message = "Выбери танк, затем укажи клетку";

    public static final class Unit {
        public float x, y, fuel;
        public final int type;
        public final float capacity;
        public final ArrayDeque<Integer> route = new ArrayDeque<>();
        Unit(int x, int y, int type) {
            this.x = x; this.y = y; this.type = type;
            capacity = type == FUEL_TRUCK ? 250f : 120f;
            fuel = capacity;
        }
    }

    public Battle() { reset(); }

    public void reset() {
        units.clear();
        units.add(new Unit(3, 6, TANK));
        units.add(new Unit(4, 8, TANK));
        units.add(new Unit(3, 10, TANK));
        units.add(new Unit(5, 6, FUEL_TRUCK));
        selected = 0;
        paused = false;
        message = "Выбери технику, затем укажи клетку";
    }

    public String unitName(int i) {
        return units.get(i).type == FUEL_TRUCK ? "Бензовоз" : "Танк " + (i + 1);
    }

    public String fuelStatus() {
        Unit u = units.get(selected);
        return unitName(selected) + " • топливо " + Math.round(u.fuel) + "/" + Math.round(u.capacity);
    }

    public boolean land(int x, int y) {
        return x >= 0 && y >= 0 && x < W && y < H
            && !(x >= 11 && x <= 13 && y != 7 && y != 8);
    }

    public void tap(int x, int y) {
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (Math.round(u.x) == x && Math.round(u.y) == y) {
                selected = i;
                message = unitName(i) + " выбран";
                return;
            }
        }
        Unit u = units.get(selected);
        // Replan from the next waypoint to avoid cutting across water mid-move.
        int start = u.route.isEmpty()
            ? Math.round(u.y) * W + Math.round(u.x) : u.route.peek();
        ArrayDeque<Integer> path = path(start, x, y);
        if (path == null) { message = "Здесь нет сухопутного пути"; return; }
        u.route.clear();
        u.route.add(start);
        u.route.addAll(path);
        message = u.fuel <= 0 ? unitName(selected) + ": нет топлива"
            : unitName(selected) + ": движение";
    }

    public ArrayDeque<Integer> path(int start, int x, int y) {
        if (!land(x, y) || start < 0 || start >= W * H || !land(start % W, start / W))
            return null;
        int end = y * W + x;
        int[] prev = new int[W * H];
        Arrays.fill(prev, -1);
        prev[start] = start;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(start);
        int[] dx = {1, -1, 0, 0}, dy = {0, 0, 1, -1};
        while (!queue.isEmpty() && prev[end] == -1) {
            int p = queue.remove();
            for (int d = 0; d < 4; d++) {
                int nx = p % W + dx[d], ny = p / W + dy[d], n = ny * W + nx;
                if (land(nx, ny) && prev[n] == -1) {
                    prev[n] = p;
                    queue.add(n);
                }
            }
        }
        if (prev[end] == -1) return null;
        ArrayDeque<Integer> result = new ArrayDeque<>();
        for (int p = end; p != start; p = prev[p]) result.addFirst(p);
        return result;
    }

    public void tick(float dt) {
        if (paused || dt <= 0) return;
        float stepTime = Math.min(dt, .1f);
        for (Unit u : units) {
            float remaining = stepTime * 3f;
            while (!u.route.isEmpty() && remaining > .00001f) {
                int p = u.route.peek();
                float dx = p % W - u.x, dy = p / W - u.y;
                float distance = (float) Math.hypot(dx, dy);
                if (distance < .00001f) {
                    u.x = p % W; u.y = p / W;
                    u.route.remove();
                    continue;
                }
                float step = Math.min(distance, Math.min(remaining, u.fuel / FUEL_PER_TILE));
                if (step <= .00001f) break;
                u.x += dx / distance * step;
                u.y += dy / distance * step;
                u.fuel = Math.max(0, u.fuel - step * FUEL_PER_TILE);
                remaining -= step;
                if (distance - step < .00001f) {
                    u.x = p % W; u.y = p / W;
                    u.route.remove();
                }
            }
        }
        // Tankers automatically transfer their reserve to nearby vehicles.
        for (Unit truck : units) {
            if (truck.type != FUEL_TRUCK || truck.fuel <= 0) continue;
            for (Unit target : units) {
                if (target.type != TANK || target.fuel >= target.capacity) continue;
                if (Math.hypot(truck.x - target.x, truck.y - target.y) > 2.25) continue;
                float amount = Math.min(24f * stepTime,
                    Math.min(truck.fuel, target.capacity - target.fuel));
                target.fuel += amount;
                truck.fuel -= amount;
            }
        }
    }
}
