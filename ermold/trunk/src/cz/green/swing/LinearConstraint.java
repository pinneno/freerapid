package cz.green.swing;

public class LinearConstraint extends ConstantConstraint {
    private double startMultiplier = 0.0;
    double sizeMultiplier = 0.0;

    LinearConstraint() {
        super();
    }

    LinearConstraint(double startMultiplier, double sizeMultiplier) {
        super();
        this.startMultiplier = startMultiplier;
        this.sizeMultiplier = sizeMultiplier;
    }

    public LinearConstraint(int startConstant, double startMultiplier,
                            int sizeConstant, double sizeMultiplier) {
        super(startConstant, sizeConstant);
        this.startMultiplier = startMultiplier;
        this.sizeMultiplier = sizeMultiplier;
    }

    LinearConstraint(int startConstant, int sizeConstant) {
        super(startConstant, sizeConstant);
    }

    public int getParentSize(int compSize) {
        int v1, v2;
        try {
            v1 = (int) ((double) (-start) / startMultiplier);
        } catch (ArithmeticException x) {
            v1 = start;
        }
        try {
            v2 = (int) ((double) (compSize - size) / sizeMultiplier);
        } catch (ArithmeticException x) {
            v2 = size;
        }
        return Math.max(v1, v2);
    }

    public int getSize(int parent) {
        return ((int) (sizeMultiplier * parent)) + size;
    }

    public int getStart(int parent) {
        return ((int) (startMultiplier * parent)) + start;
    }
}
