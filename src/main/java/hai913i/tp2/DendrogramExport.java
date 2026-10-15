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
 * Représentation lisible du dendrogramme (étape B2) : arbre indenté, liste des
 * fusions (CSV), JSON et fichier DOT pour Graphviz. Tous ces formats se lisent
 * sans exécuter le programme et laissent voir l'ordre des fusions, les membres
 * de chaque groupe et la similarité de chaque fusion.
 */
public final class DendrogramExport {

    private DendrogramExport() {
    }

    /** Arbre indenté : un nœud interne par ligne, ses deux fils en dessous. */
    public static void writeTree(Dendrogram d, Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        tree(d.root(), 0, lines);
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private static void tree(Dendrogram.Node n, int depth, List<String> out) {
        String pad = "  ".repeat(depth);
        if (n.isLeaf()) {
            out.add(pad + "- " + n.members().get(0) + "  (id " + n.id() + ")");
            return;
        }
        out.add(String.format(Locale.ROOT, "%s+ groupe %d  similarité %.4f  (%d classes)",
                pad, n.id(), n.similarity(), n.members().size()));
        tree(n.left(), depth + 1, out);
        tree(n.right(), depth + 1, out);
    }

    /** Liste des fusions dans l'ordre : étape;id;fils gauche;fils droit;similarité;membres. */
    public static void writeMerges(Dendrogram d, Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("etape;groupe;fils_gauche;fils_droit;similarite;membres");
        int step = 1;
        for (Dendrogram.Node n : d.merges()) {
            lines.add(String.format(Locale.ROOT, "%d;%d;%d;%d;%s;%s", step++, n.id(),
                    n.left().id(), n.right().id(), Double.toString(n.similarity()),
                    String.join(",", n.members())));
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /** JSON écrit à la main (aucune dépendance). */
    public static void writeJson(Dendrogram d, Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"merges\": [\n");
        int step = 1;
        for (int k = 0; k < d.merges().size(); k++) {
            Dendrogram.Node n = d.merges().get(k);
            sb.append("    {\"step\": ").append(step++)
                    .append(", \"id\": ").append(n.id())
                    .append(", \"left\": ").append(n.left().id())
                    .append(", \"right\": ").append(n.right().id())
                    .append(", \"similarity\": ").append(n.similarity())
                    .append(", \"members\": [");
            for (int i = 0; i < n.members().size(); i++) {
                sb.append(i > 0 ? ", " : "").append('"').append(n.members().get(i)).append('"');
            }
            sb.append("]}").append(k < d.merges().size() - 1 ? "," : "").append('\n');
        }
        sb.append("  ]\n}\n");
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    /** Graphviz : feuilles = classes, nœuds internes = fusions avec leur similarité. */
    public static void writeDot(Dendrogram d, Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("digraph dendrogramme {");
        lines.add("  rankdir=TB;");
        lines.add("  node [fontsize=10];");
        dot(d.root(), lines);
        lines.add("}");
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    private static void dot(Dendrogram.Node n, List<String> out) {
        if (n.isLeaf()) {
            out.add("  n" + n.id() + " [shape=box, label=\"" + n.members().get(0) + "\"];");
            return;
        }
        out.add(String.format(Locale.ROOT, "  n%d [shape=ellipse, label=\"#%d\\n%.4f\"];",
                n.id(), n.id(), n.similarity()));
        out.add("  n" + n.id() + " -> n" + n.left().id() + ";");
        out.add("  n" + n.id() + " -> n" + n.right().id() + ";");
        dot(n.left(), out);
        dot(n.right(), out);
    }

    /**
     * Vérifie les invariants du point de contrôle B2 : M feuilles, M-1 nœuds
     * internes, membres d'un nœud = union disjointe des membres de ses fils,
     * racine contenant chaque classe une seule fois.
     */
    public static boolean check(Dendrogram d, int m) {
        int[] counts = new int[2];                    // [feuilles, nœuds internes]
        boolean ok = check(d.root(), counts);
        Set<String> distinct = new HashSet<>(d.root().members());
        return ok && counts[0] == m && counts[1] == m - 1
                && d.root().members().size() == m && distinct.size() == m;
    }

    private static boolean check(Dendrogram.Node n, int[] counts) {
        if (n.isLeaf()) {
            counts[0]++;
            return n.members().size() == 1;
        }
        counts[1]++;
        Set<String> union = new HashSet<>(n.left().members());
        boolean disjoint = true;
        for (String c : n.right().members()) {
            disjoint &= union.add(c);                 // false si déjà présent
        }
        boolean sameSet = union.equals(new HashSet<>(n.members()))
                && n.members().size() == union.size();
        return disjoint && sameSet && check(n.left(), counts) && check(n.right(), counts);
    }
}