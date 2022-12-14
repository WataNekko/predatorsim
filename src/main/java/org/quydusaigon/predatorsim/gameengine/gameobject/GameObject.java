package org.quydusaigon.predatorsim.gameengine.gameobject;

import javafx.beans.property.DoubleProperty;
import javafx.scene.Group;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Stream;

import org.quydusaigon.predatorsim.App;
import org.quydusaigon.predatorsim.gameengine.component.Behaviour;
import org.quydusaigon.predatorsim.gameengine.component.Component;
import org.quydusaigon.predatorsim.gameengine.component.NodeComponent;
import org.quydusaigon.predatorsim.gameengine.util.Prefab;
import org.quydusaigon.predatorsim.gameengine.util.TransformInit;

public final class GameObject {

    private final List<Group> children;
    private final List<Component> components;
    private boolean started;

    private GameObject(int initialComponentCount, boolean started) {
        children = new ArrayList<>();
        components = new ArrayList<>(initialComponentCount);
        this.started = started;
    }

    private static GameObject getGameObjectData(Group group) {
        var obj = group.getUserData();
        if (obj instanceof GameObject)
            return (GameObject) obj;
        throw new NotAGameObjectException();
    }

    /*
     * Creation and deletion
     */

    public static Group create(TransformInit tf, Group parent, Component... components) {
        // create new Group
        var go = new Group();

        // set Transforms
        posX(go).set(tf.posX);
        posY(go).set(tf.posY);
        rotate(go).set(tf.rotate);
        scaleX(go).set(tf.scaleX);
        scaleY(go).set(tf.scaleY);

        // whether the parent has been started
        boolean started = false;
        if (parent != null) {
            var parentData = getGameObjectData(parent);
            started = parentData.started;

            // add the newly created GameObject to the parent
            parentData.children.add(go);
            parent.getChildren().add(go);
        }

        // create GameObject data
        var data = new GameObject(components.length, started);
        go.setUserData(data);

        // add initial components
        for (var c : components) {
            _addComponent(go, data, c);
        }

        return go;
    }

    public static void destroy(Group gameObject) {
        if (gameObject == null)
            return;
        var data = getGameObjectData(gameObject);

        // recursive call
        data.children.forEach(GameObject::destroy);

        // destroy components
        data.components.forEach(GameObject::destroy);

        // remove GameObject from parent
        getParent(gameObject).ifPresent(parent -> {
            parent.getChildren().remove(gameObject);
        });
    }

    /*
     * GameObject hierarchy
     */

    public static Optional<Group> getParent(Group gameObject) {
        var p = gameObject.getParent();
        return p instanceof Group
                ? Optional.of((Group) p)
                : Optional.empty();
    }

    private static List<Group> _getChildren(Group gameObject) {
        var go = getGameObjectData(gameObject);
        return go.children;
    }

    public static List<Group> getChildren(Group gameObject) {
        return Collections.unmodifiableList(
                _getChildren(gameObject));
    }

    // public static void addChild(Group parent, Group child) {
    // var go = getGameObjectData(parent);
    // go.children.add(child);
    // parent.getChildren().add(child);
    // }

    /*
     * Instantiation
     */

    public static void instantiate(Prefab prefab, TransformInit tf, Group parent) {
        prefab.instantiate(tf, parent);
    }

    public static void instantiate(Prefab prefab, TransformInit tf) {
        instantiate(prefab, tf, App.root);
    }

    public static void instantiate(Prefab prefab) {
        instantiate(prefab, TransformInit.DEFAULT);
    }

    /*
     * Components access
     */

    public static <T extends Component> Optional<T> getComponent(Group gameObject, Class<T> type) {
        var go = getGameObjectData(gameObject);

        for (var c : go.components) {
            if (type.isAssignableFrom(c.getClass())) {
                return Optional.of(type.cast(c));
            }
        }
        return Optional.empty();
    }

    public static <T extends Component> Stream<T> getComponents(Group gameObject, Class<T> type) {
        var go = getGameObjectData(gameObject);

        return go.components.stream()
                .filter(c -> type.isAssignableFrom(c.getClass()))
                .map(type::cast);
    }

    private static <T extends Component> T _addComponent(Group gameObject, GameObject data, T component) {
        // add component to data list
        data.components.add(component);
        // set component's GameObject
        component.setGameObject(gameObject);

        if (component instanceof NodeComponent) {
            // add node to Group if is NodeComponent
            var c = (NodeComponent<?>) component;
            gameObject.getChildren().add(c.getNode());
        } else if (component instanceof Behaviour) {
            // start Behaviour if GameObject has been started
            if (data.started)
                ((Behaviour) component).start();
        }

        return component;
    }

    public static <T extends Component> T addComponent(Group gameObject, T component) {
        var data = getGameObjectData(gameObject);
        return _addComponent(gameObject, data, component);
    }

    public static void destroy(Component component) {
        if (component == null)
            return;
        // invoke onDestroy event
        component.onDestroy();

        var go = component.getGameObject();
        if (go == null)
            return;
        var data = getGameObjectData(go);

        // remove component from data list
        data.components.remove(component);
        // remove component's GameObject
        component.setGameObject(null);

        // remove node from Group if is NodeComponent
        if (component instanceof NodeComponent) {
            var c = (NodeComponent<?>) component;
            go.getChildren().remove(c.getNode());
        }
    }

    /*
     * Transforms
     */

    public static DoubleProperty posX(Group gameObject) {
        return gameObject.translateXProperty();
    }

    public static DoubleProperty posY(Group gameObject) {
        return gameObject.translateYProperty();
    }

    public static DoubleProperty rotate(Group gameObject) {
        return gameObject.rotateProperty();
    }

    public static DoubleProperty scaleX(Group gameObject) {
        return gameObject.scaleXProperty();
    }

    public static DoubleProperty scaleY(Group gameObject) {
        return gameObject.scaleYProperty();
    }

    /*
     * Iterator for iterating through a GameObject's hierarchy, in a top-down,
     * depth-first-search manner.
     */
    private static class Iterator implements java.util.Iterator<Group> {

        private final Stack<Group> stack;

        private Iterator(Group gameObject) {
            stack = new Stack<>();
            stack.push(gameObject);
        }

        @Override
        public boolean hasNext() {
            return !stack.empty();
        }

        @Override
        public Group next() {
            var go = stack.pop();
            stack.addAll(_getChildren(go));
            return go;
        }

    }

    public static Iterable<Group> iter(Group gameObject) {
        return new Iterable<Group>() {

            @Override
            public java.util.Iterator<Group> iterator() {
                return new Iterator(gameObject);
            }

        };
    }

    /*
     * Iterator operations
     */

    public static void start(Group root) {
        for (var go : iter(root)) {
            // start Behaviours
            getComponents(go, Behaviour.class)
                    .forEach(Behaviour::start);
            getGameObjectData(go).started = true;
        }
    }

    public static void update(Group root) {
        for (var go : iter(root)) {
            // update Behaviours
            getComponents(go, Behaviour.class)
                    .forEach(Behaviour::update);
        }
    }

}
