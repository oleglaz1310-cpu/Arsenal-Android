import com.oleg.arsenal.Battle;

public class BattleChecks {
    static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        Battle b = new Battle();
        check(b.path(6 * Battle.W + 3, 12, 1) == null);
        check(b.path(6 * Battle.W + 3, -1, 1) == null);
        b.tap(20, 2);
        check(!b.units.get(0).route.isEmpty());
        int prev = 6 * Battle.W + 3;
        for (int p : b.units.get(0).route) {
            check(b.land(p % Battle.W, p / Battle.W));
            check(Math.abs(p % Battle.W - prev % Battle.W) + Math.abs(p / Battle.W - prev / Battle.W) <= 1);
            prev = p;
        }
        b.paused = true; b.tick(.1f); check(b.units.get(0).x == 3);
        b.paused = false;
        for (int i=0; i<1000; i++) b.tick(.02f);
        check(b.units.get(0).x == 20 && b.units.get(0).y == 2);
        b.tap(4,8); check(b.selected == 1);
        b.tap(19,4); b.tick(.07f); b.tap(2,2);
        for (int i=0; i<1000; i++) b.tick(.02f);
        check(b.units.get(1).x == 2 && b.units.get(1).y == 2);
        b.reset(); check(b.selected == 0 && b.units.get(0).route.isEmpty());
        System.out.println("PASS: water, boundaries, bridge route, pause, arrival, selection, mid-move order, reset");
    }
}
