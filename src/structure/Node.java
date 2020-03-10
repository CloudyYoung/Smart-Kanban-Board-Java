package structure;

import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;

import com.google.gson.annotations.*;

import java.lang.reflect.Constructor;

public abstract class Node {

  /**
   * int id: the users id -> associated to the user name and password int parentId:????? int
   * grandparentId:???? String title: this will be the titles of each column on the board String
   * note:???? String type: type will be used to determine where the user is in the application
   */
  @Expose(serialize = false, deserialize = true)
  private int id;

  @Expose private String title;
  @Expose private String note;
  @Expose private int parentId;

  private boolean existing = false;
  private Node parent;
  private HashMap<Integer, Node> nodes = new HashMap<Integer, Node>();

  /**
   * Creates a "dictionary" so it will be easier to determine the parent class of each main class ie
   * if the user wants to add something to column we can use this hash map to identify that the
   * parent of column is board, and board's parent is kanban
   *
   * <p>This will also be used to assign type on {@see #getTypeByLevel} method
   *
   * @see #getTypeByLevel
   */
  private final HashMap<String, Integer> TYPES =
      new HashMap<String, Integer>() {
        private static final long serialVersionUID = 3312582702053699017L;

        {
          put("Kanban", 0);
          put("Board", 1);
          put("Column", 2);
          put("Event", 3);
        }
      };

  /**
   * constructor for node
   *
   * @param id as an int
   * @param title as a string
   * @param note as a string
   */
  public Node(int id, String title, String note) {
    this.setId(id);
    this.setTitleLocal(title);
    this.setNoteLocal(note);
  }

  public Node() {}

  public Node(String title, String note) {
    this.setTitleLocal(title);
    this.setNoteLocal(note);
  }

  public Node(HttpBody obj) {
    if (!this.getType().equals("Kanban")) {
      this.setId(obj.getInt("id"));
      this.setTitleLocal(obj.getString("title"));
      this.setNoteLocal(obj.getString("note"));
      this.existing = true;
    }
    if (obj != null) {
      this.extractChildrenNodes(obj);
    }
  }

  private void extractChildrenNodes(HttpBody obj) {
    String childType = Node.typeLower(Node.typePlural(this.getChildType()));
    HttpBody value = obj.getList(childType);
    if (value == null) {
      return;
    }
    Collection<Object> list = value.values();
    for (Object each2 : list) {
      try {
        String type = Node.typeClass(childType);
        Class<?> cls = Class.forName(type);
        Constructor<?> constructor = cls.getConstructor(HttpBody.class);
        Object objNew = constructor.newInstance(each2);
        if (objNew instanceof Node) {
          Node nodeNew = (Node) objNew;
          nodeNew.setParentLocal(this);
          this.nodes.put(nodeNew.getId(), nodeNew);
        }
      } catch (Exception e) {
        // e.printStackTrace();
        // e.getCause();
        // fail silently
      }
    }
  }

  public boolean isExisting() {
    return this.existing;
  }

  /**
   * assigns type using the hashmap above
   *
   * @see Attribute: Node.TYPES
   * @param type as a string
   * @param level as an int
   * @return ret as a string which again can vary depending on the hashmap above
   */
  private String getTypeByLevel(String type, int level) {
    int lvl = TYPES.get(type);
    lvl += level;

    String ret = "";
    Iterator<?> keysItr = this.TYPES.keySet().iterator();

    while (keysItr.hasNext()) {
      String key = (String) keysItr.next();
      int value = (int) this.TYPES.get(key);

      if (value == lvl) {
        ret = key;
      }
    }

    return ret;
  }

  /**
   * gets the users id
   *
   * @return the user's id as an int
   */
  public int getId() {
    return this.id;
  }

  /**
   * gets the parentid
   *
   * @return the parentId as an int
   */
  public Node getParent() {
    return this.parent;
  }

  /**
   * sets the id
   *
   * @param aId as an int
   */
  private void setId(int aId) {
    this.id = aId;
  }

  /**
   * sets the parent id
   *
   * @param aParentId as an int
   */
  private void setParentLocal(Node aParent) {
    this.parent = aParent;
    this.parentId = aParent.getId();
  }

  public HttpRequest setParent(Node parent) {
    if (this instanceof Event) {
      String parentType = Node.typeLower(Node.typePlural(this.getParentType()));
      HttpRequest req = this.set(parentType + "_id", parent.getId() + "");
      if (req.isSucceeded()) {
        this.setParentLocal(parent);
      }
      return req;
    }
    return null;
  }

  /**
   * gets the title
   *
   * @return the title as a string this is the title of the of the columns on the board
   */
  public String getTitle() {
    return this.title;
  }

  /**
   * sets the title
   *
   * @param aTitle as a string
   */
  public void setTitleLocal(String title) {
    this.title = title;
  }

  public HttpRequest setTitle(String title) {
    HttpRequest req = this.set("title", title);
    if (req.isSucceeded()) {
      this.setTitleLocal(title);
    }
    return req;
  }

