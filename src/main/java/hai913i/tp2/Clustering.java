package hai913i.tp2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Clustering hiérarchique ascendant à lien moyen (étape B1).
 *
 * Départ : une classe = un groupe. À chaque itération on fusionne les deux
 * groupes les plus similaires, jusqu'à n'en avoir plus qu'un.
 *
 * Similarité de deux groupes disjoints : moyenne de C(a,b) sur TOUS les
 * couples (a,b) de G1 x G2, couples de couplage nul compris, c'est-à-dire
 * somme / (|G1| * |G2|). Elle est recalculée à partir des couplages entre
 * classes, jamais à partir des anciennes similarités de groupes.
 *
 * Égalités : chaque groupe a un identifiant stable (classes 0..M-1 par ordre
 * alphabétique, puis M, M+1, ... pour les fusions). Un couple s'écrit
 * (min, max) et, à similarité égale (tolérance 1e-9), on retient le plus petit
 * couple dans l'ordre lexicographique.
 */
public final class Clustering {

    private static final double EPS = 1e-9;

    private Clustering() {
    }

    public static Dendrogram run(Coupling coupling) {
        List<String> classes = coupling.getClasses();      // triées
        int m = classes.size();
        if (m == 0) {
            throw new IllegalArgumentException("aucune classe à regrouper");
        }
        // groupes actifs, indexés par identifiant (TreeMap : ordre croissant)
        Map<Integer, Dendrogram.Node> active = new TreeMap<>();
        for (int i = 0; i < m; i++) {
            active.put(i, new Dendrogram.Node(i, null, null, 0.0, List.of(classes.get(i))));
        }
        List<Dendrogram.Node> merges = new ArrayList<>();
        int nextId = m;
        while (active.size() > 1) {
            int bestI = -1;
            int bestJ = -1;
            double best = -1.0;
            List<Integer> ids = new ArrayList<>(active.keySet());
            for (int x = 0; x < ids.size(); x++) {
                for (int y = x + 1; y < ids.size(); y++) {
                    int i = ids.get(x);                // i < j toujours
                    int j = ids.get(y);
                    double s = similarity(coupling, active.get(i), active.get(j));
                    // strictement meilleur (au-delà de la tolérance) : sinon on
                    // garde le premier couple rencontré = le plus petit
                    if (s > best + EPS) {
                        best = s;
                        bestI = i;
                        bestJ = j;
                    }
                }
            }
            Dendrogram.Node g1 = active.remove(bestI);
            Dendrogram.Node g2 = active.remove(bestJ);
            List<String> members = new ArrayList<>(g1.members());
            members.addAll(g2.members());
            members.sort(null);
            Dendrogram.Node merged = new Dendrogram.Node(nextId++, g1, g2, best, members);
            active.put(merged.id(), merged);
            merges.add(merged);
        }
        Dendrogram.Node root = active.values().iterator().next();
        return new Dendrogram(root, merges);
    }

    /** Lien moyen : somme des C(a,b) sur G1 x G2, divisée par |G1| * |G2|. */
    static double similarity(Coupling coupling, Dendrogram.Node g1, Dendrogram.Node g2) {
        double sum = 0.0;
        for (String a : g1.members()) {
            for (String b : g2.members()) {
                sum += coupling.get(a, b);             // 0 si aucun arc
            }
        }
        return sum / ((double) g1.members().size() * g2.members().size());
    }
}