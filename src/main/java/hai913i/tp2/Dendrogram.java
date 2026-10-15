package hai913i.tp2;

import java.util.List;

/**
 * Dendrogramme : arbre binaire dont les feuilles sont les classes et dont
 * chaque nœud interne représente une fusion (étapes B1 et B2).
 */
public final class Dendrogram {

    /**
     * Un nœud de l'arbre.
     *
     * @param id         identifiant stable : 0..M-1 pour les feuilles (ordre
     *                   alphabétique), puis M, M+1, ... pour les fusions
     * @param left       fils de plus petit identifiant (null pour une feuille)
     * @param right      fils de plus grand identifiant (null pour une feuille)
     * @param similarity similarité de la fusion (lien moyen) ; 0 pour une feuille
     * @param members    classes du groupe, triées par nom qualifié
     */
    public record Node(int id, Node left, Node right,
                       double similarity, List<String> members) {

        public boolean isLeaf() {
            return left == null;
        }
    }

    private final Node root;
    private final List<Node> merges;   // nœuds internes, dans l'ordre des fusions

    public Dendrogram(Node root, List<Node> merges) {
        this.root = root;
        this.merges = List.copyOf(merges);
    }

    public Node root() {
        return root;
    }

    /** Les M-1 fusions dans l'ordre où elles ont eu lieu. */
    public List<Node> merges() {
        return merges;
    }
}