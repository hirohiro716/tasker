package com.hirohiro716.desktop.task.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.hirohiro716.desktop.task.Task;
import com.hirohiro716.desktop.task.TaskProperty;
import com.hirohiro716.scent.StringObject;
import com.hirohiro716.scent.datetime.Datetime;
import com.hirohiro716.scent.filesystem.File;
import com.hirohiro716.scent.io.ByteArray;
import com.hirohiro716.scent.io.json.JSONArray;
import com.hirohiro716.scent.io.json.JSONObject;
import com.hirohiro716.scent.io.json.JSONValue;
import com.hirohiro716.scent.io.json.ParseException;

/**
 * 設定ファイルのクラス。
 */
public class Config extends JSONObject {

    /**
     * コンストラクタ。JSON定義文を指定する。
     * 
     * @param json
     * @throws ParseException
     */
    public Config(File file) throws IOException, ParseException {
        StringObject json = new StringObject();
        file.read(new File.ProcessAfterReadingLine() {

            @Override
            public void call(String line, BufferedReader bufferedReader) throws IOException {
                json.append(line);
            }
        });
        this.file = file;
        JSONObject jsonObject = new JSONObject(json.toString());
        this.setContent(jsonObject.getContent());
    }

    private File file;

    /**
     * ウィンドウの横方向位置を取得する。
     * 
     * @return
     */
    public int getWindowLocationX() {
        StringObject value = new StringObject();
        if (this.isNull(ConfigProperty.WINDOW_LOCATION_X) == false) {
            value.append(this.get(ConfigProperty.WINDOW_LOCATION_X));
        } else {
            value.append(ConfigProperty.WINDOW_LOCATION_X.getDefaultValue());
        }
        return value.toInteger();
    }
    
    /**
     * ウィンドウの縦方向位置を取得する。
     * 
     * @return
     */
    public int getWindowLocationY() {
        StringObject value = new StringObject();
        if (this.isNull(ConfigProperty.WINDOW_LOCATION_Y) == false) {
            value.append(this.get(ConfigProperty.WINDOW_LOCATION_Y));
        } else {
            value.append(ConfigProperty.WINDOW_LOCATION_Y.getDefaultValue());
        }
        return value.toInteger();
    }

    /**
     * タスク保持日数。
     */
    public static final int RETENTION_PERIOD_DAYS = 10;

    /**
     * タスクを取得する。
     * 
     * @return
     */
    public Task[] getTasks() {
        List<Task> tasks = new ArrayList<>();
        try {
            StringObject value = new StringObject();
            if (this.isNull(ConfigProperty.TASKS) == false) {
                value.append(this.get(ConfigProperty.TASKS));
            } else {
                value.append(ConfigProperty.TASKS.getDefaultValue());
            }
            Datetime limit = new Datetime();
            limit.addDay(Config.RETENTION_PERIOD_DAYS * -1);
            JSONArray jsonArray = new JSONArray(value.toString());
            for (JSONValue<?> jsonValue : jsonArray.getContent()) {
                Task task = new Task(jsonValue.toString(), this);
                Datetime completedTime = task.getCompletedTime();
                if (completedTime != null) {
                    if (limit.getAllMilliSecond() > completedTime.getAllMilliSecond()) {
                        this.removeTask(task.getID());
                        continue;
                    }
                }
                tasks.add(task);
            }
        } catch (ParseException exception) {
            exception.printStackTrace();
        }
        return tasks.toArray(new Task[] {});
    }

    /**
     * 指定されたIDに該当するタスクが存在する場合はtrueを返す。
     * 
     * @param id
     * @return
     */
    public boolean existsTask(Object id) {
        StringObject stringID = new StringObject(id);
        if (stringID.toLong() != null) {
            for (Task task : this.getTasks()) {
                if (task.isNull(TaskProperty.ID) == false && stringID.equals(task.get(TaskProperty.ID).getContent())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 指定されたIDに該当するタスクを削除する。
     * 
     * @param id
     */
    public void removeTask(Object id) {
        try {
            StringObject value = new StringObject();
            if (this.isNull(ConfigProperty.TASKS) == false) {
                value.append(this.get(ConfigProperty.TASKS));
            } else {
                value.append(ConfigProperty.TASKS.getDefaultValue());
            }
            JSONArray jsonArrayForUpdate = new JSONArray();
            JSONArray jsonArray = new JSONArray(value.toString());
            for (JSONValue<?> jsonValue : jsonArray.getContent()) {
                JSONObject task = (JSONObject) jsonValue;
                if (StringObject.newInstance(id).equals(task.get(TaskProperty.ID).getContent()) == false) {
                    jsonArrayForUpdate.add(task);
                }
            }
            this.put(ConfigProperty.TASKS, jsonArrayForUpdate);
        } catch (ParseException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * 指定されたタスクを設定にセットする。
     * 
     * @param task
     */
    public void setTask(Task task) {
        try {
            boolean isUpdate = false;
            StringObject value = new StringObject();
            if (this.isNull(ConfigProperty.TASKS) == false) {
                value.append(this.get(ConfigProperty.TASKS));
            } else {
                value.append(ConfigProperty.TASKS.getDefaultValue());
            }
            List<Task> taskListForUpdate = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(value.toString());
            for (JSONValue<?> jsonValue : jsonArray.getContent()) {
                Task nativeTask = new Task(jsonValue.toString(), this);
                if (nativeTask.getID() == task.getID()) {
                    taskListForUpdate.add(task);
                    isUpdate = true;
                } else {
                    taskListForUpdate.add(nativeTask);
                }
            }
            if (isUpdate == false) {
                taskListForUpdate.add(task);
            }
            taskListForUpdate.sort(new Comparator<Task>() {

                @Override
                public int compare(Task o1, Task o2) {
                    long sortDiff = o1.getSort() - o2.getSort();
                    if (sortDiff != 0) {
                        return (int) sortDiff;
                    }
                    long idDiff = o1.getID() - o2.getID();
                    if (idDiff != 0) {
                        return (int) idDiff;
                    }
                    return 0;
                }
            });
            JSONArray jsonArrayForUpdate = new JSONArray();
            for (int index = 0; index < taskListForUpdate.size(); index++) {
                Task taskForUpdate = taskListForUpdate.get(index);
                taskForUpdate.put(TaskProperty.SORT, index * 2);
                jsonArrayForUpdate.add(taskForUpdate);
            }
            this.put(ConfigProperty.TASKS, jsonArrayForUpdate);
        } catch (ParseException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * 現在の設定をコンストラクタで指定されたファイルに上書き保存する。
     * 
     * @param file
     * @throws IOException
     */
    public void saveToFile() throws IOException {
        ByteArray byteArray = new ByteArray(this.toString().getBytes());
        byteArray.saveToFile(this.file);
    }
}
