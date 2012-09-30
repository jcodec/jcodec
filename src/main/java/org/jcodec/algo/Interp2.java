package org.jcodec.algo;

//Here is the Java code for my interpolation applet on 
//
//   http://www.wam.umd.edu/~petersd/interp.html
//
//************************************************************************
//Interp2 Applet by T. von Petersdorff
//
//Tobias von Petersdorff          
//Department of Mathematics       
//University of Maryland          
//College Park, MD 20742          
//e-mail:       tvp@math.umd.edu                 
//WWW:          http://www.wam.umd.edu/~petersd/ 
//PGP public key available via finger and WWW    
//************************************************************************
//part of the code stolen from
//       Curve Applet by Michael Heinrichs
//Bug: You can drag points out of the drawing area. But then you have
//   no way of moving them. 

import java.awt.*;
import java.applet.*;

import java.util.Vector;

public class Interp2 extends Applet {
    public void init() {
        setLayout(new BorderLayout());
        InterpPanel dp = new InterpPanel();
        add("Center", dp);
        add("South", new CurveControls(dp));
        add("North", new CurveControls2(dp));
    }

    public boolean handleEvent(Event e) {
        switch (e.id) {
        case Event.WINDOW_DESTROY:
            System.exit(0);
            return true;
        default:
            return false;
        }
    }

    public static void main(String args[]) {
        Frame f = new Frame("Interp");
        Interp2 interp = new Interp2();
        interp.init();
        interp.start();

        f.add("Center", interp);
        f.show();
    }
}

class ControlPoint extends Object {
    public int x;
    public int y;
    public static final int PT_SIZE = 4;

    public ControlPoint(int a, int b) {
        x = a;
        y = b;
    }

    public boolean within(int a, int b) {
        if (a >= x - PT_SIZE && b >= y - PT_SIZE && a <= x + PT_SIZE && b <= y + PT_SIZE)
            return true;
        else
            return false;
    }
}

class InterpPanel extends Panel {
    public static final int POLY = 0;
    public static final int NAT_SPL = 1;
    public static final int NAK_SPL = 2;
    private int mode = POLY;

    public static final int ADD = 0;
    public static final int MOVE = 1;
    public static final int DELETE = 2;
    private int action = ADD;

    private Vector points = new Vector(16, 4);

    // If a control point is being moved, this is the index into the list
    // of the moving point. Otherwise it contains -1
    private int moving_point;
    private int precision = 10;

    public InterpPanel() {
        setBackground(Color.white);
    }

