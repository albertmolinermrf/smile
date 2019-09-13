/*******************************************************************************
 * Copyright (c) 2010-2019 Haifeng Li
 *
 * Smile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Smile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Smile.  If not, see <https://www.gnu.org/licenses/>.
 *******************************************************************************/

package smile.classification;

import java.util.*;
import java.util.stream.IntStream;

import smile.base.cart.*;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.data.vector.BaseVector;
import smile.math.MathEx;
import smile.sort.QuickSort;

/**
 * Decision tree for classification. A decision tree can be learned by
 * splitting the training set into subsets based on an attribute value
 * test. This process is repeated on each derived subset in a recursive
 * manner called recursive partitioning. The recursion is completed when
 * the subset at a node all has the same value of the target variable,
 * or when splitting no longer adds value to the predictions.
 * <p>
 * The algorithms that are used for constructing decision trees usually
 * work top-down by choosing a variable at each step that is the next best
 * variable to use in splitting the set of items. "Best" is defined by how
 * well the variable splits the set into homogeneous subsets that have
 * the same value of the target variable. Different algorithms use different
 * formulae for measuring "best". Used by the CART algorithm, Gini impurity
 * is a measure of how often a randomly chosen element from the set would
 * be incorrectly labeled if it were randomly labeled according to the
 * distribution of labels in the subset. Gini impurity can be computed by
 * summing the probability of each item being chosen times the probability
 * of a mistake in categorizing that item. It reaches its minimum (zero) when
 * all cases in the node fall into a single target category. Information gain
 * is another popular measure, used by the ID3, C4.5 and C5.0 algorithms.
 * Information gain is based on the concept of entropy used in information
 * theory. For categorical variables with different number of levels, however,
 * information gain are biased in favor of those attributes with more levels. 
 * Instead, one may employ the information gain ratio, which solves the drawback
 * of information gain. 
 * <p>
 * Classification and Regression Tree techniques have a number of advantages
 * over many of those alternative techniques.
 * <dl>
 * <dt>Simple to understand and interpret.</dt>
 * <dd>In most cases, the interpretation of results summarized in a tree is
 * very simple. This simplicity is useful not only for purposes of rapid
 * classification of new observations, but can also often yield a much simpler
 * "model" for explaining why observations are classified or predicted in a
 * particular manner.</dd>
 * <dt>Able to handle both numerical and categorical data.</dt>
 * <dd>Other techniques are usually specialized in analyzing datasets that
 * have only one type of variable. </dd>
 * <dt>Tree methods are nonparametric and nonlinear.</dt>
 * <dd>The final results of using tree methods for classification or regression
 * can be summarized in a series of (usually few) logical if-then conditions
 * (tree nodes). Therefore, there is no implicit assumption that the underlying
 * relationships between the predictor variables and the dependent variable
 * are linear, follow some specific non-linear link function, or that they
 * are even monotonic in nature. Thus, tree methods are particularly well
 * suited for data mining tasks, where there is often little a priori
 * knowledge nor any coherent set of theories or predictions regarding which
 * variables are related and how. In those types of data analytics, tree
 * methods can often reveal simple relationships between just a few variables
 * that could have easily gone unnoticed using other analytic techniques.</dd>
 * </dl>
 * One major problem with classification and regression trees is their high
 * variance. Often a small change in the data can result in a very different
 * series of splits, making interpretation somewhat precarious. Besides,
 * decision-tree learners can create over-complex trees that cause over-fitting.
 * Mechanisms such as pruning are necessary to avoid this problem.
 * Another limitation of trees is the lack of smoothness of the prediction
 * surface.
 * <p>
 * Some techniques such as bagging, boosting, and random forest use more than
 * one decision tree for their analysis.
 * 
 * @see AdaBoost
 * @see GradientTreeBoost
 * @see RandomForest
 * 
 * @author Haifeng Li
 */
public class DecisionTree extends CART implements SoftClassifier<Tuple> {
    private static final long serialVersionUID = 2L;

    /**
     * The splitting rule.
     */
    private SplitRule rule = SplitRule.GINI;
    /**
     * The number of classes.
     */
    private int k = 2;

    @Override
    protected double impurity(LeafNode node) {
        return ((DecisionNode) node).impurity(rule);
    }

    @Override
    protected LeafNode newNode() {
        return new DecisionNode(new int[k]);
    }

    @Override
    protected void updateNode(LeafNode leaf, int i) {
        DecisionNode node = (DecisionNode) leaf;
        node.add(y.getInt(i), samples[i]);
    }

    @Override
    protected void calculateOutput(LeafNode leaf) {
        DecisionNode node = (DecisionNode) leaf;
        node.calculateOutput();
    }

    @Override
    protected Optional<Split> findBestSplit(int column, double impurity, int lo, int hi) {
        return Optional.empty();
    }

