package hai913i.tp2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class CouplingTest {

    private static Coupling mini() throws IOException {
        Facts f = Facts.load(Path.of("src", "test", "resources"), "facts-mini");
        return new Coupling(f);
    }

    @Test
    void arcInterneEcarte() throws IOException {
        // 4 arcs au total, dont A.f -> A.g (interne) : |E_inter| = 3
        assertEquals(3, mini().getEInter());
    }

    @Test
    void valeursDeCouplage() throws IOException {
        Coupling c = mini();
        assertEquals(2.0 / 3, c.get("demo.A", "demo.B"), 1e-9);
        assertEquals(1.0 / 3, c.get("demo.B", "demo.C"), 1e-9);
        assertEquals(0.0, c.get("demo.A", "demo.C"), 1e-9);
    }

    @Test
    void symetrieEtDiagonale() throws IOException {
        Coupling c = mini();
        assertEquals(c.get("demo.A", "demo.B"), c.get("demo.B", "demo.A"), 1e-9);
        assertEquals(0.0, c.get("demo.A", "demo.A"), 1e-9);
    }

    @Test
    void sommeEgaleUn() throws IOException {
        double somme = mini().pairsByCoupling().stream()
                .mapToDouble(Coupling.Pair::coupling).sum();
        assertEquals(1.0, somme, 1e-9);
    }
}