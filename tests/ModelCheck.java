import java.nio.file.*;
import java.net.*;
import javax.tools.ToolProvider;

class ModelCheck {
    public static void main(String[] args) throws Exception {
        Path out = Files.createTempDirectory("arsenal-check-");
        int code = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", out.toString(),
            "app/src/main/java/com/oleg/arsenal/Battle.java", "tests/BattleChecks.java");
        if (code != 0) throw new AssertionError("Compilation failed");
        try (URLClassLoader loader = new URLClassLoader(new URL[]{out.toUri().toURL()})) {
            loader.loadClass("BattleChecks").getMethod("main", String[].class).invoke(null, (Object)new String[0]);
        }
    }
}
