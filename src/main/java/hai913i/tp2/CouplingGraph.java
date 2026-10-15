package hai913i.tp2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Graphe de couplage pondéré (étape A2) : un nœud par classe, une arête non
 * orientée par couple de couplage non nul, de poids C(A,B).
 *
 * Format texte relisible (UTF-8, séparateur ";") :
 *   N;nom.qualifié.de.la.classe        (un nœud)
 *   E;classeA;classeB;r;couplage       (une arête, A < B)
 */
public final class CouplingGraph {

    private final List<String> nodes;
    private final List<Coupling.Pair> edges;

    public CouplingGraph(List<String> nodes, List<Coupling.Pair> edges) {
        this.nodes = List.copyOf(nodes);
        this.edges = List.copyOf(edges);
    }

    /** Construit le graphe à partir du couplage calculé en A1. */
    public static CouplingGraph from(Coupling coupling) {
        return new CouplingGraph(coupling.getClasses(), coupling.pairsByCoupling());
    }

    public List<String> nodes() {
        return nodes;
    }

    /** Arêtes, de la plus lourde à la plus légère. */
    public List<Coupling.Pair> edges() {
        return edges;
    }

    /** Les n arêtes les plus lourdes. */
    public List<Coupling.Pair> top(int n) {
        return edges.subList(0, Math.min(n, edges.size()));
    }

    /** Classes sans aucune arête. */
    public List<String> isolated() {
        Set<String> connected = new HashSet<>();
        for (Coupling.Pair e : edges) {
            connected.add(e.a());
            connected.add(e.b());
        }
        List<String> out = new ArrayList<>();
        for (String n : nodes) {
            if (!connected.contains(n)) {
                out.add(n);
            }
        }
        return out;
    }

    /** Poids d'une arête, 0 si le couple est absent. */
    public double weight(String a, String b) {
        for (Coupling.Pair e : edges) {
            if ((e.a().equals(a) && e.b().equals(b))
                    || (e.a().equals(b) && e.b().equals(a))) {
                return e.coupling();
            }
        }
        return 0.0;
    }

    /** Export relisible par {@link #read(Path)}. */
    public void write(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String n : nodes) {
            lines.add("N;" + n);
        }
        for (Coupling.Pair e : edges) {
            // Double.toString : la valeur relue est exactement la même
            lines.add("E;" + e.a() + ";" + e.b() + ";" + e.r() + ";" + e.coupling());
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /** Recharge un graphe exporté par {@link #write(Path)}. */
    public static CouplingGraph read(Path file) throws IOException {
        List<String> nodes = new ArrayList<>();
        List<Coupling.Pair> edges = new ArrayList<>();
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            if (line.isBlank()) {
                continue;
            }
            String[] f = line.split(";", -1);
            if (f[0].equals("N") && f.length == 2) {
                nodes.add(f[1]);
            } else if (f[0].equals("E") && f.length == 5) {
                try {
                    edges.add(new Coupling.Pair(f[1], f[2],
                            Integer.parseInt(f[3]), Double.parseDouble(f[4])));
                } catch (NumberFormatException e) {
                    throw new IOException("ligne invalide : " + line);
                }
            } else {
                throw new IOException("ligne invalide : " + line);
            }
        }
        return new CouplingGraph(nodes, edges);
    }

    /** Export Graphviz : dot -Tpng graphe.dot -o graphe.png */
    public void writeDot(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("graph couplage {");
        lines.add("  node [shape=box, fontsize=10];");
        for (String n : nodes) {
            lines.add("  \"" + n + "\";");
        }
        for (Coupling.Pair e : edges) {
            lines.add(String.format(Locale.ROOT,
                    "  \"%s\" -- \"%s\" [label=\"%.4f\", penwidth=%.2f];",
                    e.a(), e.b(), e.coupling(), 1.0 + 20.0 * e.coupling()));
        }
        lines.add("}");
        Files.write(file, lines, StandardCharsets.UTF_8);
    }
}