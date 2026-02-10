package com.hirohiro716.desktop.task;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gnome.gdk.Event;
import org.gnome.gdk.EventButton;
import org.gnome.gdk.EventFocus;
import org.gnome.gdk.MouseButton;
import org.gnome.gdk.Pixbuf;
import org.gnome.gdk.RGBA;
import org.gnome.glib.Handler;
import org.gnome.gtk.Box;
import org.gnome.gtk.Button;
import org.gnome.gtk.ButtonsType;
import org.gnome.gtk.CheckButton;
import org.gnome.gtk.Dialog;
import org.gnome.gtk.FileChooserAction;
import org.gnome.gtk.FileChooserDialog;
import org.gnome.gtk.Gtk;
import org.gnome.gtk.IconSize;
import org.gnome.gtk.Image;
import org.gnome.gtk.Label;
import org.gnome.gtk.Menu;
import org.gnome.gtk.MenuItem;
import org.gnome.gtk.MessageDialog;
import org.gnome.gtk.MessageType;
import org.gnome.gtk.Orientation;
import org.gnome.gtk.Paned;
import org.gnome.gtk.ResponseType;
import org.gnome.gtk.ScrolledWindow;
import org.gnome.gtk.StateFlags;
import org.gnome.gtk.Stock;
import org.gnome.gtk.TextBuffer;
import org.gnome.gtk.TextView;
import org.gnome.gtk.ToggleButton;
import org.gnome.gtk.Widget;
import org.gnome.gtk.Window;
import org.gnome.gtk.WrapMode;
import org.gnome.pango.EllipsizeMode;

import com.hirohiro716.desktop.task.config.Config;
import com.hirohiro716.desktop.task.config.ConfigProperty;
import com.hirohiro716.scent.ExceptionMessenger;
import com.hirohiro716.scent.RoundNumber;
import com.hirohiro716.scent.StringObject;
import com.hirohiro716.scent.datetime.Datetime;
import com.hirohiro716.scent.filesystem.Directory;
import com.hirohiro716.scent.filesystem.File;
import com.hirohiro716.scent.io.json.ParseException;

/**
 * タスク一覧を表示するアプリケーションのクラス。
 * 
 * apt install libjava-gnome-java
 * GTK_CSD=1 java -jar tasker.jar
 */
public class Tasker {

    private static Window window;

    private static Config config;

    private static Box mainBox;

    private static Box buttonsBox;

    private static ScrolledWindow tasksScrolledWindow;

    private static Box tasksBox;

    private static Button addButton;

    private static Button sortButton;

    private static Map<Widget, String> widgetAndData = new HashMap<>();

    private static Map<Widget, Label> widgetAndNumberOfItemsLabel = new HashMap<>();

    private static Box unsavedBox = null;

    private static TextView unsavedTextView = null;

    private static ScrolledWindow sortScrolledWindow;

    private static Box sortBox;

    private static Button sortEndButton;

    private static boolean windowIsClosed = false;