    /**
     * Constructor. Learns a classification tree for AdaBoost and Random Forest.
     * @param formula a symbolic description of the model to be fitted.
     * @param data the data frame of the explanatory and response variables.
     * @param nodeSize the minimum size of leaf nodes.
     * @param maxNodes the maximum number of leaf nodes in the tree.
     * @param mtry the number of input variables to pick to split on at each
     * node. It seems that sqrt(p) give generally good performance, where p
     * is the number of variables.
     * @param rule the splitting rule.
     * @param order the index of training values in ascending order. Note
     * that only numeric attributes need be sorted.
     * @param samples the sample set of instances for stochastic learning.
     * samples[i] is the number of sampling for instance i.
     */
    public DecisionTree(Formula formula, DataFrame data, SplitRule rule, int nodeSize, int maxNodes, int mtry, int[] samples, int[][] order) {
        super(formula, data, nodeSize, maxNodes, mtry, samples, order);
        this.rule = rule;

        // class label set.
        int[] y = this.y.toIntArray();
        int[] labels = MathEx.unique(y);
        Arrays.sort(labels);
        
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] < 0) {
                throw new IllegalArgumentException("Negative class label: " + labels[i]);
            }

            if (labels[i] != i) {
                throw new IllegalArgumentException("Missing class: " + i);
            }
        }

        k = labels.length;
        if (k < 2) {
            throw new IllegalArgumentException("Only one class.");            
        }

        int[] count = new int[k];
        for (int i = 0; i < samples.length; i++) {
            count[y[i]] += samples[i];
        }

        LeafNode node = new DecisionNode(count);
        this.root = node;

        Optional<Split> split = findBestSplit(node, 0, index.length, null);

        if (maxNodes == Integer.MAX_VALUE) {
            // deep-first split
            split.map(s -> split(s, null));
        } else {
            // best-first split
            PriorityQueue<Split> queue = new PriorityQueue<>(2 * maxNodes, Split.comparator.reversed());
            split.map(s -> queue.add(s));

            for (int leaves = 1; leaves < this.maxNodes && !queue.isEmpty(); ) {
                if (split(queue.poll(), queue)) leaves++;
            }
        }

        clear();
    }

    /**
     * Learns a classification tree for AdaBoost and Random Forest.
     * @param formula a symbolic description of the model to be fitted.
     * @param data the data frame of the explanatory and response variables.
     */
    public static DecisionTree fit(Formula formula, DataFrame data) {
        return fit(formula, data, new Properties());
    }

    /**
     * Learns a classification tree for AdaBoost and Random Forest.
     * The hyper-parameters in <code>prop</code> include
     * <ul>
     * <li><code>smile.cart.split.rule</code>
     * <li><code>smile.cart.node.size</code>
     * <li><code>smile.cart.max.nodes</code>
     * <li><code>smile.cart.mtry</code>
     * </ul>
     * @param formula a symbolic description of the model to be fitted.
     * @param data the data frame of the explanatory and response variables.
     * @param prop Training algorithm hyper-parameters and properties.
     */
    public static DecisionTree fit(Formula formula, DataFrame data, Properties prop) {
        SplitRule rule = SplitRule.valueOf(prop.getProperty("smile.cart.split.rule", "GINI"));
        int nodeSize = Integer.parseInt(prop.getProperty("smile.cart.node.size", "5"));
        int maxNodes = Integer.parseInt(prop.getProperty("smile.cart.max.nodes", "6"));
        int mtry = Integer.parseInt(prop.getProperty("smile.cart.mtry", "-1"));
        return fit(formula, data, rule, nodeSize, maxNodes);
    }

    /**
     * Learns a classification tree for AdaBoost and Random Forest.
     * @param formula a symbolic description of the model to be fitted.
     * @param data the data frame of the explanatory and response variables.
     * @param rule the splitting rule.
     * @param nodeSize the minimum size of leaf nodes.
     * @param maxNodes the maximum number of leaf nodes in the tree.
     */
    public static DecisionTree fit(Formula formula, DataFrame data, SplitRule rule, int nodeSize, int maxNodes) {
        return new DecisionTree(formula, data, rule, nodeSize, maxNodes, -1, null, null);
    }

    @Override
    public int predict(Tuple x) {
        DecisionNode leaf = (DecisionNode) root.predict(x);
        return leaf.output();
    }

    /**
     * Predicts the class label of an instance and also calculate a posteriori
     * probabilities. The posteriori estimation is based on sample distribution
     * in the leaf node. It is not accurate at all when be used in a single tree.
     * It is mainly used by RandomForest in an ensemble way.
     */
    @Override
    public int predict(Tuple x, double[] posteriori) {
        DecisionNode leaf = (DecisionNode) root.predict(x);
        // add-k smoothing
        double n = leaf.size() + k;
        int[] count = leaf.count();
        for (int i = 0; i < count.length; i++) {
            posteriori[i] = (count[i] + 1) / n;
        }
        return leaf.output();
    }
}
