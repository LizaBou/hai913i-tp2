# HAI913I – TP 2 : Compréhension et architectures

Outil en ligne de commande qui part des faits d'un projet Java (classes,
méthodes, appels), calcule le couplage entre classes, construit un graphe
pondéré, regroupe les classes par clustering hiérarchique ascendant (lien
moyen), exporte le dendrogramme et le coupe en modules selon un seuil CP.

- Auteure : Liza Bourouina (travail individuel)
- Dépôt Git : https://github.com/LizaBou/hai913i-tp2
- Vidéo de démonstration : fichier `video/demonstration.mp4` de l'archive
  (voir aussi `liens.txt`)

## 1. Projet

- **Chemin d'entrée retenu : chemin 2** (jeu de données canonique fourni
  sur Moodle). Les faits sont lus par la classe `Facts` ; le reste du
  programme ne manipule que cette interface.
- **Entrées** : un dossier contenant `<préfixe>-classes.csv`,
  `<préfixe>-methods.csv`, `<préfixe>-calls.csv` (séparateur « ; », UTF-8).
  Le jeu de validation est dans `data/facts-validation/`.
- **Sorties** (dossier `sorties/`, créé dans le répertoire d'exécution) :
  `graphe-couplage.txt` et `.dot`, `dendrogramme.txt`, `.json` et `.dot`,
  `fusions.csv`, `modules-cp<valeur>.txt`.
- **Structure** (paquetage `hai913i.tp2`) : `Facts` (entrée), `Coupling`
  (A1), `CouplingGraph` (A2), `Clustering` et `Dendrogram` (B1),
  `DendrogramExport` (B2), `ModuleCut` (B3), `Main` (ligne de commande).
- **Limites connues** : seuls les appels de méthodes sont comptés (ni
  héritage, ni `new`, ni exceptions) ; pas d'interface graphique ; pas
  d'analyse directe du code source.

## 2. Environnement

- JDK : OpenJDK 21.0.12 (Java 17 ou plus requis)
- Maven : 3.8.7
- Graphviz : 2.43.0 (conversion des fichiers `.dot` en images)
- Système : Ubuntu 24.04.5 LTS
- IDE : IntelliJ IDEA (le projet se construit aussi en ligne de
  commande)
- Dépendances : JUnit 5 pour les tests uniquement

## 3. Construction

Depuis une copie neuve du dépôt :

```
git clone https://github.com/LizaBou/hai913i-tp2.git
cd hai913i-tp2
mvn clean package
```

La commande compile, lance les 15 tests et produit `target/hai913i-tp2.jar`.

## 4. Exécution

Depuis la racine du projet :

```
java -jar target/hai913i-tp2.jar data/facts-validation
java -jar target/hai913i-tp2.jar data/facts-validation --cp 0.015
java -jar target/hai913i-tp2.jar data/facts-validation --cp 0.045
```

- `--cp <valeur>` : seuil de couplage interne moyen (défaut 0.025).
- `--prefix <préfixe>` : force le préfixe des fichiers de faits.
- Images : `dot -Tpng sorties/dendrogramme.dot -o sorties/dendrogramme.png`
  (idem pour `graphe-couplage.dot`).
- Codes de sortie : 0 succès ; 1 arguments invalides ; 2 faits absents,
  illisibles ou incohérents.

Dans IntelliJ : Run → Edit Configurations, classe principale
`hai913i.tp2.Main`, arguments `data/facts-validation`, répertoire de
travail = racine du projet.

## 5. Validation

Jeu de validation : 15 classes, 59 méthodes, 44 arcs, 37 arcs inter-classes.

| Étape | Valeur attendue | Résultat |
|---|---|---|
| A0 | 15 classes, 59 méthodes, 44 arcs | identique |
| A1 | 19 paires non nulles sur 105, C max = 0,1081, somme = 1 | identique |
| A2 | 15 nœuds, 19 arêtes, isolées : Dvd et LoanException | identique |
| B1 | 14 fusions : 0,1081 ; 0,0811 ; 0,0608 ; … ; deux dernières à 0 | identique |
| B2 | 15 feuilles, 14 nœuds internes | identique |
| B3 | CP 0,015 : 4 modules ; 0,025 : 7 ; 0,045 : 10 (M/2 violée) | identique |

Tests automatiques : `mvn test` (15 tests, jeu mini de 3 classes dont les
valeurs sont calculées à la main).

## 6. Dépannage

| Symptôme | Cause et remède |
|---|---|
| « dossier introuvable » (code 2) | mauvais chemin ; vérifier le dossier des CSV |
| fichiers `-classes.csv` absents | le dossier ne contient pas les 3 fichiers |
| « valeur de --cp invalide » (code 1) | `--cp` doit être un nombre, par ex. 0.025 |
| « contrainte M/2 VIOLÉE » | CP trop élevé : choisir une valeur plus basse |
| `dot` introuvable | `sudo apt install graphviz` |
| `sorties/` absent | lancer depuis la racine du projet |

## 7. Usage de l'IA

J'ai utilisé l'assistant Claude (Anthropic) pour comprendre le sujet,
produire des premières versions de code et de tests, comprendre l'outillage
(IntelliJ, Maven, Graphviz, Git) et structurer les documents. J'ai relu,
compris, exécuté et vérifié le code, comparé les résultats aux valeurs de
contrôle du sujet, et je suis responsable de l'ensemble du rendu. Le détail
figure dans la section 9 du compte-rendu.

## 8. Références

- B. Rima, HAI913I – TP 2 : Compréhension et architectures, sujet et annexe
  sur le clustering hiérarchique, Université de Montpellier, 2026.
- Documentation de JUnit 5, Maven et Graphviz.