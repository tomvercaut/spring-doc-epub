package tv.spring.doc.epub.model

/**
 * Represents an item in the navigation tree.
 */
class NavItem(// Depth of the node in the navigation tree.
    var depth: Int = 0,
    /** Link to another webpage or document section.*/
    var href: String = "",
    /** Text displayed on the navigation tree item.*/
    var name: String = "",
    /** List of nested navigation tree items.*/
    val children: MutableList<NavItem> = mutableListOf()
) {

    /**
     * Clears the properties of the object.
     */
    fun clear() {
        depth = 0
        href = ""
        name = ""
        children.clear()
    }

    /**
     * Checks if the properties are empty.
     *
     * @return true if all the properties in the navigation item are empty, false otherwise.
     */
    fun isEmpty(): Boolean {
        return depth == 0 && href.isEmpty() && name.isEmpty() && children.isEmpty()
    }
}