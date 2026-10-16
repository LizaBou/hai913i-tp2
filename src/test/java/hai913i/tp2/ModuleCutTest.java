package hai913i.tp2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ModuleCutTest {

    private static Coupling coupling;
    private static Dendrogram dendro;

    private static void charger() throws IOException {
        Facts f = Facts.load(Path.of("src", "test", "resources"), "facts-mini");
        coupling = new Coupling(f);
        dendro = Clustering.run(coupling);
    }

    @Test
    void cp020DonneUnSeulModule() throws IOException {
        charger();
        // couplage interne moyen de {A,B,C} = (2/3 + 1/3 + 0) / 3 = 1/3 > 0,20
        ModuleCut.Result r = ModuleCut.cut(dendro, coupling, 0.20);
        assertEquals(1, r.modules().size());
        assertEquals(3, r.modules().get(0).members().size());
        assertTrue(r.respectsLimit());          // M/2 = 1, un module : respecté
    }

    @Test
    void cp050DonneDeuxModulesEtViolleLaContrainte() throws IOException {
        charger();
        // 1/3 <= 0,50 : on descend ; {A,B} a un couplage interne 2/3 > 0,50
        ModuleCut.Result r = ModuleCut.cut(dendro, coupling, 0.50);
        assertEquals(2, r.modules().size());
        assertEquals(1, r.maxModules());        // 3 / 2 = 1 (division entière)
        assertFalse(r.respectsLimit());         // 2 > 1 : violée
    }

    @Test
    void lesModulesFormentUnePartition() throws IOException {
        charger();
        ModuleCut.Result r = ModuleCut.cut(dendro, coupling, 0.50);
        int total = r.modules().stream().mapToInt(m -> m.members().size()).sum();
        assertEquals(3, total);                 // aucune classe oubliée ni doublée
    }
}