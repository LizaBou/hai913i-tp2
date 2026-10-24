package hai913i.tp2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class ClusteringTest {

    private static Dendrogram construire() throws IOException {
        Facts f = Facts.load(Path.of("src", "test", "resources"), "facts-mini");
        return Clustering.run(new Coupling(f));
    }

    @Test
    void lesFusionsDuJeuMiniSontCellesCalculeesAlaMain() throws IOException {
        Dendrogram d = construire();
        List<Dendrogram.Node> m = d.merges();

        assertEquals(2, m.size());                       // M - 1 = 2 fusions

        // étape 1 : {A,B}, C(A,B) = 2/3
        assertEquals(2.0 / 3.0, m.get(0).similarity(), 1e-9);
        assertEquals(2, m.get(0).members().size());

        // étape 2 : {A,B} avec {C} = (C(A,C) + C(B,C)) / (2 x 1) = (0 + 1/3) / 2 = 1/6
        assertEquals(1.0 / 6.0, m.get(1).similarity(), 1e-9);
        assertEquals(3, m.get(1).members().size());
    }

    @Test
    void laRacineEstLaDerniereFusionEtLeResultatEstDeterministe() throws IOException {
        Dendrogram d1 = construire();
        Dendrogram d2 = construire();

        assertEquals(d1.merges().get(1).id(), d1.root().id());
        assertEquals(d1.root().members(), d1.root().members());

        for (int i = 0; i < d1.merges().size(); i++) {
            assertEquals(d1.merges().get(i).id(), d2.merges().get(i).id());
            assertEquals(d1.merges().get(i).similarity(),
                    d2.merges().get(i).similarity(), 1e-12);
            assertEquals(d1.merges().get(i).members(), d2.merges().get(i).members());
        }
    }
}