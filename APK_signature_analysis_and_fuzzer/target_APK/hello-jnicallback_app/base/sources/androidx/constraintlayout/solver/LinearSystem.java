package androidx.constraintlayout.solver;

import androidx.constraintlayout.solver.SolverVariable;
import androidx.constraintlayout.solver.widgets.ConstraintAnchor;
import androidx.constraintlayout.solver.widgets.ConstraintWidget;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
/* loaded from: classes.dex */
public class LinearSystem {
    private static final boolean DEBUG = false;
    public static final boolean FULL_DEBUG = false;
    private static int POOL_SIZE = 1000;
    public static Metrics sMetrics;
    final Cache mCache;
    private Row mGoal;
    ArrayRow[] mRows;
    private final Row mTempGoal;
    int mVariablesID = 0;
    private HashMap<String, SolverVariable> mVariables = null;
    private int TABLE_SIZE = 32;
    private int mMaxColumns = 32;
    public boolean graphOptimizer = false;
    private boolean[] mAlreadyTestedCandidates = new boolean[32];
    int mNumColumns = 1;
    int mNumRows = 0;
    private int mMaxRows = 32;
    private SolverVariable[] mPoolVariables = new SolverVariable[POOL_SIZE];
    private int mPoolVariablesCount = 0;
    private ArrayRow[] tempClientsCopy = new ArrayRow[32];

    /* loaded from: classes.dex */
    public interface Row {
        void addError(SolverVariable solverVariable);

        void clear();

        SolverVariable getKey();

        SolverVariable getPivotCandidate(LinearSystem linearSystem, boolean[] zArr);

        void initFromRow(Row row);

        boolean isEmpty();
    }

    public LinearSystem() {
        this.mRows = null;
        this.mRows = new ArrayRow[32];
        releaseRows();
        Cache cache = new Cache();
        this.mCache = cache;
        this.mGoal = new GoalRow(cache);
        this.mTempGoal = new ArrayRow(cache);
    }

    public void fillMetrics(Metrics metrics) {
        sMetrics = metrics;
    }

    public static Metrics getMetrics() {
        return sMetrics;
    }

    private void increaseTableSize() {
        int i = this.TABLE_SIZE * 2;
        this.TABLE_SIZE = i;
        this.mRows = (ArrayRow[]) Arrays.copyOf(this.mRows, i);
        Cache cache = this.mCache;
        cache.mIndexedVariables = (SolverVariable[]) Arrays.copyOf(cache.mIndexedVariables, this.TABLE_SIZE);
        int i2 = this.TABLE_SIZE;
        this.mAlreadyTestedCandidates = new boolean[i2];
        this.mMaxColumns = i2;
        this.mMaxRows = i2;
        Metrics metrics = sMetrics;
        if (metrics != null) {
            metrics.tableSizeIncrease++;
            Metrics metrics2 = sMetrics;
            metrics2.maxTableSize = Math.max(metrics2.maxTableSize, this.TABLE_SIZE);
            Metrics metrics3 = sMetrics;
            metrics3.lastTableSize = metrics3.maxTableSize;
        }
    }

    private void releaseRows() {
        int i = 0;
        while (true) {
            ArrayRow[] arrayRowArr = this.mRows;
            if (i < arrayRowArr.length) {
                ArrayRow row = arrayRowArr[i];
                if (row != null) {
                    this.mCache.arrayRowPool.release(row);
                }
                this.mRows[i] = null;
                i++;
            } else {
                return;
            }
        }
    }

    public void reset() {
        for (int i = 0; i < this.mCache.mIndexedVariables.length; i++) {
            SolverVariable variable = this.mCache.mIndexedVariables[i];
            if (variable != null) {
                variable.reset();
            }
        }
        this.mCache.solverVariablePool.releaseAll(this.mPoolVariables, this.mPoolVariablesCount);
        this.mPoolVariablesCount = 0;
        Arrays.fill(this.mCache.mIndexedVariables, (Object) null);
        HashMap<String, SolverVariable> hashMap = this.mVariables;
        if (hashMap != null) {
            hashMap.clear();
        }
        this.mVariablesID = 0;
        this.mGoal.clear();
        this.mNumColumns = 1;
        for (int i2 = 0; i2 < this.mNumRows; i2++) {
            this.mRows[i2].used = false;
        }
        releaseRows();
        this.mNumRows = 0;
    }

