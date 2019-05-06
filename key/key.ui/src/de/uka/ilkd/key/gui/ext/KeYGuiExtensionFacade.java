package de.uka.ilkd.key.gui.ext;

import static de.uka.ilkd.key.gui.ext.KeYExtConst.PATH;
import static de.uka.ilkd.key.gui.ext.KeYExtConst.PRIORITY;

import java.awt.Component;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JToolBar;

import de.uka.ilkd.key.gui.MainWindow;
import de.uka.ilkd.key.pp.PosInSequent;

/**
 * Facade for retrieving the GUI extensions.
 *
 * @author Alexander Weigl
 * @version 1 (07.02.19)
 */
public final class KeYGuiExtensionFacade {
    //region panel extension
    public static List<KeYPaneExtension> getAllPanels() {
        return getExtension(KeYPaneExtension.class, Comparator.comparingInt(KeYPaneExtension::priority));
    }

    /**
     * Try to find a specific implementation of a {@link KeYPaneExtension}
     *
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T extends KeYPaneExtension> Optional<T> getPanel(Class<T> clazz) {
        Optional<KeYPaneExtension> v = getAllPanels().stream()
                .filter(it -> it.getClass().isAssignableFrom(clazz))
                .findAny();
        return (Optional<T>) v;
    }
    //endregion

    //region main menu extension

    /**
     * Retrieves all known implementation of the {@link KeYMainMenuExtension}
     *
     * @return a list
     */
    public static List<KeYMainMenuExtension> getMainMenuExtensions() {
        return getExtension(KeYMainMenuExtension.class, Comparator.comparingInt(KeYMainMenuExtension::getPriority));
    }

    /*
    public static Optional<Action> getMainMenuExtensions(String name) {
        Spliterator<KeYMainMenuExtension> iter = ServiceLoader.load(KeYMainMenuExtension.class).spliterator();
        return StreamSupport.stream(iter, false)
                .flatMap(it -> it.getMainMenuActions(mainWindow).stream())
                .filter(Objects::nonNull)
                .filter(it -> it.getValue(Action.NAME).equals(name))
                .findAny();
    }*/

    /**
     * Creates the extension menu of all known {@link KeYMainMenuExtension}.
     *
     * @return a menu
     */
    public static JMenu createExtensionMenu(MainWindow mainWindow) {
        ToIntFunction<Action> func = (Action a) -> {
            Integer i = (Integer) a.getValue(PRIORITY);
            if (i == null) {
                return 0;
            } else {
                return i;
            }
        };

        List<KeYMainMenuExtension> kmm = getMainMenuExtensions();
        JMenu menu = new JMenu("Extensions");
        for (KeYMainMenuExtension it : kmm) {
            List<Action> actions = it.getMainMenuActions(mainWindow);
            actions.sort(Comparator.comparingInt(func));
            sortActionsIntoMenu(actions, menu);
        }
        return menu;
    }
    //endregion

    //region Menu Helper
    private static void sortActionsIntoMenu(List<Action> actions, JMenu menu) {
        actions.forEach(act -> sortActionIntoMenu(act, menu));
    }

    private static void sortActionIntoMenu(Action act, JMenu menu) {
        Object path = act.getValue(PATH);
        String spath;
        if (path == null) {
            spath = "";
        } else {
            spath = path.toString();
        }
        Iterator<String> mpath = Pattern.compile(Pattern.quote(".")).splitAsStream(spath).iterator();
        JMenu a = findMenu(menu, mpath);
        a.add(act);
    }

    private static JMenu findMenu(JMenu menu, Iterator<String> mpath) {
        if (mpath.hasNext()) {
            String cur = mpath.next();
            Component[] children = menu.getMenuComponents();
            for (int i = 0; i < children.length; i++) {
                if (Objects.equals(children[i].getName(), cur)) {
                    JMenu sub = (JMenu) children[i];
                    return findMenu(sub, mpath);
                }
            }
            JMenu m = new JMenu(cur);
            m.setName(cur);
            menu.add(m);
            return findMenu(m, mpath);
        } else {
            return menu;
        }
    }
    //endregion

    //region Toolbar

    /**
     * Retrieves all known implementation of the {@link KeYToolbarExtension}
     *
     * @return a list
     */
    public static List<KeYToolbarExtension> getToolbarExtensions() {
        return getExtension(KeYToolbarExtension.class, Comparator.comparingInt(KeYToolbarExtension::getPriority));
    }

    /**
     * Creates all toolbars for the known extension.
     *
     * @param mainWindow non-null
     * @return
     */
    public static List<JToolBar> createToolbars(MainWindow mainWindow) {
        return getToolbarExtensions().stream()
                .map(it -> it.getToolbar(mainWindow))
                .collect(Collectors.toList());
    }
    //endregion


    //region Term menu

    /**
     * Retrieves all known implementations of the {@link KeYMainMenuExtension}.
     *
     * @return all known implementations of the {@link KeYMainMenuExtension}.
     */
    public static List<KeYTermMenuExtension> getTermMenuExtensions() {
        return getExtension(KeYTermMenuExtension.class);
    }

    public static List<Action> getTermMenuActions(MainWindow window, PosInSequent pos) {
        return getTermMenuExtensions().stream()
                .flatMap(it -> it.getTermMenuActions(window, pos).stream())
                .collect(Collectors.toList());
    }

    public static JMenu createTermMenu(MainWindow window, PosInSequent pos) {
        JMenu menu = new JMenu("Extensions");
        getTermMenuActions(window, pos).forEach(it -> sortActionIntoMenu(it, menu));
        return menu;
    }
    //endregion

    //region Term info

    /**
     * Retrieves all known implementations of the {@link KeYTermInfoExtension}.
     *
     * @return all known implementations of the {@link KeYTermInfoExtension}.
     */
    public static List<KeYTermInfoExtension> getTermInfoExtensions() {
        return getExtension(
                KeYTermInfoExtension.class,
                Comparator.comparingInt(KeYTermInfoExtension::getPriority));
    }

    /**
     *
     * @param window the main window.
     * @param pos the position the user selected.
     * @return every term info string from every loaded extension.
     */
    public static List<String> getTermInfoStrings(MainWindow window, PosInSequent pos) {
        return getTermInfoExtensions().stream().flatMap(
                it -> it.getTermInfoStrings(window, pos).stream()).collect(Collectors.toList());
    }
    //endregion

    /**
     * Retrieves extensions via {@link ServiceLoader}.
     *
     * @param c   the interface class
     * @param <T> the interface of the service
     * @return a list of all found service implementations
     */

    private static Map<Class<?>, List<Object>> extensionCache = new HashMap<>();
    @SuppressWarnings("unchecked")
    private static <T> List<T> getExtension(Class<T> c) {
        return (List<T>) extensionCache.computeIfAbsent(c, (k) -> {
            Spliterator<T> iter = ServiceLoader.load(c).spliterator();
            return StreamSupport.stream(iter, false)
                    .collect(Collectors.toList());
        });
    }


    /**
     * Retrieves extensions via {@link ServiceLoader}, includes a sorting via <code>comp</code>.
     *
     * @param c    the interface class
     * @param comp a comporator for sorting
     * @param <T>  the interface of the service
     * @return a list of all found service implementations
     */
    private static <T> List<T> getExtension(Class<T> c, Comparator<? super T> comp) {
        List<T> seq = getExtension(c);
        seq.sort(comp);
        return seq;
    }

}
