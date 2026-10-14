package hai913i.tp2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Couplage entre classes (étape A1).
 *
 * C(A,B) = r(A,B) / |E_inter|, où r(A,B) est le nombre d'arcs du graphe
 * d'appel entre A et B (dans un sens ou dans l'autre) et |E_inter| le nombre
 * d'arcs dont les deux classes sont différentes.
 */
public final class Coupling {

    /** Un couple de classes distinctes avec son nombre d'arcs et son couplage. */
    public record Pair(String a, String b, int r, double coupling) {
    }

    private final List<String> classes;                    // triées par nom qualifié
    private final Map<String, Integer> r = new HashMap<>(); // clé "A|B" avec A < B
    private int eInter;                                     // |E_inter|

    public Coupling(Facts facts) {
        this.classes = facts.classNames();
        for (Facts.CallArc arc : facts.calls()) {
            if (arc.callerClass().equals(arc.calleeClass())) {
                continue;                       // arc interne : cohésion, pas couplage
            }
            eInter++;                           // un ARC, pas un site d'appel
            r.merge(key(arc.callerClass(), arc.calleeClass()), 1, Integer::sum);
        }
    }

    /** Clé ordonnée : A|B et B|A donnent la même clé (le sens est perdu). */
    private static String key(String a, String b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    /** Nombre d'arcs r(A,B) entre deux classes. */
    public int arcs(String a, String b) {
        return a.equals(b) ? 0 : r.getOrDefault(key(a, b), 0);
    }

    /** Couplage C(A,B) ; 0 si A = B ou si le couple est absent. */
    public double get(String a, String b) {
        if (eInter == 0) {
            return 0.0;
        }
        return (double) arcs(a, b) / eInter;
    }

    /** |E_inter| : nombre d'arcs inter-classes. */
    public int getEInter() {
        return eInter;
    }

    /** Les M classes, triées par nom qualifié. */
    public List<String> getClasses() {
        return classes;
    }

    /** Les couples de couplage non nul, du plus couplé au moins couplé. */
    public List<Pair> pairsByCoupling() {
        List<Pair> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : r.entrySet()) {
            String[] ab = e.getKey().split("\\|");
            out.add(new Pair(ab[0], ab[1], e.getValue(),
                    (double) e.getValue() / eInter));
        }
        out.sort(Comparator.comparingDouble(Pair::coupling).reversed()
                .thenComparing(Pair::a).thenComparing(Pair::b));
        return out;
    }
}