    public SolverVariable createObjectVariable(Object anchor) {
        if (anchor == null) {
            return null;
        }
        if (this.mNumColumns + 1 >= this.mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = null;
        if (anchor instanceof ConstraintAnchor) {
            variable = ((ConstraintAnchor) anchor).getSolverVariable();
            if (variable == null) {
                ((ConstraintAnchor) anchor).resetSolverVariable(this.mCache);
                variable = ((ConstraintAnchor) anchor).getSolverVariable();
            }
            if (variable.id == -1 || variable.id > this.mVariablesID || this.mCache.mIndexedVariables[variable.id] == null) {
                if (variable.id != -1) {
                    variable.reset();
                }
                int i = this.mVariablesID + 1;
                this.mVariablesID = i;
                this.mNumColumns++;
                variable.id = i;
                variable.mType = SolverVariable.Type.UNRESTRICTED;
                this.mCache.mIndexedVariables[this.mVariablesID] = variable;
            }
        }
        return variable;
    }

    public ArrayRow createRow() {
        ArrayRow row = this.mCache.arrayRowPool.acquire();
        if (row == null) {
            row = new ArrayRow(this.mCache);
        } else {
            row.reset();
        }
        SolverVariable.increaseErrorId();
        return row;
    }

    public SolverVariable createSlackVariable() {
        Metrics metrics = sMetrics;
        if (metrics != null) {
            metrics.slackvariables++;
        }
        if (this.mNumColumns + 1 >= this.mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(SolverVariable.Type.SLACK, null);
        int i = this.mVariablesID + 1;
        this.mVariablesID = i;
        this.mNumColumns++;
        variable.id = i;
        this.mCache.mIndexedVariables[this.mVariablesID] = variable;
        return variable;
    }

    public SolverVariable createExtraVariable() {
        Metrics metrics = sMetrics;
        if (metrics != null) {
            metrics.extravariables++;
        }
        if (this.mNumColumns + 1 >= this.mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(SolverVariable.Type.SLACK, null);
        int i = this.mVariablesID + 1;
        this.mVariablesID = i;
        this.mNumColumns++;
        variable.id = i;
        this.mCache.mIndexedVariables[this.mVariablesID] = variable;
        return variable;
    }

    private void addError(ArrayRow row) {
        row.addError(this, 0);
    }

    private void addSingleError(ArrayRow row, int sign) {
        addSingleError(row, sign, 0);
    }

    void addSingleError(ArrayRow row, int sign, int strength) {
        SolverVariable error = createErrorVariable(strength, null);
        row.addSingleError(error, sign);
    }

    private SolverVariable createVariable(String name, SolverVariable.Type type) {
        Metrics metrics = sMetrics;
        if (metrics != null) {
            metrics.variables++;
        }
        if (this.mNumColumns + 1 >= this.mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(type, null);
        variable.setName(name);
        int i = this.mVariablesID + 1;
        this.mVariablesID = i;
        this.mNumColumns++;
        variable.id = i;
        if (this.mVariables == null) {
            this.mVariables = new HashMap<>();
        }
        this.mVariables.put(name, variable);
        this.mCache.mIndexedVariables[this.mVariablesID] = variable;
        return variable;
    }

    public SolverVariable createErrorVariable(int strength, String prefix) {
        Metrics metrics = sMetrics;
        if (metrics != null) {
            metrics.errors++;
        }
        if (this.mNumColumns + 1 >= this.mMaxColumns) {
            increaseTableSize();
        }
        SolverVariable variable = acquireSolverVariable(SolverVariable.Type.ERROR, prefix);
        int i = this.mVariablesID + 1;
        this.mVariablesID = i;
        this.mNumColumns++;
        variable.id = i;
        variable.strength = strength;
        this.mCache.mIndexedVariables[this.mVariablesID] = variable;
        this.mGoal.addError(variable);
        return variable;
    }

    private SolverVariable acquireSolverVariable(SolverVariable.Type type, String prefix) {
        SolverVariable variable = this.mCache.solverVariablePool.acquire();
        if (variable == null) {
            variable = new SolverVariable(type, prefix);
            variable.setType(type, prefix);
        } else {
            variable.reset();
            variable.setType(type, prefix);
        }
        int i = this.mPoolVariablesCount;
        int i2 = POOL_SIZE;
        if (i >= i2) {
            int i3 = i2 * 2;
            POOL_SIZE = i3;
            this.mPoolVariables = (SolverVariable[]) Arrays.copyOf(this.mPoolVariables, i3);
        }
        SolverVariable[] solverVariableArr = this.mPoolVariables;
        int i4 = this.mPoolVariablesCount;
        this.mPoolVariablesCount = i4 + 1;
        solverVariableArr[i4] = variable;
        return variable;
    }

    Row getGoal() {
        return this.mGoal;
    }

    ArrayRow getRow(int n) {
        return this.mRows[n];
    }

    float getValueFor(String name) {
        SolverVariable v = getVariable(name, SolverVariable.Type.UNRESTRICTED);
        if (v == null) {
            return 0.0f;
        }
        return v.computedValue;
    }

    public int getObjectVariableValue(Object anchor) {
        SolverVariable variable = ((ConstraintAnchor) anchor).getSolverVariable();
        if (variable != null) {
            return (int) (variable.computedValue + 0.5f);
        }
        return 0;
    }

    SolverVariable getVariable(String name, SolverVariable.Type type) {
        if (this.mVariables == null) {
            this.mVariables = new HashMap<>();
        }
        SolverVariable variable = this.mVariables.get(name);
        if (variable == null) {
            return createVariable(name, type);
        }
        return variable;
    }

    public void minimize() throws Exception {
        Metrics metrics = sMetrics;
        if (metrics != null) {
            metrics.minimize++;
        }
        if (this.graphOptimizer) {
            Metrics metrics2 = sMetrics;
            if (metrics2 != null) {
                metrics2.graphOptimizer++;
            }
            boolean fullySolved = true;
            int i = 0;
            while (true) {
                if (i >= this.mNumRows) {
                    break;
                }
                ArrayRow r = this.mRows[i];
                if (r.isSimpleDefinition) {
                    i++;
                } else {
                    fullySolved = false;
                    break;
                }
            }
            if (!fullySolved) {
                minimizeGoal(this.mGoal);
                return;
            }
            Metrics metrics3 = sMetrics;
            if (metrics3 != null) {
                metrics3.fullySolved++;
            }
            computeValues();
            return;
        }
        minimizeGoal(this.mGoal);
    }

    void minimizeGoal(Row goal) throws Exception {
        Metrics metrics = sMetrics;
        if (metrics != null) {
            metrics.minimizeGoal++;
            Metrics metrics2 = sMetrics;
            metrics2.maxVariables = Math.max(metrics2.maxVariables, this.mNumColumns);
            Metrics metrics3 = sMetrics;
            metrics3.maxRows = Math.max(metrics3.maxRows, this.mNumRows);
        }
        updateRowFromVariables((ArrayRow) goal);
        enforceBFS(goal);
        optimize(goal, false);
        computeValues();
    }

    private final void updateRowFromVariables(ArrayRow row) {
        if (this.mNumRows > 0) {
            row.variables.updateFromSystem(row, this.mRows);
            if (row.variables.currentSize == 0) {
                row.isSimpleDefinition = true;
            }
        }
    }

    public void addConstraint(ArrayRow row) {
        SolverVariable pivotCandidate;
        if (row == null) {
            return;
        }
        Metrics metrics = sMetrics;
        if (metrics != null) {
            metrics.constraints++;
            if (row.isSimpleDefinition) {
                sMetrics.simpleconstraints++;
            }
        }
        if (this.mNumRows + 1 >= this.mMaxRows || this.mNumColumns + 1 >= this.mMaxColumns) {
            increaseTableSize();
        }
        boolean added = false;
        if (!row.isSimpleDefinition) {
            updateRowFromVariables(row);
            if (row.isEmpty()) {
                return;
            }
            row.ensurePositiveConstant();
            if (row.chooseSubject(this)) {
                SolverVariable extra = createExtraVariable();
                row.variable = extra;
                addRow(row);
                added = true;
                this.mTempGoal.initFromRow(row);
                optimize(this.mTempGoal, true);
                if (extra.definitionId == -1) {
                    if (row.variable == extra && (pivotCandidate = row.pickPivot(extra)) != null) {
                        Metrics metrics2 = sMetrics;
                        if (metrics2 != null) {
                            metrics2.pivots++;
                        }
                        row.pivot(pivotCandidate);
                    }
                    if (!row.isSimpleDefinition) {
                        row.variable.updateReferencesWithNewDefinition(row);
                    }
                    this.mNumRows--;
                }
            }
            if (!row.hasKeyVariable()) {
                return;
            }
        }
        if (!added) {
            addRow(row);
        }
    }

    private final void addRow(ArrayRow row) {
        if (this.mRows[this.mNumRows] != null) {
            this.mCache.arrayRowPool.release(this.mRows[this.mNumRows]);
        }
        this.mRows[this.mNumRows] = row;
        row.variable.definitionId = this.mNumRows;
        this.mNumRows++;
        row.variable.updateReferencesWithNewDefinition(row);
    }

    private final int optimize(Row goal, boolean b) {
        Metrics metrics = sMetrics;
        if (metrics != null) {
            metrics.optimize++;
        }
        boolean done = false;
        int tries = 0;
        for (int i = 0; i < this.mNumColumns; i++) {
            this.mAlreadyTestedCandidates[i] = false;
        }
        while (!done) {
            Metrics metrics2 = sMetrics;
            if (metrics2 != null) {
                metrics2.iterations++;
            }
            tries++;
            if (tries >= this.mNumColumns * 2) {
                return tries;
            }
            if (goal.getKey() != null) {
                this.mAlreadyTestedCandidates[goal.getKey().id] = true;
            }
            SolverVariable pivotCandidate = goal.getPivotCandidate(this, this.mAlreadyTestedCandidates);
            if (pivotCandidate != null) {
                if (this.mAlreadyTestedCandidates[pivotCandidate.id]) {
                    return tries;
                }
                this.mAlreadyTestedCandidates[pivotCandidate.id] = true;
            }
            if (pivotCandidate != null) {
                float min = Float.MAX_VALUE;
                int pivotRowIndex = -1;
                for (int i2 = 0; i2 < this.mNumRows; i2++) {
                    ArrayRow current = this.mRows[i2];
                    SolverVariable variable = current.variable;
                    if (variable.mType != SolverVariable.Type.UNRESTRICTED && !current.isSimpleDefinition && current.hasVariable(pivotCandidate)) {
                        float a_j = current.variables.get(pivotCandidate);
                        if (a_j < 0.0f) {
                            float value = (-current.constantValue) / a_j;
                            if (value < min) {
                                min = value;
                                pivotRowIndex = i2;
                            }
                        }
                    }
                }
                if (pivotRowIndex > -1) {
                    ArrayRow pivotEquation = this.mRows[pivotRowIndex];
                    pivotEquation.variable.definitionId = -1;
                    Metrics metrics3 = sMetrics;
                    if (metrics3 != null) {
                        metrics3.pivots++;
                    }
                    pivotEquation.pivot(pivotCandidate);
                    pivotEquation.variable.definitionId = pivotRowIndex;
                    pivotEquation.variable.updateReferencesWithNewDefinition(pivotEquation);
                } else {
                    done = true;
                }
            } else {
                done = true;
            }
        }
        return tries;
    }

    private int enforceBFS(Row goal) throws Exception {
        float f;
        int tries = 0;
        boolean infeasibleSystem = false;
        int i = 0;
        while (true) {
            f = 0.0f;
            if (i >= this.mNumRows) {
                break;
            }
            SolverVariable variable = this.mRows[i].variable;
            if (variable.mType == SolverVariable.Type.UNRESTRICTED || this.mRows[i].constantValue >= 0.0f) {
                i++;
            } else {
                infeasibleSystem = true;
                break;
            }
        }
        if (infeasibleSystem) {
            boolean done = false;
            tries = 0;
            while (!done) {
                Metrics metrics = sMetrics;
                if (metrics != null) {
                    metrics.bfs++;
                }
                tries++;
                float min = Float.MAX_VALUE;
                int strength = 0;
                int pivotRowIndex = -1;
                int pivotColumnIndex = -1;
                int i2 = 0;
                while (i2 < this.mNumRows) {
                    ArrayRow current = this.mRows[i2];
                    SolverVariable variable2 = current.variable;
                    if (variable2.mType != SolverVariable.Type.UNRESTRICTED && !current.isSimpleDefinition && current.constantValue < f) {
                        int j = 1;
                        while (j < this.mNumColumns) {
                            SolverVariable candidate = this.mCache.mIndexedVariables[j];
                            float a_j = current.variables.get(candidate);
                            if (a_j > f) {
                                for (int k = 0; k < 7; k++) {
                                    float value = candidate.strengthVector[k] / a_j;
                                    if ((value < min && k == strength) || k > strength) {
                                        min = value;
                                        pivotRowIndex = i2;
                                        pivotColumnIndex = j;
                                        strength = k;
                                    }
                                }
                            }
                            j++;
                            f = 0.0f;
                        }
                    }
                    i2++;
                    f = 0.0f;
                }
                if (pivotRowIndex != -1) {
                    ArrayRow pivotEquation = this.mRows[pivotRowIndex];
                    pivotEquation.variable.definitionId = -1;
                    Metrics metrics2 = sMetrics;
                    if (metrics2 != null) {
                        metrics2.pivots++;
                    }
                    pivotEquation.pivot(this.mCache.mIndexedVariables[pivotColumnIndex]);
                    pivotEquation.variable.definitionId = pivotRowIndex;
                    pivotEquation.variable.updateReferencesWithNewDefinition(pivotEquation);
                } else {
                    done = true;
                }
                if (tries > this.mNumColumns / 2) {
                    done = true;
                }
                f = 0.0f;
            }
        }
        return tries;
    }

    private void computeValues() {
        for (int i = 0; i < this.mNumRows; i++) {
            ArrayRow row = this.mRows[i];
            row.variable.computedValue = row.constantValue;
        }
    }

    private void displayRows() {
        displaySolverVariables();
        String s = "";
        for (int i = 0; i < this.mNumRows; i++) {
            s = (s + this.mRows[i]) + "\n";
        }
        System.out.println(s + this.mGoal + "\n");
    }

    void displayReadableRows() {
        displaySolverVariables();
        String s = " #  ";
        for (int i = 0; i < this.mNumRows; i++) {
            s = (s + this.mRows[i].toReadableString()) + "\n #  ";
        }
        if (this.mGoal != null) {
            s = s + this.mGoal + "\n";
        }
        System.out.println(s);
    }

    public void displayVariablesReadableRows() {
        displaySolverVariables();
        String s = "";
        for (int i = 0; i < this.mNumRows; i++) {
            if (this.mRows[i].variable.mType == SolverVariable.Type.UNRESTRICTED) {
                s = (s + this.mRows[i].toReadableString()) + "\n";
            }
        }
        System.out.println(s + this.mGoal + "\n");
    }

    public int getMemoryUsed() {
        int actualRowSize = 0;
        for (int i = 0; i < this.mNumRows; i++) {
            ArrayRow[] arrayRowArr = this.mRows;
            if (arrayRowArr[i] != null) {
                actualRowSize += arrayRowArr[i].sizeInBytes();
            }
        }
        return actualRowSize;
    }

    public int getNumEquations() {
        return this.mNumRows;
    }

    public int getNumVariables() {
        return this.mVariablesID;
    }

    void displaySystemInformations() {
        int rowSize = 0;
        for (int i = 0; i < this.TABLE_SIZE; i++) {
            ArrayRow[] arrayRowArr = this.mRows;
            if (arrayRowArr[i] != null) {
                rowSize += arrayRowArr[i].sizeInBytes();
            }
        }
        int actualRowSize = 0;
        for (int i2 = 0; i2 < this.mNumRows; i2++) {
            ArrayRow[] arrayRowArr2 = this.mRows;
            if (arrayRowArr2[i2] != null) {
                actualRowSize += arrayRowArr2[i2].sizeInBytes();
            }
        }
        PrintStream printStream = System.out;
        StringBuilder append = new StringBuilder().append("Linear System -> Table size: ").append(this.TABLE_SIZE).append(" (");
        int i3 = this.TABLE_SIZE;
        printStream.println(append.append(getDisplaySize(i3 * i3)).append(") -- row sizes: ").append(getDisplaySize(rowSize)).append(", actual size: ").append(getDisplaySize(actualRowSize)).append(" rows: ").append(this.mNumRows).append("/").append(this.mMaxRows).append(" cols: ").append(this.mNumColumns).append("/").append(this.mMaxColumns).append(" ").append(0).append(" occupied cells, ").append(getDisplaySize(0)).toString());
    }

    private void displaySolverVariables() {
        String s = "Display Rows (" + this.mNumRows + "x" + this.mNumColumns + ")\n";
        System.out.println(s);
    }

    private String getDisplaySize(int n) {
        int mb = ((n * 4) / 1024) / 1024;
        if (mb > 0) {
            return "" + mb + " Mb";
        }
        int kb = (n * 4) / 1024;
        return kb > 0 ? "" + kb + " Kb" : "" + (n * 4) + " bytes";
    }

    public Cache getCache() {
        return this.mCache;
    }

    private String getDisplayStrength(int strength) {
        if (strength == 1) {
            return "LOW";
        }
        if (strength == 2) {
            return "MEDIUM";
        }
        if (strength == 3) {
            return "HIGH";
        }
        if (strength == 4) {
            return "HIGHEST";
        }
        if (strength == 5) {
            return "EQUALITY";
        }
        if (strength == 6) {
            return "FIXED";
        }
        return "NONE";
    }

    public void addGreaterThan(SolverVariable a, SolverVariable b, int margin, int strength) {
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = 0;
        row.createRowGreaterThan(a, b, slack, margin);
        if (strength != 6) {
            float slackValue = row.variables.get(slack);
            addSingleError(row, (int) ((-1.0f) * slackValue), strength);
        }
        addConstraint(row);
    }

    public void addGreaterThan(SolverVariable a, int b) {
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = 0;
        row.createRowGreaterThan(a, b, slack);
        addConstraint(row);
    }

    public void addGreaterBarrier(SolverVariable a, SolverVariable b, boolean hasMatchConstraintWidgets) {
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = 0;
        row.createRowGreaterThan(a, b, slack, 0);
        if (hasMatchConstraintWidgets) {
            float slackValue = row.variables.get(slack);
            addSingleError(row, (int) ((-1.0f) * slackValue), 1);
        }
        addConstraint(row);
    }

    public void addLowerThan(SolverVariable a, SolverVariable b, int margin, int strength) {
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = 0;
        row.createRowLowerThan(a, b, slack, margin);
        if (strength != 6) {
            float slackValue = row.variables.get(slack);
            addSingleError(row, (int) ((-1.0f) * slackValue), strength);
        }
        addConstraint(row);
    }

    public void addLowerBarrier(SolverVariable a, SolverVariable b, boolean hasMatchConstraintWidgets) {
        ArrayRow row = createRow();
        SolverVariable slack = createSlackVariable();
        slack.strength = 0;
        row.createRowLowerThan(a, b, slack, 0);
        if (hasMatchConstraintWidgets) {
            float slackValue = row.variables.get(slack);
            addSingleError(row, (int) ((-1.0f) * slackValue), 1);
        }
        addConstraint(row);
    }

    public void addCentering(SolverVariable a, SolverVariable b, int m1, float bias, SolverVariable c, SolverVariable d, int m2, int strength) {
        ArrayRow row = createRow();
        row.createRowCentering(a, b, m1, bias, c, d, m2);
        if (strength != 6) {
            row.addError(this, strength);
        }
        addConstraint(row);
    }

    public void addRatio(SolverVariable a, SolverVariable b, SolverVariable c, SolverVariable d, float ratio, int strength) {
        ArrayRow row = createRow();
        row.createRowDimensionRatio(a, b, c, d, ratio);
        if (strength != 6) {
            row.addError(this, strength);
        }
        addConstraint(row);
    }

    public ArrayRow addEquality(SolverVariable a, SolverVariable b, int margin, int strength) {
        ArrayRow row = createRow();
        row.createRowEquals(a, b, margin);
        if (strength != 6) {
            row.addError(this, strength);
        }
        addConstraint(row);
        return row;
    }

    public void addEquality(SolverVariable a, int value) {
        int idx = a.definitionId;
        if (a.definitionId != -1) {
            ArrayRow row = this.mRows[idx];
            if (row.isSimpleDefinition) {
                row.constantValue = value;
                return;
            } else if (row.variables.currentSize == 0) {
                row.isSimpleDefinition = true;
                row.constantValue = value;
                return;
            } else {
                ArrayRow newRow = createRow();
                newRow.createRowEquals(a, value);
                addConstraint(newRow);
                return;
            }
        }
        ArrayRow row2 = createRow();
        row2.createRowDefinition(a, value);
        addConstraint(row2);
    }

    public void addEquality(SolverVariable a, int value, int strength) {
        int idx = a.definitionId;
        if (a.definitionId != -1) {
            ArrayRow row = this.mRows[idx];
            if (row.isSimpleDefinition) {
                row.constantValue = value;
                return;
            }
            ArrayRow newRow = createRow();
            newRow.createRowEquals(a, value);
            newRow.addError(this, strength);
            addConstraint(newRow);
            return;
        }
        ArrayRow row2 = createRow();
        row2.createRowDefinition(a, value);
        row2.addError(this, strength);
        addConstraint(row2);
    }

    public static ArrayRow createRowEquals(LinearSystem linearSystem, SolverVariable variableA, SolverVariable variableB, int margin, boolean withError) {
        ArrayRow row = linearSystem.createRow();
        row.createRowEquals(variableA, variableB, margin);
        if (withError) {
            linearSystem.addSingleError(row, 1);
        }
        return row;
    }

    public static ArrayRow createRowDimensionPercent(LinearSystem linearSystem, SolverVariable variableA, SolverVariable variableB, SolverVariable variableC, float percent, boolean withError) {
        ArrayRow row = linearSystem.createRow();
        if (withError) {
            linearSystem.addError(row);
        }
        return row.createRowDimensionPercent(variableA, variableB, variableC, percent);
    }

    public static ArrayRow createRowGreaterThan(LinearSystem linearSystem, SolverVariable variableA, SolverVariable variableB, int margin, boolean withError) {
        SolverVariable slack = linearSystem.createSlackVariable();
        ArrayRow row = linearSystem.createRow();
        row.createRowGreaterThan(variableA, variableB, slack, margin);
        if (withError) {
            float slackValue = row.variables.get(slack);
            linearSystem.addSingleError(row, (int) ((-1.0f) * slackValue));
        }
        return row;
    }

    public static ArrayRow createRowLowerThan(LinearSystem linearSystem, SolverVariable variableA, SolverVariable variableB, int margin, boolean withError) {
        SolverVariable slack = linearSystem.createSlackVariable();
        ArrayRow row = linearSystem.createRow();
        row.createRowLowerThan(variableA, variableB, slack, margin);
        if (withError) {
            float slackValue = row.variables.get(slack);
            linearSystem.addSingleError(row, (int) ((-1.0f) * slackValue));
        }
        return row;
    }

    public static ArrayRow createRowCentering(LinearSystem linearSystem, SolverVariable variableA, SolverVariable variableB, int marginA, float bias, SolverVariable variableC, SolverVariable variableD, int marginB, boolean withError) {
        ArrayRow row = linearSystem.createRow();
        row.createRowCentering(variableA, variableB, marginA, bias, variableC, variableD, marginB);
        if (withError) {
            row.addError(linearSystem, 4);
        }
        return row;
    }

    public void addCenterPoint(ConstraintWidget widget, ConstraintWidget target, float angle, int radius) {
        SolverVariable Al = createObjectVariable(widget.getAnchor(ConstraintAnchor.Type.LEFT));
        SolverVariable At = createObjectVariable(widget.getAnchor(ConstraintAnchor.Type.TOP));
        SolverVariable Ar = createObjectVariable(widget.getAnchor(ConstraintAnchor.Type.RIGHT));
        SolverVariable Ab = createObjectVariable(widget.getAnchor(ConstraintAnchor.Type.BOTTOM));
        SolverVariable Bl = createObjectVariable(target.getAnchor(ConstraintAnchor.Type.LEFT));
        SolverVariable Bt = createObjectVariable(target.getAnchor(ConstraintAnchor.Type.TOP));
        SolverVariable Br = createObjectVariable(target.getAnchor(ConstraintAnchor.Type.RIGHT));
        SolverVariable Bb = createObjectVariable(target.getAnchor(ConstraintAnchor.Type.BOTTOM));
        ArrayRow row = createRow();
        float angleComponent = (float) (Math.sin(angle) * radius);
        row.createRowWithAngle(At, Ab, Bt, Bb, angleComponent);
        addConstraint(row);
        ArrayRow row2 = createRow();
        float angleComponent2 = (float) (Math.cos(angle) * radius);
        row2.createRowWithAngle(Al, Ar, Bl, Br, angleComponent2);
        addConstraint(row2);
    }
}
