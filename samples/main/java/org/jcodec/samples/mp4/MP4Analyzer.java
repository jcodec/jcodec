package org.jcodec.samples.mp4;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import org.jcodec.common.Tuple;
import org.jcodec.common.Tuple._2;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Box.AtomField;
import org.jcodec.containers.mp4.boxes.NodeBox;

import net.miginfocom.swing.MigLayout;

public class MP4Analyzer extends JPanel implements TreeSelectionListener {
    private static final int MAX_COUNT = 100;
    private JTree tree;

    private final static Set<Class> primitive = new HashSet<Class>();

    static {
        primitive.add(Boolean.class);
        primitive.add(Byte.class);
        primitive.add(Short.class);
        primitive.add(Integer.class);
        primitive.add(Long.class);
        primitive.add(Float.class);
        primitive.add(Double.class);
        primitive.add(Character.class);
    }

    private static boolean playWithLineStyle = false;
    private static String lineStyle = "Horizontal";

    private static boolean useSystemLookAndFeel = false;
    private JPanel rightPane;

    class BoxNode {
        private Box box;
        private Atom atom;

        public BoxNode(Box box, Atom atom) {
            this.box = box;
            this.atom = atom;
        }

        @Override
        public String toString() {
            return box == null ? atom.getHeader().getFourcc() : box.getFourcc();
        }
    }