    public void setAction(int action) {
        // Change the action type
        switch (action) {
        case ADD:
        case MOVE:
        case DELETE:
            this.action = action;
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void setCurveType(int mode) {
        // Change the curve display type
        switch (mode) {
        case POLY:
        case NAT_SPL:
            this.mode = mode;
            break;
        default:
            throw new IllegalArgumentException();
        }
    }

    public void clearPoints() {
        points.removeAllElements();
    }

    private int findPoint(int a, int b) {
        // Scan the list of control points to find out which (if any) point
        // contains the coordinates: a,b.
        // If a point is found, return the point's index, otherwise return -1
        int max = points.size();

        for (int i = 0; i < max; i++) {
            ControlPoint pnt = (ControlPoint) points.elementAt(i);
            if (pnt.within(a, b)) {
                return i;
            }
        }
        return -1;
    }

    public boolean handleEvent(Event e) {
        switch (e.id) {
        case Event.MOUSE_DOWN:
            // How we handle a MOUSE_DOWN depends on the action mode
            switch (action) {
            case ADD:
                // Add a new control point at the specified location
                int np = points.size();
                ControlPoint pnt;
                // keep points sorted by x: insert in correct place
                int i;
                for (i = 0; i <= np - 1 && ((ControlPoint) points.elementAt(i)).x < e.x; i++) {
                }
                pnt = new ControlPoint(e.x, e.y);
                if (i <= np - 1) {
                    points.insertElementAt(pnt, i);
                } else {
                    points.addElement(pnt);
                }
                repaint();
                break;
            case MOVE:
                // Attempt to select the point at the location specified.
                // If there is no point at the location, findPoint returns
                // -1 (i.e. there is no point to be moved)
                moving_point = findPoint(e.x, e.y);
                break;
            case DELETE:
                // Delete a point if one has been clicked
                int delete_pt = findPoint(e.x, e.y);
                if (delete_pt >= 0) {
                    points.removeElementAt(delete_pt);
                    repaint();
                }
                break;
            default:
                throw new IllegalArgumentException();
            }
            return true;
        case Event.MOUSE_UP:
            // We only care about MOUSE_UP's if we've been moving a control
            // point. If so, drop the control point.
            if (moving_point >= 0) {
                moving_point = -1;
                repaint();
            }
            return true;
        case Event.MOUSE_DRAG:
            // We only care about MOUSE_DRAG's while we are moving a control
            // point. Otherwise, do nothing.
            if (moving_point >= 0) {
                int np = points.size();
                // test if e.x is between x of points.elementAt(moving_point+1)
                // and points.elementAt(moving_point-1)
                if ((moving_point == np - 1 || e.x <= ((ControlPoint) points.elementAt(moving_point + 1)).x)
                        && (moving_point == 0 || e.x >= ((ControlPoint) points.elementAt(moving_point - 1)).x)) {
                    ControlPoint pnt = (ControlPoint) points.elementAt(moving_point);
                    pnt.x = e.x;
                    pnt.y = e.y;
                } else {
                    // otherwise find correct slot (or just always do this?)
                    points.removeElementAt(moving_point);
                    int i;
                    for (i = 0; i <= np - 2 && ((ControlPoint) points.elementAt(i)).x < e.x; i++) {
                    }
                    ControlPoint pnt2 = new ControlPoint(e.x, e.y);
                    if (i <= np - 1) {
                        points.insertElementAt(pnt2, i);
                    } else {
                        points.addElement(pnt2);
                    }
                    moving_point = i;
                }
                repaint();
            }
            return true;
        case Event.WINDOW_DESTROY:
            System.exit(0);
            return true;
        default:
            return false;
        }
    }

    private void solveTridiag(float sub[], float diag[], float sup[], float b[], int n) {
        /*
         * solve linear system with tridiagonal n by n matrix a using Gaussian
         * elimination *without* pivoting where a(i,i-1) = sub[i] for 2<=i<=n
         * a(i,i) = diag[i] for 1<=i<=n a(i,i+1) = sup[i] for 1<=i<=n-1 (the
         * values sub[1], sup[n] are ignored) right hand side vector b[1:n] is
         * overwritten with solution NOTE: 1...n is used in all arrays, 0 is
         * unused
         */
        int i;
        /* factorization and forward substitution */
        for (i = 2; i <= n; i++) {
            sub[i] = sub[i] / diag[i - 1];
            diag[i] = diag[i] - sub[i] * sup[i - 1];
            b[i] = b[i] - sub[i] * b[i - 1];
        }
        b[n] = b[n] / diag[n];
        for (i = n - 1; i >= 1; i--) {
            b[i] = (b[i] - sup[i] * b[i + 1]) / diag[i];
        }
    }

    public void paint(Graphics g) {
        int np = points.size(); // number of points
        float d[] = new float[np]; // Newton form coefficients
        float x[] = new float[np]; // x-coordinates of nodes
        float y;
        float t;
        float oldy = 0;
        float oldt = 0;

        int npp = np * precision; // number of points used for drawing
        g.setColor(getForeground());
        g.setPaintMode();

        // draw a border around the canvas
        g.drawRect(0, 0, size().width - 1, size().height - 1);
        if (np > 0) {
            // draw the control points
            for (int i = 0; i < np; i++) {
                ControlPoint p = (ControlPoint) points.elementAt(i);
                x[i] = p.x;
                d[i] = p.y;
                g.drawRect(p.x - p.PT_SIZE, p.y - p.PT_SIZE, p.PT_SIZE * 2, p.PT_SIZE * 2);
                // g.drawString(String.valueOf(i),p.x+p.PT_SIZE,p.y-p.PT_SIZE);
            }
            switch (mode) {
            case (POLY):
                // use divided difference algorithm to compute Newton form
                // coefficients
                for (int k = 1; k <= np - 1; k++) {
                    for (int i = 0; i <= np - 1 - k; i++) {
                        d[i] = (d[i + 1] - d[i]) / (x[i + k] - x[i]);
                    }
                }

                // for equidistant points along x-axis evaluate polynomial and
                // draw line
                float dt = ((float) size().width - 1) / npp;
                for (int k = 0; k <= npp; k++) {
                    t = k * dt;
                    // evaluate polynomial at t
                    y = d[0];
                    for (int i = 1; i <= np - 1; i++) {
                        y = y * (t - x[i]) + d[i];
                    }
                    // draw line
                    g.drawLine((int) oldt, (int) oldy, (int) t, (int) y);
                    oldt = t;
                    oldy = y;
                }
                break;
            case (NAT_SPL):
                if (np > 1) {
                    float a[] = new float[np];
                    float t1;
                    float t2;
                    float h[] = new float[np];
                    for (int i = 1; i <= np - 1; i++) {
                        h[i] = x[i] - x[i - 1];
                    }
                    if (np > 2) {
                        float sub[] = new float[np - 1];
                        float diag[] = new float[np - 1];
                        float sup[] = new float[np - 1];

                        for (int i = 1; i <= np - 2; i++) {
                            diag[i] = (h[i] + h[i + 1]) / 3;
                            sup[i] = h[i + 1] / 6;
                            sub[i] = h[i] / 6;
                            a[i] = (d[i + 1] - d[i]) / h[i + 1] - (d[i] - d[i - 1]) / h[i];
                        }
                        solveTridiag(sub, diag, sup, a, np - 2);
                    }
                    // note that a[0]=a[np-1]=0
                    // draw
                    oldt = x[0];
                    oldy = d[0];
                    g.drawLine((int) oldt, (int) oldy, (int) oldt, (int) oldy);
                    for (int i = 1; i <= np - 1; i++) { // loop over intervals
                                                        // between nodes
                        for (int j = 1; j <= precision; j++) {
                            t1 = (h[i] * j) / precision;
                            t2 = h[i] - t1;
                            y = ((-a[i - 1] / 6 * (t2 + h[i]) * t1 + d[i - 1]) * t2 + (-a[i] / 6 * (t1 + h[i]) * t2 + d[i])
                                    * t1)
                                    / h[i];
                            t = x[i - 1] + t1;
                            g.drawLine((int) oldt, (int) oldy, (int) t, (int) y);
                            oldt = t;
                            oldy = y;
                        }
                    }
                }
                break;
            }
        }
    }
}

class CurveControls extends Panel {
    InterpPanel target;
    Checkbox cb_add;
    Checkbox cb_move;
    Checkbox cb_delete;
    String st_add_label = "Add Points";
    String st_move_label = "Move Points";
    String st_delete_label = "Delete Points";

    public CurveControls(InterpPanel target) {
        this.target = target;
        setLayout(new FlowLayout(FlowLayout.CENTER));
        setBackground(Color.lightGray);
        Button clear = new Button("Clear");
        add("West", clear);

        CheckboxGroup action_group = new CheckboxGroup();
        add(cb_add = new Checkbox(st_add_label, action_group, true));
        add(cb_move = new Checkbox(st_move_label, action_group, false));
        add(cb_delete = new Checkbox(st_delete_label, action_group, false));
    }

    public void paint(Graphics g) {
        Rectangle r = bounds();

        g.setColor(Color.lightGray);
        g.draw3DRect(0, 0, r.width, r.height, false);
    }

    public boolean action(Event e, Object arg) {
        if (e.target instanceof Checkbox) {
            String cbox = ((Checkbox) (e.target)).getLabel();
            if (cbox.equals(st_add_label)) {
                target.setAction(InterpPanel.ADD);
            } else if (cbox.equals(st_move_label)) {
                target.setAction(InterpPanel.MOVE);
            } else if (cbox.equals(st_delete_label)) {
                target.setAction(InterpPanel.DELETE);
            }
        } else if (e.target instanceof Button) {
            String button = ((Button) (e.target)).getLabel();
            if (button.equals("Clear")) {
                target.clearPoints();

                // After clearing the control points, put the user back into
                // ADD mode, since none of the other modes make any sense.
                cb_add.setState(true);
                cb_delete.setState(false);
                cb_move.setState(false);
                target.setAction(InterpPanel.ADD);
                target.repaint();
            }
        }
        return true;
    }
}

class CurveControls2 extends Panel {
    InterpPanel target;
    Checkbox cb_poly;
    Checkbox cb_nat_spl;
    String st_poly_label = "Polynomial";
    String st_nat_spl_label = "Cubic Spline";

    public CurveControls2(InterpPanel target) {
        this.target = target;
        setLayout(new FlowLayout(1));

        CheckboxGroup type_group = new CheckboxGroup();
        add(cb_poly = new Checkbox(st_poly_label, type_group, true));
        add(cb_nat_spl = new Checkbox(st_nat_spl_label, type_group, false));
    }

    public void paint(Graphics g) {
        Rectangle r = bounds();

        g.setColor(Color.lightGray);
        g.draw3DRect(0, 0, r.width, r.height, false);
    }

    public boolean handleEvent(Event e) {
        switch (e.id) {
        case Event.ACTION_EVENT:
            // Handle other action events
            if (e.target instanceof Checkbox) {
                String cbox = ((Checkbox) (e.target)).getLabel();
                if (cbox.equals(st_poly_label)) {
                    target.setCurveType(InterpPanel.POLY);
                } else if (cbox.equals(st_nat_spl_label)) {
                    target.setCurveType(InterpPanel.NAT_SPL);
                }
            }
            target.repaint();
            return (true);
        default:
            return (false);
        }
    }
}

// *************************************************************************
// end of code of Interp2 Applet
// *************************************************************************
