package hai913i.tp2;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Point d'entrée : charge les faits (A0), calcule le couplage (A1),
 * exporte le graphe pondéré (A2) et construit le dendrogramme (B1).
 *
 * Codes de sortie : 0 si tout va bien, 1 si les arguments sont invalides,
 * 2 si les fichiers sont illisibles.
 */
public final class Main {

    /** Sortie standard forcée en UTF-8. */
    private static final PrintStream OUT =
            new PrintStream(System.out, true, StandardCharsets.UTF_8);

    /** Sortie d'erreur, forcée en UTF-8. */
    private static final PrintStream ERR =
            new PrintStream(System.err, true, StandardCharsets.UTF_8);

    /** Seuil de coupe par défaut, repris du point de contrôle B3 du sujet. */
    private static final double CP_DEFAUT = 0.025;

    private Main() {
    }

    /**
     * @param args {@code <dossier-des-faits> [--cp <valeur>] [--prefix <préfixe>]}
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            usage("chemin du dossier des faits manquant");
            return;
        }
        Path dir = Path.of(args[0]);
        double cp = CP_DEFAUT;
        String prefix = null;
        for (int i = 1; i < args.length; i++) {
            if ("--cp".equals(args[i]) && i + 1 < args.length) {
                try {
                    cp = Double.parseDouble(args[++i]);
                } catch (NumberFormatException e) {
                    usage("valeur de --cp invalide : " + args[i]);
                    return;
                }
            } else if ("--prefix".equals(args[i]) && i + 1 < args.length) {
                prefix = args[++i];
            } else {
                usage("argument inconnu : " + args[i]);
                return;
            }
        }
        if (cp < 0.0) {
            usage("--cp doit être positif ou nul");
            return;
        }

        Facts facts;
        try {
            if (prefix == null) {
                prefix = Facts.discoverPrefix(dir);
            }
            facts = Facts.load(dir, prefix);
        } catch (IOException e) {
            ERR.println("Lecture des faits impossible : " + e.getMessage());
            System.exit(2);
            return;
        }

        // Étape A0 : compteurs
        OUT.println("Dossier des faits   : " + dir.toAbsolutePath());
        OUT.println("Jeu de données      : " + prefix);
        OUT.printf(Locale.ROOT, "Classes             : %d%n",
                facts.classes().size());
        OUT.printf(Locale.ROOT, "Méthodes            : %d%n",
                facts.methodOwners().size());
        OUT.printf(Locale.ROOT, "Arcs du graphe      : %d%n",
                facts.calls().size());
        OUT.printf(Locale.ROOT, "Seuil CP retenu     : %.4f%n", cp);

        // Étape A1 : couplage entre classes
        Coupling coupling = new Coupling(facts);
        List<Coupling.Pair> pairs = coupling.pairsByCoupling();
        double somme = 0.0;
        for (Coupling.Pair p : pairs) {
            somme += p.coupling();
        }
        OUT.println();
        OUT.println("=== A1 : couplage entre classes ===");
        OUT.printf(Locale.ROOT, "Arcs internes écartés : %d%n",
                facts.calls().size() - coupling.getEInter());
        OUT.printf(Locale.ROOT, "Arcs inter-classes    : %d%n", coupling.getEInter());
        OUT.printf(Locale.ROOT, "Couples non nuls      : %d sur %d possibles%n",
                pairs.size(), nbCouples(coupling.getClasses().size()));
        OUT.printf(Locale.ROOT, "Somme des couplages   : %.4f%n", somme);
        OUT.println("Couples par couplage décroissant :");
        for (Coupling.Pair p : pairs) {
            OUT.printf(Locale.ROOT, "  %.4f  r=%d  %s – %s%n",
                    p.coupling(), p.r(), p.a(), p.b());
        }

        // Étape A2 : graphe de couplage pondéré
        CouplingGraph graph = CouplingGraph.from(coupling);
        OUT.println();
        OUT.println("=== A2 : graphe de couplage pondéré ===");
        OUT.printf(Locale.ROOT, "Nœuds : %d, arêtes : %d%n",
                graph.nodes().size(), graph.edges().size());
        OUT.println("Les 5 couples les plus couplés :");
        for (Coupling.Pair p : graph.top(5)) {
            OUT.printf(Locale.ROOT, "  %.4f  %s – %s%n", p.coupling(), p.a(), p.b());
        }
        OUT.println("Classes isolées : " + graph.isolated());
        try {
            Path sorties = Path.of("sorties");
            Files.createDirectories(sorties);
            graph.write(sorties.resolve("graphe-couplage.txt"));
            graph.writeDot(sorties.resolve("graphe-couplage.dot"));
            CouplingGraph relu = CouplingGraph.read(sorties.resolve("graphe-couplage.txt"));
            OUT.println("Export rechargé identique : "
                    + (relu.edges().equals(graph.edges())
                    && relu.nodes().equals(graph.nodes())));
        } catch (IOException e) {
            ERR.println("Écriture des sorties impossible : " + e.getMessage());
            System.exit(2);
        }

        // Étape B1 : clustering hiérarchique ascendant, lien moyen
        Dendrogram dendro = Clustering.run(coupling);
        OUT.println();
        OUT.println("=== B1 : clustering hiérarchique (lien moyen) ===");
        OUT.printf(Locale.ROOT, "Fusions : %d%n", dendro.merges().size());
        int etape = 1;
        for (Dendrogram.Node n : dendro.merges()) {
            OUT.printf(Locale.ROOT, "%2d  %.4f  {%s} + {%s}%n", etape++, n.similarity(),
                    String.join(", ", n.left().members()),
                    String.join(", ", n.right().members()));
        }

        // Étape B2 : représentation et export du dendrogramme
        OUT.println();
        OUT.println("=== B2 : dendrogramme ===");
        OUT.println("Arbre valide : "
                + DendrogramExport.check(dendro, coupling.getClasses().size()));
        try {
            Path sorties = Path.of("sorties");
            Files.createDirectories(sorties);
            DendrogramExport.writeTree(dendro, sorties.resolve("dendrogramme.txt"));
            DendrogramExport.writeMerges(dendro, sorties.resolve("fusions.csv"));
            DendrogramExport.writeJson(dendro, sorties.resolve("dendrogramme.json"));
            DendrogramExport.writeDot(dendro, sorties.resolve("dendrogramme.dot"));
            OUT.println("Fichiers écrits dans sorties/ : dendrogramme.txt, "
                    + "fusions.csv, dendrogramme.json, dendrogramme.dot");
        } catch (IOException e) {
            ERR.println("Écriture des sorties impossible : " + e.getMessage());
            System.exit(2);
        }
        // Étape B3 : coupe du dendrogramme avec le seuil CP
        ModuleCut.Result coupe = ModuleCut.cut(dendro, coupling, cp);
        OUT.println();
        OUT.println("=== B3 : modules (coupe du dendrogramme) ===");
        for (String ligne : ModuleCut.describe(coupe)) {
            OUT.println(ligne);
        }
        try {
            Path sorties = Path.of("sorties");
            Files.createDirectories(sorties);
            ModuleCut.write(coupe, sorties.resolve(
                    String.format(Locale.ROOT, "modules-cp%.4f.txt", cp)));
        } catch (IOException e) {
            ERR.println("Écriture des sorties impossible : " + e.getMessage());
            System.exit(2);
        }
        OUT.println();
        OUT.println("Étapes A0 à B3 faites.");
    }

    /** Nombre de couples possibles de classes distinctes : M(M-1)/2. */
    private static int nbCouples(int m) {
        return m * (m - 1) / 2;
    }

    /** Affiche un message d'erreur, le mode d'emploi, et sort avec le code 1. */
    private static void usage(String message) {
        ERR.println("Erreur : " + message);
        ERR.println();
        ERR.println("Usage : java -jar hai913i-tp2.jar <dossier-des-faits>"
                + " [--cp <valeur>] [--prefix <préfixe>]");
        ERR.println();
        ERR.println("  <dossier-des-faits>  dossier contenant les trois fichiers");
        ERR.println("                       <préfixe>-classes.csv,");
        ERR.println("                       <préfixe>-methods.csv et");
        ERR.println("                       <préfixe>-calls.csv");
        ERR.println("  --cp <valeur>        seuil de couplage interne moyen"
                + " (défaut : " + CP_DEFAUT + ")");
        ERR.println("  --prefix <préfixe>   force le préfixe ; par défaut il est");
        ERR.println("                       déduit du dossier");
        System.exit(1);
    }
}