  public void setNoteLocal(String note) {
    this.note = note;
  }

  public HttpRequest setNote(String note) {
    HttpRequest req = this.set("note", note);
    if (req.isSucceeded()) {
      this.setNoteLocal(note);
    }
    return req;
  }

  public String getNote() {
    return this.note;
  }

  protected HttpRequest set(String key, String value) {
    HttpBody body = new HttpBody();
    body.put(key, value);

    HttpRequest req = new HttpRequest();
    req.setRequestUrl("/" + Node.typeLower(Node.typePlural(this.getType())) + "/" + this.getId());
    req.setRequestMethod("PUT");
    req.setRequestBody(body);
    req.setRequestCookie(Node.getRequestCookie());
    req.send();
    System.out.println(req.getRequestBodyString());
    System.out.println(req.getResponseBodyString());
    return req;
  }

  /**
   * gets the parent type
   *
   * @return the objects current type
   */
  public String getParentType() {
    return this.getParentType(this.getType());
  }

  /**
   * gets the parent type with the type name
   *
   * @param aType as a string this is the parent's type
   * @return the parent's type calculated using hash map above more in-depth reading of the parent
   *     type as it also returns the type with its name
   */
  public String getParentType(String aType) {
    return this.getParentType(aType, -1);
  }

  /**
   * gets the parent type with the type name and the level
   *
   * @param aType - the parent's type as a string
   * @param aLevel - the parent's level as an int
   * @return the parent's type with name and level
   */
  public String getParentType(String aType, int aLevel) {
    return this.getTypeByLevel(aType, Math.abs(aLevel) * -1);
  }

  /** @return */
  public String getChildType() {
    return this.getChildType(this.getType());
  }

  /**
   * @param aType
   * @return
   */
  public String getChildType(String aType) {
    return this.getChildType(aType, 1);
  }

  /**
   * @param aType
   * @param aLevel
   * @return
   */
  public String getChildType(String aType, int aLevel) {
    return this.getTypeByLevel(aType, Math.abs(aLevel));
  }

  /**
   * converts id, title and note to a string for output
   *
   * @return the id, title and note as a combined string
   */
  public String toString() {
    return this.getType()
        + " (id: "
        + this.getId()
        + ", title: \""
        + this.getTitle()
        + "\", note: \""
        + this.getNote()
        + "\", nodes: "
        + this.getNodes().toString()
        + "\")";
  }

  private static HttpBody getRequestCookie() {
    HttpBody cookie = new HttpBody();
    cookie.put("PHPSESSID", User.current.getSessionId());
    return cookie;
  }

  public HttpRequest add() {
    HttpRequest req = new HttpRequest();
    req.setRequestUrl("/" + Node.typeLower(Node.typePlural(this.getType())));
    req.setRequestMethod("POST");
    req.setRequestBody(this);
    req.setRequestCookie(Node.getRequestCookie());
    req.send();
    return req;
  }

  public HttpRequest remove() {
    HttpRequest req = new HttpRequest();
    req.setRequestUrl("/" + Node.typeLower(Node.typePlural(this.getType())) + "/" + this.getId());
    req.setRequestMethod("DELETE");
    req.setRequestCookie(Node.getRequestCookie());
    req.send();
    return req;
  }

  /**
   * Adds a node
   *
   * @param aNode
   * @return
   */
  public Node addNode(Node aNode) {
    this.nodes.put(aNode.getId(), aNode);
    return aNode;
  }

  /**
   * removes a node
   *
   * @param id as an int this is the node's id
   * @return if node is successfully removed
   */
  public boolean removeNode(int id) {
    return this.nodes.remove(id) != null;
  }

  public Node getNode(int id) {
    return this.nodes.get(id);
  }

  public Collection<Node> getNodes() {
    return this.nodes.values();
  }

  public String getType() {
    return this.getClass().getSimpleName();
  }

  public static String typeClass(String type) {
    return "structure." + Node.typeProper(Node.typeSingular(type));
  }

  public static String typePlural(String type) {
    return type.endsWith("s") || type.length() <= 0 ? type : type + "s";
  }

  public static String typeSingular(String type) {
    return type.endsWith("s") && type.length() > 0 ? type.substring(0, type.length() - 1) : type;
  }

  public static String typeProper(String type) {
    return type.substring(0, 1).toUpperCase() + type.toLowerCase().substring(1);
  }

  public static String typeLower(String type) {
    return type.toLowerCase();
  }

  public static String typeUpper(String type) {
    return type.toUpperCase();
  }

  public static void main(String[] args) {
    User user = new User();
    user.authenticate("cloudy", "cloudy");
    System.out.println(user);
    user.fetchKanban();
    System.out.println(Kanban.current);

    Node node = Kanban.current.getNode(50);
    System.out.println(node);
    node.setTitle("new title");
    System.out.println(node);

    // Kanban kanban = new Kanban();
    // Board aNode = new Board("new Node2 cloudyyyyyy", "", "#00b0f0");
    // kanban.addNode(aNode);
  }
}