    public MP4Analyzer(String filename) throws IOException {
        super(new GridLayout(1, 0));

        DefaultMutableTreeNode top = new DefaultMutableTreeNode(filename);
        createNodes(top, filename);

        tree = new JTree(top);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        tree.addTreeSelectionListener(this);

        if (playWithLineStyle) {
            System.out.println("line style = " + lineStyle);
            tree.putClientProperty("JTree.lineStyle", lineStyle);
        }

        JScrollPane treeView = new JScrollPane(tree);

        rightPane = new JPanel(new MigLayout());
        JScrollPane rightView = new JScrollPane(rightPane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(treeView);
        splitPane.setRightComponent(rightView);

        treeView.setMinimumSize(new Dimension(300, 500));
        rightView.setMinimumSize(new Dimension(500, 500));
        splitPane.setDividerLocation(300);
        splitPane.setPreferredSize(new Dimension(1024, 768));

        add(splitPane);
    }

    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

        if (node == null)
            return;

        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
            BoxNode box = (BoxNode) nodeInfo;
            displayBox(box);
        }
    }

    private void displayBox(BoxNode box) {
        rightPane.removeAll();
        rightPane.revalidate();
        rightPane.repaint();
        if (box.box != null) {
            HashMap<Integer, Tuple._2<String, Object>> map = new HashMap<Integer, Tuple._2<String, Object>>();
            Method[] methods = box.box.getClass().getMethods();
            for (Method method : methods) {
                if (!isDefined(method)) {
                    continue;
                }
                AtomField annotation = method.getAnnotation(Box.AtomField.class);
                try {
                    Object value = method.invoke(box.box);
                    map.put(annotation.idx(), new Tuple._2<String, Object>(toName(method), value));
                } catch (Exception e) {
                }
            }
            for (int i = 0; i < 1000; i++) {
                if (map.containsKey(i)) {
                    _2<String, Object> field = map.get(i);
                    rightPane.add(new JLabel(field.v0));
                    rightPane.add(renderValue(field.v1), "wrap");
                }
            }
        } else if (box.atom != null) {
            rightPane.add(new JLabel("Offset"));
            rightPane.add(new JTextField(String.valueOf(box.atom.getOffset()), 20), "wrap");
            rightPane.add(new JLabel("Size"));
            rightPane.add(new JTextField(String.valueOf(box.atom.getHeader().getSize()), 20), "wrap");
        }
        rightPane.revalidate();
        rightPane.repaint();
    }

    private JComponent renderValue(Object obj) {
        if (obj == null) {
            return new JTextField("null", 20);
        }
        if (primitive.contains(obj.getClass())) {
            return new JTextField(String.valueOf(obj), 20);
        }

        String className = obj.getClass().getName();
        if (className.startsWith("java.lang") && !className.equals("java.lang.String")) {
            return new JTextField("null", 20);
        }

        if (obj instanceof ByteBuffer)
            obj = NIOUtils.toArray((ByteBuffer) obj);

        if (obj == null) {
            return new JTextField("null", 20);
        } else if (obj instanceof String) {
            return new JTextField((String) obj, 50);
        } else if (obj instanceof Map) {
            JPanel ret = new JPanel(new MigLayout());
            Iterator it = ((Map) obj).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                ret.add(new JLabel(String.valueOf(e.getKey()), JLabel.TRAILING));
                ret.add(renderValue(e.getValue()), "wrap");
            }
            return ret;
        } else if (obj instanceof Iterable) {
            JPanel ret = new JPanel(new MigLayout());
            Iterator it = ((Iterable) obj).iterator();
            while (it.hasNext()) {
                ret.add(renderValue(it.next()), "wrap");
            }
            return ret;
        } else if (obj instanceof Object[]) {
            JPanel ret = new JPanel(new MigLayout());
            int len = Array.getLength(obj);
            for (int i = 0; i < Math.min(MAX_COUNT, len); i++) {
                ret.add(renderValue(Array.get(obj, i)), "wrap");
            }
            return ret;
        } else if (obj instanceof long[]) {
            StringBuilder bldr = new StringBuilder();
            long[] a = (long[]) obj;
            int i = 0;
            for (; i < Math.min(MAX_COUNT, a.length); i++) {
                bldr.append(a[i] + " ");
            }
            if (i < MAX_COUNT)
                bldr.append("...");
            JTextArea ret = new JTextArea(bldr.toString(), 10, 50);
            ret.setLineWrap(true);
            return ret;
        } else if (obj instanceof int[]) {
            StringBuilder bldr = new StringBuilder();
            int[] a = (int[]) obj;
            int i = 0;
            for (; i < Math.min(MAX_COUNT, a.length); i++) {
                bldr.append(a[i] + " ");
            }
            if (i < MAX_COUNT)
                bldr.append("...");
            JTextArea ret = new JTextArea(bldr.toString(), 10, 50);
            ret.setLineWrap(true);
            return ret;
        } else if (obj instanceof float[]) {
            StringBuilder bldr = new StringBuilder();
            float[] a = (float[]) obj;
            int i = 0;
            for (; i < Math.min(MAX_COUNT, a.length); i++) {
                bldr.append(a[i] + " ");
            }
            if (i < MAX_COUNT)
                bldr.append("...");
            JTextArea ret = new JTextArea(bldr.toString(), 10, 50);
            ret.setLineWrap(true);
            return ret;
        } else if (obj instanceof double[]) {
            StringBuilder bldr = new StringBuilder();
            double[] a = (double[]) obj;
            int i = 0;
            for (; i < Math.min(MAX_COUNT, a.length); i++) {
                bldr.append(a[i] + " ");
            }
            if (i < MAX_COUNT)
                bldr.append("...");
            JTextArea ret = new JTextArea(bldr.toString(), 10, 50);
            ret.setLineWrap(true);
            return ret;
        } else if (obj instanceof short[]) {
            StringBuilder bldr = new StringBuilder();
            short[] a = (short[]) obj;
            int i = 0;
            for (; i < Math.min(MAX_COUNT, a.length); i++) {
                bldr.append(a[i] + " ");
            }
            if (i < MAX_COUNT)
                bldr.append("...");
            JTextArea ret = new JTextArea(bldr.toString(), 10, 50);
            ret.setLineWrap(true);
            return ret;
        } else if (obj instanceof byte[]) {
            StringBuilder bldr = new StringBuilder();
            byte[] a = (byte[]) obj;
            int i = 0;
            for (; i < Math.min(MAX_COUNT, a.length); i++) {
                bldr.append(a[i] + " ");
            }
            if (i < MAX_COUNT)
                bldr.append("...");
            JTextArea ret = new JTextArea(bldr.toString(), 10, 50);
            ret.setLineWrap(true);
            return ret;
        } else if (obj instanceof boolean[]) {
            StringBuilder bldr = new StringBuilder();
            boolean[] a = (boolean[]) obj;
            int i = 0;
            for (; i < Math.min(MAX_COUNT, a.length); i++) {
                bldr.append(a[i] + " ");
            }
            if (i < MAX_COUNT)
                bldr.append("...");
            JTextArea ret = new JTextArea(bldr.toString(), 10, 50);
            return ret;
        } else if (obj.getClass().isEnum()) {
            return new JTextField(String.valueOf(obj), 50);
        } else {
            JPanel ret = new JPanel(new MigLayout());
            Method[] methods = obj.getClass().getMethods();
            for (Method method : methods) {
                if (!isDefined(method)) {
                    continue;
                }
                String name = toName(method);
                Object value;
                try {
                    value = method.invoke(obj);
                    ret.add(new JLabel(name));
                    ret.add(renderValue(value), "wrap");
                } catch (Exception e) {
                    System.err.println("could not do it");
                }
            }
            return ret;
        }
    }

    private static String toName(Method method) {
        char[] name = method.getName().toCharArray();
        int ind = name[0] == 'g' ? 3 : 2;
        name[ind] = Character.toLowerCase(name[ind]);
        return new String(name, ind, name.length - ind);
    }

    private boolean isDefined(Method method) {
        if (!Modifier.isPublic(method.getModifiers()))
            return false;
        if (!method.getName().startsWith("get")
                && !(method.getName().startsWith("is") && method.getReturnType() == Boolean.TYPE))
            return false;
        if (method.getParameterTypes().length != 0)
            return false;
        if (void.class.equals(method.getReturnType()))
            return false;
        if (Modifier.isStatic(method.getModifiers()))
            return false;
        if (!method.isAnnotationPresent(Box.AtomField.class))
            return false;
        return true;
    }

    private void createNodes(DefaultMutableTreeNode top, String filename) throws IOException {
        DefaultMutableTreeNode category = null;

        FileChannelWrapper ch = null;
        try {
            ch = NIOUtils.readableChannel(new File(filename));
            List<Atom> rootAtoms = MP4Util.getRootAtoms(ch);
            for (Atom atom : rootAtoms) {
                category = new DefaultMutableTreeNode(new BoxNode(null, atom));
                top.add(category);
                String fourcc = atom.getHeader().getFourcc();
                if ("moov".equals(fourcc)) {
                    NodeBox moov = (NodeBox) atom.parseBox(ch);
                    addSub(moov, category);
                }
            }
        } finally {
            if (ch != null)
                ch.close();
        }
    }

    private void addSub(NodeBox node, DefaultMutableTreeNode category) throws IOException {
        for (Box box : node.getBoxes()) {
            DefaultMutableTreeNode sub = new DefaultMutableTreeNode(new BoxNode(box, null));
            if (box instanceof NodeBox) {
                addSub((NodeBox) box, sub);
            }
            category.add(sub);

        }
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be invoked
     * from the event dispatch thread.
     * 
     * @param filename
     * @throws IOException
     */
    private static void createAndShowGUI(String filename) throws IOException {
        if (useSystemLookAndFeel) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Couldn't use system look and feel.");
            }
        }

        JFrame frame = new JFrame("MP4Analyzer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(new MP4Analyzer(filename));

        frame.pack();
        frame.setVisible(true);
    }

    public static void main(final String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    createAndShowGUI(args[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