    /**
     * 可能であれば設定ファイルに保存する。
     */
    private static void saveToConfigFileIfPossible() {
        try {
            Tasker.config.saveToFile();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * タスク表示時間。
     */
    private static final int DISPLAY_PERIOD_HOURS = 12;

    /**
     * 指定されたタスクの行を作成する。
     * 
     * @param task
     * @param focus フォーカスする場合はtrueを指定。
     * @param editable 編集可能にする場合はtrueを指定。
     * @return
     */
    private static Box createTaskBox(Task task, boolean focus, boolean editable) {
        Box box = new Box(Orientation.HORIZONTAL, 0);
        Tasker.widgetAndData.put(box, task.toString());
        // Complete
        CheckButton checkButton = new CheckButton();
        if (task.getCompletedTime() != null) {
            checkButton.setActive(true);
        }
        checkButton.connect(new ToggleButton.Toggled() {

            @Override
            public void onToggled(ToggleButton toggleButton) {
                try {
                    Task task = new Task(Tasker.widgetAndData.get(box), Tasker.config);
                    if (checkButton.getActive()) {
                        task.put(TaskProperty.COMPLETED_TIME, Datetime.newInstance().getAllMilliSecond());
                    } else {
                        task.put(TaskProperty.COMPLETED_TIME, (Object) null);
                    }
                    Tasker.config.setTask(task);
                    Tasker.widgetAndData.put(box, task.toString());
                    Tasker.saveToConfigFileIfPossible();
                } catch (ParseException exception) {
                    exception.printStackTrace();
                }
            }
        });
        checkButton.setTooltipText(StringObject.join("チェックされたタスクは", Tasker.DISPLAY_PERIOD_HOURS, "時間後に非表示になり、", Config.RETENTION_PERIOD_DAYS, "日後に削除されます。").toString());
        box.packStart(checkButton, false, false, 10);
        // Description
        TextView descriptionTextView = new TextView();
        descriptionTextView.getBuffer().setText(task.getDescription());
        descriptionTextView.setMarginLeft(2);
        descriptionTextView.setMarginRight(2);
        descriptionTextView.setWrapMode(WrapMode.WORD_CHAR);
        descriptionTextView.setAcceptsTab(false);
        descriptionTextView.setEditable(editable);
        RGBA readonlyColor = new RGBA(0, 0, 0, 0.5);
        if (editable == false) {
            descriptionTextView.overrideColor(StateFlags.NORMAL, readonlyColor);
        }
        descriptionTextView.connect(new Widget.FocusOutEvent() {

            @Override
            public boolean onFocusOutEvent(Widget widget, EventFocus eventFocus) {
                try {
                    Task task = new Task(Tasker.widgetAndData.get(box), Tasker.config);
                    task.put(TaskProperty.DESCRIPTION, descriptionTextView.getBuffer().getText());
                    Tasker.config.setTask(task);
                    Tasker.widgetAndData.put(box, task.toString());
                    Tasker.saveToConfigFileIfPossible();
                    Tasker.unsavedBox = null;
                    Tasker.unsavedTextView = null;
                } catch (ParseException exception) {
                    exception.printStackTrace();
                }
                return false;
            }
        });
        descriptionTextView.getBuffer().connect(new TextBuffer.Changed() {

            @Override
            public void onChanged(TextBuffer textBuffer) {
                Tasker.unsavedBox = box;
                Tasker.unsavedTextView = descriptionTextView;
            }
        });
        if (focus) {
            Gtk.idleAdd(new Handler() {

                @Override
                public boolean run() {
                    descriptionTextView.grabFocus();
                    return false;
                }
            });
        }
        ScrolledWindow scrolledWindow = new ScrolledWindow();
        scrolledWindow.setSizeRequest(-1, 50);
        scrolledWindow.add(descriptionTextView);
        box.packStart(scrolledWindow, true, true, 0);
        descriptionTextView.connect(new Widget.FocusInEvent() {

            @Override
            public boolean onFocusInEvent(Widget widget, EventFocus eventFocus) {
                int numberOfLines = 0;
                for (String line: StringObject.newInstance(descriptionTextView.getBuffer().getText()).split("\n")) {
                    numberOfLines += RoundNumber.FLOOR.calculate(line.length() / 20);
                    numberOfLines++;
                }
                if (numberOfLines >= 3) {
                    scrolledWindow.setSizeRequest(-1, 100);
                }
                return false;
            }
        });
        descriptionTextView.connect(new Widget.FocusOutEvent() {

            @Override
            public boolean onFocusOutEvent(Widget widget, EventFocus eventFocus) {
                scrolledWindow.setSizeRequest(-1, 50);
                return false;
            }
        });
        // Buttons box
        Box buttonsBox = new Box(Orientation.HORIZONTAL, 5);
        box.packEnd(buttonsBox, false, false, 10);
        // Directory
        Button directoryButton = new Button();
        directoryButton.setTooltipText("右クリックで再選択することができます。");
        if (Directory.newInstance(task.getDirectory()).exists()) {
            directoryButton.setImage(new Image(Stock.DIRECTORY, IconSize.BUTTON));
        } else {
            directoryButton.setImage(new Image(Stock.ADD, IconSize.BUTTON));
        }
        Tasker.widgetAndData.put(directoryButton, task.getDirectory());
        directoryButton.overrideBackground(StateFlags.NORMAL, new RGBA(0, 0, 0, 0));
        directoryButton.overrideBackground(StateFlags.ACTIVE, new RGBA(0, 0, 0, 0.1));
        directoryButton.connect(new Widget.ButtonPressEvent() {

            @Override
            public boolean onButtonPressEvent(Widget widget, EventButton eventButton) {
                Directory directory = new Directory(Tasker.widgetAndData.get(widget));
                if (directory.exists() && eventButton.getButton() == MouseButton.LEFT) {
                    try {
                        Process process = Runtime.getRuntime().exec(new String[] {"xdg-open", directory.getAbsolutePath()});
                        process.waitFor();
                    } catch (Exception exception) {
                        MessageDialog messageDialog = new MessageDialog(Tasker.window, false, MessageType.ERROR, ButtonsType.OK, ExceptionMessenger.newInstance(exception).make());
                        messageDialog.showAll();
                    }
                }
                if (directory.exists() == false || eventButton.getButton() == MouseButton.RIGHT) {
                    FileChooserDialog fileChooserDialog = new FileChooserDialog("", Tasker.window, FileChooserAction.SELECT_FOLDER);
                    fileChooserDialog.showAll();
                    fileChooserDialog.connect(new Dialog.Response() {

                        @Override
                        public void onResponse(Dialog dialog, ResponseType responseType) {
                            if (responseType == ResponseType.OK) {
                                try {
                                    Task task = new Task(Tasker.widgetAndData.get(box), Tasker.config);
                                    task.put(TaskProperty.DIRECTORY, fileChooserDialog.getFilename());
                                    Tasker.config.setTask(task);
                                    Tasker.config.saveToFile();
                                    Tasker.updateTaskBoxesWithConfig(task, false);
                                    Tasker.tasksBox.showAll();
                                } catch (ParseException exception) {
                                    exception.printStackTrace();
                                } catch (IOException exception) {
                                    MessageDialog messageDialog = new MessageDialog(Tasker.window, false, MessageType.ERROR, ButtonsType.OK, ExceptionMessenger.newInstance(exception).make());
                                    messageDialog.showAll();
                                }
                            }
                            fileChooserDialog.hide();
                        }
                    });
                }
                return false;
            }
        });
        buttonsBox.add(directoryButton);
        // Number of items
        Label numberOfItemsLabel = new Label("-");
        numberOfItemsLabel.setSizeRequest(28, 28);
        numberOfItemsLabel.overrideColor(StateFlags.NORMAL, new RGBA(0, 0, 0, 0.5));
        buttonsBox.add(numberOfItemsLabel);
        Tasker.widgetAndNumberOfItemsLabel.put(box, numberOfItemsLabel);
        // Menu
        Button menuButton = new Button();
        menuButton.setImage(new Image(Stock.PREFERENCES, IconSize.BUTTON));
        menuButton.overrideBackground(StateFlags.NORMAL, new RGBA(0, 0, 0, 0));
        menuButton.overrideBackground(StateFlags.ACTIVE, new RGBA(0, 0, 0, 0.1));
        menuButton.connect(new Button.Clicked() {

            @Override
            public void onClicked(Button button) {
                Menu menu = new Menu();
                MenuItem descriptionReadonlySwitchMenuItem = new MenuItem("ロック切り替え");
                descriptionReadonlySwitchMenuItem.connect(new MenuItem.Activate() {

                    @Override
                    public void onActivate(MenuItem menuItem) {
                        boolean isReadonly = false;
                        try {
                            Task task = new Task(Tasker.widgetAndData.get(box), Tasker.config);
                            isReadonly = task.getDescriptionIsReadonly() == false;
                            task.put(TaskProperty.DESCRIPTION_IS_READONLY, isReadonly);
                            Tasker.config.setTask(task);
                            Tasker.widgetAndData.put(box, task.toString());
                            Tasker.saveToConfigFileIfPossible();
                        } catch (ParseException exception) {
                            exception.printStackTrace();
                        }
                        if (isReadonly) {
                            descriptionTextView.setEditable(false);
                            descriptionTextView.overrideColor(StateFlags.NORMAL, readonlyColor);
                        } else {
                            descriptionTextView.setEditable(true);
                            descriptionTextView.overrideColor(StateFlags.NORMAL, null);
                            descriptionTextView.grabFocus();
                        }
                    }
                });
                menu.append(descriptionReadonlySwitchMenuItem);
                MenuItem removeTaskMenuItem = new MenuItem("削除");
                removeTaskMenuItem.connect(new MenuItem.Activate() {

                    @Override
                    public void onActivate(MenuItem menuItem) {
                        try {
                            Task task = new Task(Tasker.widgetAndData.get(box), Tasker.config);
                            Tasker.config.removeTask(task.getID());
                            Tasker.tasksBox.remove(box);
                            Tasker.saveToConfigFileIfPossible();
                        } catch (ParseException exception) {
                            exception.printStackTrace();
                        }
                    }
                });
                menu.append(removeTaskMenuItem);
                menu.showAll();
                menu.popup();
            }
        });
        buttonsBox.add(menuButton);
        Label spacer = new Label();
        spacer.setSizeRequest(1, 1);
        spacer.overrideColor(StateFlags.NORMAL, new RGBA(0, 0, 0, 0));
        buttonsBox.add(spacer);
        return box;
    }

    /**
     * 設定でタスクの行を更新する。
     * 
     * @param focusTask フォーカスするタスク。
     * @param focusTaskEditable フォーカスするタスクが編集可能な場合はtrueを指定。
     */
    private static void updateTaskBoxesWithConfig(Task focusTask, boolean focusTaskEditable) {
        for (Widget widget : Tasker.tasksBox.getChildren()) {
            Tasker.tasksBox.remove(widget);
        }
        Datetime limit = new Datetime();
        limit.addDay(-1);
        Task[] tasks = Tasker.config.getTasks();
        for (int index = 0; index < tasks.length; index++) {
            Task task = tasks[index];
            Datetime completedTime = task.getCompletedTime();
            if (completedTime != null) {
                if (limit.getAllMilliSecond() > completedTime.getAllMilliSecond()) {
                    continue;
                }
            }
            boolean focus = focusTask != null && task.getID() == focusTask.getID();
            boolean editable = task.getDescriptionIsReadonly() == false || focus && focusTaskEditable;
            Box taskBox = Tasker.createTaskBox(task, focus, editable);
            if (index == 0) {
                Paned topSpacer = new Paned(Orientation.HORIZONTAL);
                Tasker.tasksBox.packStart(topSpacer, false, false, 0);
                Tasker.tasksBox.add(taskBox);
            } else if (index == tasks.length - 1) {
                Tasker.tasksBox.add(taskBox);
                Paned bottomSpacer = new Paned(Orientation.HORIZONTAL);
                Tasker.tasksBox.packStart(bottomSpacer, false, false, 0);
            } else {
                Tasker.tasksBox.add(taskBox);
            }
        }
    }

    /**
     * 指定されたタスクの並び替え項目を作成する。
     * 
     * @param task
     * @param focusToUp 上ボタンにフォーカスする場合はtrueを指定。
     * @param focusToDown 下ボタンにフォーカスする場合はtrueを指定。
     * @return
     */
    private static Box createSortItemBox(Task task, boolean focusToUp, boolean focusToDown) {
        Box box = new Box(Orientation.HORIZONTAL, 0);
        Tasker.widgetAndData.put(box, task.toString());
        // Description
        StringObject description = new StringObject(task.getDescription());
        description.replaceLF("｜");
        Label descriptionLabel = new Label(description.toString());
        descriptionLabel.setLineWrap(false);
        descriptionLabel.setEllipsize(EllipsizeMode.END);
        descriptionLabel.overrideColor(StateFlags.NORMAL, new RGBA(0, 0, 0, 0.5));
        box.packStart(descriptionLabel, false, false, 10);
        // Buttons box
        Box buttonsBox = new Box(Orientation.HORIZONTAL, 5);
        box.packEnd(buttonsBox, false, false, 10);
        // Up
        Button upButton = new KeyPressableButton();
        upButton.setImage(new Image(Stock.GO_UP, IconSize.BUTTON));
        upButton.overrideBackground(StateFlags.NORMAL, new RGBA(0, 0, 0, 0));
        upButton.overrideBackground(StateFlags.ACTIVE, new RGBA(0, 0, 0, 0.1));
        upButton.connect(new Button.Clicked() {

            @Override
            public void onClicked(Button button) {
                try {
                    Task task = new Task(Tasker.widgetAndData.get(box), Tasker.config);
                    Datetime limit = new Datetime();
                    limit.addDay(-1);
                    long lower = 0;
                    for (Task lowerTask : Tasker.config.getTasks()) {
                        if (lowerTask.getID() == task.getID()) {
                            break;
                        }
                        Datetime completedTime = lowerTask.getCompletedTime();
                        if (completedTime == null || limit.getAllMilliSecond() < completedTime.getAllMilliSecond()) {
                            lower = lowerTask.getSort();
                        }
                    }
                    task.put(TaskProperty.SORT, lower - 1);
                    Tasker.config.setTask(task);
                    Tasker.config.saveToFile();
                    Tasker.updateSortBoxWithConfig(task, null);
                    Tasker.sortBox.showAll();
                } catch (ParseException exception) {
                    exception.printStackTrace();
                } catch (IOException exception) {
                    MessageDialog messageDialog = new MessageDialog(Tasker.window, false, MessageType.ERROR, ButtonsType.OK, ExceptionMessenger.newInstance(exception).make());
                    messageDialog.showAll();
                }
            }
        });
        if (focusToUp) {
            Gtk.idleAdd(new Handler() {

                @Override
                public boolean run() {
                    upButton.grabFocus();
                    return false;
                }
            });
        }
        buttonsBox.add(upButton);
        // Down
        Button downButton = new KeyPressableButton();
        downButton.setImage(new Image(Stock.GO_DOWN, IconSize.BUTTON));
        downButton.overrideBackground(StateFlags.NORMAL, new RGBA(0, 0, 0, 0));
        downButton.overrideBackground(StateFlags.ACTIVE, new RGBA(0, 0, 0, 0.1));
        downButton.connect(new Button.Clicked() {

            @Override
            public void onClicked(Button button) {
                try {
                    Task task = new Task(Tasker.widgetAndData.get(box), Tasker.config);
                    List<Task> tasks = new ArrayList<>(Arrays.asList(Tasker.config.getTasks()));
                    Collections.reverse(tasks);
                    Datetime limit = new Datetime();
                    limit.addDay(-1);
                    long higher = Integer.MAX_VALUE;
                    for (Task higherTask : tasks) {
                        if (higherTask.getID() == task.getID()) {
                            break;
                        }
                        Datetime completedTime = higherTask.getCompletedTime();
                        if (completedTime == null || limit.getAllMilliSecond() < completedTime.getAllMilliSecond()) {
                            higher = higherTask.getSort();
                        }
                    }
                    task.put(TaskProperty.SORT, higher + 1);
                    Tasker.config.setTask(task);
                    Tasker.config.saveToFile();
                    Tasker.updateSortBoxWithConfig(null, task);
                    Tasker.sortBox.showAll();
                } catch (ParseException exception) {
                    exception.printStackTrace();
                } catch (IOException exception) {
                    MessageDialog messageDialog = new MessageDialog(Tasker.window, false, MessageType.ERROR, ButtonsType.OK, ExceptionMessenger.newInstance(exception).make());
                    messageDialog.showAll();
                }
            }
        });
        if (focusToDown) {
            Gtk.idleAdd(new Handler() {

                @Override
                public boolean run() {
                    downButton.grabFocus();
                    return false;
                }
            });
        }
        buttonsBox.add(downButton);
        Label spacer = new Label();
        spacer.setSizeRequest(1, 1);
        spacer.overrideColor(StateFlags.NORMAL, new RGBA(0, 0, 0, 0));
        buttonsBox.add(spacer);
        return box;
    }

    /**
     * 設定で並び替えボックスを更新する。
     * 
     * @param taskForFocusToUp 上ボタンにフォーカスする場合はタスクを指定。
     * @param taskForFocusToDown 下ボタンにフォーカスする場合はタスクを指定。
     */
    private static void updateSortBoxWithConfig(Task taskForFocusToUp, Task taskForFocusToDown) {
        for (Widget widget : Tasker.sortBox.getChildren()) {
            Tasker.sortBox.remove(widget);
        }
        Datetime limit = new Datetime();
        limit.addDay(-1);
        Task[] tasks = Tasker.config.getTasks();
        for (int index = 0; index < tasks.length; index++) {
            Task task = tasks[index];
            Datetime completedTime = task.getCompletedTime();
            if (completedTime != null) {
                if (limit.getAllMilliSecond() > completedTime.getAllMilliSecond()) {
                    continue;
                }
            }
            boolean focusToUp = taskForFocusToUp != null && task.getID() == taskForFocusToUp.getID();
            boolean focusToDown = taskForFocusToDown != null && task.getID() == taskForFocusToDown.getID();
            Box sortItemBox = Tasker.createSortItemBox(task, focusToUp, focusToDown);
            if (index == 0) {
                Paned topSpacer = new Paned(Orientation.HORIZONTAL);
                Tasker.sortBox.packStart(topSpacer, false, false, 0);
                Tasker.sortBox.add(sortItemBox);
            } else if (index == tasks.length - 1) {
                Tasker.sortBox.add(sortItemBox);
                Paned bottomSpacer = new Paned(Orientation.HORIZONTAL);
                Tasker.sortBox.packStart(bottomSpacer, false, false, 0);
            } else {
                Tasker.sortBox.add(sortItemBox);
            }
        }
    }

    /**
     * アプリケーションを開始する。
     * 
     * @param args
     */
    public static void main(String[] args) throws Exception {
        Gtk.init(args);
        Gtk.setProgramName(Tasker.class.getName());
        // Config
        try {
            Directory configDirectory = new Directory(Gtk.getUserConfigDir());
            configDirectory = new Directory(configDirectory, "hirohiro716");
            configDirectory = new Directory(configDirectory, "tasker");
            if (configDirectory.exists() == false) {
                configDirectory.create();
            }
            File configFile = new File(configDirectory, "config.json");
            if (configFile.exists() == false) {
                configFile.create();
            }
            Tasker.config = new Config(configFile);
        } catch (Exception exception) {
            exception.printStackTrace();
            return;
        }
        // Window
        Tasker.window = new Window();
        StringObject title = new StringObject("タスク - ");
        title.append(System.getProperty("user.name"));
        title.append("@");
        title.append(InetAddress.getLocalHost().getHostName());
        Tasker.window.setTitle(title.toString());
        Tasker.window.setIcon(new Pixbuf(Tasker.class.getResourceAsStream("media/icon.svg").readAllBytes()));
        Tasker.window.setDefaultSize(430, 550);
        Tasker.window.setResizable(false);
        Tasker.window.setStick(true);
        Tasker.window.move(config.getWindowLocationX(), config.getWindowLocationY());
        window.connect(new Window.DeleteEvent() {

            @Override
            public boolean onDeleteEvent(Widget widget, Event event) {
                try {
                    if (Tasker.unsavedBox != null && Tasker.unsavedTextView != null) {
                        Task task = new Task(Tasker.widgetAndData.get(Tasker.unsavedBox), Tasker.config);
                        task.put(TaskProperty.DESCRIPTION, Tasker.unsavedTextView.getBuffer().getText());
                        Tasker.config.setTask(task);
                    }
                    Tasker.config.put(ConfigProperty.WINDOW_LOCATION_X, Tasker.window.getPositionX());
                    Tasker.config.put(ConfigProperty.WINDOW_LOCATION_Y, Tasker.window.getPositionY());
                    Tasker.config.saveToFile();
                } catch (Exception exception) {
                    MessageDialog messageDialog = new MessageDialog(Tasker.window, false, MessageType.ERROR, ButtonsType.OK, ExceptionMessenger.newInstance(exception).make());
                    messageDialog.showAll();
                    return true;
                }
                Gtk.mainQuit();
                Tasker.windowIsClosed = true;
                return false;
            }
        });
        // Tasks
        Tasker.mainBox = new Box(Orientation.VERTICAL, 0);
        Tasker.window.add(Tasker.mainBox);
        Tasker.tasksScrolledWindow = new ScrolledWindow();
        Tasker.mainBox.packStart(Tasker.tasksScrolledWindow, true, true, 0);
        Tasker.tasksBox = new Box(Orientation.VERTICAL, 20);
        Tasker.tasksScrolledWindow.addWithViewport(Tasker.tasksBox);
        Tasker.updateTaskBoxesWithConfig(null, false);
        // Sort
        Tasker.sortScrolledWindow = new ScrolledWindow();
        Tasker.sortBox = new Box(Orientation.VERTICAL, 10);
        Tasker.sortScrolledWindow.addWithViewport(Tasker.sortBox);
        Tasker.sortScrolledWindow.show();
        // Buttons
        Tasker.buttonsBox = new Box(Orientation.HORIZONTAL, 10);
        Tasker.mainBox.packEnd(Tasker.buttonsBox, false, false, 15);
        Label spacer = new Label();
        spacer.setSizeRequest(5, 1);
        spacer.overrideColor(StateFlags.NORMAL, new RGBA(0, 0, 0, 0));
        Tasker.buttonsBox.packEnd(spacer, false, false, 0);
        Tasker.addButton = new Button("追加");
        Tasker.addButton.connect(new Button.Clicked() {

            @Override
            public void onClicked(Button button) {
                try {
                    Task task = new Task(Tasker.config);
                    Tasker.config.setTask(task);
                    Tasker.config.saveToFile();
                    Tasker.updateTaskBoxesWithConfig(task, true);
                    Tasker.tasksBox.showAll();
                } catch (ParseException exception) {
                    exception.printStackTrace();
                } catch (IOException exception) {
                    MessageDialog messageDialog = new MessageDialog(Tasker.window, false, MessageType.ERROR, ButtonsType.OK, ExceptionMessenger.newInstance(exception).make());
                    messageDialog.showAll();
                }
            }
        });
        Tasker.buttonsBox.packEnd(Tasker.addButton, false, false, 0);
        Tasker.sortButton = new Button("並び替え");
        Tasker.sortButton.connect(new Button.Clicked() {

            @Override
            public void onClicked(Button button) {
                Tasker.mainBox.remove(Tasker.tasksScrolledWindow);
                Tasker.mainBox.packStart(Tasker.sortScrolledWindow, true, true, 0);
                Tasker.buttonsBox.remove(Tasker.addButton);
                Tasker.buttonsBox.remove(Tasker.sortButton);
                Tasker.buttonsBox.packEnd(Tasker.sortEndButton, false, false, 0);
                Tasker.updateSortBoxWithConfig(null, null);
                Tasker.sortBox.showAll();
            }
        });
        Tasker.buttonsBox.packEnd(Tasker.sortButton, false, false, 0);
        Tasker.sortEndButton = new Button("戻る");
        Tasker.sortEndButton.connect(new Button.Clicked() {

            @Override
            public void onClicked(Button button) {
                Tasker.mainBox.remove(Tasker.sortScrolledWindow);
                Tasker.mainBox.packStart(Tasker.tasksScrolledWindow, true, true, 0);
                Tasker.buttonsBox.remove(Tasker.sortEndButton);
                Tasker.buttonsBox.packEnd(Tasker.addButton, false, false, 0);
                Tasker.buttonsBox.packEnd(Tasker.sortButton, false, false, 0);
                Tasker.updateTaskBoxesWithConfig(null, false);
                Tasker.tasksBox.showAll();
            }
        });
        Tasker.sortEndButton.show();
        window.showAll();
        // Number of items updater
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    if (Tasker.windowIsClosed) {
                        return;
                    }
                    try {
                        for (Widget box : Tasker.widgetAndData.keySet()) {
                            try {
                                Task task = new Task(Tasker.widgetAndData.get(box), Tasker.config);
                                Directory directory = new Directory(task.getDirectory());
                                Label label = Tasker.widgetAndNumberOfItemsLabel.get(box);
                                if (label != null) {
                                    StringObject text = new StringObject("-");
                                    if (directory.exists()) {
                                        int numberOfItems = directory.getFilesystemItems(".{1,}").length;
                                        if (numberOfItems > 0) {
                                            text.set(numberOfItems);
                                        }
                                    }
                                    Gtk.idleAdd(new Handler() {

                                        @Override
                                        public boolean run() {
                                            label.setLabel(text.toString());
                                            return false;
                                        }
                                    });
                                }
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }
                        Thread.sleep(2000);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        Gtk.main();
    }
}
