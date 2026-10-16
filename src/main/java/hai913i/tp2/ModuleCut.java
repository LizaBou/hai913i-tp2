package hai913i.tp2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Coupe du dendrogramme en modules (étape B3).
 *
 * Parcours depuis la racine : si le couplage interne moyen du nœud est
 * strictement supérieur à CP, le nœud devient un module et on s'arrête dans
 * cette branche ; sinon on descend dans les deux fils. Une feuille est
 * toujours un module d'une seule classe. Une fois la coupe terminée, on
 * vérifie la contrainte globale : au plus M/2 modules (division entière).
 *
 * Attention : le couplage interne moyen n'est PAS la similarité de la
 * fusion. C'est la moyenne de C(a,b) sur les n(n-1)/2 couples de classes du
 * groupe ; la similarité de fusion est une quantité entre les deux fils.
 */
public final class ModuleCut {

    /** Un module : ses classes et son couplage interne moyen (NaN si une seule classe). */
    public record Module(List<String> members, double internalCoupling) {
    }

    /** Résultat d'une coupe pour un seuil donné. */
    public record Result(double cp, List<Module> modules, int maxModules) {

        /** La contrainte "au plus M/2 modules" est-elle respectée ? */
        public boolean respectsLimit() {
            return modules.size() <= maxModules;
        }
    }

    private ModuleCut() {
    }

    public static Result cut(Dendrogram d, Coupling coupling, double cp) {
        List<Module> modules = new ArrayList<>();
        walk(d.root(), coupling, cp, modules);
        int m = d.root().members().size();
        return new Result(cp, modules, m / 2);
    }

    private static void walk(Dendrogram.Node n, Coupling coupling, double cp,
                             List<Module> out) {
        if (n.isLeaf()) {
            out.add(new Module(n.members(), Double.NaN));
            return;
        }
        double avg = internalCoupling(n.members(), coupling);
        if (avg > cp) {                       // strictement supérieur
            out.add(new Module(n.members(), avg));
        } else {
            walk(n.left(), coupling, cp, out);
            walk(n.right(), coupling, cp, out);
        }
    }

    /** Moyenne de C(a,b) sur tous les couples {a,b} du groupe (au moins 2 classes). */
    static double internalCoupling(List<String> members, Coupling coupling) {
        int n = members.size();
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                sum += coupling.get(members.get(i), members.get(j));
            }
        }
        return sum / (n * (n - 1) / 2.0);
    }

    /** Texte lisible du résultat (affichage et fichier). */
    public static List<String> describe(Result r) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ROOT, "CP = %.4f : %d modules (maximum autorisé M/2 = %d)",
                r.cp(), r.modules().size(), r.maxModules()));
        if (r.respectsLimit()) {
            lines.add("Contrainte M/2 : respectée");
        } else {
            lines.add("ATTENTION : contrainte M/2 VIOLÉE (" + r.modules().size()
                    + " modules > " + r.maxModules() + ") : coupe rejetée");
        }
        int k = 1;
        for (Module mod : r.modules()) {
            String avg = Double.isNaN(mod.internalCoupling())
                    ? "couplage interne moyen : sans objet (une seule classe)"
                    : String.format(Locale.ROOT, "couplage interne moyen : %.4f",
                    mod.internalCoupling());
            lines.add(String.format(Locale.ROOT, "Module %d (%d classes, %s)",
                    k++, mod.members().size(), avg));
            for (String c : mod.members()) {
                lines.add("    " + c);
            }
        }
        return lines;
    }

    public static void write(Result r, Path file) throws IOException {
        Files.write(file, describe(r), StandardCharsets.UTF_8);
    }
}