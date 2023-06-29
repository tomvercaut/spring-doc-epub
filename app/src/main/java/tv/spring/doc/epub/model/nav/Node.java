package tv.spring.doc.epub.model.nav;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the navigation tree.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Node {
    /** Depth of the node in the navigation tree.*/
    private int depth = 0;
    /** Link to another webpage or document section.*/
    private String href = "";
    /** Text displayed on the navigation tree item.*/
    private String name = "";
    /** List of nested navigation tree items.*/
    private List<Node> children = new ArrayList<>();

    /**
     * Clear all the properties and set them to their default values.
     */
    public void clear() {
        depth = 0;
        href = "";
        name = "";
        children.clear();
    }
}
