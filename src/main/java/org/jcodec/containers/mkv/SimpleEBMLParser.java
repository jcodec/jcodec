package org.jcodec.containers.mkv;

import static org.jcodec.containers.mkv.Type.Attachments;
import static org.jcodec.containers.mkv.Type.Cluster;
import static org.jcodec.containers.mkv.Type.Cues;
import static org.jcodec.containers.mkv.Type.EBML;
import static org.jcodec.containers.mkv.Type.Info;
import static org.jcodec.containers.mkv.Type.SeekHead;
import static org.jcodec.containers.mkv.Type.Segment;
import static org.jcodec.containers.mkv.Type.Tracks;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.containers.mkv.ebml.BinaryElement;
import org.jcodec.containers.mkv.ebml.Element;
import org.jcodec.containers.mkv.ebml.MasterElement;

public class SimpleEBMLParser {

        private FileChannel is;
        private LinkedList<MasterElement> trace = new LinkedList<MasterElement>();
        private ArrayList<MasterElement> tree = new ArrayList<MasterElement>();

        public SimpleEBMLParser(FileChannel iFS) {
            this.is = iFS;
        }

        public void printParsedTree() {
            for (MasterElement e : tree) {
                printTree(0, e);
            }

        }

        private void printTree(int i, Element e) {
            System.out.println(printPaddedType(i, e).toString());
            if (e instanceof MasterElement) {
                MasterElement parent = (MasterElement) e;
                for (Element child : parent.children) {
                    printTree(i + 1, child);
                }
                System.out.println(printPaddedType(i, e).append(" CLOSED.").toString());
            }
        }

        public void parse() throws IOException {
            Element e = null;

            while ((e = nextElement()) != null) {
                if (!isSpecifiedHeader(e.getId()))
                    System.err.println("Unspecified header: " + Reader.printAsHex(e.getId()) + " at " + e.offset);

                while (!possibleChild(trace.peekFirst(), e))
                    closeElem(trace.removeFirst());

                openElem(e);

                if (e instanceof MasterElement) {
                    trace.push((MasterElement) e);
                } else {
                    if (e instanceof BinaryElement) {
                        if ((trace.peekFirst().dataOffset + trace.peekFirst().size) < (e.offset + e.size))
                            System.out.println("FYI: " + e.type + " ending at " + (e.offset + e.size) + " exceeds parent element ending at " + (trace.peekFirst().dataOffset + trace.peekFirst().size) + ". Tying to resume parsing by seeking to the suggested end of parent master element.");

                        if ((trace.peekFirst().dataOffset + trace.peekFirst().size) < (e.offset + e.size)) {
                            // This strange condition is dictated by matroska test suite test4.mkv, test7.mkv and test2.mkv
                            // test4.mkv contains junk bytes in between level 0 element (i.g. EBML, Segment) and level 1 elements (i.g. Cluster, SeekHead, Info...)
                            // test7.mkv contains and element that exceeds the size of the second cluster, but the cluster size contains correct info
                            // test2.mkv The 6'th Seek element (there's only one SeekHead) has it's children located outside the specified size and a Void element as a padding to the next element,
                            // thus seeking to the end of the parent result in infinite loop

                            is.position((trace.peekFirst().dataOffset + trace.peekFirst().size));
                            // // is.seek(e.offset+e.size);
                            // System.out.println(trace.peekFirst().type +" > "+e.type);
                        } else
                            try {
                                e.readData(is);
                            } catch (OutOfMemoryError oome) {
                                System.err.println(e.type + " 0x" + Reader.printAsHex(e.getId()) + " size: " + e.size + " offset: 0x" + Long.toHexString(e.offset));
                                System.err.println(" top in trace " + trace.peekFirst().type + " 0x" + Reader.printAsHex(trace.peekFirst().getId()) + " size: " + trace.peekFirst().size + " offset: 0x" + Long.toHexString(trace.peekFirst().offset));
                                throw oome;
                            }
                    } else {
                        // Currently there are no elements that are neither Master nor Binary
                        // should never actually get here
                        e.skipData(is);
                    }

                    trace.peekFirst().addChildElement(e);
                }
            }

            while (trace.peekFirst() != null)
                closeElem(trace.removeFirst());

        }

        private boolean possibleChild(MasterElement parent, Element child) {
            if (parent != null && Cluster.equals(parent.type) && child != null && !Cluster.equals(child.type) && !Info.equals(child.type) && !SeekHead.equals(child.type) && !Tracks.equals(child.type) && !Cues.equals(child.type) && !Attachments.equals(child.type) && !Type.Tags.equals(child.type)
                    && !Type.Chapters.equals(child.type))
                return true;

            return Type.possibleChild(parent, child);
        }

        private void openElem(Element e) {
//            System.out.println(e.type.name() + (e instanceof MasterElement ? " master " : "") + " id: 0x" + Reader.printAsHex(e.id) + " " + e.offset + " " + e.size);
        }

        private static StringBuilder printPaddedType(int size, Element e) {
            StringBuilder sb = new StringBuilder();
            for (; size > 0; size--) {
                sb.append("    ");
            }
            sb.append(e.type);
            return sb;
        }

        private void closeElem(MasterElement e) {
            if (trace.peekFirst() == null) {
                tree.add(e);
            } else {
                trace.peekFirst().addChildElement(e);
            }
//            System.out.println(e.type.name() + " CLOSING ");
        }

        private Element nextElement() throws IOException {
            // Read the type.
            long offset = is.position();
            if (offset >= is.size())
                return null;

            byte[] typeId = Reader.getRawEbmlBytes(is);

            while ((typeId == null || !isSpecifiedHeader(typeId)) && offset < is.size()) {
                offset++;
                // System.err.println("seeking to: "+offset);
                is.position(offset);
                typeId = Reader.getRawEbmlBytes(is);
            }

            // Read the size.
            byte[] ebmlCodedElementSize = Reader.getEbmlBytes(is);
            long size = Reader.bytesToLong(ebmlCodedElementSize);

            // Zero sized element is valid
            if (size == 0)
                ;

            // according to MatroskaDocType.createElement return value is never 'null'
            Element elem = Type.createElementById(typeId);
            elem.offset = offset;
            elem.dataOffset = is.position();

            // Set it's size
            elem.size = size;

            return elem;
        }

        public boolean isSpecifiedHeader(byte[] b) {
            // Junk elements with some crazy types are allowed in Cluster
            if (!trace.isEmpty() && Type.Cluster.equals(trace.peekFirst().type))
                return true;

            return Type.isSpecifiedHeader(b);
        }

        public List<MasterElement> getTree() {
            return tree;
        }

        public boolean matchesHierarchy(byte[] typeId) {
            if (Arrays.equals(EBML.id, typeId) || Arrays.equals(Segment.id, typeId))
                return true;

            for (int i = 0; i < trace.size(); i++)
                if (Type.possibleChild(trace.get(i), typeId))
                    return true;

            return false;
        }

    }