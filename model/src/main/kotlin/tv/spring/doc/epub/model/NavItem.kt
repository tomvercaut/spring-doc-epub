package tv.spring.doc.epub.model

class NavItem {
    // Depth of the node in the navigation tree.
    var depth: Int = 0
    /** Link to another webpage or document section.*/
    var href: String = ""
    /** Text displayed on the navigation tree item.*/
    var name: String = ""
    /** List of nested navigation tree items.*/
    val children: MutableList<NavItem> = mutableListOf()

    fun clear(): Unit {
        depth = 0
        href = ""
        name = ""
        children.clear()
    }

    fun isEmpty(): Boolean {
        return depth == 0 && href.isEmpty() && name.isEmpty() && children.isEmpty()
    }
}