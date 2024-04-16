package com.pkmngen.mods.asm;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public final class AsmUtils {

    private AsmUtils() {}

    public static Node[] findTag(Node node, String tagName) {
        NodeList children = node.getChildNodes();
        List<Node> filtered = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals(tagName)) {
                filtered.add(child);
            }
        }
        return filtered.toArray(new Node[0]);
    }

    public static String getAttribute(Node node, String attr) {
        if (!node.hasAttributes()) {
            return null;
        }
        Node attrNode = node.getAttributes().getNamedItem(attr);
        if (attrNode == null) {
            return null;
        }
        return attrNode.getTextContent();
    }

}
